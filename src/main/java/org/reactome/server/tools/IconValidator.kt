package org.reactome.server.tools

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.martiansoftware.jsap.JSAPResult
import kotlinx.coroutines.*
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.tuple.Pair
import org.reactome.server.tools.model.Icon
import org.reactome.server.tools.model.Reference
import org.reactome.server.tools.model.query.Resolver
import org.reactome.server.tools.model.query.Resolver.Payload.ResolvedResource
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandler
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.Pattern
import java.util.stream.Collectors
import javax.xml.bind.JAXBContext
import javax.xml.bind.JAXBException
import kotlin.coroutines.CoroutineContext

@Suppress("unused")
class IconValidator(config: JSAPResult) : Checker, CoroutineScope {

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    private val uniprotReferences: MutableMap<String, MutableList<Pair<Icon, String>>> = ConcurrentHashMap()
    private val bodyHandler: BodyHandler<*>
    private val CATEGORIES: List<String>
    private val REFERENCES: List<String>
    private val iconNameCheck = config.getBoolean("iconNameCheck")

    private val checkReferences = config.getBoolean("checkReferences")

    private val directory: String = config.getString("directory")
    private val error = AtomicInteger(0)
    private val countRef = AtomicInteger(0)
    private var xmlNum = 0

    override fun getFailedChecks() = run { error.get() }

    override fun getTotalChecks() = run { xmlNum }

    override fun process() {
        val filesInDir = File(directory)

        val xmlFiles = filesInDir.listFiles { dir: File?, name: String -> name.endsWith(".xml") }

        if (xmlFiles != null) {
            xmlNum = xmlFiles.size
            Arrays.stream(xmlFiles)
                .parallel()
                .forEach { file: File ->
                    val icon = convertXmlToObj(file)
                    if (icon != null) {
                        validateXmlObj(file, icon)
                    }
                }
            processBatchChecking()
        }
    }

    private fun convertXmlToObj(file: File): Icon? {
        val jaxbContext: JAXBContext
        try {
            jaxbContext = JAXBContext.newInstance(Icon::class.java)
            val jaxbUnmarshaller = jaxbContext.createUnmarshaller()
            return jaxbUnmarshaller.unmarshal(file) as Icon
        } catch (e: JAXBException) {
            errorLogger.error(e.cause?.message + " File: " + file.name)
            error.incrementAndGet()
        }
        return null
    }

    private fun validateXmlObj(xmlFile: File, icon: Icon) {
        println("${countRef.incrementAndGet()} / $xmlNum xml files analysed")
        for (category in icon.categories) {
            if (!CATEGORIES.contains(category.lowercase())) {
                errorLogger.error("[$category] at the element \"category\" is not in the list CATEGORIES in the ${xmlFile.name}.")
                error.incrementAndGet()
            }
        }

        val person = icon.person
        if (person == null) {
            errorLogger.error("Element \"person\" is not found in ${xmlFile.name}.")
            error.incrementAndGet()
        }

        val references = icon.references
        if (references != null) {
            for (reference in references) {
                if (!REFERENCES.contains(reference.db)) {
                    errorLogger.error("[" + reference.db + "] at element \"reference\" is not in the list REFERENCE in " + xmlFile.name + ".")
                    error.incrementAndGet()
                } else if (checkReferences && !checkReference(reference, icon, xmlFile.name, 0)) {
                    error.incrementAndGet()
                }
            }
        } else {
            logger.debug("Element \"reference\" was not found in " + xmlFile.name + ".")
        }

        val synonyms = icon.synonyms
        if (synonyms != null) {
            for (synonym in synonyms) {
                if (synonym == "") {
                    errorLogger.error("Element \"synonym\" is missing value in " + xmlFile.name + ".")
                    error.incrementAndGet()
                }
            }
        }
    }

    private var prevQueryTime: Instant = Instant.now()

    init {
        this.bodyHandler =
            if (iconNameCheck) HttpResponse.BodyHandlers.ofString() else HttpResponse.BodyHandlers.discarding()
        this.CATEGORIES = readFile(config.getString("categoriesfile"))
        this.REFERENCES = readFile(config.getString("referencesfile"))
    }

    private fun checkReference(reference: Reference, icon: Icon, iconId: String, recursionLevel: Int): Boolean {
        if (reference.db == "UNIPROT") {
            uniprotReferences.computeIfAbsent(reference.id) { ArrayList() }.add(Pair.of(icon, iconId))
            return true
        }

        val isFinalTry = recursionLevel > 2
        val errorMessage: String
        var request: HttpRequest?
        var response: HttpResponse<*>
        val urls = dbToUrlBuilders.getOrDefault(reference.db, dbToUrlBuilders["DEFAULT"]!!)(reference)

        for (url in urls) {
            try {
                if (Duration.between(prevQueryTime, Instant.now()).toMillis() < QUERY_INTERVAL) {
                    Thread.sleep(QUERY_INTERVAL.toLong())
                }

                prevQueryTime = Instant.now()
                request = HttpRequest.newBuilder(URI.create(url)).build()
                response = httpClient.send(request, bodyHandler)
                if (response.statusCode() / 100 == 2) {
                    if (iconNameCheck && !StringUtils.containsIgnoreCase(
                            (response as HttpResponse<String?>).body(),
                            icon.name
                        )
                    ) {
                        errorLogger.warn(
                            "{} : {} can be found at {}, but the result do not contains {}",
                            iconId, reference, response.uri(), icon.name
                        )
                    }
                    return true
                }
            } catch (e: IOException) {
                errorLogger.warn(
                    String.format(
                        "Checking %s : %s threw %s while testing following url : %s",
                        iconId,
                        reference,
                        e.message,
                        url
                    )
                )
                e.printStackTrace()
            } catch (e: InterruptedException) {
                errorLogger.warn(
                    String.format(
                        "Checking %s : %s threw %s while testing following url : %s",
                        iconId,
                        reference,
                        e.message,
                        url
                    )
                )
                e.printStackTrace()
            }
        }
        errorMessage =
            "$iconId : $reference cannot be found at the following urls $urls. This might be due to a non supported database. Please contact eragueneau@ebi.ac.uk in such case"


        if (!isFinalTry) {
            try {
                Thread.sleep(1000)
            } catch (ignored: InterruptedException) {
            }
            println("$iconId : $reference check failed on its attempt #${recursionLevel + 1}")
            return checkReference(reference, icon, iconId, recursionLevel + 1)
        } else {
            errorLogger.error(errorMessage)
            return false
        }
    }

    private fun processBatchChecking() {
        val correctAccessions = Utils.queryUniprot(uniprotReferences.keys)
        for (queriedAccession in uniprotReferences.keys) {
            if (correctAccessions.contains(queriedAccession)) continue
            for (iconAndName in uniprotReferences[queriedAccession]!!) {
                error.incrementAndGet()
                errorLogger.error(
                    "{} : {} doesn't seem to exist",
                    iconAndName.right, Reference("UNIPROT", queriedAccession)
                )
            }
        }
    }


    private fun readFile(fileName: String): List<String> {
        val result: List<String>
        try {
            Files.lines(Paths.get(fileName)).use { lines ->
                result = lines.collect(Collectors.toList())
            }
        } catch (e: IOException) {
            error.incrementAndGet()
            throw RuntimeException("Cannot read file: $fileName")
        }
        return result
    }


    companion object {
        private const val QUERY_INTERVAL = 100
        private val logger: Logger = LoggerFactory.getLogger("logger")
        private val errorLogger: Logger = LoggerFactory.getLogger("errorLogger")

        val mapper: ObjectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())

        val httpClient: HttpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.of(10, ChronoUnit.SECONDS))
            .build()

        private val prefixPattern: Pattern = Pattern.compile("^[A-Za-z.]+:")

        private val identifiersPrefixes: Map<String, List<String>> = mapOf(
            "ENA" to listOf("ena.embl"),
            "PUBCHEM" to listOf("pubchem.compound"),
            "PUBCHEM-BIOASSAY" to listOf("pubchem.bioassay"),
            "PUBCHEM-SUBSTANCE" to listOf("pubchem.substance"),
            "IUPHAR" to listOf("iuphar.family", "iuphar.ligand", "iuphar.receptor"),
            "NCBI" to listOf("ncbigene", "ncbiprotein")
        )

        private val idBuilders: Map<String, (String) -> String> = mapOf(
            "ncbiprotein" to { it.replace(':', '_') }
        )

        private val dbToUrlBuilders: Map<String, (Reference) -> List<String>> = mapOf(
            "UNIPROT-T" to { listOf("https://www.uniprot.org/taxonomy/${it.id}") },
            "KEGG" to { listOf("http://rest.kegg.jp/get/${it.id}") },
            "MESH" to { listOf("https://www.ncbi.nlm.nih.gov/mesh/?term=${it.id}") },
            "OPL" to {
                listOf(
                    "https://www.ebi.ac.uk/ols/api/ontologies/opl/terms?iri=http://purl.obolibrary.org/obo/${
                        it.id.replace(
                            ':',
                            '_'
                        )
                    }"
                )
            },
            "DEFAULT" to { reference ->
                identifiersPrefixes
                    .getOrDefault(reference.db, listOf(reference.db))
                    .map { prefix: String ->
                        "https://resolver.api.identifiers.org/$prefix:" +
                                idBuilders
                                    .getOrDefault(prefix) { prefixPattern.matcher(it).replaceFirst("") }(reference.id)
                    }
                    .flatMap { resolverURL: String? ->
                        if (resolverURL == null) return@flatMap emptyList()

                        val request = HttpRequest.newBuilder(URI.create(resolverURL)).build()
                        try {
                            val response: String = httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body()
                            val resolver: Resolver = mapper.readValue(response)
                            return@flatMap resolver.payload.resolvedResources.map { it.compactIdentifierResolvedUrl }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            return@flatMap emptyList()
                        }
                    }
            }
        )
    }
}
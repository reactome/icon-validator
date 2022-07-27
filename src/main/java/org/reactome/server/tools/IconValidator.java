package org.reactome.server.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.martiansoftware.jsap.JSAPResult;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.reactome.server.tools.model.Icon;
import org.reactome.server.tools.model.Person;
import org.reactome.server.tools.model.Reference;
import org.reactome.server.tools.model.query.Resolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@SuppressWarnings("unused")
public class IconValidator implements Checker {
    private static final int QUERY_INTERVAL = 100;
    private static final Logger logger = LoggerFactory.getLogger("logger");
    private static final Logger errorLogger = LoggerFactory.getLogger("errorLogger");
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.of(10, ChronoUnit.SECONDS))
            .build();
    private final Map<String, List<Pair<Icon, String>>> uniprotReferences = new ConcurrentHashMap<>();
    private final HttpResponse.BodyHandler<?> bodyHandler;
    private final List<String> CATEGORIES;
    private final List<String> REFERENCES;
    private final boolean iconNameCheck;
    private final boolean checkReferences;
    private final String directory;
    private final AtomicInteger error = new AtomicInteger(0);
    private final AtomicInteger countRef = new AtomicInteger(0);
    private int xmlNum = 0;

    public IconValidator(JSAPResult config) {
        this.directory = config.getString("directory");
        this.checkReferences = config.getBoolean("checkReferences");
        this.iconNameCheck = config.getBoolean("iconNameCheck");
        this.bodyHandler = iconNameCheck ? HttpResponse.BodyHandlers.ofString() : HttpResponse.BodyHandlers.discarding();
        this.CATEGORIES = readFile(config.getString("categoriesfile"));
        this.REFERENCES = readFile(config.getString("referencesfile"));
    }

    @Override
    public void process() {

        File filesInDir = new File(directory);

        File[] xmlFiles = filesInDir.listFiles((dir, name) -> name.endsWith(".xml"));

        if (xmlFiles != null) {
            xmlNum = xmlFiles.length;
            Arrays.stream(xmlFiles)
                    .parallel()
                    .forEach(file -> {
                        Icon icon = convertXmlToObj(file);
                        if (icon != null) {
                            validateXmlObj(file, icon);
                        }
                    });
            processBatchChecking();
        }
    }

    private Icon convertXmlToObj(File file) {
        JAXBContext jaxbContext;
        try {
            jaxbContext = JAXBContext.newInstance(Icon.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            return (Icon) jaxbUnmarshaller.unmarshal(file);
        } catch (JAXBException e) {
            errorLogger.error(e.getCause().getMessage() + " File: " + file.getName());
            error.incrementAndGet();
        }
        return null;
    }

    private void validateXmlObj(File xmlFile, Icon icon) {
        System.out.println(countRef.incrementAndGet() + " / " + xmlNum + " xml files analysed");
        List<String> categories = icon.getCategories();
        for (String category : categories) {
            if (!CATEGORIES.contains(category.toLowerCase())) {
                errorLogger.error("[" + category + "] at the element \"category\" is not in the list CATEGORIES in the " + xmlFile.getName() + ".");
                error.incrementAndGet();
            }
        }

        List<Person> person = icon.getPerson();
        if (person == null) {
            errorLogger.error("Element \"person\" is not found in " + xmlFile.getName() + ".");
            error.incrementAndGet();
        }

        List<Reference> references = icon.getReferences();
        if (references != null) {
            for (Reference reference : references) {
                if (!REFERENCES.contains(reference.getDb())) {
                    errorLogger.error("[" + reference.getDb() + "] at element \"reference\" is not in the list REFERENCE in " + xmlFile.getName() + ".");
                    error.incrementAndGet();
                } else if (checkReferences && !checkReference(reference, icon, xmlFile.getName(), 0)) {
                    error.incrementAndGet();
                }
            }
        } else {
            logger.debug("Element \"reference\" was not found in " + xmlFile.getName() + ".");
        }

        List<String> synonyms = icon.getSynonyms();
        if (synonyms != null) {
            for (String synonym : synonyms) {
                if (synonym.equals("")) {
                    errorLogger.error("Element \"synonym\" is missing value in " + xmlFile.getName() + ".");
                    error.incrementAndGet();
                }
            }
        }
    }

    private static final Pattern prefixPattern = Pattern.compile("^[A-Za-z.]+:");

    private static final Map<String, List<String>> identifiersPrefixes = Map.of(
            "ENA", List.of("ena.embl"),
            "PUBCHEM", List.of("pubchem.compound", "pubchem.substance", "pubchem.bioassay"),
            "IUPHAR", List.of("iuphar.family", "iuphar.ligand", "iuphar.receptor"),
            "NCBI", List.of("ncbigene", "ncbiprotein")
    );

    private static final Map<String, Function<String, String>> idBuilders = Map.of(
            "ncbiprotein", id -> id.replace(':', '_')
    );

    private static final Map<String, Function<Reference, List<String>>> dbToUrlBuilders = Map.of(
            "KEGG", reference -> List.of("http://rest.kegg.jp/get/" + reference.getId()),
            "OPL", reference -> List.of("https://www.ebi.ac.uk/ols/api/ontologies/opl/terms?iri=http://purl.obolibrary.org/obo/" + reference.getId().replace(':', '_')),
            "DEFAULT", reference -> identifiersPrefixes
                    .getOrDefault(reference.getDb(), List.of(reference.getDb()))
                    .stream()
                    .map(prefix -> "https://resolver.api.identifiers.org/" + prefix + ":" +
                            idBuilders.getOrDefault(prefix, id -> prefixPattern.matcher(id).replaceFirst("")).apply(reference.getId())
                    )
                    .flatMap(resolverURL -> {
                        HttpRequest request = HttpRequest.newBuilder(URI.create(resolverURL)).build();
                        try {
                            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                            Resolver resolver = new ObjectMapper().readValue(response.body(), Resolver.class);
                            return resolver.payload.resolvedResources.stream().map(resolvedResource -> resolvedResource.compactIdentifierResolvedUrl);
                        } catch (IOException | InterruptedException e) {
                            e.printStackTrace();
                            return Stream.empty();
                        }
                    })
                    .collect(toList())
    );

    private Instant prevQueryTime = Instant.now();

    @SuppressWarnings("unchecked")
    private boolean checkReference(Reference reference, Icon icon, String iconId, int recursionLevel) {
        if (reference.getDb().equals("UNIPROT")) {
            uniprotReferences.computeIfAbsent(reference.getId(), id -> new ArrayList<>()).add(Pair.of(icon, iconId));
            return true;
        }

        boolean isFinalTry = recursionLevel > 2;
        String errorMessage;
        HttpRequest request;
        HttpResponse<?> response;
        List<String> urls = dbToUrlBuilders.getOrDefault(reference.getDb(), dbToUrlBuilders.get("DEFAULT")).apply(reference);

        for (String url : urls) {
            try {
                if (Duration.between(prevQueryTime, Instant.now()).toMillis() < QUERY_INTERVAL) {
                    Thread.sleep(QUERY_INTERVAL);
                }

                prevQueryTime = Instant.now();
                request = HttpRequest.newBuilder(URI.create(url)).build();
                response = httpClient.send(request, bodyHandler);
                if (response.statusCode() / 100 == 2) {
                    if (iconNameCheck && !StringUtils.containsIgnoreCase(((HttpResponse<String>) response).body(), icon.getName())) {
                        errorLogger.warn("{} : {} can be found at {}, but the result do not contains {}",
                                iconId, reference, response.uri(), icon.getName());
                    }
                    return true;
                }
            } catch (IOException | InterruptedException e) {
                errorLogger.warn(String.format("Checking %s : %s threw %s while testing following url : %s", iconId, reference, e.getMessage(), url));
                e.printStackTrace();
            }
        }
        errorMessage = String.format("%s : %s cannot be found at the following urls %s. This might be due to a non supported database. Please contact eragueneau@ebi.ac.uk in such case",
                iconId, reference, urls);


        if (!isFinalTry) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
            System.out.format("%s : %s check failed on its attempt #%d\n", iconId, reference, recursionLevel + 1);
            return checkReference(reference, icon, iconId, recursionLevel + 1);
        } else {
            errorLogger.error(errorMessage);
            return false;
        }
    }

    private void processBatchChecking() {
        Set<String> correctAccessions = Utils.queryUniprot(uniprotReferences.keySet());
        for (String queriedAccession : uniprotReferences.keySet()) {
            if (correctAccessions.contains(queriedAccession)) continue;
            for (Pair<Icon, String> iconAndName : uniprotReferences.get(queriedAccession)) {
                error.incrementAndGet();
                errorLogger.error("{} : {} doesn't seem to exist",
                        iconAndName.getRight(), new Reference("UNIPROT", queriedAccession));
            }
        }
    }


    private List<String> readFile(String fileName) {
        List<String> result;
        try (Stream<String> lines = Files.lines(Paths.get(fileName))) {
            result = lines.collect(toList());
        } catch (IOException e) {
            error.incrementAndGet();
            throw new RuntimeException("Cannot read file: " + fileName);
        }
        return result;
    }

    @Override
    public int getFailedChecks() {
        return error.get();
    }

    @Override
    public int getTotalChecks() {
        return xmlNum;
    }
}
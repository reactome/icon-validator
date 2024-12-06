package org.reactome.server.tools

import com.martiansoftware.jsap.JSAPResult
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.reactome.server.tools.model.Icon
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.stream.Collectors
import javax.xml.bind.JAXBContext
import javax.xml.bind.JAXBException

@Suppress("unused")
class DuplicateChecker(private val config: JSAPResult) : Checker {
    var _failedChecks: Int = 0
    var _totalChecks: Int = 0

    override fun getFailedChecks(): Int {
        return _failedChecks
    }

    override fun getTotalChecks(): Int {
        return _totalChecks
    }

    override fun process() {
        val directory = config.getString("directory")

        val filesInDir = File(directory)

        val xmlFiles = filesInDir.listFiles { _: File?, name: String -> name.endsWith(".xml") }

        val duplicatedReferenceId = xmlFiles?.let { duplicatedId(it) } ?: emptyList()

        try {
            // write potential duplicate item into a csv file
            val csvFile = File("potential_duplicate.csv")
            val writer = Files.newBufferedWriter(Paths.get(csvFile.path))
            val csvPrinter = CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("Name", "Category", "Reference", "File"))

            if (xmlFiles != null) {
                for (file in xmlFiles) {
                    _totalChecks = xmlFiles.size
                    val icon = convertXmlToObj(file)
                    if (icon != null) {
                        findDuplicated(file, icon, duplicatedReferenceId, csvPrinter)
                    }
                }
            }
            csvPrinter.flush()
            csvPrinter.close()
        } catch (e: IOException) {
            errorLogger.error("Could not write potential_duplicate.csv")
            e.printStackTrace()
        }
    }

    private fun convertXmlToObj(file: File): Icon? {
        val jaxbContext: JAXBContext
        try {
            jaxbContext = JAXBContext.newInstance(Icon::class.java)
            val jaxbUnmarshaller = jaxbContext.createUnmarshaller()
            return jaxbUnmarshaller.unmarshal(file) as Icon
        } catch (ignored: JAXBException) {
        }
        return null
    }

    @Throws(IOException::class)
    private fun findDuplicated(xmlFile: File, icon: Icon, duplicated: List<String?>, csvPrinter: CSVPrinter) {
        val references = icon.references

        if (references != null) {
            for (reference in references) {
                if (duplicated.contains(reference.id)) {
                    csvPrinter.printRecord(
                        icon.name,
                        icon.categories.toString().replace("[", "").replace("]", ""),
                        reference.id,
                        xmlFile.name
                    )
                }
            }
        }
    }

    private fun duplicatedId(xmlFiles: Array<File>): List<String> {
        return xmlFiles
            .flatMap { convertXmlToObj(it)?.references ?: emptyList() }
            .map { it.id }
            .groupingBy { it }
            .eachCount()
            .filter { it.value > 1 }
            .keys
            .toList()
    }

    companion object {
        private val errorLogger: Logger = LoggerFactory.getLogger("errorLogger")
    }
}
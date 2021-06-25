package org.reactome.server.tools;

import com.martiansoftware.jsap.JSAPResult;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.reactome.server.tools.model.Icon;
import org.reactome.server.tools.model.Reference;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


@SuppressWarnings("unused")
public class DuplicateChecker {

    private int errorDuplicate = 0;
    private int xmlNum = 0;

    public void process(JSAPResult config) throws IOException {
        String directory = config.getString("directory");

        File filesInDir = new File(directory);

        File[] xmlFiles = filesInDir.listFiles((dir, name) -> name.endsWith(".xml"));

        List<String> duplicatedReferenceId = duplicatedId(xmlFiles);

        // write potential duplicate item into a csv file
        File csvFile = new File("potential_duplicate.csv");
        BufferedWriter writer = Files.newBufferedWriter(Paths.get(csvFile.getPath()));
        CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("Name", "Category", "Reference", "File"));

        if (xmlFiles != null) {
            for (File file : xmlFiles) {
                xmlNum = xmlFiles.length;
                Icon icon = convertXmlToObj(file);
                if (icon != null) {
                    findDuplicated(file, icon, duplicatedReferenceId, csvPrinter);
                }
            }
        }
        csvPrinter.flush();
        csvPrinter.close();
    }

    private Icon convertXmlToObj(File file) {
        JAXBContext jaxbContext;
        try {
            jaxbContext = JAXBContext.newInstance(Icon.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            return (Icon) jaxbUnmarshaller.unmarshal(file);
        } catch (JAXBException ignored) {
        }
        return null;
    }

    private void findDuplicated(File xmlFile, Icon icon, List<String> duplicated, CSVPrinter csvPrinter) throws IOException {

        List<Reference> references = icon.getReferences();

        if (references != null) {
            for (Reference reference : references) {
                if (duplicated.contains(reference.getId())) {
                    csvPrinter.printRecord(icon.getName(), icon.getCategories().toString().replace("[", "").replace("]", ""), reference.getId(), xmlFile.getName());
                }
            }
        }
    }

    private List<String> duplicatedId(File[] xmlFiles) {
        List<String> referenceId = new ArrayList<>();
        if (xmlFiles != null) {
            for (File file : xmlFiles) {
                Icon icon = convertXmlToObj(file);
                if (icon != null && icon.getReferences() != null && icon.getReferences().size() > 0) {
                    for (Reference reference : icon.getReferences()) {
                        referenceId.add(reference.getId());
                    }
                }
            }
        }
        //get duplicate items

        return referenceId.stream()
                .filter(e -> Collections.frequency(referenceId, e) > 1)
                .distinct()
                .collect(Collectors.toList());
    }

    public int getError() {
        return errorDuplicate;
    }

    public void setError(int errorDuplicate) {
        this.errorDuplicate = errorDuplicate;
    }

    public int getXmlNum() {
        return xmlNum;
    }

    public void setXmlNum(int xmlNum) {
        this.xmlNum = xmlNum;
    }
}
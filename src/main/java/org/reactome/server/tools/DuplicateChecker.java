package org.reactome.server.tools;

import com.martiansoftware.jsap.JSAPResult;
import org.reactome.server.tools.model.Icon;
import org.reactome.server.tools.model.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


@SuppressWarnings("ALL")
public class DuplicateChecker {

    private static final Logger duplicateLogger = LoggerFactory.getLogger("duplicateLogger");

    private int errorDuplicate = 0;
    private int xmlNum = 0;

    public void process(JSAPResult config) {
        String directory = config.getString("directory");

        File filesInDir = new File(directory);

        File[] xmlFiles = filesInDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".xml");
            }
        });

        List<String> duplicatedReferenceId = duplicatedId(xmlFiles);

        //header in duplication.log
        duplicateLogger.info("Name, Category, Reference ID, File");

        if (xmlFiles != null) {
            for (File file : xmlFiles) {
                xmlNum = xmlFiles.length;
                Icon icon = convertXmlToObj(file);
                if (icon != null) {
                    findDuplicated(file, icon, duplicatedReferenceId);
                }
            }
        }
    }

    private Icon convertXmlToObj(File file) {
        JAXBContext jaxbContext;
        try {
            jaxbContext = JAXBContext.newInstance(Icon.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            Icon icon = (Icon) jaxbUnmarshaller.unmarshal(file);
            return icon;
        } catch (JAXBException e) {
            duplicateLogger.error(e.getCause().getMessage() + " File: " + file.getName());
            errorDuplicate++;
        }
        return null;
    }

    private void findDuplicated(File xmlFile, Icon icon, List duplicated) {

        List<Reference> references = icon.getReferences();
        if (references != null) {
            for (Reference reference : references) {
                if (duplicated.contains(reference.getId())) {
                    duplicateLogger.info(icon.getName() + ", " + icon.getCategories() + ", " + reference.getId() + ", " + xmlFile.getName());
                    errorDuplicate++;
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
        List<String> duplicated = referenceId.stream()
                .filter(e -> Collections.frequency(referenceId, e) > 1)
                .distinct()
                .collect(Collectors.toList());

        return duplicated;
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
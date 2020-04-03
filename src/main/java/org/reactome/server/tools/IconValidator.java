package org.reactome.server.tools;

import com.martiansoftware.jsap.JSAPResult;
import org.reactome.server.tools.model.Icon;
import org.reactome.server.tools.model.Person;
import org.reactome.server.tools.model.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("ALL")
public class IconValidator {

    private static final Logger logger = LoggerFactory.getLogger("logger");
    private static final Logger errorLogger = LoggerFactory.getLogger("errorLogger");

    private List<String> CATEGORIES;
    private List<String> REFERENCES;

    private int error = 0;
    private int xmlNum = 0;

    public void process(JSAPResult config) {
        String directory = config.getString("directory");

        CATEGORIES = readFile(config.getString("categoriesfile"));
        REFERENCES = readFile(config.getString("referencesfile"));

        File filesInDir = new File(directory);

        File[] xmlFiles = filesInDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".xml");
            }
        });

        if (xmlFiles != null) {
            for (File file : xmlFiles) {
                xmlNum = xmlFiles.length;
                Icon icon = convertXmlToObj(file);
                if (icon != null) {
                    validateXmlObj(file, icon);
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
            errorLogger.error(e.getCause().getMessage() + " File: " + file.getName());
            error++;
        }
        return null;
    }

    private void validateXmlObj(File xmlFile, Icon icon) {
        List<String> categories = icon.getCategories();
        for (String category : categories) {
            if (!CATEGORIES.contains(category.toLowerCase())) {
                errorLogger.error("[" + category + "] at the element \"category\" is not in the list CATEGORIES in the " + xmlFile.getName() + ".");
                error++;
            }
        }

        List<Person> person = icon.getPerson();
        if (person == null) {
            errorLogger.error("Element \"person\" is not found in " + xmlFile.getName() + ".");
            error++;
        }

        List<Reference> references = icon.getReferences();
        if (references != null) {
            for (Reference reference : references) {
                if (!REFERENCES.contains(reference.getDb())) {
                    errorLogger.error("[" + reference.getDb() + "] at element \"reference\" is not in the list REFERENCE in " + xmlFile.getName() + ".");
                    error++;
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
                    error++;
                }
            }
        }
    }

    private List<String> readFile(String fileName) {
        List<String> result = null;
        try (Stream<String> lines = Files.lines(Paths.get(fileName))) {
            result = lines.collect(Collectors.toList());
        } catch (IOException e) {
            error++;
            throw new RuntimeException("Cannot read file: " + fileName);
        }
        return result;
    }

    public int getError() {
        return error;
    }

    public void setError(int error) {
        this.error = error;
    }

    public int getXmlNum() {
        return xmlNum;
    }

    public void setXmlNum(int xmlNum) {
        this.xmlNum = xmlNum;
    }
}
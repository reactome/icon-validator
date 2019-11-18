package org.reactome.server.tools;

import com.martiansoftware.jsap.*;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IconValidator {

    private static final Logger logger = LoggerFactory.getLogger("logger");
    private static final Logger errorLogger = LoggerFactory.getLogger("errorLogger");

    private final static List<String> CATEGORIES = new ArrayList<String>(Arrays.asList("arrow", "cell_element",  "cell_type", "compound", "human_tissue", "protein", "receptor", "transporter"));
    private final static List<String> REFERENCES = new ArrayList<String>(Arrays.asList("UNIPROT", "GO", "CHEBI", "ENSEMBL", "CL", "UBERON", "INTERPRO", "MESH", "KEGG", "ENA", "SO", "BTO", "RFAM", "PUBCHEM", "PFAM", "COMPLEXPORTAL", "OMIT"));

    private int error = 0;
    private int xmlNum = 0;

    public static void main(String[] args) throws Exception {

        SimpleJSAP jsap = new SimpleJSAP(
                IconValidator.class.getName(),
                "Validates all the icon metadata before it is indexed during data release",
                new Parameter[] {
                         new FlaggedOption( "directory", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'd', "directory", "The place of icon XML s to import").setList(true).setListSeparator(',')
                        , new FlaggedOption( "force", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'f', "force", "force icon validator to execute ")
                }
        );

        JSAPResult config = jsap.parse(args);

        if(jsap.messagePrinted()) System.exit( 1 );

        IconValidator iv = new IconValidator();
        iv.process(config);
        if (iv.error > 0) System.exit(1);

    }

    private void process(JSAPResult config) {

        String directory = config.getString("directory");
        File filesInDir = new File(directory);

        File[] xmlFiles = filesInDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".xml");
            }
        });

        if (xmlFiles != null){
            for (File file : xmlFiles) {
                xmlNum = xmlFiles.length;
                Icon icon = convertXmlToObj(file);
                if( icon != null){
                    validateXmlObj(file, icon);
                }
            }
        }
        errorLogger.error(error + " errors are found in " + xmlNum + " XML files");
    }

    private Icon convertXmlToObj(File file){

        JAXBContext jaxbContext;
        try {
            jaxbContext = JAXBContext.newInstance(Icon.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            Icon icon = (Icon) jaxbUnmarshaller.unmarshal(file);
            return icon;
        } catch (JAXBException e) {
            errorLogger.error(e.getCause().getMessage());
            error++;
        }
        return null;
    }

    private void validateXmlObj(File xmlFile, Icon icon){

        List<String> categories = icon.getCategories();
        for (String category : categories) {
            if(!CATEGORIES.contains(category.toLowerCase())){
                errorLogger.error("Category [" + category + "] is not in the list CATEGORIES in the " + xmlFile.getName() + ".");
                error++;
            }
        }

        List<Person> person= icon.getPerson();
        if(person == null){
            errorLogger.error("Person not found in " + xmlFile.getName() + ".");
            error++;
        }

        List<Reference> references = icon.getReferences();
        if(references != null){
            for (Reference reference : references) {
                if(!REFERENCES.contains(reference.getDb())){
                    errorLogger.error("["+ reference.getDb() + "] is not in the list REFERENCE in " + xmlFile.getName() + ".");
                    error++;
                }
            }
        } else {
            logger.debug("reference was not found in " + xmlFile.getName() + ".");
        }

        List<String> synonyms = icon.getSynonyms();
        if(synonyms != null) {
            for (String synonym : synonyms) {
                if (synonym.equals("")){
                    errorLogger.error("Synonym is missing value in " + xmlFile.getName() + ".");
                    error++;
                }
            }
        }
    }
}
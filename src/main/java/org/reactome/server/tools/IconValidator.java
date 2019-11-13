package org.reactome.server.tools;

import com.martiansoftware.jsap.*;
import org.reactome.server.tools.model.Icon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IconValidator {

    private static final Logger logger = LoggerFactory.getLogger(IconValidator.class.getName());


    private final static List<String> CATEGORIES = new ArrayList<String>(Arrays.asList("arrow", "cell element",  "cell type", "compound", "human tissue", "protein", "receptor", "transporter"));

    // directory = Users/chuqiao/Dev/Icons/LIB
    public static void main(String[] args) throws Exception {

        SimpleJSAP jsap = new SimpleJSAP(
                IconValidator.class.getName(),
                "Validates all the icon metadata before it is indexed during data release",
                new Parameter[] {
                         new FlaggedOption( "directory", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'd', "directory", "The place of icon XML s to import").setList(true).setListSeparator(',')
                       , new FlaggedOption( "out", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'o', "output", "The full path of the output binary file")
                }
        );

        JSAPResult config = jsap.parse(args);

        if( jsap.messagePrinted() ) System.exit( 1 );

        new IconValidator().process(config);

    }

    public void process(JSAPResult config) {
        String directory = config.getString("directory");

        File fileToPath = new File(directory);
        File[] listOfAllFiles = fileToPath.listFiles();

        //Iterate all format files in folder
        for (File file : listOfAllFiles) {
            if (file.isFile() && file.getName().endsWith(".xml")) {
               toObj(directory, file);
            }
        }
    }

    public Icon toObj(String directory, File xmlFiles){

        File xmlFile = new File(directory, xmlFiles.getName());

        JAXBContext jaxbContext;

        try {

            /* Icon i = new Icon();
            List<Person> persons = new ArrayList<Person>();
            persons.add(new Person("c","aaa",null, "gggg"));
            persons.add(new Person("a","bbbb",null, "ccccc"));
            i.setPerson(persons);

            Marshaller jaxb = jaxbContext.createMarshaller();
            jaxb.marshal(i, new File("icon.xml"));
            */

            jaxbContext = JAXBContext.newInstance(Icon.class);

            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

            Icon icon = (Icon) jaxbUnmarshaller.unmarshal(xmlFile);

            List<String> cat = icon.getCategories();

            for (String item : cat) {
                if(!CATEGORIES.contains(item.toLowerCase())){
                    //Todo: category is not found in category list ot no items in categories tag
                    logger.info("category is not correct in " + xmlFile.getName() );
                }
            }

            System.out.println(icon);
            // Todo
            return icon;

        } catch (JAXBException e) {
            e.printStackTrace();
        }
        // Todo
        return null;
    }
}

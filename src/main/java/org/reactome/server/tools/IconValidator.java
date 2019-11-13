package org.reactome.server.tools;

import com.martiansoftware.jsap.*;
import org.reactome.server.tools.model.Icon;
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

    private static final Logger logger = LoggerFactory.getLogger(IconValidator.class.getName());

    private final static List<String> CATEGORIES = new ArrayList<String>(Arrays.asList("arrow", "cell_element",  "cell_type", "compound", "human_tissue", "protein", "receptor", "transporter"));
    private final static List<String> REFERENCES = new ArrayList<String>(Arrays.asList("arrow", "cell_element",  "cell_type", "compound", "human_tissue", "protein", "receptor", "transporter"));

    // directory = Users/chuqiao/Dev/Icons/LIB
    public static void main(String[] args) throws Exception {

        SimpleJSAP jsap = new SimpleJSAP(
                IconValidator.class.getName(),
                "Validates all the icon metadata before it is indexed during data release",
                new Parameter[] {
                         new FlaggedOption( "directory", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'd', "directory", "The place of icon XML s to import").setList(true).setListSeparator(',')
                       , new FlaggedOption( "out", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'o', "output", "The full path of the output binary file")
                        , new FlaggedOption( "force", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'f', "force", "The full path of the output binary file")
                }
        );

        JSAPResult config = jsap.parse(args);

        if( jsap.messagePrinted() ) System.exit( 1 );

        new IconValidator().process(config);

    }

    public void process(JSAPResult config) {

        String directory = config.getString("directory");
        File dir = new File(directory);

        // fileFilter(dir);
        File[] listOfAllFiles = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".xml");
            }
        });

        for (File file : listOfAllFiles) {
            if (file.isFile()) {
                convertXmlToObj(file);
            }
        }

        //Iterate all format files in folder
       /* for (File file : listOfAllFiles) {
            if (file.isFile() && file.getName().endsWith(".xml")) {
                convertXmlToObj(directory, file);
            }
        }*/

    }

    private void fileFilter(File dir){
        File[] listOfAllFiles = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".xml");
            }
        });

        for (File file : listOfAllFiles) {
            if (file.isFile()) {
                convertXmlToObj(file);
            }
        }

    }

    public Icon convertXmlToObj(File xmlFile){

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

            //Todo: split
            List<String> cat = icon.getCategories();
            for (String item : cat) {
                if(!CATEGORIES.contains(item.toLowerCase())){
                    //Todo: category is not found in category list ot no items in categories tag
                    logger.info("category " + item + " is not correct in " + xmlFile.getName() );
                }
            }

           /* int sizePerson = icon.getPerson().size();
              if(sizePerson == 0){
                System.out.println("no person tag");
            }*/

            // Todo
            System.out.println(icon);
            return icon;


        } catch (JAXBException e) {
            e.printStackTrace();
        }
        // Todo
        return null;
    }
}

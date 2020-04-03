package org.reactome.server.tools;

import com.martiansoftware.jsap.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@SuppressWarnings("ALL")
public class Main {

    private static final Logger errorLogger = LoggerFactory.getLogger("errorLogger");
    private static final Logger duplicateLogger = LoggerFactory.getLogger("duplicateLogger");

    public static void main(String[] args) throws Exception {

        SimpleJSAP jsap = new SimpleJSAP(
                IconValidator.class.getName(),
                "Validates all the icon metadata before it is indexed during data release",
                new Parameter[]{
                        new FlaggedOption("directory", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'd', "directory", "The place of icon XML to import").setList(true).setListSeparator(',')
                        , new FlaggedOption("referencesfile", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'r', "referencesfile", "A file containing references")
                        , new FlaggedOption("categoriesfile", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'c', "categoriesfile", "A file containing categories")
                        , new FlaggedOption("force", JSAP.BOOLEAN_PARSER, "false", JSAP.NOT_REQUIRED, 'f', "force", "<< NOT RECOMMENDED >> Forces icon validator to pass and suppress errors.")
                }
        );

        JSAPResult config = jsap.parse(args);

        if (jsap.messagePrinted()) System.exit(1);


        // Run duplicateChecker before Iconvalidator, it won't block the application.
        DuplicateChecker dc = new DuplicateChecker();
        dc.process(config);
        if (dc.getError() > 0 && !config.getBoolean("force")) {
            duplicateLogger.info(dc.getError() + " potential duplicates are found in " + dc.getXmlNum() + " XML files.");
        }

        IconValidator iv = new IconValidator();
        iv.process(config);
        if (iv.getError() > 0 && !config.getBoolean("force")) {
            errorLogger.info(iv.getError() + " errors are found in " + iv.getXmlNum() + " XML files.");
            System.exit(1);
        }
    }
}

package org.reactome.server.tools;

import com.martiansoftware.jsap.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IconValidator {

    private static final Logger logger = LoggerFactory.getLogger(IconValidator.class.getName());

    // /Users/chuqiao/Dev/Icons/LIB

    public static void main(String[] args) throws Exception {

        SimpleJSAP jsap = new SimpleJSAP(
                IconValidator.class.getName(),
                "Validates all the icon metadata before it is indexed during data release",
                new Parameter[] {
                         new FlaggedOption( "experiments", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'e', "experiments", "The list of experiments (urls) to import, comma separated optionally with names").setList(true).setListSeparator(',')
                       , new FlaggedOption( "output", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'o', "output", "The full path of the output binary file")
                       , new FlaggedOption( "nulls", JSAP.STRING_PARSER, "", JSAP.REQUIRED, 'n', "nulls", "How empty (null) values are handled, e.g \"0.0\" will replace ane empty value with zeroes. will omit those lines with an empty value.")

                }
        );

        JSAPResult config = jsap.parse(args);

        if( jsap.messagePrinted() ) System.exit( 1 );

    }
}

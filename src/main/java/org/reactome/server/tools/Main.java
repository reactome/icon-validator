package org.reactome.server.tools;

import com.martiansoftware.jsap.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;


@SuppressWarnings("unused")
public class Main {

    private static final Logger errorLogger = LoggerFactory.getLogger("errorLogger");
    private static final Logger duplicateLogger = LoggerFactory.getLogger("duplicateLogger");
    private static final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();

    public static void main(String[] args) throws Exception {

        SimpleJSAP jsap = new SimpleJSAP(
                IconValidator.class.getName(),
                "Validates all the icon metadata before it is indexed during data release",
                new Parameter[]{
                        new FlaggedOption("directory", JSAP.STRING_PARSER, "../LIB", JSAP.NOT_REQUIRED, 'd', "directory", "The place of icon XML to import").setList(true).setListSeparator(',')
                        , new FlaggedOption("referencesfile", JSAP.STRING_PARSER, "references.txt", JSAP.NOT_REQUIRED, 'r', "referencesfile", "A file containing references")
                        , new FlaggedOption("categoriesfile", JSAP.STRING_PARSER, "categories.txt", JSAP.NOT_REQUIRED, 'c', "categoriesfile", "A file containing categories")
                        , new FlaggedOption("force", JSAP.BOOLEAN_PARSER, "false", JSAP.NOT_REQUIRED, 'f', "force", "<< NOT RECOMMENDED >> Forces icon validator to pass and suppress errors.")
                        , new FlaggedOption("checkReferences", JSAP.BOOLEAN_PARSER, "true", JSAP.NOT_REQUIRED, 'e', "checkReferences", "<< LONG PROCESS >> Check that all references are still active")
                        , new FlaggedOption("iconNameCheck", JSAP.BOOLEAN_PARSER, "false", JSAP.NOT_REQUIRED, 'i', "iconNameCheck", "<< LONG PROCESS >> Add warnings when the icon name cannot be found inside the reference link content")
                }
        );

        JSAPResult config = jsap.parse(args);

        if (jsap.messagePrinted()) System.exit(1);

        List<Future<?>> checkers = new ArrayList<>();
        checkers.add(executor.submit(new DuplicateChecker(config)));
        checkers.add(executor.submit(new ExtensionChecker(config)));
        IconValidator validator = new IconValidator(config);
        checkers.add(executor.submit(validator));

        while (true) {
            if (checkers.stream().allMatch(Future::isDone)) {
                if (validator.getFailedChecks() > 0 && !config.getBoolean("force")) {
                    errorLogger.info(validator.getFailedChecks() + " errors are found in " + validator.getTotalChecks() + " XML files.");
                    System.exit(1);
                }
                System.exit(0);
            }
        }
    }
}

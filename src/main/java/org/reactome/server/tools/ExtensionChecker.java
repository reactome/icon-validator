package org.reactome.server.tools;

import com.martiansoftware.jsap.JSAPResult;
import org.apache.commons.collections4.ListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExtensionChecker implements Checker {
    private static final Logger errorLogger = LoggerFactory.getLogger("errorLogger");

    private final JSAPResult config;
    private final List<String> extensions = List.of(".svg", ".emf", ".png", ".xml");
    private final Map<String, List<String>> idsToExtensions = new HashMap<>();
    private int error = 0;

    public ExtensionChecker(JSAPResult config) {
        this.config = config;
    }

    @Override
    public void process() {
        String directory = config.getString("directory");

        File filesInDir = new File(directory);

        File[] files = filesInDir.listFiles();
        if (files != null && files.length != 0) {
            for (File file : files) {
                String fullName = file.getName();
                String id = fullName.substring(0, fullName.length() - 4);
                if (!id.startsWith("R-ICO-")) continue;
                String ext = fullName.substring(fullName.length() - 4);
                idsToExtensions.compute(id, (key, value) -> {
                    if (value == null) value = new ArrayList<>();
                    value.add(ext);
                    return value;
                });
            }
        }

        for (String id : idsToExtensions.keySet()) {
            List<String> es = idsToExtensions.get(id);
            if (!es.containsAll(extensions)) {
                ++error;
                errorLogger.error("{} does not have {} file(s)", id, String.join(", ", ListUtils.subtract(extensions, es)));
            }
        }

    }

    @Override
    public int getFailedChecks() {
        return error;
    }

    @Override
    public int getTotalChecks() {
        return idsToExtensions.size();
    }
}

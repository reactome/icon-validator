package org.reactome.server.tools;

import com.martiansoftware.jsap.JSAPResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates EHLD SVG files exported from Figma.
 *
 * Checks:
 *   1. Filename matches R-HSA-[numbers].svg
 *   2. No duplicate filenames
 *   3. BG group exists
 *   4. LOGO exists somewhere inside BG
 *   5. At least one REGION-R-HSA-[numbers] group exists
 *   6. Each REGION has a matching OVERLAY inside it
 *   7. FG is optional (warning if missing)
 */
public class EhldValidator implements Checker {

    private static final Logger logger = LoggerFactory.getLogger("logger");
    private static final Logger errorLogger = LoggerFactory.getLogger("errorLogger");

    private static final Pattern FILENAME_PATTERN = Pattern.compile("^R-HSA-\\d+\\.svg$");
    private static final Pattern REGION_PATTERN = Pattern.compile("^REGION-R-HSA-(\\d+)$");
    private static final Pattern OVERLAY_PATTERN = Pattern.compile("^OVERLAY-R-HSA-(\\d+)$");
    private static final Pattern LOGO_PATTERN = Pattern.compile("^LOGO(_\\d+)?$");

    private final String directory;
    private final AtomicInteger error = new AtomicInteger(0);
    private int totalFiles = 0;

    public EhldValidator(JSAPResult config) {
        this.directory = config.getString("ehldDirectory");
    }

    @Override
    public void process() {
        File dir = new File(directory);

        File[] allFiles = dir.listFiles();
        if (allFiles == null || allFiles.length == 0) {
            errorLogger.error("EHLD directory is empty or does not exist: {}", directory);
            error.incrementAndGet();
            return;
        }

        List<File> svgFiles = new ArrayList<>();
        List<String> invalidNames = new ArrayList<>();
        Map<String, Integer> nameCount = new HashMap<>();

        for (File file : allFiles) {
            if (!file.getName().endsWith(".svg")) continue;
            svgFiles.add(file);
            String name = file.getName().toLowerCase();
            nameCount.merge(name, 1, Integer::sum);
            if (!FILENAME_PATTERN.matcher(file.getName()).matches()) {
                invalidNames.add(file.getName());
            }
        }

        totalFiles = svgFiles.size();
        System.out.println("EHLD Validator: found " + totalFiles + " SVG files");

        // Check invalid filenames
        for (String name : invalidNames) {
            errorLogger.error("EHLD filename '{}' does not match R-HSA-[numbers].svg", name);
            error.incrementAndGet();
        }

        // Check duplicates (case-insensitive)
        for (Map.Entry<String, Integer> entry : nameCount.entrySet()) {
            if (entry.getValue() > 1) {
                errorLogger.error("EHLD duplicate filename: {} appears {} times", entry.getKey(), entry.getValue());
                error.incrementAndGet();
            }
        }

        // Validate each SVG
        int count = 0;
        for (File svgFile : svgFiles) {
            if (FILENAME_PATTERN.matcher(svgFile.getName()).matches()) {
                validateSvg(svgFile);
            }
            count++;
            if (count % 50 == 0) {
                System.out.println(count + " / " + totalFiles + " EHLD files validated");
            }
        }
        System.out.println(totalFiles + " / " + totalFiles + " EHLD files validated");
    }

    private void validateSvg(File svgFile) {
        String filename = svgFile.getName();
        String stableId = filename.replace(".svg", "");

        Document doc;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            doc = builder.parse(svgFile);
        } catch (Exception e) {
            errorLogger.error("EHLD '{}': could not parse SVG: {}", filename, e.getMessage());
            error.incrementAndGet();
            return;
        }

        // Find the top-level group with id matching the stable ID
        Element topGroup = findElementById(doc.getDocumentElement(), stableId);
        if (topGroup == null) {
            errorLogger.error("EHLD '{}': no group with id '{}' found", filename, stableId);
            error.incrementAndGet();
            return;
        }

        // Check BG group exists
        Element bgElement = findElementById(topGroup, "BG");
        if (bgElement == null) {
            errorLogger.error("EHLD '{}': missing 'BG' group", filename);
            error.incrementAndGet();
        } else {
            // Check LOGO exists inside BG
            if (!hasLogoInside(bgElement)) {
                errorLogger.error("EHLD '{}': missing 'LOGO' inside 'BG' group", filename);
                error.incrementAndGet();
            }
        }

        // Check FG (optional, just warn)
        Element fgElement = findElementById(topGroup, "FG");
        if (fgElement == null) {
            logger.warn("EHLD '{}': no 'FG' group found (optional)", filename);
        }

        // Check REGION-R-HSA-[numbers] groups
        List<Element> regions = findElementsMatchingId(topGroup, REGION_PATTERN);
        if (regions.isEmpty()) {
            errorLogger.error("EHLD '{}': no REGION-R-HSA-[numbers] groups found", filename);
            error.incrementAndGet();
        } else {
            for (Element region : regions) {
                String regionId = region.getAttribute("id");
                Matcher regionMatcher = REGION_PATTERN.matcher(regionId);
                if (!regionMatcher.matches()) continue;
                String regionNum = regionMatcher.group(1);

                // Find all OVERLAY IDs inside this region
                List<Element> overlays = findElementsMatchingId(region, OVERLAY_PATTERN);
                if (overlays.isEmpty()) {
                    errorLogger.error("EHLD '{}': REGION '{}' has no OVERLAY-R-HSA-[numbers] inside it", filename, regionId);
                    error.incrementAndGet();
                    continue;
                }

                // Check at least one overlay matches the region number
                boolean hasMatch = false;
                List<String> overlayIds = new ArrayList<>();
                for (Element overlay : overlays) {
                    String overlayId = overlay.getAttribute("id");
                    overlayIds.add(overlayId);
                    Matcher overlayMatcher = OVERLAY_PATTERN.matcher(overlayId);
                    if (overlayMatcher.matches() && overlayMatcher.group(1).equals(regionNum)) {
                        hasMatch = true;
                    }
                }

                if (!hasMatch) {
                    errorLogger.error("EHLD '{}': REGION '{}' contains overlays {} but none match expected 'OVERLAY-R-HSA-{}'",
                            filename, regionId, overlayIds, regionNum);
                    error.incrementAndGet();
                }
            }
        }
    }

    /**
     * Recursively find the first element with the given id attribute.
     */
    private Element findElementById(Element root, String targetId) {
        if (targetId.equals(root.getAttribute("id"))) {
            return root;
        }
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element) {
                Element result = findElementById((Element) child, targetId);
                if (result != null) return result;
            }
        }
        return null;
    }

    /**
     * Recursively find all elements whose id matches the given pattern.
     */
    private List<Element> findElementsMatchingId(Element root, Pattern pattern) {
        List<Element> matches = new ArrayList<>();
        String id = root.getAttribute("id");
        if (id != null && pattern.matcher(id).matches()) {
            matches.add(root);
        }
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element) {
                matches.addAll(findElementsMatchingId((Element) child, pattern));
            }
        }
        return matches;
    }

    /**
     * Check if a LOGO element exists anywhere inside the given element.
     */
    private boolean hasLogoInside(Element element) {
        String id = element.getAttribute("id");
        if (id != null && LOGO_PATTERN.matcher(id).matches()) {
            return true;
        }
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element && hasLogoInside((Element) child)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getFailedChecks() {
        return error.get();
    }

    @Override
    public int getTotalChecks() {
        return totalFiles;
    }
}

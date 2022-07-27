package org.reactome.server.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.reactome.server.tools.model.query.IDMappingStatus;
import org.reactome.server.tools.model.query.IDMappingSubmission;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodySubscriber;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Utils {
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    private static final ObjectMapper mapper = new ObjectMapper();

    public static String encodeURL(Map<String, Object> postData) {
        return encodeURL(postData,
                collection -> collection.stream()
                        .map(Object::toString)
                        .collect(Collectors.joining(" "))
        );
    }

    public static String encodeURL(Map<String, Object> postData, Function<Collection<?>, String> collectionEncoder) {
        return postData.keySet().stream()
                .map(key -> {
                    Object o = postData.get(key);
                    String s = (o instanceof Collection) ? collectionEncoder.apply((Collection<?>) o) : o.toString();
                    return key + "=" + URLEncoder.encode(s, StandardCharsets.UTF_8);
                })
                .collect(Collectors.joining("&"));
    }

    public static <T> Stream<List<T>> slice(Collection<T> collection, int sliceSize) {
        return Stream.iterate(0, i -> i + sliceSize).limit(collection.size() / sliceSize + 1)
                .map(start -> collection.stream().skip(start).limit(sliceSize).collect(Collectors.toUnmodifiableList()));
    }

    public static HttpResponse<String> getRequest(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(url))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public static <T> T getRequest(String url, Class<T> returnClass) throws IOException, InterruptedException {
        return mapper.readValue(getRequest(url).body(), returnClass);
    }

    public static HttpResponse<String> postRequest(String url, Map<String, Object> postData) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(encodeURL(postData)))
                .uri(URI.create(url))
                .header("User-Agent", "IntAct Bot") // add request header
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public static <T> T postRequest(String url, Map<String, Object> postData, Class<T> returnClass) throws IOException, InterruptedException {
        return mapper.readValue(postRequest(url, postData).body(), returnClass);
    }

    // Dirty but couldn't manage to make it work with httpClient
    public static Set<String> queryUniprot(Set<String> identifiersToTest) {
        Map<String, Object> postData = Map.of(
                "ids", String.join(",", identifiersToTest),
                "from", "UniProtKB_AC-ID",
                "to", "UniProtKB"
        );

        try {
            IDMappingSubmission jobResponse = postRequest("https://rest.uniprot.org/idmapping/run", postData, IDMappingSubmission.class);
            String jobId = jobResponse.jobId;
            boolean onGoing = true;
            while (onGoing) {
                Thread.sleep(2000);
                HttpResponse<String> statusResponse = getRequest("https://rest.uniprot.org/idmapping/status/" + jobId);
                onGoing = statusResponse.statusCode() != 303;
                if (String.valueOf(statusResponse.statusCode()).charAt(0) == '4')
                    throw new RuntimeException("ID Mapping job failed");
            }

            HttpResponse<String> result = getRequest("https://rest.uniprot.org/idmapping/uniprotkb/results/stream/" + jobId + "?fields=accession&format=tsv");
            return Arrays.stream(result.body().split("\n"))
                    .skip(1) // skip header
                    .map(s -> s.split("\t")[0])
                    .collect(Collectors.toSet());

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}

package org.reactome.server.tools

import com.fasterxml.jackson.databind.ObjectMapper
import org.reactome.server.tools.model.query.IDMappingSubmission
import java.io.IOException
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.function.Function
import java.util.stream.Collectors

object Utils {
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .followRedirects(HttpClient.Redirect.NEVER)
        .build()
    private val mapper = ObjectMapper()

    private fun encodeURL(postData: Map<String, Any>): String {
        return encodeURL(postData) { collection: Collection<*> ->
            collection.joinToString(" ") { obj: Any? -> obj.toString() }
        }
    }

    private fun encodeURL(postData: Map<String, Any?>, collectionEncoder: Function<Collection<*>, String>): String {
        return postData.keys.stream()
            .map { key: String ->
                val o = postData[key]
                val s = if (o is Collection<*>) collectionEncoder.apply(o) else o.toString()
                key + "=" + URLEncoder.encode(s, StandardCharsets.UTF_8)
            }
            .collect(Collectors.joining("&"))
    }

    fun <T> slice(collection: Collection<T>, sliceSize: Int): List<List<T>> {
        return collection.chunked(sliceSize)
    }

    @Throws(IOException::class, InterruptedException::class)
    fun getRequest(url: String): HttpResponse<String> {
        val request = HttpRequest.newBuilder()
            .GET()
            .uri(URI.create(url))
            .build()
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    @Throws(IOException::class, InterruptedException::class)
    fun <T> getRequest(url: String, returnClass: Class<T>?): T {
        return mapper.readValue(getRequest(url).body(), returnClass)
    }

    @Throws(IOException::class, InterruptedException::class)
    fun postRequest(url: String, postData: Map<String, Any>): HttpResponse<String> {
        val request = HttpRequest.newBuilder()
            .POST(HttpRequest.BodyPublishers.ofString(encodeURL(postData)))
            .uri(URI.create(url))
            .header("User-Agent", "IntAct Bot") // add request header
            .header("Content-Type", "application/x-www-form-urlencoded")
            .build()
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    @Throws(IOException::class, InterruptedException::class)
    fun <T> postRequest(url: String, postData: Map<String, Any>, returnClass: Class<T>?): T {
        return mapper.readValue(postRequest(url, postData).body(), returnClass)
    }

    // Dirty but couldn't manage to make it work with httpClient
    fun queryUniprot(identifiersToTest: Set<String?>): Set<String> {
        val postData = java.util.Map.of<String, Any?>(
            "ids", java.lang.String.join(",", identifiersToTest),
            "from", "UniProtKB_AC-ID",
            "to", "UniProtKB"
        )

        try {
            val jobResponse = postRequest(
                "https://rest.uniprot.org/idmapping/run", postData,
                IDMappingSubmission::class.java
            )
            val jobId = jobResponse.jobId
            var onGoing = true
            var waitingTime = 5000
            while (onGoing) {
                Thread.sleep(waitingTime.toLong())
                val statusResponse = getRequest(
                    "https://rest.uniprot.org/idmapping/status/$jobId"
                )
                onGoing = statusResponse.statusCode() != 303
                if (statusResponse.statusCode() == 429) waitingTime += 1000
                else if (statusResponse.statusCode()
                        .toString()[0] == '4'
                ) throw RuntimeException("ID Mapping job failed")
            }

            val result =
                getRequest("https://rest.uniprot.org/idmapping/uniprotkb/results/stream/$jobId?fields=accession&format=tsv")
            return Arrays.stream(result.body().split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
                .skip(1) // skip header
                .map { s: String -> s.split("\t".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0] }
                .collect(Collectors.toSet())
        } catch (e: IOException) {
            throw RuntimeException(e)
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        }
    }
}

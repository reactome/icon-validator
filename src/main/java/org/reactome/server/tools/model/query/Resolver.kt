package org.reactome.server.tools.model.query

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Resolver(
    val apiVersion: String,
    val errorMessage: String?,
    val payload: Payload
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Payload(
        val parsedCompactIdentifier: ParsedCompactIdentifier?,
        val resolvedResources: List<ResolvedResource>
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        data class ParsedCompactIdentifier(
            val providerCode: String? = null,
            val namespace: String,
            val localId: String,
            val rawRequest: String,
            val namespaceDeprecationDate: String? = null,
            val deprecatedNamespace: Boolean? = null,
            val namespaceEmbeddedInLui: Boolean,
        )
        @JsonIgnoreProperties(ignoreUnknown = true)
        data class ResolvedResource(
            val id: Int,
            val mirId: String,
            val providerCode: String? = null,
            val compactIdentifierResolvedUrl: String,
            val description: String,
            val resourceHomeUrl: String,
            val namespacePrefix: String,
            val namespaceDeprecationDate: String? = null,
            val resourceDeprecationDate: String? = null,
            val deprecatedResource: Boolean?= null,
            val deprecatedNamespace: Boolean? = null,
            val official: Boolean,
            val location: Location,
            val institution: Institution,
            val recommendation: Recommendation,
        ) {

            @JsonIgnoreProperties(ignoreUnknown = true)
            data class Location(
                val countryCode: String,
                val countryName: String,
            )

            @JsonIgnoreProperties(ignoreUnknown = true)
            data class Recommendation(
                val recommendationIndex: Int,
                val recommendationExplanation: String,
            )

            @JsonIgnoreProperties(ignoreUnknown = true)
            data class Institution(
                val id: Int,
                val name: String,
                val homeUrl: String,
                val description: String,
                val location: Location,
            )
        }
    }
}



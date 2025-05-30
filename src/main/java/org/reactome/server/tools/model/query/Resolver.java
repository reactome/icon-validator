package org.reactome.server.tools.model.query;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Jacksonized
@Builder
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class Resolver {
    public final String apiVersion;
    public final String errorMessage;
    public final Payload payload;

    @Jacksonized
    @Builder
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Payload {
        public final ParsedCompactIdentifier parsedCompactIdentifier;
        public final List<ResolvedResource> resolvedResources;

        @Jacksonized
        @Builder
        @ToString
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ParsedCompactIdentifier {
            public final String providerCode;
            public final String namespace;
            public final String localId;
            public final String rawRequest;
            public final String namespaceDeprecationDate;
            public final Boolean deprecatedNamespace;
            public final Boolean namespaceEmbeddedInLui;
        }

        @Jacksonized
        @Builder
        @ToString
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ResolvedResource {
            public final Integer id;
            public final String mirId;
            public final String providerCode;
            public final String compactIdentifierResolvedUrl;
            public final String description;
            public final String resourceHomeUrl;
            public final String namespacePrefix;
            public final String namespaceDeprecationDate;
            public final String resourceDeprecationDate;
            public final Boolean deprecatedResource;
            public final Boolean deprecatedNamespace;
            public final Boolean official;

            public final Location location;
            public final Institution institution;
            public final Recommendation recommendation;

            @Jacksonized
            @Builder
            @ToString
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Location {
                public final String countryCode;
                public final String countryName;
            }

            @Jacksonized
            @Builder
            @ToString
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Recommendation {
                public final Integer recommendationIndex;
                public final String recommendationExplanation;
            }

            @Jacksonized
            @Builder
            @ToString
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Institution {
                public final Integer id;
                public final String name;
                public final String homeUrl;
                public final String description;
                public final Location location;
            }
        }
    }
}

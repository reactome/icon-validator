package org.reactome.server.tools.model.query;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

@Jacksonized
@Builder
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class IDMappingSubmission {
    public final String jobId;
}

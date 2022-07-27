package org.reactome.server.tools.model.query;

import lombok.Builder;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

@Jacksonized
@Builder
@ToString
public class IDMappingStatus {
    public final String jobStatus;
}

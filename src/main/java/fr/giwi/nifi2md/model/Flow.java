/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * nifi2md - documents an Apache NiFi flow as Markdown with Mermaid diagrams.
 */
package fr.giwi.nifi2md.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Raw shape of an exported NiFi flow definition (the JSON you download from the
 * NiFi UI, or that lives in a versioned flow).
 *
 * <p>Only {@link #flowContents()} is deserialized eagerly. The remaining collections
 * differ between NiFi versions (object keyed by id, or array), so they are kept as
 * {@link JsonNode} and normalized later by {@code FlowParser}.</p>
 *
 * @param flowContents             the root process group holding the whole canvas
 * @param externalControllerServices controller services living outside the group tree
 * @param parameterContexts        parameter contexts
 * @param parameterProviders       parameter provider contexts
 * @param flowEncodingVersion      version marker of the exported document
 * @param latest                   whatever the exporter stored as "latest"
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Flow(
        ProcessGroup flowContents,
        JsonNode externalControllerServices,
        JsonNode parameterContexts,
        JsonNode parameterProviders,
        String flowEncodingVersion,
        JsonNode latest) {
}

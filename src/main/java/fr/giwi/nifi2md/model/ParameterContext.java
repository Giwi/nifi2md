/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * nifi2md - documents an Apache NiFi flow as Markdown with Mermaid diagrams.
 */
package fr.giwi.nifi2md.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * A named bag of parameters referenced by expression language in the flow.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ParameterContext(
        String identifier,
        String name,
        String description,
        List<Parameter> parameters) {
}

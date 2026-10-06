/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * nifi2md - documents an Apache NiFi flow as Markdown with Mermaid diagrams.
 */
package fr.giwi.nifi2md.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A funnel: merges multiple connections into one output.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Funnel(
        String identifier,
        String instanceIdentifier,
        String comments,
        Position position) {
}

/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * nifi2md - documents an Apache NiFi flow as Markdown with Mermaid diagrams.
 */
package fr.giwi.nifi2md.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A free-form text label placed on the canvas.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Label(
        String identifier,
        String label,
        Position position,
        Double width,
        Double height) {
}

/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * nifi2md - documents an Apache NiFi flow as Markdown with Mermaid diagrams.
 */
package fr.giwi.nifi2md.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A single parameter inside a parameter context.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Parameter(
        String name,
        String description,
        Boolean sensitive,
        Object value) {

    public boolean isSensitive() {
        return Boolean.TRUE.equals(sensitive);
    }
}

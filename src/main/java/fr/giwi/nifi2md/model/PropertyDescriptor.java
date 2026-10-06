/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * nifi2md - documents an Apache NiFi flow as Markdown with Mermaid diagrams.
 */
package fr.giwi.nifi2md.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Metadata describing a single component property.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PropertyDescriptor(
        String name,
        String displayName,
        boolean sensitive,
        boolean dynamic,
        boolean identifiesControllerService) {

    public String label() {
        return (displayName == null || displayName.isBlank()) ? name : displayName;
    }
}

/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * nifi2md - documents an Apache NiFi flow as Markdown with Mermaid diagrams.
 */
package fr.giwi.nifi2md.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * A connection (queue + relationship routing) between two components.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Connection(
        String identifier,
        String name,
        Connectable source,
        Connectable destination,
        List<String> selectedRelationships,
        String flowFileExpiration,
        Long backPressureObjectThreshold,
        String backPressureDataSizeThreshold,
        Integer labelIndex) {
}

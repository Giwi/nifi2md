/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * nifi2md - documents an Apache NiFi flow as Markdown with Mermaid diagrams.
 */
package fr.giwi.nifi2md.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * An input or output port of a process group.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Port(
        String identifier,
        String instanceIdentifier,
        String name,
        String comments,
        String type,
        Position position,
        Boolean allowRemoteAccess,
        String portFunction,
        String scheduledState,
        Integer concurrentlySchedulableTaskCount) {

    public boolean isInput() {
        return !"OUTPUT_PORT".equalsIgnoreCase(type);
    }
}

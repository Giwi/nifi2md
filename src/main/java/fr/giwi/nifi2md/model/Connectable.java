/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * nifi2md - documents an Apache NiFi flow as Markdown with Mermaid diagrams.
 */
package fr.giwi.nifi2md.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * One endpoint of a connection. The {@code type} is one of
 * PROCESSOR, INPUT_PORT, OUTPUT_PORT, FUNNEL, REMOTE_INPUT_PORT or REMOTE_OUTPUT_PORT.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Connectable(
        String id,
        String type,
        String groupId,
        String name,
        String instanceIdentifier) {

    public String displayType() {
        if (type == null) {
            return "UNKNOWN";
        }
        return switch (type) {
            case "PROCESSOR" -> "Processor";
            case "INPUT_PORT" -> "Input Port";
            case "OUTPUT_PORT" -> "Output Port";
            case "FUNNEL" -> "Funnel";
            case "REMOTE_INPUT_PORT" -> "Remote Input Port";
            case "REMOTE_OUTPUT_PORT" -> "Remote Output Port";
            default -> type;
        };
    }
}

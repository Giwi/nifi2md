/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * nifi2md - documents an Apache NiFi flow as Markdown with Mermaid diagrams.
 */
package fr.giwi.nifi2md.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A remote process group pointing at another NiFi instance over HTTP(S).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RemoteProcessGroup(
        String identifier,
        String instanceIdentifier,
        String name,
        String comments,
        String urls,
        String transportProtocol,
        String yieldDuration,
        String timeout,
        Position position) {
}

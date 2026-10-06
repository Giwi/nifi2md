/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * nifi2md - documents an Apache NiFi flow as Markdown with Mermaid diagrams.
 */
package fr.giwi.nifi2md.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The NAR bundle that provides a component implementation.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Bundle(String group, String artifact, String version) {

    public String coordinates() {
        String g = group == null ? "?" : group;
        String a = artifact == null ? "?" : artifact;
        String v = version == null ? "?" : version;
        return g + ":" + a + ":" + v;
    }
}

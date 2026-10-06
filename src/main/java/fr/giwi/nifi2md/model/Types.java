/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * nifi2md - documents an Apache NiFi flow as Markdown with Mermaid diagrams.
 */
package fr.giwi.nifi2md.model;

/**
 * Small helpers for rendering fully qualified component type names.
 */
public final class Types {

    private Types() {
    }

    /**
     * Returns the last segment of a fully qualified Java type name,
     * e.g. {@code org.apache.nifi.processors.standard.LogAttribute -> LogAttribute}.
     */
    public static String simpleName(String fqcn) {
        if (fqcn == null || fqcn.isBlank()) {
            return "(unknown)";
        }
        int dot = fqcn.lastIndexOf('.');
        return dot < 0 ? fqcn : fqcn.substring(dot + 1);
    }
}

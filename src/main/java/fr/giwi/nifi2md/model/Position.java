/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * nifi2md - documents an Apache NiFi flow as Markdown with Mermaid diagrams.
 */
package fr.giwi.nifi2md.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A coordinate inside a NiFi canvas.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Position(double x, double y) {
}

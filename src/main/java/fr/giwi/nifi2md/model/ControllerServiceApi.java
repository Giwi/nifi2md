/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * nifi2md - documents an Apache NiFi flow as Markdown with Mermaid diagrams.
 */
package fr.giwi.nifi2md.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The interface (API type) a controller service advertises to consumers.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ControllerServiceApi(String type, Bundle bundle) {
}

/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * nifi2md - documents an Apache NiFi flow as Markdown with Mermaid diagrams.
 */
package fr.giwi.nifi2md.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

/**
 * A controller service: a shared service (connection pool, record reader, SSL context...)
 * referenced by processors and other services.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ControllerService(
        String identifier,
        String instanceIdentifier,
        String name,
        String comments,
        String type,
        Bundle bundle,
        Map<String, Object> properties,
        Map<String, PropertyDescriptor> propertyDescriptors,
        List<ControllerServiceApi> controllerServiceApis,
        String scheduledState,
        String bulletinLevel,
        String componentType) {

    public String simpleType() {
        return Types.simpleName(type);
    }
}

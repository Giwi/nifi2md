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
 * A NiFi processor instance.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Processor(
        String identifier,
        String instanceIdentifier,
        String name,
        String comments,
        String type,
        Bundle bundle,
        Map<String, Object> properties,
        Map<String, PropertyDescriptor> propertyDescriptors,
        List<String> autoTerminatedRelationships,
        String scheduledState,
        String schedulingStrategy,
        String schedulingPeriod,
        String executionNode,
        Integer concurrentlySchedulableTaskCount,
        String penaltyDuration,
        String yieldDuration,
        String bulletinLevel,
        String runDurationMillis,
        Position position) {

    public String simpleType() {
        return Types.simpleName(type);
    }
}

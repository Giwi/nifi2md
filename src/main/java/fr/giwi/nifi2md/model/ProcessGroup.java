/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * nifi2md - documents an Apache NiFi flow as Markdown with Mermaid diagrams.
 */
package fr.giwi.nifi2md.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * A NiFi process group: a canvas region grouping processors, connections and ports.
 * The root process group of a flow lives under {@code flowContents} of the document.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProcessGroup(
        String identifier,
        String instanceIdentifier,
        String name,
        String comments,
        Position position,
        List<ProcessGroup> processGroups,
        List<RemoteProcessGroup> remoteProcessGroups,
        List<Processor> processors,
        List<Port> inputPorts,
        List<Port> outputPorts,
        List<Connection> connections,
        List<Label> labels,
        List<Funnel> funnels,
        List<ControllerService> controllerServices,
        String defaultFlowFileExpiration,
        Integer defaultBackPressureObjectThreshold,
        String defaultBackPressureDataSizeThreshold,
        String scheduledState,
        String componentType) {
}

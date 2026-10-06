/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * nifi2md - documents an Apache NiFi flow as Markdown with Mermaid diagrams.
 */
package fr.giwi.nifi2md.render;

import fr.giwi.nifi2md.eip.EipCatalog;
import fr.giwi.nifi2md.eip.EipPattern;
import fr.giwi.nifi2md.model.Connectable;
import fr.giwi.nifi2md.model.Connection;
import fr.giwi.nifi2md.model.Funnel;
import fr.giwi.nifi2md.model.Label;
import fr.giwi.nifi2md.model.Port;
import fr.giwi.nifi2md.model.ProcessGroup;
import fr.giwi.nifi2md.model.Processor;
import fr.giwi.nifi2md.model.RemoteProcessGroup;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Renders a single NiFi process group as a Mermaid {@code flowchart}.
 *
 * <p>Node shapes encode the Enterprise Integration Pattern role of a processor:
 * rhombus for routers, hexagon for split/merge, trapezoid for translators,
 * stadium for channel endpoints, subroutine for nested process groups.
 * A connection pointing at a port of a child group is drawn against the child
 * group node, so the diagram stays readable at every level of the hierarchy.</p>
 */
public final class MermaidRenderer {

    /** Mermaid flowchart directions accepted by this renderer. */
    private static final List<String> DIRECTIONS = List.of("LR", "RL", "TB", "TD", "BT");

    private final String direction;

    public MermaidRenderer(String direction) {
        this.direction = normalizeDirection(direction);
    }

    public static String normalizeDirection(String direction) {
        if (direction == null) {
            return "LR";
        }
        String d = direction.trim().toUpperCase();
        if (DIRECTIONS.contains(d)) {
            // "TD" renders top-down like "TB", both are accepted by Mermaid.
            return "TD".equals(d) ? "TB" : d;
        }
        return "LR";
    }

    /** True when the process group holds at least one node worth drawing. */
    public static boolean hasDiagram(ProcessGroup group) {
        return group != null && (
                notEmpty(group.processGroups())
                        || notEmpty(group.processors())
                        || notEmpty(group.inputPorts())
                        || notEmpty(group.outputPorts())
                        || notEmpty(group.funnels())
                        || notEmpty(group.remoteProcessGroups()));
    }

    private static boolean notEmpty(List<?> list) {
        return list != null && !list.isEmpty();
    }

    public String render(ProcessGroup group) {
        StringBuilder body = new StringBuilder();
        Map<String, String> decls = new LinkedHashMap<>();
        Map<String, String> shapeByNode = new LinkedHashMap<>();
        Map<String, String> idByComponent = new LinkedHashMap<>();
        int[] seq = {0};

        // 1. Nested process groups: one node per group, plus a mapping from each
        //    of their ports to that node so cross-boundary edges stay inside this view.
        for (ProcessGroup child : nullSafe(group.processGroups())) {
            String id = node(shapeByNode, decls, seq, "g");
            decls.put(id, subroutine(id, child.name()));
            shapeByNode.put(id, "group");
            idByComponent.putIfAbsent(child.identifier(), id);
            for (Port p : nullSafe(child.inputPorts())) {
                idByComponent.putIfAbsent(p.identifier(), id);
            }
            for (Port p : nullSafe(child.outputPorts())) {
                idByComponent.putIfAbsent(p.identifier(), id);
            }
            for (RemoteProcessGroup rpg : nullSafe(child.remoteProcessGroups())) {
                idByComponent.putIfAbsent(rpg.identifier(), id);
            }
        }

        // 2. Processors, classified through the EIP catalogue.
        for (Processor p : nullSafe(group.processors())) {
            String id = node(shapeByNode, decls, seq, "p");
            Optional<EipPattern> pattern = EipCatalog.forProcessor(p.type());
            decls.put(id, processorShape(id, p, pattern));
            shapeByNode.put(id, shapeForProcessorShape(pattern.orElse(null)));
            idByComponent.put(p.identifier(), id);
        }

        // 3. Ports of this group.
        for (Port p : nullSafe(group.inputPorts())) {
            String id = node(shapeByNode, decls, seq, "i");
            decls.put(id, stadium(id, p.name()));
            shapeByNode.put(id, "inputPort");
            idByComponent.put(p.identifier(), id);
        }
        for (Port p : nullSafe(group.outputPorts())) {
            String id = node(shapeByNode, decls, seq, "o");
            decls.put(id, stadium(id, p.name()));
            shapeByNode.put(id, "outputPort");
            idByComponent.put(p.identifier(), id);
        }

        // 4. Funnels and canvas labels.
        for (Funnel f : nullSafe(group.funnels())) {
            String id = node(shapeByNode, decls, seq, "f");
            decls.put(id, circle(id, Texts.mermaidLabel(f.comments() != null ? f.comments() : "funnel")));
            shapeByNode.put(id, "funnel");
            idByComponent.put(f.identifier(), id);
        }
        for (Label l : nullSafe(group.labels())) {
            if (l.label() == null || l.label().isBlank()) {
                continue;
            }
            String id = node(shapeByNode, decls, seq, "l");
            decls.put(id, rect(id, Texts.mermaidLabel(l.label())));
            shapeByNode.put(id, "label");
            idByComponent.put(l.identifier(), id);
        }

        // 5. Remote process groups reach out to another NiFi instance.
        for (RemoteProcessGroup rpg : nullSafe(group.remoteProcessGroups())) {
            String id = node(shapeByNode, decls, seq, "r");
            decls.put(id, hexagon(id, rpg.name()));
            shapeByNode.put(id, "remote");
            idByComponent.put(rpg.identifier(), id);
        }

        // 6. Connections become edges. Anything not declared above is an external
        //    reference, rendered with the name NiFi stored on the endpoint.
        Set<String> edges = new LinkedHashSet<>();
        for (Connection c : nullSafe(group.connections())) {
            String from = resolve(c.source(), idByComponent, decls, shapeByNode, seq);
            String to = resolve(c.destination(), idByComponent, decls, shapeByNode, seq);
            if (from == null || to == null || from.equals(to)) {
                continue;
            }
            String label = edgeLabel(c);
            edges.add("    " + from + " -->" + (label.isEmpty() ? "" : "|\"" + label + "\"|") + " " + to);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("flowchart ").append(direction).append('\n');
        decls.forEach((id, decl) -> sb.append("    ").append(decl).append('\n'));
        edges.forEach(e -> sb.append(e).append('\n'));

        Map<String, List<String>> byShape = new LinkedHashMap<>();
        shapeByNode.forEach((id, shape) -> byShape.computeIfAbsent(shape, k -> new ArrayList<>()).add(id));
        for (Map.Entry<String, List<String>> e : byShape.entrySet()) {
            String cls = classFor(e.getKey());
            if (cls == null) {
                continue;
            }
            sb.append("    class ").append(String.join(",", e.getValue())).append(' ').append(cls).append('\n');
        }
        sb.append(classDefs());
        return sb.toString();
    }

    // ------------------------------------------------------------------ helpers

    private String node(Map<String, String> shapeByNode, Map<String, String> decls, int[] seq, String prefix) {
        return prefix + (++seq[0]);
    }

    private String resolve(Connectable endpoint,
                           Map<String, String> idByComponent,
                           Map<String, String> decls,
                           Map<String, String> shapeByNode,
                           int[] seq) {
        if (endpoint == null || endpoint.id() == null) {
            return null;
        }
        String known = idByComponent.get(endpoint.id());
        if (known != null) {
            return known;
        }
        String id = node(shapeByNode, decls, seq, "x");
        String name = endpoint.name() == null || endpoint.name().isBlank()
                ? endpoint.displayType()
                : endpoint.name();
        decls.put(id, round(id, Texts.mermaidLabel(name)));
        shapeByNode.put(id, "external");
        idByComponent.put(endpoint.id(), id);
        return id;
    }

    private String edgeLabel(Connection c) {
        List<String> parts = new ArrayList<>();
        for (String r : nullSafe(c.selectedRelationships())) {
            if (r != null && !r.isBlank() && !parts.contains(r)) {
                parts.add(r);
            }
        }
        if (parts.isEmpty() && c.name() != null && !c.name().isBlank()) {
            parts.add(c.name());
        }
        // Port-to-port connections carry no relationship: name the port involved so
        // two parallel edges between the same pair of groups stay tellable apart.
        if (parts.isEmpty()) {
            Connectable target = c.destination();
            Connectable source = c.source();
            if (isNamedPort(target)) {
                parts.add(target.name());
            } else if (isNamedPort(source)) {
                parts.add(source.name());
            }
        }
        return String.join(", ", parts);
    }

    private static boolean isNamedPort(Connectable endpoint) {
        if (endpoint == null || endpoint.name() == null || endpoint.name().isBlank()) {
            return false;
        }
        String type = endpoint.type();
        return type != null && type.contains("PORT");
    }

    private String processorShape(String id, Processor p, Optional<EipPattern> pattern) {
        String label = Texts.mermaidLabel(p.name() != null && !p.name().isBlank() ? p.name() : p.simpleType());
        if (pattern.isEmpty()) {
            return round(id, label);
        }
        EipPattern ep = pattern.get();
        String epId = ep.id();
        return switch (epId) {
            case "content-based-router", "message-filter", "routing-slip", "process-manager" ->
                    decision(id, label);
            case "splitter", "aggregator", "resequencer", "recipient-list", "pipes-and-filters" ->
                    hexagon(id, label);
            case "message-translator", "content-enricher", "content-filter", "envelope-wrapper", "normalizer" ->
                    trapezoid(id, label);
            case "message-gateway", "service-activator", "polling-consumer",
                 "event-driven-consumer", "channel-adapter", "request-reply", "return-address",
                 "transactional-client", "file-transfer", "shared-database" ->
                    stadium(id, label);
            default -> round(id, label);
        };
    }

    private String shapeForProcessorShape(EipPattern ep) {
        if (ep == null) {
            return "processor";
        }
        return switch (ep.id()) {
            case "content-based-router", "message-filter", "routing-slip", "process-manager" -> "router";
            case "splitter", "aggregator", "resequencer", "recipient-list", "pipes-and-filters" -> "fanout";
            case "message-translator", "content-enricher", "content-filter", "envelope-wrapper", "normalizer" ->
                    "transform";
            case "message-gateway", "service-activator", "polling-consumer",
                 "event-driven-consumer", "channel-adapter", "request-reply", "return-address",
                 "transactional-client", "file-transfer", "shared-database" -> "endpoint";
            default -> "processor";
        };
    }

    // ------------------------------------------------------------------ shapes

    private static String rect(String id, String label) {
        return id + "[\"" + label + "\"]";
    }

    private static String round(String id, String label) {
        return id + "(\"" + label + "\")";
    }

    private static String stadium(String id, String label) {
        return id + "([\"" + Texts.mermaidLabel(label) + "\"])";
    }

    private static String subroutine(String id, String label) {
        return id + "[[\"" + Texts.mermaidLabel(label) + "\"]]";
    }

    private static String hexagon(String id, String label) {
        return id + "{{\"" + Texts.mermaidLabel(label) + "\"}}";
    }

    private static String trapezoid(String id, String label) {
        return id + "[/\"" + Texts.mermaidLabel(label) + "\"/]";
    }

    private static String decision(String id, String label) {
        return id + "{\"" + Texts.mermaidLabel(label) + "\"}";
    }

    private static String circle(String id, String label) {
        return id + "(\"" + label + "\")";
    }

    // ------------------------------------------------------------------ styling

    private static String classFor(String shape) {
        return switch (shape) {
            case "router" -> "eipRouter";
            case "fanout" -> "eipFanout";
            case "transform" -> "eipTransform";
            case "endpoint" -> "eipEndpoint";
            case "processor" -> "processor";
            case "inputPort" -> "inputPort";
            case "outputPort" -> "outputPort";
            case "group" -> "groupNode";
            case "funnel" -> "funnelNode";
            case "label" -> "labelNode";
            case "remote" -> "remoteNode";
            case "external" -> "externalNode";
            default -> null;
        };
    }

    private static String classDefs() {
        return "    classDef eipRouter fill:#e0f2fe,stroke:#0369a1,color:#0c4a6e,stroke-width:2px\n"
                + "    classDef eipFanout fill:#fef3c7,stroke:#d97706,color:#78350f,stroke-width:2px\n"
                + "    classDef eipTransform fill:#ecfccb,stroke:#65a30d,color:#365314,stroke-width:2px\n"
                + "    classDef eipEndpoint fill:#dcfce7,stroke:#16a34a,color:#14532d,stroke-width:2px\n"
                + "    classDef processor fill:#ede9fe,stroke:#7c3aed,color:#4c1d95,stroke-width:2px\n"
                + "    classDef inputPort fill:#ccfbf1,stroke:#0d9488,color:#134e4a\n"
                + "    classDef outputPort fill:#ffe4e6,stroke:#e11d48,color:#881337\n"
                + "    classDef groupNode fill:#e2e8f0,stroke:#334155,color:#0f172a,stroke-width:2px\n"
                + "    classDef funnelNode fill:#f3e8ff,stroke:#9333ea,color:#581c87\n"
                + "    classDef labelNode fill:#fef9c3,stroke:#ca8a04,color:#713f12\n"
                + "    classDef remoteNode fill:#e0e7ff,stroke:#4f46e5,color:#312e81\n"
                + "    classDef externalNode fill:#f8fafc,stroke:#64748b,color:#334155\n";
    }

    private static <T> List<T> nullSafe(List<T> list) {
        return list == null ? List.of() : list;
    }
}

/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * nifi2md - documents an Apache NiFi flow as Markdown with Mermaid diagrams.
 */
package fr.giwi.nifi2md.render;

import fr.giwi.nifi2md.eip.EipCatalog;
import fr.giwi.nifi2md.model.Connectable;
import fr.giwi.nifi2md.model.Connection;
import fr.giwi.nifi2md.model.ControllerService;
import fr.giwi.nifi2md.model.ControllerServiceApi;
import fr.giwi.nifi2md.model.Funnel;
import fr.giwi.nifi2md.model.FlowParser;
import fr.giwi.nifi2md.model.Label;
import fr.giwi.nifi2md.model.Parameter;
import fr.giwi.nifi2md.model.ParameterContext;
import fr.giwi.nifi2md.model.Port;
import fr.giwi.nifi2md.model.ProcessGroup;
import fr.giwi.nifi2md.model.Processor;
import fr.giwi.nifi2md.model.PropertyDescriptor;
import fr.giwi.nifi2md.model.RemoteProcessGroup;
import fr.giwi.nifi2md.model.Types;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Turns a parsed NiFi flow into a set of Markdown files.
 *
 * <p>The main file documents the whole flow: summary, parameter contexts,
 * controller services and a tree of links. Every process group
 * gets its own file, containing the Mermaid diagram of the group, its processors,
 * connections, ports and child group links. Files cross-reference each other with
 * relative links so a documentation site or a plain Markdown viewer can navigate it.</p>
 */
public final class MarkdownGenerator {

    /** Name of the entry point file of the generated documentation. */
    public static final String MAIN_FILE = "index.md";

    private static final String DIRECTORY = "process-groups";

    /**
     * Generation options.
     *
     * @param title              document title, defaults to the root process group name
     * @param includeDiagrams    emit Mermaid diagrams
     * @param includeProperties  emit component property tables
     * @param maxPropertyLength  truncate property values, 0 disables truncation
     * @param direction          Mermaid flowchart direction (LR, TB, RL, BT)
     */
    public record Options(
            String title,
            boolean includeDiagrams,
            boolean includeProperties,
            int maxPropertyLength,
            String direction) {

        public static Options defaults() {
            return new Options(null, true, true, 200, "LR");
        }
    }

    /**
     * The generated documentation set: entry point plus one file per content entry.
     *
     * @param mainFileName name of the entry file, e.g. {@code index.md}
     * @param files        relative path to file content, insertion ordered
     */
    public record Document(String mainFileName, Map<String, String> files) {

        public int fileCount() {
            return files.size();
        }
    }

    private final Options options;
    private final MermaidRenderer mermaid;

    public MarkdownGenerator(Options options) {
        this.options = options == null ? Options.defaults() : options;
        this.mermaid = new MermaidRenderer(this.options.direction());
    }

    private record GroupNode(
            ProcessGroup group,
            String title,
            String heading,
            String fileBase,
            GroupNode parent,
            List<GroupNode> children) {

        String relativePath() {
            return DIRECTORY + "/" + fileBase + ".md";
        }

        String linkFromMain() {
            return DIRECTORY + "/" + fileBase + ".md";
        }
    }

    // ------------------------------------------------------------------ entry point

    public Document generate(FlowParser.FlowData data, String sourceFileName) {
        Set<String> used = new LinkedHashSet<>();
        GroupNode root = buildTree(data.root(), null, null, used);

        Map<String, String> files = new LinkedHashMap<>();
        for (GroupNode node : flatten(root)) {
            files.put(node.relativePath(), groupFile(node, data, sourceFileName));
        }
        files.put(MAIN_FILE, mainFile(root, data, sourceFileName, flatten(root)));
        // Reorder so the main file comes first.
        Map<String, String> ordered = new LinkedHashMap<>();
        ordered.put(MAIN_FILE, files.get(MAIN_FILE));
        files.forEach((path, content) -> {
            if (!MAIN_FILE.equals(path)) {
                ordered.put(path, content);
            }
        });
        return new Document(MAIN_FILE, ordered);
    }

    private GroupNode buildTree(ProcessGroup group, GroupNode parent, String heading, Set<String> used) {
        String name = (group.name() == null || group.name().isBlank()) ? "Process Group" : group.name();
        String path = heading == null ? name : heading;
        String fileBase = parent == null
                ? unique(Texts.slug(name), used)
                : unique(parent.fileBase() + "-" + Texts.slug(name), used);
        GroupNode node = new GroupNode(group, name, path, fileBase, parent, new ArrayList<>());
        for (ProcessGroup child : nullSafe(group.processGroups())) {
            node.children().add(buildTree(child, node, path + " / " + childName(child), used));
        }
        return node;
    }

    private static String childName(ProcessGroup group) {
        String name = group.name();
        return (name == null || name.isBlank()) ? "Process Group" : name;
    }

    private static String unique(String candidate, Set<String> used) {
        String base = candidate.isBlank() || candidate.equals("-") ? "flow" : candidate;
        String result = base;
        int i = 1;
        while (!used.add(result)) {
            result = base + "-" + (++i);
        }
        return result;
    }

    private static List<GroupNode> flatten(GroupNode node) {
        List<GroupNode> out = new ArrayList<>();
        collect(node, out);
        return out;
    }

    private static void collect(GroupNode node, List<GroupNode> out) {
        out.add(node);
        for (GroupNode child : node.children()) {
            collect(child, out);
        }
    }

    // ------------------------------------------------------------------ main file

    private String mainFile(GroupNode root, FlowParser.FlowData data, String sourceFileName, List<GroupNode> all) {
        StringBuilder sb = new StringBuilder();
        String title = (options.title() != null && !options.title().isBlank())
                ? options.title()
                : (root.group().name() != null && !root.group().name().isBlank()
                        ? root.group().name()
                        : "NiFi Flow");

        sb.append("# ").append(title).append('\n').append('\n');
        if (root.group().comments() != null && !root.group().comments().isBlank()) {
            sb.append(root.group().comments().trim()).append('\n').append('\n');
        }
        sb.append("> Generated by **nifi2md** from `").append(sourceFileName)
                .append("` on ").append(LocalDate.now())
                .append(". Flow encoding version: ").append(data.flowEncodingVersion())
                .append(".\n\n");

        appendToc(sb, all);
        appendSummary(sb, data, root);
        appendStructure(sb, root);
        appendParameterContexts(sb, data.parameterContexts());
        appendControllerServices(sb, collectServices(root, data));
        return sb.toString();
    }

    private void appendToc(StringBuilder sb, List<GroupNode> all) {
        sb.append("## Contents\n\n");
        sb.append("- [Flow summary](#flow-summary)\n");
        sb.append("- [Process group index](#process-group-index)\n");
        sb.append("- [Parameter contexts](#parameter-contexts)\n");
        sb.append("- [Controller services](#controller-services)\n");
        sb.append("- Process groups:\n");
        for (GroupNode node : all) {
            sb.append("  - [").append(node.heading()).append("](")
                    .append(node.linkFromMain()).append(")\n");
        }
        sb.append('\n');
    }

    private void appendSummary(StringBuilder sb, FlowParser.FlowData data, GroupNode root) {
        Counts c = countAll(root.group());
        sb.append("## Flow summary\n\n");
        sb.append("| Metric | Count |\n| --- | ---: |\n");
        sb.append("| Process groups | ").append(c.processGroups).append(" |\n");
        sb.append("| Processors | ").append(c.processors).append(" |\n");
        sb.append("| Connections | ").append(c.connections).append(" |\n");
        sb.append("| Input ports | ").append(c.inputPorts).append(" |\n");
        sb.append("| Output ports | ").append(c.outputPorts).append(" |\n");
        sb.append("| Controller services | ").append(c.controllerServices
                + data.externalControllerServices().size()).append(" |\n");
        sb.append("| Remote process groups | ").append(c.remoteProcessGroups).append(" |\n");
        sb.append("| Funnels | ").append(c.funnels).append(" |\n");
        sb.append("| Canvas labels | ").append(c.labels).append(" |\n");
        sb.append("| Parameter contexts | ").append(data.parameterContexts().size()).append(" |\n");
        sb.append("| Parameter providers | ").append(data.parameterProviderCount()).append(" |\n");
        sb.append('\n');
    }

    private void appendStructure(StringBuilder sb, GroupNode root) {
        sb.append("## Process group index\n\n");
        sb.append("Every process group has its own file.\n\n");
        appendStructureTree(sb, root, 0);
        sb.append('\n');
    }

    private void appendStructureTree(StringBuilder sb, GroupNode node, int depth) {
        sb.append("- [").append(node.heading()).append("](").append(node.linkFromMain()).append(")");
        Counts c = countAll(node.group());
        sb.append(" \u2014 ").append(c.processors).append(" processors, ")
                .append(c.connections).append(" connections\n");
        for (GroupNode child : node.children()) {
            sb.append("  ".repeat(depth + 1));
            appendStructureTree(sb, child, depth + 1);
        }
    }

    // ------------------------------------------------------------------ parameter contexts

    private void appendParameterContexts(StringBuilder sb, List<ParameterContext> contexts) {
        sb.append("## Parameter contexts\n\n");
        if (contexts == null || contexts.isEmpty()) {
            sb.append("_This flow does not define any parameter context._\n\n");
            return;
        }
        for (ParameterContext ctx : contexts) {
            sb.append("### ").append(Texts.orDash(ctx.name())).append('\n').append('\n');
            if (ctx.description() != null && !ctx.description().isBlank()) {
                sb.append(ctx.description().trim()).append('\n').append('\n');
            }
            List<Parameter> params = nullSafe(ctx.parameters());
            if (params.isEmpty()) {
                sb.append("_No parameters._\n\n");
                continue;
            }
            sb.append("| Name | Value | Sensitive | Description |\n");
            sb.append("| --- | --- | :---: | --- |\n");
            for (Parameter p : params) {
                String value = p.isSensitive() ? "********" : Texts.truncate(format(p.value()), options.maxPropertyLength());
                sb.append("| ").append(Texts.markdownCell(p.name()))
                        .append(" | ").append(Texts.markdownCell(value))
                        .append(" | ").append(p.isSensitive() ? "yes" : "no")
                        .append(" | ").append(Texts.markdownCell(Texts.orDash(p.description())))
                        .append(" |\n");
            }
            sb.append('\n');
        }
    }

    // ------------------------------------------------------------------ controller services

    private record ServiceUsage(ControllerService service, String group, GroupNode node) {
    }

    private List<ServiceUsage> collectServices(GroupNode root, FlowParser.FlowData data) {
        List<ServiceUsage> out = new ArrayList<>();
        collectServices(root, out);
        for (ControllerService cs : data.externalControllerServices()) {
            out.add(new ServiceUsage(cs, "(external)", null));
        }
        return out;
    }

    private void collectServices(GroupNode node, List<ServiceUsage> out) {
        for (ControllerService cs : nullSafe(node.group().controllerServices())) {
            out.add(new ServiceUsage(cs, node.heading(), node));
        }
        for (GroupNode child : node.children()) {
            collectServices(child, out);
        }
    }

    private void appendControllerServices(StringBuilder sb, List<ServiceUsage> services) {
        sb.append("## Controller services\n\n");
        if (services.isEmpty()) {
            sb.append("_This flow does not define any controller service._\n\n");
            return;
        }
        sb.append("| Name | Type | State | APIs | Group |\n");
        sb.append("| --- | --- | --- | --- | --- |\n");
        for (ServiceUsage u : services) {
            ControllerService cs = u.service();
            sb.append("| ").append(Texts.markdownCell(Texts.orDash(cs.name())))
                    .append(" | ").append(Texts.markdownCell(cs.simpleType()))
                    .append(" | ").append(Texts.markdownCell(Texts.orDash(cs.scheduledState())))
                    .append(" | ").append(Texts.markdownCell(apiSummary(cs)))
                    .append(" | ").append(Texts.markdownCell(u.node() == null
                            ? u.group()
                            : "[" + u.group() + "](" + u.node().linkFromMain() + ")"))
                    .append(" |\n");
        }
        sb.append('\n');

        sb.append("### Controller service configuration\n\n");
        Set<String> duplicates = duplicateNames(services);
        for (ServiceUsage u : services) {
            ControllerService cs = u.service();
            sb.append("#### ").append(Texts.orDash(cs.name()));
            if (duplicates.contains(Texts.orDash(cs.name()))) {
                sb.append(" (").append(u.group()).append(')');
            }
            sb.append('\n').append('\n');
            sb.append("- **Type:** `").append(Texts.orDash(cs.type())).append("`\n");
            sb.append("- **Bundle:** ").append(cs.bundle() == null ? "-" : cs.bundle().coordinates()).append('\n');
            sb.append("- **State:** ").append(Texts.orDash(cs.scheduledState())).append('\n');
            sb.append("- **Group:** ").append(u.node() == null ? u.group()
                    : "[" + u.group() + "](" + u.node().linkFromMain() + ")").append('\n');
            if (cs.comments() != null && !cs.comments().isBlank()) {
                sb.append("- **Comments:** ").append(cs.comments().trim()).append('\n');
            }
            sb.append('\n');
            appendProperties(sb, cs.properties(), cs.propertyDescriptors());
        }
    }

    /** Names used by more than one service, so headings can be disambiguated. */
    private static Set<String> duplicateNames(List<ServiceUsage> services) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (ServiceUsage u : services) {
            String name = Texts.orDash(u.service().name());
            counts.merge(name, 1, Integer::sum);
        }
        Set<String> dup = new LinkedHashSet<>();
        counts.forEach((name, n) -> {
            if (n > 1) {
                dup.add(name);
            }
        });
        return dup;
    }

    private static String apiSummary(ControllerService cs) {        List<ControllerServiceApi> apis = nullSafe(cs.controllerServiceApis());
        List<String> out = new ArrayList<>();
        for (ControllerServiceApi api : apis) {
            out.add(Types.simpleName(api.type()));
        }
        return out.isEmpty() ? "-" : String.join(", ", out);
    }

    // ------------------------------------------------------------------ group file

    private String groupFile(GroupNode node, FlowParser.FlowData data, String sourceFileName) {
        ProcessGroup group = node.group();
        StringBuilder sb = new StringBuilder();

        sb.append("# Process Group: ").append(node.heading()).append('\n').append('\n');
        sb.append("[\u2190 Main document](").append("../").append(MAIN_FILE).append(")");
        if (node.parent() != null) {
            sb.append(" \u00b7 [Parent: ").append(node.parent().heading())
                    .append("](").append(node.parent().fileBase()).append(".md)");
        }
        sb.append('\n').append('\n');
        sb.append("> File `").append(node.relativePath()).append("` of the documentation set generated from `")
                .append(sourceFileName).append("`.\n\n");

        if (group.comments() != null && !group.comments().isBlank()) {
            sb.append(group.comments().trim()).append('\n').append('\n');
        }

        Counts c = countAll(group);
        sb.append("| Processors | Connections | Input ports | Output ports | Controller services |\n");
        sb.append("| ---: | ---: | ---: | ---: | ---: |\n");
        sb.append("| ").append(c.processors).append(" | ").append(c.connections)
                .append(" | ").append(c.inputPorts).append(" | ").append(c.outputPorts)
                .append(" | ").append(c.controllerServices).append(" |\n\n");

        if (options.includeDiagrams() && MermaidRenderer.hasDiagram(group)) {
            sb.append("## Diagram\n\n");
            sb.append("```mermaid\n");
            sb.append(mermaid.render(group));
            sb.append("```\n\n");
        }

        appendProcessors(sb, group);
        appendConnections(sb, group);
        appendPorts(sb, group);
        appendChildGroups(sb, node);
        appendLocalServices(sb, group, node);
        appendRemoteGroups(sb, group);
        appendFunnelAndLabels(sb, group);

        sb.append("---\n\n");
        sb.append("[\u2190 Back to the main document](../").append(MAIN_FILE).append(")\n");
        return sb.toString();
    }

    private void appendProcessors(StringBuilder sb, ProcessGroup group) {
        List<Processor> processors = nullSafe(group.processors());
        if (processors.isEmpty()) {
            return;
        }
        sb.append("## Processors\n\n");
        sb.append("| Name | Type | Integration pattern | Schedule | State | Details |\n");
        sb.append("| --- | --- | --- | --- | --- | --- |\n");
        for (Processor p : processors) {
            String pattern = EipCatalog.forProcessor(p.type())
                    .map(ep -> "[" + ep.name() + "](" + ep.url() + ")")
                    .orElse("\u2013");
            String schedule = Texts.orDash(p.schedulingStrategy())
                    + (isBlank(p.schedulingPeriod()) ? "" : " / " + p.schedulingPeriod());
            sb.append("| ").append(Texts.markdownCell(Texts.orDash(p.name())))
                    .append(" | `").append(Texts.markdownCell(p.simpleType())).append('`')
                    .append(" | ").append(pattern)
                    .append(" | ").append(Texts.markdownCell(schedule))
                    .append(" | ").append(Texts.markdownCell(Texts.orDash(p.scheduledState())))
                    .append(" | [open](#processor-").append(Texts.slug(Texts.orDash(p.name()))).append(") |\n");
        }
        sb.append('\n');

        if (!options.includeProperties()) {
            return;
        }
        sb.append("### Processor configuration\n\n");
        for (Processor p : processors) {
            sb.append("#### Processor: ").append(Texts.orDash(p.name())).append('\n').append('\n');
            sb.append("- **Type:** `").append(Texts.orDash(p.type())).append("`\n");
            if (p.bundle() != null) {
                sb.append("- **Bundle:** ").append(p.bundle().coordinates()).append('\n');
            }
            EipCatalog.forProcessor(p.type()).ifPresent(ep ->
                    sb.append("- **Pattern:** [" + ep.name() + "](" + ep.url() + ") \u2014 ").append(ep.summary()).append('\n'));
            sb.append("- **Schedule:** ").append(Texts.orDash(p.schedulingStrategy()))
                    .append(", period ").append(Texts.orDash(p.schedulingPeriod()))
                    .append(", state ").append(Texts.orDash(p.scheduledState())).append('\n');
            sb.append("- **Execution node:** ").append(Texts.orDash(p.executionNode()))
                    .append(", concurrent tasks ").append(p.concurrentlySchedulableTaskCount() == null
                            ? "-" : String.valueOf(p.concurrentlySchedulableTaskCount())).append('\n');
            sb.append("- **Yield / penalty:** ").append(Texts.orDash(p.yieldDuration()))
                    .append(" / ").append(Texts.orDash(p.penaltyDuration())).append('\n');
            if (p.runDurationMillis() != null) {
                sb.append("- **Run duration:** ").append(p.runDurationMillis()).append('\n');
            }
            List<String> auto = nullSafe(p.autoTerminatedRelationships());
            sb.append("- **Auto-terminated relationships:** ")
                    .append(auto.isEmpty() ? "\u2013" : Texts.markdownCell(String.join(", ", auto))).append('\n');
            if (p.comments() != null && !p.comments().isBlank()) {
                sb.append("- **Comments:** ").append(p.comments().trim()).append('\n');
            }
            sb.append('\n');
            appendProperties(sb, p.properties(), p.propertyDescriptors());
        }
    }

    private void appendProperties(StringBuilder sb,
                                  Map<String, Object> properties,
                                  Map<String, PropertyDescriptor> descriptors) {
        if (!options.includeProperties()) {
            return;
        }
        Map<String, Object> map = properties == null ? Map.of() : properties;
        if (map.isEmpty()) {
            sb.append("_No configured properties._\n\n");
            return;
        }
        sb.append("| Property | Value |\n| --- | --- |\n");
        for (Map.Entry<String, Object> e : map.entrySet()) {
            PropertyDescriptor d = descriptors == null ? null : descriptors.get(e.getKey());
            boolean sensitive = d != null && d.sensitive();
            String value = sensitive
                    ? "********"
                    : Texts.truncate(format(e.getValue()), options.maxPropertyLength());
            String name = (d != null && d.dynamic()) ? e.getKey() + " *(dynamic)*" : e.getKey();
            sb.append("| ").append(Texts.markdownCell(name))
                    .append(" | ").append(Texts.markdownCell(value)).append(" |\n");
        }
        sb.append('\n');
    }

    private void appendConnections(StringBuilder sb, ProcessGroup group) {
        List<Connection> connections = nullSafe(group.connections());
        if (connections.isEmpty()) {
            return;
        }
        sb.append("## Connections\n\n");
        sb.append("| Source | Relationship | Destination | Name | Expiration |\n");
        sb.append("| --- | --- | --- | --- | --- |\n");
        for (Connection c : connections) {
            List<String> rels = nullSafe(c.selectedRelationships());
            List<String> parts = new ArrayList<>();
            for (String r : rels) {
                if (!isBlank(r) && !parts.contains(r)) {
                    parts.add(r);
                }
            }
            String relationship = parts.isEmpty() ? "\u2013" : String.join(", ", parts);
            sb.append("| ").append(endpoint(c.source()))
                    .append(" | ").append(Texts.markdownCell(relationship))
                    .append(" | ").append(endpoint(c.destination()))
                    .append(" | ").append(Texts.markdownCell(Texts.orDash(c.name())))
                    .append(" | ").append(Texts.markdownCell(Texts.orDash(c.flowFileExpiration())))
                    .append(" |\n");
        }
        sb.append('\n');
    }

    private static String endpoint(Connectable c) {
        if (c == null) {
            return "-";
        }
        String name = isBlank(c.name()) ? c.displayType() : c.name();
        return "**" + Texts.markdownCell(c.displayType()) + ":** " + Texts.markdownCell(name);
    }

    private void appendPorts(StringBuilder sb, ProcessGroup group) {
        List<Port> inputs = nullSafe(group.inputPorts());
        List<Port> outputs = nullSafe(group.outputPorts());
        if (inputs.isEmpty() && outputs.isEmpty()) {
            return;
        }
        if (!inputs.isEmpty()) {
            sb.append("## Input ports\n\n");
            appendPortTable(sb, inputs);
        }
        if (!outputs.isEmpty()) {
            sb.append("## Output ports\n\n");
            appendPortTable(sb, outputs);
        }
    }

    private void appendPortTable(StringBuilder sb, List<Port> ports) {
        sb.append("| Name | State | Remote access | Function |\n");
        sb.append("| --- | --- | :---: | --- |\n");
        for (Port p : ports) {
            sb.append("| ").append(Texts.markdownCell(Texts.orDash(p.name())))
                    .append(" | ").append(Texts.markdownCell(Texts.orDash(p.scheduledState())))
                    .append(" | ").append(Boolean.TRUE.equals(p.allowRemoteAccess()) ? "yes" : "no")
                    .append(" | ").append(Texts.markdownCell(Texts.orDash(p.portFunction())))
                    .append(" |\n");
        }
        sb.append('\n');
    }

    private void appendChildGroups(StringBuilder sb, GroupNode node) {
        if (node.children().isEmpty()) {
            return;
        }
        sb.append("## Child process groups\n\n");
        sb.append("| Process group | Processors | Connections | Child groups | Documentation |\n");
        sb.append("| --- | ---: | ---: | ---: | --- |\n");
        for (GroupNode child : node.children()) {
            Counts c = countAll(child.group());
            sb.append("| ").append(Texts.markdownCell(child.heading()))
                    .append(" | ").append(c.processors)
                    .append(" | ").append(c.connections)
                    .append(" | ").append(child.children().size())
                    .append(" | [open](").append(child.fileBase()).append(".md) |\n");
        }
        sb.append('\n');
    }

    private void appendLocalServices(StringBuilder sb, ProcessGroup group, GroupNode node) {
        List<ControllerService> services = nullSafe(group.controllerServices());
        if (services.isEmpty()) {
            return;
        }
        sb.append("## Controller services in this group\n\n");
        sb.append("| Name | Type | State | APIs |\n");
        sb.append("| --- | --- | --- | --- |\n");
        for (ControllerService cs : services) {
            sb.append("| ").append(Texts.markdownCell(Texts.orDash(cs.name())))
                    .append(" | `").append(Texts.markdownCell(cs.simpleType())).append('`')
                    .append(" | ").append(Texts.markdownCell(Texts.orDash(cs.scheduledState())))
                    .append(" | ").append(Texts.markdownCell(apiSummary(cs)))
                    .append(" |\n");
        }
        sb.append('\n');
        sb.append("Full configuration: [controller services](../").append(MAIN_FILE)
                .append("#controller-services).\n\n");
    }

    private void appendRemoteGroups(StringBuilder sb, ProcessGroup group) {
        List<RemoteProcessGroup> rpgs = nullSafe(group.remoteProcessGroups());
        if (rpgs.isEmpty()) {
            return;
        }
        sb.append("## Remote process groups\n\n");
        sb.append("| Name | Target | Transport | Timeout |\n");
        sb.append("| --- | --- | --- | --- |\n");
        for (RemoteProcessGroup rpg : rpgs) {
            sb.append("| ").append(Texts.markdownCell(Texts.orDash(rpg.name())))
                    .append(" | ").append(Texts.markdownCell(Texts.orDash(rpg.urls())))
                    .append(" | ").append(Texts.markdownCell(Texts.orDash(rpg.transportProtocol())))
                    .append(" | ").append(Texts.markdownCell(Texts.orDash(rpg.timeout())))
                    .append(" |\n");
        }
        sb.append('\n');
    }

    private void appendFunnelAndLabels(StringBuilder sb, ProcessGroup group) {
        List<Funnel> funnels = nullSafe(group.funnels());
        List<Label> labels = nullSafe(group.labels());
        if (funnels.isEmpty() && labels.isEmpty()) {
            return;
        }
        if (!funnels.isEmpty()) {
            sb.append("## Funnels\n\n");
            sb.append("Funnels merge several incoming connections into one outgoing connection: ")
                    .append(funnels.size()).append(" funnel(s) in this group.\n\n");
        }
        if (!labels.isEmpty()) {
            sb.append("## Canvas labels\n\n");
            for (Label l : labels) {
                if (l.label() != null && !l.label().isBlank()) {
                    sb.append("- ").append(l.label().trim()).append('\n');
                }
            }
            sb.append('\n');
        }
    }

    // ------------------------------------------------------------------ helpers

    private static final class Counts {
        int processGroups;
        int processors;
        int connections;
        int inputPorts;
        int outputPorts;
        int controllerServices;
        int remoteProcessGroups;
        int funnels;
        int labels;
    }

    private Counts countAll(ProcessGroup group) {
        Counts c = new Counts();
        walk(group, c);
        return c;
    }

    private void walk(ProcessGroup group, Counts c) {
        if (group == null) {
            return;
        }
        c.processGroups++;
        c.processors += nullSafe(group.processors()).size();
        c.connections += nullSafe(group.connections()).size();
        c.inputPorts += nullSafe(group.inputPorts()).size();
        c.outputPorts += nullSafe(group.outputPorts()).size();
        c.controllerServices += nullSafe(group.controllerServices()).size();
        c.remoteProcessGroups += nullSafe(group.remoteProcessGroups()).size();
        c.funnels += nullSafe(group.funnels()).size();
        c.labels += nullSafe(group.labels()).size();
        for (ProcessGroup child : nullSafe(group.processGroups())) {
            walk(child, c);
        }
    }

    private static String format(Object value) {
        if (value == null) {
            return "-";
        }
        String s = String.valueOf(value).trim();
        return s.isEmpty() ? "-" : s;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static <T> List<T> nullSafe(List<T> list) {
        return list == null ? List.of() : list;
    }
}

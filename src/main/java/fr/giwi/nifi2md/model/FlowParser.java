/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * nifi2md - documents an Apache NiFi flow as Markdown with Mermaid diagrams.
 */
package fr.giwi.nifi2md.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Reads a NiFi flow definition JSON document and normalizes the collections whose
 * shape varies between NiFi releases (object keyed by id, or array).
 */
public final class FlowParser {

    private final ObjectMapper mapper;

    public FlowParser() {
        this.mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public FlowData parse(Path file) throws IOException {
        return normalize(mapper.readValue(file.toFile(), Flow.class));
    }

    public FlowData parse(String json) throws IOException {
        return normalize(mapper.readValue(json, Flow.class));
    }

    /**
     * A flow definition ready to be documented.
     *
     * @param flow                      the raw document
     * @param root                      the root process group (never null)
     * @param parameterContexts         all parameter contexts, order preserved
     * @param externalControllerServices controller services declared outside the group tree
     * @param parameterProviderCount    number of parameter providers, informational only
     */
    public record FlowData(
            Flow flow,
            ProcessGroup root,
            List<ParameterContext> parameterContexts,
            List<ControllerService> externalControllerServices,
            int parameterProviderCount) {

        public String flowEncodingVersion() {
            String v = flow.flowEncodingVersion();
            return v == null ? "-" : v;
        }
    }

    private FlowData normalize(Flow flow) throws IOException {
        if (flow.flowContents() == null) {
            throw new IOException("not a NiFi flow definition: missing \"flowContents\"");
        }
        List<ParameterContext> contexts = asList(flow.parameterContexts(), ParameterContext.class);
        List<ControllerService> external =
                asList(flow.externalControllerServices(), ControllerService.class);
        int providers = count(flow.parameterProviders());
        return new FlowData(flow, flow.flowContents(), contexts, external, providers);
    }

    /**
     * Accepts either an array of objects or an object whose values are objects.
     */
    private <T> List<T> asList(JsonNode node, Class<T> type) {
        List<T> out = new ArrayList<>();
        if (node == null || node.isNull()) {
            return out;
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                if (item.isObject()) {
                    out.add(mapper.convertValue(item, type));
                }
            }
        } else if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                JsonNode value = fields.next().getValue();
                if (value.isObject()) {
                    out.add(mapper.convertValue(value, type));
                }
            }
        }
        return out;
    }

    private int count(JsonNode node) {
        if (node == null || node.isNull()) {
            return 0;
        }
        return node.isArray() || node.isObject() ? node.size() : 0;
    }
}

/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * nifi2md - documents an Apache NiFi flow as Markdown with Mermaid diagrams.
 */
package fr.giwi.nifi2md.eip;

import fr.giwi.nifi2md.model.Types;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Maps Apache NiFi processor types onto Enterprise Integration Patterns.
 *
 * <p>Two mapping strategies are combined:</p>
 * <ol>
 *   <li>an explicit table keyed by the fully qualified processor class name, which is exact,</li>
 *   <li>a keyword fallback applied to the simple class name for processors we do not know,
 *       so documentation still labels well known roles (splitter, poller, translator...).</li>
 * </ol>
 */
public final class EipCatalog {

    private EipCatalog() {
    }

    private static final Map<String, EipPattern> PATTERNS = new LinkedHashMap<>();

    private static void reg(String id, String name, String category, String page, String summary) {
        PATTERNS.put(id, new EipPattern(id, name, category, page, summary));
    }

    static {
        // --- Message Routing -------------------------------------------------
        reg("pipes-and-filters", "Pipes and Filters", "Message Routing", "PipesAndFilters.html",
                "Splits a task into a chain of independent stages connected by channels.");
        reg("content-based-router", "Content-Based Router", "Message Routing", "ContentBasedRouter.html",
                "Inspects message content and routes it to exactly one of several alternative paths.");
        reg("message-filter", "Message Filter", "Message Routing", "Filter.html",
                "Passes through messages that match a criterion and discards the rest.");
        reg("recipient-list", "Recipient List", "Message Routing", "RecipientList.html",
                "Sends the same message to a list of recipients derived from its content or configuration.");
        reg("splitter", "Splitter", "Message Routing", "Sequencer.html",
                "Breaks a composite message into several individual messages.");
        reg("aggregator", "Aggregator", "Message Routing", "Aggregator.html",
                "Collects related messages and combines them back into a single message.");
        reg("resequencer", "Resequencer", "Message Routing", "Resequencer.html",
                "Reorders a stream of messages so they arrive in a meaningful sequence.");
        reg("routing-slip", "Routing Slip", "Message Routing", "RoutingTable.html",
                "Determines the message path dynamically, one step at a time, based on the message itself.");
        reg("process-manager", "Process Manager", "Message Routing", "ProcessManager.html",
                "Keeps track of a longer business process and tells each message what to do next.");

        // --- Message Transformation -----------------------------------------
        reg("message-translator", "Message Translator", "Message Transformation", "MessageTranslator.html",
                "Converts a message from one format or structure into another.");
        reg("content-enricher", "Content Enricher", "Message Transformation", "DataEnricher.html",
                "Augments a message with additional data that was not present in the original.");
        reg("content-filter", "Content Filter", "Message Transformation", "ContentFilter.html",
                "Removes or replaces parts of a message before it is consumed downstream.");
        reg("envelope-wrapper", "Envelope Wrapper", "Message Transformation", "EnvelopeWrapper.html",
                "Wraps the payload so its original format survives transport through an alien channel.");
        reg("normalizer", "Normalizer", "Message Transformation", "Normalizer.html",
                "Converts different message variants of the same business event into a canonical form.");
        reg("claim-check", "Claim Check", "Message Transformation", "StoreInLibrary.html",
                "Stores a large payload elsewhere and lets the message carry a claim check key instead.");

        // --- Messaging Channels ----------------------------------------------
        reg("message-channel", "Message Channel", "Messaging Channels", "MessageChannel.html",
                "A logical pathway connecting senders and receivers of messages.");
        reg("dead-letter-channel", "Dead Letter Channel", "Messaging Channels", "DeadLetterChannel.html",
                "Captures messages that could not be processed so they can be handled separately.");
        reg("invalid-message-channel", "Invalid Message Channel", "Messaging Channels", "InvalidMessageChannel.html",
                "Routes messages that fail validation to a dedicated inspection queue.");
        reg("channel-adapter", "Channel Adapter", "Messaging Channels", "ChannelAdapter.html",
                "Connects a messaging channel to an external system so both can talk to each other.");
        reg("guaranteed-delivery", "Guaranteed Delivery", "Messaging Channels", "GuaranteedMessaging.html",
                "Makes sure a message is not lost until the receiver acknowledges it.");

        // --- Message Construction --------------------------------------------
        reg("request-reply", "Request-Reply", "Message Construction", "RequestReply.html",
                "Combines a request message with a correlated reply, making an asynchronous call feel synchronous.");
        reg("return-address", "Return Address", "Message Construction", "ReturnAddress.html",
                "Puts the reply channel inside the request so the receiver knows where to answer.");

        // --- Message Endpoints ------------------------------------------------
        reg("message-gateway", "Messaging Gateway", "Message Endpoints", "MessagingGateway.html",
                "Lets application code call an integration API while hiding the messaging plumbing behind it.");
        reg("service-activator", "Service Activator", "Message Endpoints", "MessagingAdapter.html",
                "Attaches an application service to a channel so it can be invoked by messages.");
        reg("polling-consumer", "Polling Consumer", "Message Endpoints", "PollingConsumer.html",
                "Pulls messages from a channel whenever the application is ready to handle them.");
        reg("event-driven-consumer", "Event-Driven Consumer", "Message Endpoints", "EventDrivenConsumer.html",
                "Is pushed messages by the channel as soon as they arrive.");
        reg("transactional-client", "Transactional Client", "Message Endpoints", "TransactionalClient.html",
                "Keeps message operations inside a transaction so failures do not leave partial state.");

        // --- System Management ------------------------------------------------
        reg("wire-tap", "Wire Tap", "System Management", "WireTap.html",
                "Taps a message flow and sends a copy to a monitoring path without disturbing the main path.");
        reg("message-history", "Message History", "System Management", "MessageHistory.html",
                "Records the route a message took for diagnostics and troubleshooting.");
        reg("idempotent-receiver", "Idempotent Receiver", "System Management", "IdempotentReceiver.html",
                "Filters out duplicate messages so processing them twice has no extra effect.");
        reg("test-message", "Test Message", "System Management", "TestMessage.html",
                "Examines a message to decide whether it is worth routing further.");
        reg("message-store", "Message Store", "System Management", "MessageStore.html",
                "Persists messages so another process can pick them up later.");

        // --- Integration Styles ------------------------------------------------
        reg("file-transfer", "File Transfer", "Integration Styles", "FileTransferIntegration.html",
                "Exchanges data by dropping files in a shared location instead of messaging directly.");
        reg("shared-database", "Shared Database", "Integration Styles", "SharedDataBaseIntegration.html",
                "Applications integrate by reading and writing the same database.");
        reg("remote-procedure-invocation", "Remote Procedure Invocation", "Integration Styles",
                "EncapsulatedSynchronousIntegration.html",
                "Application A invokes a method directly on application B over the network.");
    }

    /**
     * Explicit mapping: fully qualified NiFi processor class name to pattern id.
     */
    private static final Map<String, String> BY_TYPE = new LinkedHashMap<>();

    private static void map(String processorType, String patternId) {
        BY_TYPE.put(processorType, patternId);
    }

    static {
        // --- Routing ---------------------------------------------------------
        map("org.apache.nifi.processors.standard.RouteOnAttribute", "content-based-router");
        map("org.apache.nifi.processors.standard.RouteOnProperty", "content-based-router");
        map("org.apache.nifi.processors.standard.RouteToAttribute", "routing-slip");
        map("org.apache.nifi.processors.standard.DistributeFlowFile", "recipient-list");
        map("org.apache.nifi.processors.standard.ScanAttribute", "message-filter");
        map("org.apache.nifi.processors.attributes.UpdateAttribute", "content-enricher");
        map("org.apache.nifi.processors.standard.SplitRecord", "splitter");
        map("org.apache.nifi.processors.standard.SplitText", "splitter");
        map("org.apache.nifi.processors.standard.SplitJson", "splitter");
        map("org.apache.nifi.processors.standard.SplitXml", "splitter");
        map("org.apache.nifi.processors.standard.MergeRecord", "aggregator");
        map("org.apache.nifi.processors.standard.MergeContent", "aggregator");
        map("org.apache.nifi.processors.standard.Resequencer", "resequencer");

        // --- Transformation ---------------------------------------------------
        map("org.apache.nifi.processors.standard.ReplaceText", "message-translator");
        map("org.apache.nifi.processors.standard.JoltTransformJSON", "message-translator");
        map("org.apache.nifi.processors.standard.EvaluateXPath", "message-translator");
        map("org.apache.nifi.processors.standard.EvaluateXSLT", "message-translator");
        map("org.apache.nifi.processors.standard.QueryRecord", "message-translator");
        map("org.apache.nifi.processors.standard.UpdateRecord", "content-enricher");
        map("org.apache.nifi.processors.standard.TransformRecord", "message-translator");
        map("org.apache.nifi.processors.standard.EncryptContent", "envelope-wrapper");
        map("org.apache.nifi.processors.standard.DecryptContent", "envelope-wrapper");
        map("org.apache.nifi.processors.standard.NormalizeContent", "normalizer");

        // --- Validation --------------------------------------------------------
        map("org.apache.nifi.processors.standard.ValidateRecord", "test-message");
        map("org.apache.nifi.processors.standard.ValidateContent", "test-message");
        map("org.apache.nifi.processors.standard.DetectDuplicate", "idempotent-receiver");
        map("org.apache.nifi.processors.standard.RetryFlowFile", "guaranteed-delivery");

        // --- Endpoints and adapters -------------------------------------------
        map("org.apache.nifi.processors.standard.HandleHttpRequest", "message-gateway");
        map("org.apache.nifi.processors.standard.HandleHttpResponse", "return-address");
        map("org.apache.nifi.processors.standard.InvokeHTTP", "request-reply");
        map("org.apache.nifi.processors.standard.InvokeHTTPv2", "request-reply");
        map("org.apache.nifi.processors.standard.ListenHTTP", "channel-adapter");
        map("org.apache.nifi.processors.standard.ListenAndServeFTP", "channel-adapter");
        map("org.apache.nifi.processors.standard.GetFile", "polling-consumer");
        map("org.apache.nifi.processors.standard.ListenFile", "polling-consumer");
        map("org.apache.nifi.processors.standard.FetchFile", "polling-consumer");
        map("org.apache.nifi.processors.standard.GetFTP", "polling-consumer");
        map("org.apache.nifi.processors.standard.PutFile", "file-transfer");
        map("org.apache.nifi.processors.standard.PutFTP", "file-transfer");
        map("org.apache.nifi.processors.standard.PutDatabaseRecord", "shared-database");
        map("org.apache.nifi.processors.standard.SelectSQL", "shared-database");
        map("org.apache.nifi.processors.standard.ExecuteSQL", "shared-database");
        map("org.apache.nifi.processors.standard.ExecuteSQLRecord", "shared-database");
        map("org.apache.nifi.processors.standard.ExecuteProcess", "service-activator");
        map("org.apache.nifi.processors.standard.ExecuteGroovyScript", "service-activator");
        map("org.apache.nifi.processors.groovyx.ExecuteGroovyScript", "service-activator");
        map("org.apache.nifi.processors.standard.ExecuteScript", "service-activator");
        map("org.apache.nifi.processors.standard.LookupRecord", "content-enricher");
        map("org.apache.nifi.processors.standard.SegmentContent", "splitter");
        map("org.apache.nifi.processors.standard.CombineContent", "aggregator");

        // --- Logging and diagnostics -------------------------------------------
        map("org.apache.nifi.processors.standard.LogAttribute", "wire-tap");
        map("org.apache.nifi.processors.standard.LogMessage", "wire-tap");
        map("org.apache.nifi.processors.standard.CaptureAttribute", "message-history");
        map("org.apache.nifi.processors.standard.PurgeHistory", "message-history");
    }

    private static final Map<String, String> FALLBACK_RULES = new LinkedHashMap<>();

    private static void rule(String keyword, String patternId) {
        FALLBACK_RULES.put(keyword, patternId);
    }

    static {
        rule("rout", "content-based-router");
        rule("distribut", "recipient-list");
        rule("split", "splitter");
        rule("merge", "aggregator");
        rule("aggregate", "aggregator");
        rule("resequence", "resequencer");
        rule("filter", "message-filter");
        rule("validate", "test-message");
        rule("duplicate", "idempotent-receiver");
        rule("replace", "message-translator");
        rule("transform", "message-translator");
        rule("convert", "message-translator");
        rule("evaluat", "message-translator");
        rule("query", "message-translator");
        rule("jolt", "message-translator");
        rule("encrypt", "envelope-wrapper");
        rule("decrypt", "envelope-wrapper");
        rule("normaliz", "normalizer");
        rule("update", "content-enricher");
        rule("enrich", "content-enricher");
        rule("lookup", "content-enricher");
        rule("record", "message-translator");
        rule("invok", "request-reply");
        rule("request", "request-reply");
        rule("respond", "return-address");
        rule("handlehttp", "message-gateway");
        rule("gateway", "message-gateway");
        rule("listen", "polling-consumer");
        rule("consume", "event-driven-consumer");
        rule("poll", "polling-consumer");
        rule("fetch", "polling-consumer");
        rule("getfile", "polling-consumer");
        rule("putfile", "file-transfer");
        rule("put", "channel-adapter");
        rule("publish", "channel-adapter");
        rule("send", "channel-adapter");
        rule("script", "service-activator");
        rule("process", "service-activator");
        rule("log", "wire-tap");
        rule("history", "message-history");
    }

    /**
     * Resolves the pattern implemented by a processor type.
     *
     * @param processorType fully qualified NiFi processor class name, may be {@code null}
     */
    public static Optional<EipPattern> forProcessor(String processorType) {
        if (processorType == null || processorType.isBlank()) {
            return Optional.empty();
        }
        String id = BY_TYPE.get(processorType);
        if (id != null) {
            return Optional.of(PATTERNS.get(id));
        }
        String simple = Types.simpleName(processorType).toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> e : FALLBACK_RULES.entrySet()) {
            if (simple.contains(e.getKey())) {
                return Optional.of(PATTERNS.get(e.getValue()));
            }
        }
        return Optional.empty();
    }

    /** Every pattern in the catalogue, in book order. */
    public static Map<String, EipPattern> patterns() {
        return Collections.unmodifiableMap(PATTERNS);
    }

    /** Lookup by id, e.g. {@code content-based-router}. */
    public static Optional<EipPattern> byId(String id) {
        return Optional.ofNullable(PATTERNS.get(id));
    }
}

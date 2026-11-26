package groq4j.models.chat;

import groq4j.enums.ServiceTier;
import groq4j.models.common.Usage;
import groq4j.models.common.XGroq;

import java.util.List;
import java.util.Optional;

/**
 * A non streaming chat completion response.
 *
 * <p>{@code usage} is optional because it is absent on some responses, and {@code xGroq} carries
 * the Groq request id used with support.
 */
public record ChatCompletionResponse(
    String id,
    String object,
    long created,
    String model,
    List<Choice> choices,
    Optional<Usage> usage,
    Optional<String> systemFingerprint,
    Optional<XGroq> xGroq,
    Optional<ServiceTier> serviceTier,
    List<ExecutedTool> executedTools,
    List<McpToolList> mcpToolLists
) {
    public ChatCompletionResponse(String id, String object, long created, String model,
                                  List<Choice> choices, Optional<Usage> usage,
                                  Optional<String> systemFingerprint, Optional<XGroq> xGroq,
                                  Optional<ServiceTier> serviceTier) {
        this(id, object, created, model, choices, usage, systemFingerprint, xGroq, serviceTier,
            List.of(), List.of());
    }

    public ChatCompletionResponse {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("id cannot be null or empty");
        }
        if (object == null || object.trim().isEmpty()) {
            throw new IllegalArgumentException("object cannot be null or empty");
        }
        if (model == null || model.trim().isEmpty()) {
            throw new IllegalArgumentException("model cannot be null or empty");
        }
        if (choices == null || choices.isEmpty()) {
            throw new IllegalArgumentException("choices cannot be null or empty");
        }
    }

    public String getFirstContent() {
        return choices.isEmpty() ? "" : choices.getFirst().getContent();
    }

    public Optional<Usage> getUsage() {
        return usage;
    }

    public boolean hasToolCalls() {
        return choices.stream().anyMatch(Choice::hasToolCalls);
    }

    public boolean hasExecutedTools() {
        return !executedTools.isEmpty();
    }

    public String getRequestId() {
        return xGroq.map(XGroq::id).orElse(null);
    }
}

package groq4j.models.chat;

import groq4j.enums.ServiceTier;
import groq4j.models.common.Usage;
import groq4j.models.common.XGroq;

import java.util.List;
import java.util.Optional;

/**
 * One server-sent event of a streamed chat completion. Concatenate {@code delta().content()}
 * across chunks to rebuild the answer; the last chunk carries usage when
 * {@code stream_options.include_usage} is set.
 */
public record ChatCompletionChunk(
    String id,
    String object,
    long created,
    String model,
    List<ChunkChoice> choices,
    Optional<String> systemFingerprint,
    Optional<Usage> usage,
    Optional<XGroq> xGroq,
    Optional<ServiceTier> serviceTier
) {
    public String getContent() {
        return choices.stream()
            .map(choice -> choice.delta().content().orElse(""))
            .reduce("", String::concat);
    }

    public String getReasoning() {
        return choices.stream()
            .map(choice -> choice.delta().reasoning().orElse(""))
            .reduce("", String::concat);
    }

    public boolean hasToolCalls() {
        return choices.stream().anyMatch(choice -> !choice.delta().toolCalls().isEmpty());
    }

    public boolean isFinal() {
        return choices.stream().anyMatch(choice -> choice.finishReason().isPresent());
    }
}

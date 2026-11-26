package groq4j.models.chat;

import groq4j.enums.CitationOptions;
import groq4j.enums.ReasoningEffort;
import groq4j.enums.ReasoningFormat;
import groq4j.enums.ServiceTier;
import groq4j.models.common.Message;
import groq4j.models.common.Tool;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A chat completion request. Fields map one to one onto the documented request body of
 * {@code POST /openai/v1/chat/completions}.
 */
public record ChatCompletionRequest(
    List<Message> messages,
    String model,
    Optional<Double> frequencyPenalty,
    Optional<Map<String, Integer>> logitBias,
    Optional<Boolean> logprobs,
    Optional<Integer> maxCompletionTokens,
    Optional<Integer> n,
    Optional<Boolean> parallelToolCalls,
    Optional<Double> presencePenalty,
    Optional<ReasoningEffort> reasoningEffort,
    Optional<ReasoningFormat> reasoningFormat,
    Optional<JsonSchemaResponseFormat> responseFormat,
    Optional<SearchSettings> searchSettings,
    Optional<Integer> seed,
    Optional<ServiceTier> serviceTier,
    Optional<List<String>> stop,
    Optional<Double> temperature,
    Optional<ToolChoice> toolChoice,
    Optional<List<Tool>> tools,
    Optional<Integer> topLogprobs,
    Optional<Double> topP,
    Optional<String> user,
    Optional<CitationOptions> citationOptions,
    Optional<List<ChatCompletionDocument>> documents,
    Optional<Boolean> disableToolValidation,
    Optional<Boolean> includeReasoning,
    Optional<Map<String, String>> metadata
) {
    public ChatCompletionRequest {
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("messages cannot be null or empty");
        }
        if (model == null || model.trim().isEmpty()) {
            throw new IllegalArgumentException("model cannot be null or empty");
        }
        if (includeReasoning.orElse(false) && reasoningFormat.isPresent()) {
            throw new IllegalArgumentException("includeReasoning and reasoningFormat are mutually exclusive");
        }

        frequencyPenalty.ifPresent(value -> validateRange(value, -2.0, 2.0, "frequencyPenalty"));
        presencePenalty.ifPresent(value -> validateRange(value, -2.0, 2.0, "presencePenalty"));
        temperature.ifPresent(value -> validateRange(value, 0.0, 2.0, "temperature"));
        topP.ifPresent(value -> validateRange(value, 0.0, 1.0, "topP"));

        n.ifPresent(value -> {
            if (value != 1) {
                throw new IllegalArgumentException("n must be 1 (only supported value)");
            }
        });

        topLogprobs.ifPresent(value -> {
            if (value < 0 || value > 20) {
                throw new IllegalArgumentException("topLogprobs must be between 0 and 20");
            }
        });

        maxCompletionTokens.ifPresent(value -> {
            if (value < 1) {
                throw new IllegalArgumentException("maxCompletionTokens must be positive");
            }
        });

        stop.ifPresent(sequences -> {
            if (sequences.size() > 4) {
                throw new IllegalArgumentException("stop can contain at most 4 sequences");
            }
        });

        tools.ifPresent(list -> {
            if (list.size() > 128) {
                throw new IllegalArgumentException("tools can contain at most 128 functions");
            }
        });

        documents.ifPresent(list -> list.forEach(document -> {
            if (document.type() == null) {
                throw new IllegalArgumentException("document type is required");
            }
        }));

        metadata.ifPresent(map -> {
            if (map.size() > 16) {
                throw new IllegalArgumentException("metadata can contain at most 16 pairs");
            }
        });
    }

    private static void validateRange(double value, double min, double max, String field) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(field + " must be between " + min + " and " + max);
        }
    }
}

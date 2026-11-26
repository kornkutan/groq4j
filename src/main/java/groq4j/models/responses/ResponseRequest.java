package groq4j.models.responses;

import groq4j.enums.ReasoningEffort;
import groq4j.enums.ServiceTier;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A Responses API request. {@code input} is either a single string or a list of input items.
 * Groq does not support {@code previous_response_id} or {@code store}, so send the full history
 * on every call.
 */
public record ResponseRequest(
    String model,
    Optional<String> inputText,
    List<ResponseInputItem> inputItems,
    Optional<String> instructions,
    Optional<Integer> maxOutputTokens,
    Optional<Map<String, String>> metadata,
    Optional<Boolean> parallelToolCalls,
    Optional<ReasoningEffort> reasoningEffort,
    Optional<ServiceTier> serviceTier,
    Optional<Double> temperature,
    Optional<ResponseTextFormat> textFormat,
    Optional<Object> toolChoice,
    List<ResponseTool> tools,
    Optional<Double> topP,
    Optional<String> truncation,
    Optional<String> user
) {
    public ResponseRequest {
        if (model == null || model.trim().isEmpty()) {
            throw new IllegalArgumentException("model cannot be null or empty");
        }
        if (inputText.isEmpty() && inputItems.isEmpty()) {
            throw new IllegalArgumentException("input text or input items are required");
        }
        if (inputText.isPresent() && !inputItems.isEmpty()) {
            throw new IllegalArgumentException("provide either input text or input items, not both");
        }
        temperature.ifPresent(value -> validateRange(value, 0.0, 2.0, "temperature"));
        topP.ifPresent(value -> validateRange(value, 0.0, 1.0, "topP"));
        metadata.ifPresent(value -> {
            if (value.size() > 16) {
                throw new IllegalArgumentException("metadata can contain at most 16 pairs");
            }
        });
        if (tools.size() > 128) {
            throw new IllegalArgumentException("tools can contain at most 128 functions");
        }
    }

    private static void validateRange(double value, double min, double max, String field) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(field + " must be between " + min + " and " + max);
        }
    }
}

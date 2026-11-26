package groq4j.models.embeddings;

import groq4j.enums.EmbeddingEncodingFormat;

import java.util.List;
import java.util.Optional;

/**
 * An embeddings request. {@code input} is either one string or up to 2048 strings per call, and
 * each input must stay within the model's context window.
 */
public record EmbeddingRequest(
    String model,
    List<String> input,
    Optional<EmbeddingEncodingFormat> encodingFormat,
    Optional<String> user
) {
    public EmbeddingRequest {
        if (model == null || model.trim().isEmpty()) {
            throw new IllegalArgumentException("model cannot be null or empty");
        }
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("input cannot be null or empty");
        }
        if (input.size() > 2048) {
            throw new IllegalArgumentException("input can contain at most 2048 entries");
        }
        input.forEach(text -> {
            if (text == null || text.isEmpty()) {
                throw new IllegalArgumentException("input entries cannot be null or empty");
            }
        });
    }

    public static EmbeddingRequest of(String model, String input) {
        return new EmbeddingRequest(model, List.of(input), Optional.empty(), Optional.empty());
    }
}

package groq4j.models.embeddings;

import java.util.Optional;

/**
 * One embedding vector. The vector is either a float array ({@code encoding_format=float}) or a
 * base64 string ({@code encoding_format=base64}); exactly one of the two is present.
 */
public record Embedding(int index, String object, Optional<double[]> vector, Optional<String> base64) {

    public int dimensions() {
        return vector.map(v -> v.length).orElse(0);
    }
}

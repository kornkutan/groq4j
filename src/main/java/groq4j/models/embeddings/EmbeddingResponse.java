package groq4j.models.embeddings;

import java.util.List;

/** The result of {@code POST /openai/v1/embeddings}. */
public record EmbeddingResponse(String object, String model, List<Embedding> data,
                                int promptTokens, int totalTokens) {

    public Embedding getFirst() {
        return data.isEmpty() ? null : data.getFirst();
    }
}

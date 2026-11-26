package groq4j.services;

import groq4j.models.embeddings.EmbeddingRequest;
import groq4j.models.embeddings.EmbeddingResponse;

/**
 * Text embeddings at {@code POST /openai/v1/embeddings}, using {@code nomic-embed-text-v1_5}.
 */
public interface EmbeddingsService {

    /**
     * Creates embeddings for one or more inputs.
     *
     * @param request the request, see {@link groq4j.builders.EmbeddingRequestBuilder}
     * @return the vectors in input order
     */
    EmbeddingResponse createEmbeddings(EmbeddingRequest request);

    /**
     * Convenience call for a single input.
     *
     * @param model the embedding model id
     * @param input the text to embed
     * @return the response, usually holding one vector
     */
    EmbeddingResponse embed(String model, String input);
}

package groq4j.services;

import groq4j.models.reranking.RerankRequest;
import groq4j.models.reranking.RerankResponse;

/**
 * Document reranking at {@code POST /openai/v1/reranking}, using {@code qwen3-reranker-4b}.
 */
public interface RerankingService {

    /**
     * Scores up to 100 documents against a query.
     *
     * @param request the request, see {@link groq4j.builders.RerankRequestBuilder}
     * @return the documents sorted by descending relevance
     */
    RerankResponse rerank(RerankRequest request);
}

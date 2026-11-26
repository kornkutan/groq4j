package groq4j.models.reranking;

import java.util.List;

/** The result of {@code POST /openai/v1/reranking}, sorted by descending relevance. */
public record RerankResponse(List<RerankResult> results) {
}

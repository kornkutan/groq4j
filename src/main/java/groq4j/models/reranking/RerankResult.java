package groq4j.models.reranking;

/** One scored document. Scores run from 0.0 to 1.0, higher means more relevant. */
public record RerankResult(String document, double score) {
}

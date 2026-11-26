package groq4j.models.reranking;

import java.util.List;
import java.util.Optional;

/** Reranking request: score documents against one query, best first in the response. */
public record RerankRequest(String model, String query, List<String> documents, Optional<String> instruction) {
    public RerankRequest {
        if (model == null || model.trim().isEmpty()) {
            throw new IllegalArgumentException("model cannot be null or empty");
        }
        if (query == null || query.isEmpty()) {
            throw new IllegalArgumentException("query cannot be null or empty");
        }
        if (documents == null || documents.isEmpty()) {
            throw new IllegalArgumentException("documents cannot be null or empty");
        }
        if (documents.size() > 100) {
            throw new IllegalArgumentException("documents can contain at most 100 entries");
        }
        documents.forEach(doc -> {
            if (doc == null || doc.isEmpty()) {
                throw new IllegalArgumentException("documents entries cannot be null or empty");
            }
        });
    }
}

package groq4j.builders;

import groq4j.models.reranking.RerankRequest;

import java.util.List;
import java.util.Optional;

public class RerankRequestBuilder {
    public static final String DEFAULT_MODEL = "qwen3-reranker-4b";

    private String model = DEFAULT_MODEL;
    private String query;
    private List<String> documents;
    private String instruction;

    public static RerankRequestBuilder create() {
        return new RerankRequestBuilder();
    }

    public static RerankRequestBuilder create(String model) {
        var builder = new RerankRequestBuilder();
        builder.model = model;
        return builder;
    }

    public RerankRequestBuilder model(String model) {
        this.model = model;
        return this;
    }

    public RerankRequestBuilder query(String query) {
        this.query = query;
        return this;
    }

    public RerankRequestBuilder documents(List<String> documents) {
        this.documents = documents;
        return this;
    }

    public RerankRequestBuilder documents(String... documents) {
        this.documents = List.of(documents);
        return this;
    }

    public RerankRequestBuilder instruction(String instruction) {
        this.instruction = instruction;
        return this;
    }

    public RerankRequest build() {
        return new RerankRequest(model, query, documents, Optional.ofNullable(instruction));
    }
}

package groq4j.builders;

import groq4j.enums.EmbeddingEncodingFormat;
import groq4j.models.embeddings.EmbeddingRequest;

import java.util.List;
import java.util.Optional;

public class EmbeddingRequestBuilder {
    public static final String DEFAULT_MODEL = "nomic-embed-text-v1_5";

    private String model = DEFAULT_MODEL;
    private List<String> input;
    private EmbeddingEncodingFormat encodingFormat;
    private String user;

    public static EmbeddingRequestBuilder create() {
        return new EmbeddingRequestBuilder();
    }

    public static EmbeddingRequestBuilder create(String model) {
        var builder = new EmbeddingRequestBuilder();
        builder.model = model;
        return builder;
    }

    public EmbeddingRequestBuilder model(String model) {
        this.model = model;
        return this;
    }

    public EmbeddingRequestBuilder input(String input) {
        this.input = List.of(input);
        return this;
    }

    public EmbeddingRequestBuilder input(List<String> input) {
        this.input = input;
        return this;
    }

    public EmbeddingRequestBuilder encodingFormat(EmbeddingEncodingFormat encodingFormat) {
        this.encodingFormat = encodingFormat;
        return this;
    }

    public EmbeddingRequestBuilder base64Encoding() {
        return encodingFormat(EmbeddingEncodingFormat.BASE64);
    }

    public EmbeddingRequestBuilder user(String user) {
        this.user = user;
        return this;
    }

    public EmbeddingRequest build() {
        return new EmbeddingRequest(model, input,
            Optional.ofNullable(encodingFormat), Optional.ofNullable(user));
    }
}

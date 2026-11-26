package groq4j.services;

import groq4j.enums.EmbeddingEncodingFormat;
import groq4j.enums.HttpMethod;
import groq4j.exceptions.GroqSerializationException;
import groq4j.models.embeddings.Embedding;
import groq4j.models.embeddings.EmbeddingRequest;
import groq4j.models.embeddings.EmbeddingResponse;
import groq4j.utils.Constants;
import groq4j.utils.HttpUtils;
import groq4j.utils.JsonUtils;
import groq4j.utils.UrlUtils;
import groq4j.utils.ValidationUtils;

import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Implementation of the embeddings endpoint. */
public class EmbeddingsServiceImpl implements EmbeddingsService {
    private final HttpClient httpClient;
    private final String apiKey;
    private final String baseUrl;

    public EmbeddingsServiceImpl(String apiKey) {
        this(HttpUtils.createHttpClient(), apiKey, Constants.BASE_URL);
    }

    public EmbeddingsServiceImpl(HttpClient httpClient, String apiKey) {
        this(httpClient, apiKey, Constants.BASE_URL);
    }

    public EmbeddingsServiceImpl(HttpClient httpClient, String apiKey, String baseUrl) {
        ValidationUtils.requireNonNull(httpClient, "httpClient");
        this.httpClient = httpClient;
        this.apiKey = apiKey;
        ValidationUtils.validateApiKey(apiKey);
        this.baseUrl = baseUrl;
    }

    public static EmbeddingsService create(String apiKey) {
        return new EmbeddingsServiceImpl(apiKey);
    }

    public static EmbeddingsService create(HttpClient httpClient, String apiKey) {
        return new EmbeddingsServiceImpl(httpClient, apiKey);
    }

    public static EmbeddingsService create(HttpClient httpClient, String apiKey, String baseUrl) {
        return new EmbeddingsServiceImpl(httpClient, apiKey, baseUrl);
    }

    @Override
    public EmbeddingResponse createEmbeddings(EmbeddingRequest request) {
        ValidationUtils.requireNonNull(request, "request");

        var body = new LinkedHashMap<String, Object>();
        body.put("model", request.model());
        body.put("input", request.input().size() == 1 ? request.input().getFirst() : request.input());
        request.encodingFormat().ifPresent(v -> body.put("encoding_format", v.getValue()));
        request.user().ifPresent(v -> body.put("user", v));

        var httpRequest = HttpUtils.createJsonRequest(
            UrlUtils.buildEmbeddingsUrl(baseUrl), apiKey, HttpMethod.POST,
            JsonUtils.toJsonString(body));

        return HttpUtils.await(HttpUtils.executeRequest(httpClient, httpRequest)
                .thenApply(this::parseResponse));
    }

    @Override
    public EmbeddingResponse embed(String model, String input) {
        return createEmbeddings(EmbeddingRequest.of(model, input));
    }

    EmbeddingResponse parseResponse(String json) {
        try {
            Map<String, Object> root = JsonUtils.parseObject(json);
            List<Embedding> embeddings = new ArrayList<>();

            for (Object raw : JsonUtils.asList(root.get("data"))) {
                Map<String, Object> item = JsonUtils.asMap(raw);
                Optional<double[]> vector = Optional.empty();
                Optional<String> base64 = Optional.empty();

                if (item.get("embedding") instanceof List<?> values) {
                    double[] parsed = new double[values.size()];
                    for (int i = 0; i < values.size(); i++) {
                        Double value = JsonUtils.doubleValue(values.get(i));
                        parsed[i] = value == null ? 0.0 : value;
                    }
                    vector = Optional.of(parsed);
                } else if (item.get("embedding") instanceof String encoded) {
                    base64 = Optional.of(encoded);
                }

                embeddings.add(new Embedding(
                    JsonUtils.extractIntValuePrimitive(JsonUtils.toJsonString(item), "index"),
                    JsonUtils.stringValue(item.get("object")),
                    vector,
                    base64));
            }

            Map<String, Object> usage = JsonUtils.asMap(root.get("usage"));
            return new EmbeddingResponse(
                JsonUtils.stringValue(root.get("object")),
                JsonUtils.stringValue(root.get("model")),
                embeddings,
                JsonUtils.doubleValue(usage.get("prompt_tokens")) == null
                    ? 0 : JsonUtils.doubleValue(usage.get("prompt_tokens")).intValue(),
                JsonUtils.doubleValue(usage.get("total_tokens")) == null
                    ? 0 : JsonUtils.doubleValue(usage.get("total_tokens")).intValue());
        } catch (Exception e) {
            throw e instanceof GroqSerializationException serialization
                ? serialization
                : new GroqSerializationException("Failed to parse embeddings response: " + e.getMessage(), e);
        }
    }
}

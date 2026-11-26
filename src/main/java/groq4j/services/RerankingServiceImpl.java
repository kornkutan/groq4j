package groq4j.services;

import groq4j.enums.HttpMethod;
import groq4j.exceptions.GroqSerializationException;
import groq4j.models.reranking.RerankRequest;
import groq4j.models.reranking.RerankResponse;
import groq4j.models.reranking.RerankResult;
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

/** Implementation of the reranking endpoint. */
public class RerankingServiceImpl implements RerankingService {
    private final HttpClient httpClient;
    private final String apiKey;
    private final String baseUrl;

    public RerankingServiceImpl(String apiKey) {
        this(HttpUtils.createHttpClient(), apiKey, Constants.BASE_URL);
    }

    public RerankingServiceImpl(HttpClient httpClient, String apiKey) {
        this(httpClient, apiKey, Constants.BASE_URL);
    }

    public RerankingServiceImpl(HttpClient httpClient, String apiKey, String baseUrl) {
        ValidationUtils.requireNonNull(httpClient, "httpClient");
        this.httpClient = httpClient;
        this.apiKey = apiKey;
        ValidationUtils.validateApiKey(apiKey);
        this.baseUrl = baseUrl;
    }

    public static RerankingService create(String apiKey) {
        return new RerankingServiceImpl(apiKey);
    }

    public static RerankingService create(HttpClient httpClient, String apiKey) {
        return new RerankingServiceImpl(httpClient, apiKey);
    }

    public static RerankingService create(HttpClient httpClient, String apiKey, String baseUrl) {
        return new RerankingServiceImpl(httpClient, apiKey, baseUrl);
    }

    @Override
    public RerankResponse rerank(RerankRequest request) {
        ValidationUtils.requireNonNull(request, "request");

        var body = new LinkedHashMap<String, Object>();
        body.put("model", request.model());
        body.put("query", request.query());
        body.put("docs", request.documents());
        request.instruction().ifPresent(v -> body.put("instruction", v));

        var httpRequest = HttpUtils.createJsonRequest(
            UrlUtils.buildRerankingUrl(baseUrl), apiKey, HttpMethod.POST,
            JsonUtils.toJsonString(body));

        return HttpUtils.await(HttpUtils.executeRequest(httpClient, httpRequest)
                .thenApply(this::parseResponse));
    }

    RerankResponse parseResponse(String json) {
        try {
            Map<String, Object> root = JsonUtils.parseObject(json);
            List<RerankResult> results = new ArrayList<>();
            for (Object raw : JsonUtils.asList(root.get("results"))) {
                Map<String, Object> item = JsonUtils.asMap(raw);
                Double score = JsonUtils.doubleValue(item.get("score"));
                results.add(new RerankResult(
                    JsonUtils.stringValue(item.get("doc")),
                    score == null ? 0.0 : score));
            }
            return new RerankResponse(results);
        } catch (Exception e) {
            throw e instanceof GroqSerializationException serialization
                ? serialization
                : new GroqSerializationException("Failed to parse reranking response: " + e.getMessage(), e);
        }
    }
}

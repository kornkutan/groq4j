package groq4j.services;

import groq4j.enums.BatchStatus;
import groq4j.enums.HttpMethod;
import groq4j.exceptions.*;
import groq4j.models.batch.BatchRequest;
import groq4j.models.batch.BatchResponse;
import groq4j.models.batch.BatchListResponse;
import groq4j.models.common.RequestCounts;
import groq4j.utils.*;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of BatchService for interacting with the Groq Batch API.
 * 
 * This service handles all communication with the Groq Batch API endpoints:
 * - POST /v1/batches - Create a new batch
 * - GET /v1/batches/{batch_id} - Retrieve a specific batch
 * - GET /v1/batches - List all batches
 * - POST /v1/batches/{batch_id}/cancel - Cancel a batch
 * 
 * The implementation provides both dependency injection and convenience factory methods
 * for easy integration in different application architectures.
 */
public class BatchServiceImpl implements BatchService {
    
    private final HttpClient httpClient;
    private final String apiKey;
    private final String baseUrl;
    
    /**
     * Constructor for dependency injection scenarios.
     * 
     * @param httpClient configured HTTP client for making requests
     * @param apiKey Groq API key for authentication
     * @throws IllegalArgumentException if any parameter is null or apiKey is empty
     */
    public BatchServiceImpl(String apiKey) {
        this(HttpUtils.createHttpClient(), apiKey, Constants.BASE_URL);
    }

    public BatchServiceImpl(HttpClient httpClient, String apiKey) {
        this(httpClient, apiKey, Constants.BASE_URL);
    }

    public BatchServiceImpl(HttpClient httpClient, String apiKey, String baseUrl) {
        ValidationUtils.requireNonNull(httpClient, "httpClient");
        ValidationUtils.validateApiKey(apiKey);
        
        this.httpClient = httpClient;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }
    
    /**
     * Convenience factory method for simple usage scenarios.
     * Creates a BatchService with default HTTP client configuration.
     * 
     * @param apiKey Groq API key for authentication
     * @return configured BatchService ready for use
     * @throws IllegalArgumentException if apiKey is null or empty
     * 
     * Example usage:
     * <pre>{@code
     * BatchService service = BatchServiceImpl.create("your-api-key");
     * BatchListResponse batches = service.listBatches();
     * }</pre>
     */
    public static BatchService create(String apiKey) {
        return new BatchServiceImpl(HttpUtils.createHttpClient(), apiKey);
    }
    
    /**
     * Factory method with custom HTTP client configuration.
     * 
     * @param httpClient custom configured HTTP client
     * @param apiKey Groq API key for authentication
     * @return configured BatchService
     * @throws IllegalArgumentException if any parameter is null or apiKey is empty
     */
    public static BatchService create(HttpClient httpClient, String apiKey) {
        return new BatchServiceImpl(httpClient, apiKey);
    }

    public static BatchService create(HttpClient httpClient, String apiKey, String baseUrl) {
        return new BatchServiceImpl(httpClient, apiKey, baseUrl);
    }
    
    @Override
    public BatchResponse createBatch(BatchRequest request) {
        ValidationUtils.requireNonNull(request, "request");

        HttpRequest httpRequest = HttpUtils.createJsonRequest(
            UrlUtils.buildBatchesUrl(baseUrl), apiKey, HttpMethod.POST, serializeBatchRequest(request));

        return parseBatchResponse(HttpUtils.sendAndReadBody(httpClient, httpRequest));
    }

    @Override
    public BatchResponse retrieveBatch(String batchId) {
        ValidationUtils.requireNonEmpty(batchId, "batchId");

        HttpRequest httpRequest = HttpUtils.createJsonRequest(
            UrlUtils.buildBatchUrl(baseUrl, batchId), apiKey, HttpMethod.GET, "");

        return parseBatchResponse(HttpUtils.sendAndReadBody(httpClient, httpRequest));
    }

    @Override
    public BatchListResponse listBatches() {
        HttpRequest httpRequest = HttpUtils.createJsonRequest(
            UrlUtils.buildBatchesUrl(baseUrl), apiKey, HttpMethod.GET, "");

        return parseBatchListResponse(HttpUtils.sendAndReadBody(httpClient, httpRequest));
    }

    @Override
    public BatchResponse cancelBatch(String batchId) {
        ValidationUtils.requireNonEmpty(batchId, "batchId");

        HttpRequest httpRequest = HttpUtils.createJsonRequest(
            UrlUtils.buildBatchCancelUrl(baseUrl, batchId), apiKey, HttpMethod.POST, "{}");

        return parseBatchResponse(HttpUtils.sendAndReadBody(httpClient, httpRequest));
    }

    private String serializeBatchRequest(BatchRequest request) {
        Map<String, Object> requestMap = new LinkedHashMap<>();
        requestMap.put("input_file_id", request.inputFileId());
        requestMap.put("endpoint", request.endpoint());
        requestMap.put("completion_window", request.completionWindow());
        requestMap.put("metadata", request.hasMetadata() ? request.getMetadataOrEmpty() : Map.of());

        return JsonUtils.toJsonString(requestMap);
    }

    private BatchListResponse parseBatchListResponse(String json) {
        Map<String, Object> root = JsonUtils.parseObject(json);
        List<BatchResponse> batches = new ArrayList<>();
        for (Object raw : JsonUtils.asList(root.get("data"))) {
            batches.add(parseBatchFromJson(JsonUtils.toJsonString(JsonUtils.asMap(raw))));
        }

        String object = JsonUtils.stringValue(root.get("object"));
        Double hasMore = JsonUtils.doubleValue(root.get("has_more"));
        return new BatchListResponse(object, batches, hasMore != null && hasMore != 0);
    }

    private BatchResponse parseBatchResponse(String json) {
        return parseBatchFromJson(json);
    }

    private BatchResponse parseBatchFromJson(String json) {
        String id = JsonUtils.extractStringValue(json, "id");
        if (id == null) {
            throw new GroqSerializationException("Invalid batch response: missing 'id' field");
        }

        Map<String, Object> root = JsonUtils.parseObject(json);

        String object = JsonUtils.stringValue(root.get("object"));
        String endpoint = JsonUtils.stringValue(root.get("endpoint"));
        String inputFileId = JsonUtils.stringValue(root.get("input_file_id"));
        String completionWindow = JsonUtils.stringValue(root.get("completion_window"));

        BatchStatus status = BatchStatus.fromValue(JsonUtils.stringValue(root.get("status")));

        long createdAt = longValue(root.get("created_at"));
        long expiresAt = longValue(root.get("expires_at"));

        Optional<Long> inProgressAt = optionalLong(root.get("in_progress_at"));
        Optional<Long> finalizingAt = optionalLong(root.get("finalizing_at"));
        Optional<Long> completedAt = optionalLong(root.get("completed_at"));
        Optional<Long> failedAt = optionalLong(root.get("failed_at"));
        Optional<Long> expiredAt = optionalLong(root.get("expired_at"));
        Optional<Long> cancellingAt = optionalLong(root.get("cancelling_at"));
        Optional<Long> cancelledAt = optionalLong(root.get("cancelled_at"));

        Optional<String> outputFileId = Optional.ofNullable(JsonUtils.stringValue(root.get("output_file_id")));
        Optional<String> errorFileId = Optional.ofNullable(JsonUtils.stringValue(root.get("error_file_id")));

        Optional<Object> errors = Optional.ofNullable(root.get("errors"));
        Optional<Map<String, String>> metadata = optionalMetadata(root.get("metadata"));

        RequestCounts requestCounts = parseRequestCounts(root.get("request_counts"));
        
        return new BatchResponse(
            id,
            object,
            endpoint,
            errors,
            inputFileId,
            completionWindow,
            status,
            outputFileId,
            errorFileId,
            createdAt,
            inProgressAt,
            expiresAt,
            finalizingAt,
            completedAt,
            failedAt,
            expiredAt,
            cancellingAt,
            cancelledAt,
            requestCounts,
            metadata
        );
    }
    
    /**
     * Parses request counts from the batch JSON.
     * 
     * @param json the batch JSON
     * @return RequestCounts object
     */
    private RequestCounts parseRequestCounts(Object rawCounts) {
        if (!(rawCounts instanceof Map<?, ?>)) {
            return new RequestCounts(0, 0, 0);
        }
        Map<String, Object> counts = JsonUtils.asMap(rawCounts);
        return new RequestCounts(
            intValue(counts.get("total")),
            intValue(counts.get("completed")),
            intValue(counts.get("failed")));
    }

    private static Optional<Long> optionalLong(Object value) {
        return Optional.ofNullable(JsonUtils.doubleValue(value)).map(Double::longValue);
    }

    private static Optional<Map<String, String>> optionalMetadata(Object value) {
        if (!(value instanceof Map<?, ?>)) {
            return Optional.empty();
        }
        Map<String, String> metadata = new LinkedHashMap<>();
        JsonUtils.asMap(value).forEach((key, entry) ->
            metadata.put(key, entry == null ? null : String.valueOf(entry)));
        return Optional.of(metadata);
    }

    private static int intValue(Object value) {
        Double number = JsonUtils.doubleValue(value);
        return number == null ? 0 : number.intValue();
    }

    private static long longValue(Object value) {
        Double number = JsonUtils.doubleValue(value);
        return number == null ? 0L : number.longValue();
    }

}
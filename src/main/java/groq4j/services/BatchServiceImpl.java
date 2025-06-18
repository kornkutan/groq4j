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
    
    /**
     * Constructor for dependency injection scenarios.
     * 
     * @param httpClient configured HTTP client for making requests
     * @param apiKey Groq API key for authentication
     * @throws IllegalArgumentException if any parameter is null or apiKey is empty
     */
    public BatchServiceImpl(HttpClient httpClient, String apiKey) {
        ValidationUtils.requireNonNull(httpClient, "httpClient");
        ValidationUtils.validateApiKey(apiKey);
        
        this.httpClient = httpClient;
        this.apiKey = apiKey;
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
    
    @Override
    public BatchResponse createBatch(BatchRequest request) {
        ValidationUtils.requireNonNull(request, "request");
        
        try {
            String requestBody = serializeBatchRequest(request);
            
            HttpRequest httpRequest = HttpUtils.createJsonRequest(
                UrlUtils.buildBatchesUrl(),
                apiKey,
                HttpMethod.POST,
                requestBody
            );
            
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            HttpUtils.handleHttpErrors(response);
            
            String responseBody = response.body();
            return parseBatchResponse(responseBody);
            
        } catch (GroqApiException e) {
            throw e;
        } catch (Exception e) {
            throw new GroqNetworkException("Failed to create batch", e);
        }
    }
    
    @Override
    public BatchResponse retrieveBatch(String batchId) {
        ValidationUtils.requireNonEmpty(batchId, "batchId");
        
        try {
            HttpRequest httpRequest = HttpUtils.createJsonRequest(
                UrlUtils.buildBatchUrl(batchId),
                apiKey,
                HttpMethod.GET,
                ""
            );
            
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            HttpUtils.handleHttpErrors(response);
            
            String responseBody = response.body();
            return parseBatchResponse(responseBody);
            
        } catch (GroqApiException e) {
            throw e;
        } catch (Exception e) {
            throw new GroqNetworkException("Failed to retrieve batch: " + batchId, e);
        }
    }
    
    @Override
    public BatchListResponse listBatches() {
        try {
            HttpRequest httpRequest = HttpUtils.createJsonRequest(
                UrlUtils.buildBatchesUrl(),
                apiKey,
                HttpMethod.GET,
                ""
            );
            
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            HttpUtils.handleHttpErrors(response);
            
            String responseBody = response.body();
            return parseBatchListResponse(responseBody);
            
        } catch (GroqApiException e) {
            throw e;
        } catch (Exception e) {
            throw new GroqNetworkException("Failed to list batches", e);
        }
    }
    
    @Override
    public BatchResponse cancelBatch(String batchId) {
        ValidationUtils.requireNonEmpty(batchId, "batchId");
        
        try {
            // Build the HTTP request (POST to /batches/{id}/cancel)
            HttpRequest httpRequest = HttpUtils.createJsonRequest(
                UrlUtils.buildBatchCancelUrl(batchId),
                apiKey,
                HttpMethod.POST,
                "{}" // Empty JSON body for cancel request
            );
            
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            HttpUtils.handleHttpErrors(response);
            
            String responseBody = response.body();
            return parseBatchResponse(responseBody);
            
        } catch (GroqApiException e) {
            throw e;
        } catch (Exception e) {
            throw new GroqNetworkException("Failed to cancel batch: " + batchId, e);
        }
    }
    
    /**
     * Serializes a BatchRequest to JSON.
     * 
     * @param request the batch request to serialize
     * @return JSON string representation
     */
    private String serializeBatchRequest(BatchRequest request) {
        try {
            Map<String, Object> requestMap = Map.of(
                "input_file_id", request.inputFileId(),
                "endpoint", request.endpoint(),
                "completion_window", request.completionWindow(),
                "metadata", request.hasMetadata() ? request.getMetadataOrEmpty() : Map.of()
            );
            
            return JsonUtils.toJsonString(requestMap);
            
        } catch (Exception e) {
            throw new GroqSerializationException("Failed to serialize batch request", e);
        }
    }
    
    /**
     * Parses a batch list response from JSON.
     * 
     * @param json JSON response from the API
     * @return parsed BatchListResponse
     * @throws GroqSerializationException if JSON parsing fails
     */
    private BatchListResponse parseBatchListResponse(String json) {
        try {
            String object = JsonUtils.extractStringValue(json, "object");
            String dataArrayJson = JsonUtils.extractArrayValue(json, "data");
            boolean hasMore = JsonUtils.extractBooleanValuePrimitive(json, "has_more");
            
            List<BatchResponse> batches = JsonUtils.parseJsonArray(dataArrayJson, this::parseBatchFromJson);
            
            return new BatchListResponse(object, batches, hasMore);
            
        } catch (Exception e) {
            throw new GroqSerializationException("Failed to parse batch list response", e);
        }
    }
    
    /**
     * Parses a single batch response from JSON.
     * 
     * @param json JSON response from the API
     * @return parsed BatchResponse
     * @throws GroqSerializationException if JSON parsing fails
     */
    private BatchResponse parseBatchResponse(String json) {
        try {
            return parseBatchFromJson(json);
        } catch (Exception e) {
            throw new GroqSerializationException("Failed to parse batch response", e);
        }
    }
    
    /**
     * Parses a single batch from JSON object.
     * 
     * @param json JSON object representing a batch
     * @return parsed BatchResponse
     */
    private BatchResponse parseBatchFromJson(String json) {
        String id = JsonUtils.extractStringValue(json, "id");
        String object = JsonUtils.extractStringValue(json, "object");
        String endpoint = JsonUtils.extractStringValue(json, "endpoint");
        String inputFileId = JsonUtils.extractStringValue(json, "input_file_id");
        String completionWindow = JsonUtils.extractStringValue(json, "completion_window");
        
        // Parse status
        String statusValue = JsonUtils.extractStringValue(json, "status");
        BatchStatus status = BatchStatus.fromValue(statusValue);
        
        // Parse timestamps
        long createdAt = JsonUtils.extractLongValuePrimitive(json, "created_at");
        long expiresAt = JsonUtils.extractLongValuePrimitive(json, "expires_at");
        
        // Parse optional timestamps
        Optional<Long> inProgressAt = JsonUtils.extractOptionalLongValue(json, "in_progress_at");
        Optional<Long> finalizingAt = JsonUtils.extractOptionalLongValue(json, "finalizing_at");
        Optional<Long> completedAt = JsonUtils.extractOptionalLongValue(json, "completed_at");
        Optional<Long> failedAt = JsonUtils.extractOptionalLongValue(json, "failed_at");
        Optional<Long> expiredAt = JsonUtils.extractOptionalLongValue(json, "expired_at");
        Optional<Long> cancellingAt = JsonUtils.extractOptionalLongValue(json, "cancelling_at");
        Optional<Long> cancelledAt = JsonUtils.extractOptionalLongValue(json, "cancelled_at");
        
        // Parse optional file IDs
        Optional<String> outputFileId = JsonUtils.extractOptionalStringValue(json, "output_file_id");
        Optional<String> errorFileId = JsonUtils.extractOptionalStringValue(json, "error_file_id");
        
        // Parse optional fields
        Optional<Object> errors = JsonUtils.extractOptionalValue(json, "errors");
        Optional<Map<String, String>> metadata = JsonUtils.extractOptionalMapValue(json, "metadata");
        
        // Parse request counts
        RequestCounts requestCounts = parseRequestCounts(json);
        
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
    private RequestCounts parseRequestCounts(String json) {
        String requestCountsJson = JsonUtils.extractObjectValue(json, "request_counts");
        if (requestCountsJson == null) {
            // Return default empty counts if not present
            return new RequestCounts(0, 0, 0);
        }
        
        int total = JsonUtils.extractIntValuePrimitive(requestCountsJson, "total");
        int completed = JsonUtils.extractIntValuePrimitive(requestCountsJson, "completed");
        int failed = JsonUtils.extractIntValuePrimitive(requestCountsJson, "failed");
        
        return new RequestCounts(total, completed, failed);
    }
}
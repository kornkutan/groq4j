package groq4j.services;

import groq4j.exceptions.*;
import groq4j.models.models.Model;
import groq4j.models.models.ModelListResponse;
import groq4j.utils.*;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of ModelsService for interacting with the Groq Models API.
 * 
 * This service handles all communication with the Groq Models API endpoints:
 * - GET /v1/models - List all available models
 * - GET /v1/models/{model} - Retrieve specific model information
 * 
 * The implementation provides both dependency injection and convenience factory methods
 * for easy integration in different application architectures.
 */
public class ModelsServiceImpl implements ModelsService {
    
    private final HttpClient httpClient;
    private final String apiKey;
    
    /**
     * Constructor for dependency injection scenarios.
     * 
     * @param httpClient configured HTTP client for making requests
     * @param apiKey Groq API key for authentication
     * @throws IllegalArgumentException if any parameter is null or apiKey is empty
     */
    public ModelsServiceImpl(HttpClient httpClient, String apiKey) {
        ValidationUtils.requireNonNull(httpClient, "httpClient");
        ValidationUtils.validateApiKey(apiKey);
        
        this.httpClient = httpClient;
        this.apiKey = apiKey;
    }
    
    /**
     * Convenience factory method for simple usage scenarios.
     * Creates a ModelsService with default HTTP client configuration.
     * 
     * @param apiKey Groq API key for authentication
     * @return configured ModelsService ready for use
     * @throws IllegalArgumentException if apiKey is null or empty
     * 
     * Example usage:
     * <pre>{@code
     * ModelsService service = ModelsServiceImpl.create("your-api-key");
     * ModelListResponse models = service.listModels();
     * }</pre>
     */
    public static ModelsService create(String apiKey) {
        return new ModelsServiceImpl(HttpUtils.createHttpClient(), apiKey);
    }
    
    /**
     * Factory method with custom HTTP client configuration.
     * 
     * @param httpClient custom configured HTTP client
     * @param apiKey Groq API key for authentication
     * @return configured ModelsService
     * @throws IllegalArgumentException if any parameter is null or apiKey is empty
     */
    public static ModelsService create(HttpClient httpClient, String apiKey) {
        return new ModelsServiceImpl(httpClient, apiKey);
    }
    
    @Override
    public ModelListResponse listModels() {
        try {
            HttpRequest request = HttpUtils.createJsonRequest(
                UrlUtils.buildModelsUrl(),
                apiKey,
                groq4j.enums.HttpMethod.GET,
                ""
            );
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            HttpUtils.handleHttpErrors(response);
            
            String responseBody = response.body();
            return parseModelListResponse(responseBody);
            
        } catch (GroqApiException e) {
            throw e;
        } catch (Exception e) {
            throw new GroqNetworkException("Failed to list models", e);
        }
    }
    
    @Override
    public Model retrieveModel(String modelId) {
        ValidationUtils.requireNonEmpty(modelId, "modelId");
        
        try {
            HttpRequest request = HttpUtils.createJsonRequest(
                UrlUtils.buildModelUrl(modelId),
                apiKey,
                groq4j.enums.HttpMethod.GET,
                ""
            );
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            HttpUtils.handleHttpErrors(response);
            
            String responseBody = response.body();
            return parseModelResponse(responseBody);
            
        } catch (GroqApiException e) {
            throw e;
        } catch (Exception e) {
            throw new GroqNetworkException("Failed to retrieve model: " + modelId, e);
        }
    }
    
    /**
     * Parses a model list response from JSON.
     * 
     * @param json JSON response from the API
     * @return parsed ModelListResponse
     * @throws GroqSerializationException if JSON parsing fails
     */
    private ModelListResponse parseModelListResponse(String json) {
        try {
            String object = JsonUtils.extractStringValue(json, "object");
            String dataArrayJson = JsonUtils.extractArrayValue(json, "data");
            
            List<Model> models = JsonUtils.parseJsonArray(dataArrayJson, this::parseModelFromJson);
            
            return new ModelListResponse(object, models);
            
        } catch (Exception e) {
            throw new GroqSerializationException("Failed to parse model list response", e);
        }
    }
    
    /**
     * Parses a single model response from JSON.
     * 
     * @param json JSON response from the API
     * @return parsed Model
     * @throws GroqSerializationException if JSON parsing fails
     */
    private Model parseModelResponse(String json) {
        try {
            return parseModelFromJson(json);
        } catch (Exception e) {
            throw new GroqSerializationException("Failed to parse model response", e);
        }
    }
    
    /**
     * Parses a single model from JSON object.
     * 
     * @param json JSON object representing a model
     * @return parsed Model
     */
    private Model parseModelFromJson(String json) {
        String id = JsonUtils.extractStringValue(json, "id");
        String object = JsonUtils.extractStringValue(json, "object");
        long created = JsonUtils.extractLongValuePrimitive(json, "created");
        String ownedBy = JsonUtils.extractStringValue(json, "owned_by");
        boolean active = JsonUtils.extractBooleanValuePrimitive(json, "active");
        int contextWindow = JsonUtils.extractIntValuePrimitive(json, "context_window");
        
        // Optional fields
        Optional<Object> publicApps = JsonUtils.extractOptionalValue(json, "public_apps");
        Optional<Integer> maxCompletionTokens = JsonUtils.extractOptionalIntValue(json, "max_completion_tokens");
        
        return new Model(
            id,
            object,
            created,
            ownedBy,
            active,
            contextWindow,
            publicApps,
            maxCompletionTokens
        );
    }
}
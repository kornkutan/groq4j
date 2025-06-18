package groq4j.services;

import groq4j.models.models.Model;
import groq4j.models.models.ModelListResponse;

/**
 * Service interface for interacting with the Groq Models API.
 * 
 * This service provides methods to list available models and retrieve specific model information.
 * All methods return data models that include utility methods for filtering and categorizing models.
 * 
 * @see <a href="https://console.groq.com/docs/models">Groq Models API Documentation</a>
 */
public interface ModelsService {
    
    /**
     * Lists all available models from the Groq API.
     * 
     * This method calls the GET /v1/models endpoint and returns a complete list of all models
     * available in your account, including both active and inactive models.
     * 
     * The returned ModelListResponse includes utility methods for filtering models by type
     * (chat, whisper, TTS) and status (active/inactive).
     * 
     * @return ModelListResponse containing all available models with filtering utilities
     * @throws groq4j.exceptions.GroqApiException if the API request fails
     * @throws groq4j.exceptions.GroqAuthenticationException if the API key is invalid
     * @throws groq4j.exceptions.GroqNetworkException if there are network connectivity issues
     * 
     * @see ModelListResponse#getChatModels()
     * @see ModelListResponse#getWhisperModels()
     * @see ModelListResponse#getTtsModels()
     * @see ModelListResponse#getActiveModels()
     * 
     * Example usage:
     * <pre>{@code
     * ModelsService service = ModelsServiceImpl.create(apiKey);
     * ModelListResponse response = service.listModels();
     * 
     * List<Model> chatModels = response.getChatModels();
     * List<Model> activeModels = response.getActiveModels();
     * boolean hasLlama = response.hasModel("llama-3.1-8b-instant");
     * }</pre>
     */
    ModelListResponse listModels();
    
    /**
     * Retrieves detailed information about a specific model.
     * 
     * This method calls the GET /v1/models/{model} endpoint and returns complete information
     * about the specified model, including its capabilities, context window, and availability.
     * 
     * @param modelId the ID of the model to retrieve (e.g., "llama-3.1-8b-instant")
     * @return Model containing detailed information about the specified model
     * @throws groq4j.exceptions.GroqApiException if the API request fails
     * @throws groq4j.exceptions.GroqAuthenticationException if the API key is invalid
     * @throws groq4j.exceptions.GroqBadRequestException if the model ID is not found
     * @throws groq4j.exceptions.GroqNetworkException if there are network connectivity issues
     * @throws IllegalArgumentException if modelId is null or empty
     * 
     * @see Model#isChatModel()
     * @see Model#isWhisperModel()
     * @see Model#isTtsModel()
     * @see Model#getEffectiveMaxTokens()
     * 
     * Example usage:
     * <pre>{@code
     * ModelsService service = ModelsServiceImpl.create(apiKey);
     * Model model = service.retrieveModel("llama-3.1-8b-instant");
     * 
     * System.out.println("Model: " + model.id());
     * System.out.println("Context Window: " + model.contextWindow());
     * System.out.println("Is Chat Model: " + model.isChatModel());
     * System.out.println("Is Active: " + model.isActive());
     * }</pre>
     */
    Model retrieveModel(String modelId);
}
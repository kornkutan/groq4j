package groq4j.services;

import groq4j.models.chat.ChatCompletionRequest;
import groq4j.models.chat.ChatCompletionResponse;

/**
 * Service interface for chat completion operations with the Groq API.
 * 
 * Provides methods to create chat completions using the Groq chat models.
 * Supports both simple convenience methods and full-featured request objects.
 */
public interface ChatService {
    
    /**
     * Creates a chat completion using a full ChatCompletionRequest object.
     * 
     * @param request The complete chat completion request with all parameters
     * @return ChatCompletionResponse containing the model's response
     * @throws groq4j.exceptions.GroqApiException if the API call fails
     * @throws groq4j.exceptions.GroqValidationException if the request is invalid
     */
    ChatCompletionResponse createCompletion(ChatCompletionRequest request);
    
    /**
     * Convenience method for simple chat completions with just model and message.
     * 
     * @param model The model ID to use (e.g., "llama-3.3-70b-versatile"), @see <a href="https://console.groq.com/docs/models">Supported Models</a>
     * @param message The user message content
     * @return ChatCompletionResponse containing the model's response
     * @throws groq4j.exceptions.GroqApiException if the API call fails
     * @throws groq4j.exceptions.GroqValidationException if parameters are invalid
     */
    ChatCompletionResponse simple(String model, String message);
    
    /**
     * Convenience method for simple chat completions with system prompt.
     * 
     * @param model The model ID to use, @see <a href="https://console.groq.com/docs/models">Supported Models</a>
     * @param systemPrompt The system message to set context
     * @param userMessage The user message content
     * @return ChatCompletionResponse containing the model's response
     * @throws groq4j.exceptions.GroqApiException if the API call fails
     * @throws groq4j.exceptions.GroqValidationException if parameters are invalid
     */
    ChatCompletionResponse simple(String model, String systemPrompt, String userMessage);
}
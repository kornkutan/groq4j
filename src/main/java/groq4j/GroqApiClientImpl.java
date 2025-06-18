package groq4j;

import groq4j.services.*;
import groq4j.utils.HttpUtils;
import groq4j.utils.ValidationUtils;

import java.net.http.HttpClient;

/**
 * Main implementation of the Groq API client providing unified access to all Groq services.
 * This implementation offers both legacy async methods and modern service access.
 * 
 * <p>Example usage:
 * <pre>{@code
 * // Create client
 * var client = GroqApiClient.create("your-api-key");
 * 
 * // Use modern service APIs
 * var response = client.chat().simple("llama-3.1-8b-instant", "Hello!");
 * var models = client.models().listModels();
 * var transcription = client.audio().transcribe(audioFile, "whisper-large-v3");
 * }</pre>
 */
public class GroqApiClientImpl implements GroqApiClient {

    private final String apiKey;
    private final HttpClient httpClient;
    private final boolean ownsHttpClient;
    
    // Service instances (lazy initialization)
    private ChatService chatService;
    private AudioService audioService;
    private ModelsService modelsService;
    private BatchService batchService;
    private FilesService filesService;

    /**
     * Creates GroqApiClient with the default HttpClient configuration.
     * Convenient for most use cases.
     * 
     * @param apiKey The Groq API key for authentication
     */
    public GroqApiClientImpl(String apiKey) {
        ValidationUtils.validateApiKey(apiKey);
        this.apiKey = apiKey;
        this.httpClient = HttpUtils.createHttpClient();
        this.ownsHttpClient = true;
    }

    /**
     * Creates GroqApiClient with custom HttpClient.
     * Recommended for production use and testing with custom configurations.
     * 
     * @param httpClient Custom HttpClient instance
     * @param apiKey The Groq API key for authentication
     */
    public GroqApiClientImpl(HttpClient httpClient, String apiKey) {
        ValidationUtils.validateApiKey(apiKey);
        ValidationUtils.requireNonNull(httpClient, "httpClient");
        this.apiKey = apiKey;
        this.httpClient = httpClient;
        this.ownsHttpClient = false;
    }

    /**
     * Static factory method for creating GroqApiClient with default settings.
     * 
     * @param apiKey The Groq API key for authentication
     * @return New GroqApiClient instance
     */
    public static GroqApiClient create(String apiKey) {
        return new GroqApiClientImpl(apiKey);
    }

    /**
     * Static factory method for creating GroqApiClient with custom HttpClient.
     * 
     * @param httpClient Custom HttpClient instance
     * @param apiKey The Groq API key for authentication
     * @return New GroqApiClient instance
     */
    public static GroqApiClient create(HttpClient httpClient, String apiKey) {
        return new GroqApiClientImpl(httpClient, apiKey);
    }

    @Override
    public ChatService chat() {
        if (chatService == null) {
            chatService = new ChatServiceImpl(httpClient, apiKey);
        }
        return chatService;
    }

    @Override
    public AudioService audio() {
        if (audioService == null) {
            audioService = new AudioServiceImpl(httpClient, apiKey);
        }
        return audioService;
    }

    @Override
    public ModelsService models() {
        if (modelsService == null) {
            modelsService = new ModelsServiceImpl(httpClient, apiKey);
        }
        return modelsService;
    }

    @Override
    public BatchService batch() {
        if (batchService == null) {
            batchService = new BatchServiceImpl(httpClient, apiKey);
        }
        return batchService;
    }

    @Override
    public FilesService files() {
        if (filesService == null) {
            filesService = new FilesServiceImpl(httpClient, apiKey);
        }
        return filesService;
    }
}
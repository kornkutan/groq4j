package groq4j;

import groq4j.services.AudioService;
import groq4j.services.AudioServiceImpl;
import groq4j.services.BatchService;
import groq4j.services.BatchServiceImpl;
import groq4j.services.ChatService;
import groq4j.services.ChatServiceImpl;
import groq4j.services.EmbeddingsService;
import groq4j.services.EmbeddingsServiceImpl;
import groq4j.services.FilesService;
import groq4j.services.FilesServiceImpl;
import groq4j.services.ModelsService;
import groq4j.services.ModelsServiceImpl;
import groq4j.services.RerankingService;
import groq4j.services.RerankingServiceImpl;
import groq4j.services.ResponsesService;
import groq4j.services.ResponsesServiceImpl;
import groq4j.utils.HttpUtils;
import groq4j.utils.ValidationUtils;

import java.net.http.HttpClient;

/**
 * Default client. Services are created on first access, so constructing a client costs nothing
 * beyond the API key check.
 */
public class GroqApiClientImpl implements GroqApiClient {

    private final String apiKey;
    private final HttpClient httpClient;

    private ChatService chatService;
    private ResponsesService responsesService;
    private EmbeddingsService embeddingsService;
    private RerankingService rerankingService;
    private AudioService audioService;
    private ModelsService modelsService;
    private BatchService batchService;
    private FilesService filesService;

    public GroqApiClientImpl(String apiKey) {
        ValidationUtils.validateApiKey(apiKey);
        this.apiKey = apiKey;
        this.httpClient = HttpUtils.createHttpClient();
    }

    public GroqApiClientImpl(HttpClient httpClient, String apiKey) {
        ValidationUtils.validateApiKey(apiKey);
        ValidationUtils.requireNonNull(httpClient, "httpClient");
        this.apiKey = apiKey;
        this.httpClient = httpClient;
    }

    public static GroqApiClient create(String apiKey) {
        return new GroqApiClientImpl(apiKey);
    }

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
    public ResponsesService responses() {
        if (responsesService == null) {
            responsesService = new ResponsesServiceImpl(httpClient, apiKey);
        }
        return responsesService;
    }

    @Override
    public EmbeddingsService embeddings() {
        if (embeddingsService == null) {
            embeddingsService = new EmbeddingsServiceImpl(httpClient, apiKey);
        }
        return embeddingsService;
    }

    @Override
    public RerankingService reranking() {
        if (rerankingService == null) {
            rerankingService = new RerankingServiceImpl(httpClient, apiKey);
        }
        return rerankingService;
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

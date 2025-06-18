package groq4j.services;

import groq4j.builders.SpeechRequestBuilder;
import groq4j.builders.TranscriptionRequestBuilder;
import groq4j.builders.TranslationRequestBuilder;
import groq4j.enums.HttpMethod;
import groq4j.models.audio.SpeechRequest;
import groq4j.models.audio.TranscriptionRequest;
import groq4j.models.audio.TranscriptionResponse;
import groq4j.models.audio.TranslationRequest;
import groq4j.models.audio.TranslationResponse;
import groq4j.models.common.XGroq;
import groq4j.utils.Constants;
import groq4j.utils.FileUtils;
import groq4j.utils.HttpUtils;
import groq4j.utils.JsonUtils;
import groq4j.utils.ResponseParser;
import groq4j.utils.UrlUtils;
import groq4j.utils.ValidationUtils;

import java.net.http.HttpClient;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of AudioService that communicates with the Groq API.
 * 
 * Handles audio transcription, translation, and speech synthesis operations
 * with support for both file upload and URL-based audio processing.
 */
public class AudioServiceImpl implements AudioService {
    private final HttpClient httpClient;
    private final String apiKey;
    private final boolean ownsHttpClient;

    /**
     * Creates AudioService with default HttpClient (convenient for quick usage).
     * Uses sensible defaults but less flexible than providing your own HttpClient.
     */
    public AudioServiceImpl(String apiKey) {
        this.httpClient = HttpUtils.createHttpClient();
        this.apiKey = apiKey;
        this.ownsHttpClient = true;
        
        ValidationUtils.validateApiKey(apiKey);
    }

    /**
     * Creates AudioService with custom HttpClient (recommended for robust production deployments).
     * Allows full control over HTTP configuration and enables testing with mocked clients.
     */
    public AudioServiceImpl(HttpClient httpClient, String apiKey) {
        this.httpClient = httpClient;
        this.apiKey = apiKey;
        this.ownsHttpClient = false;

        ValidationUtils.validateApiKey(apiKey);
    }

    /**
     * Static factory method for creating AudioService with custom HttpClient.
     * Recommended for production use and testing.
     */
    public static AudioService create(HttpClient httpClient, String apiKey) {
        return new AudioServiceImpl(httpClient, apiKey);
    }
    
    /**
     * Static factory method for creating AudioService with default settings.
     * Convenient for quick prototyping and simple use cases.
     */
    public static AudioService create(String apiKey) {
        return new AudioServiceImpl(apiKey);
    }
    
    @Override
    public TranscriptionResponse createTranscription(TranscriptionRequest request) {
        ValidationUtils.requireNonNull(request, "request");
        
        // Build the request (multipart for file, JSON for URL)
        if (request.hasFile()) {
            return createTranscriptionWithFile(request);
        } else if (request.hasUrl()) {
            return createTranscriptionWithUrl(request);
        } else {
            throw new IllegalArgumentException("Either file or URL must be provided");
        }
    }
    
    @Override
    public TranslationResponse createTranslation(TranslationRequest request) {
        ValidationUtils.requireNonNull(request, "request");
        
        // Build the request (multipart for file, JSON for URL)
        if (request.hasFile()) {
            return createTranslationWithFile(request);
        } else if (request.hasUrl()) {
            return createTranslationWithUrl(request);
        } else {
            throw new IllegalArgumentException("Either file or URL must be provided");
        }
    }
    
    @Override
    public byte[] createSpeech(SpeechRequest request) {
        ValidationUtils.requireNonNull(request, "request");
        
        String requestJson = buildSpeechRequestJson(request);

        String url = UrlUtils.buildAudioSpeechUrl(Constants.BASE_URL);
        var httpRequest = HttpUtils.createJsonRequest(url, apiKey, HttpMethod.POST, requestJson);
        
        return HttpUtils.executeRequestForBytes(httpClient, httpRequest)
            .join(); // Block for synchronous response
    }
    
    @Override
    public TranscriptionResponse simpleTranscription(String model, byte[] audioData, String filename) {
        ValidationUtils.validateModel(model);
        ValidationUtils.requireNonNull(audioData, "audioData");
        
        if (filename != null && !filename.isEmpty()) {
            FileUtils.validateAudioFile(audioData, filename);
        }
        
        var request = TranscriptionRequestBuilder.withFile(model, audioData).build();
        return createTranscription(request);
    }
    
    @Override
    public TranslationResponse simpleTranslation(String model, byte[] audioData, String filename) {
        ValidationUtils.validateModel(model);
        ValidationUtils.requireNonNull(audioData, "audioData");
        
        if (filename != null && !filename.isEmpty()) {
            FileUtils.validateAudioFile(audioData, filename);
        }
        
        var request = TranslationRequestBuilder.withFile(model, audioData).build();
        return createTranslation(request);
    }
    
    @Override
    public byte[] simpleSpeech(String model, String text, String voice) {
        ValidationUtils.validateModel(model);
        ValidationUtils.requireNonNull(text, "text");
        ValidationUtils.requireNonNull(voice, "voice");
        
        var request = SpeechRequestBuilder.create(model, text, voice).build();
        return createSpeech(request);
    }
    
    private TranscriptionResponse createTranscriptionWithFile(TranscriptionRequest request) {
        Map<String, Object> formData = buildTranscriptionFormData(request);
        
        String url = UrlUtils.buildAudioTranscriptionsUrl(Constants.BASE_URL);
        var httpRequest = HttpUtils.createMultipartRequest(url, apiKey, formData);
        
        return HttpUtils.executeRequest(httpClient, httpRequest)
            .thenApply(this::parseTranscriptionResponse)
            .join(); // Block for synchronous response
    }
    
    private TranscriptionResponse createTranscriptionWithUrl(TranscriptionRequest request) {
        String requestJson = buildTranscriptionRequestJson(request);
        
        String url = UrlUtils.buildAudioTranscriptionsUrl(Constants.BASE_URL);
        var httpRequest = HttpUtils.createJsonRequest(url, apiKey, HttpMethod.POST, requestJson);
        
        return HttpUtils.executeRequest(httpClient, httpRequest)
            .thenApply(this::parseTranscriptionResponse)
            .join(); // Block for synchronous response
    }
    
    private TranslationResponse createTranslationWithFile(TranslationRequest request) {
        Map<String, Object> formData = buildTranslationFormData(request);
        
        String url = UrlUtils.buildAudioTranslationsUrl(Constants.BASE_URL);
        var httpRequest = HttpUtils.createMultipartRequest(url, apiKey, formData);
        
        return HttpUtils.executeRequest(httpClient, httpRequest)
            .thenApply(this::parseTranslationResponse)
            .join(); // Block for synchronous response
    }
    
    private TranslationResponse createTranslationWithUrl(TranslationRequest request) {
        String requestJson = buildTranslationRequestJson(request);
        
        String url = UrlUtils.buildAudioTranslationsUrl(Constants.BASE_URL);
        var httpRequest = HttpUtils.createJsonRequest(url, apiKey, HttpMethod.POST, requestJson);
        
        return HttpUtils.executeRequest(httpClient, httpRequest)
            .thenApply(this::parseTranslationResponse)
            .join(); // Block for synchronous response
    }
    
    private Map<String, Object> buildTranscriptionFormData(TranscriptionRequest request) {
        var formData = new HashMap<String, Object>();
        
        formData.put("model", request.model());
        if (request.file().isPresent()) {
            formData.put("file", request.file().get());
        }
        
        request.language().ifPresent(lang -> formData.put("language", lang));
        request.prompt().ifPresent(prompt -> formData.put("prompt", prompt));
        request.responseFormat().ifPresent(format -> formData.put("response_format", format.getValue()));
        request.temperature().ifPresent(temp -> formData.put("temperature", temp.toString()));
        
        if (request.timestampGranularities().isPresent() && !request.timestampGranularities().get().isEmpty()) {
            var granularities = request.timestampGranularities().get().stream()
                .map(tg -> tg.getValue())
                .toList();
            formData.put("timestamp_granularities", String.join(",", granularities));
        }
        
        return formData;
    }
    
    private String buildTranscriptionRequestJson(TranscriptionRequest request) {
        var requestMap = new HashMap<String, Object>();
        
        requestMap.put("model", request.model());
        request.url().ifPresent(url -> requestMap.put("url", url));
        
        request.language().ifPresent(lang -> requestMap.put("language", lang));
        request.prompt().ifPresent(prompt -> requestMap.put("prompt", prompt));
        request.responseFormat().ifPresent(format -> requestMap.put("response_format", format.getValue()));
        request.temperature().ifPresent(temp -> requestMap.put("temperature", temp));
        
        if (request.timestampGranularities().isPresent() && !request.timestampGranularities().get().isEmpty()) {
            var granularities = request.timestampGranularities().get().stream()
                .map(tg -> tg.getValue())
                .toList();
            requestMap.put("timestamp_granularities", granularities);
        }
        
        return JsonUtils.toJsonString(requestMap);
    }
    
    private Map<String, Object> buildTranslationFormData(TranslationRequest request) {
        var formData = new HashMap<String, Object>();
        
        formData.put("model", request.model());
        if (request.file().isPresent()) {
            formData.put("file", request.file().get());
        }
        
        request.prompt().ifPresent(prompt -> formData.put("prompt", prompt));
        request.responseFormat().ifPresent(format -> formData.put("response_format", format.getValue()));
        request.temperature().ifPresent(temp -> formData.put("temperature", temp.toString()));
        
        return formData;
    }
    
    private String buildTranslationRequestJson(TranslationRequest request) {
        var requestMap = new HashMap<String, Object>();
        
        requestMap.put("model", request.model());
        request.url().ifPresent(url -> requestMap.put("url", url));
        
        request.prompt().ifPresent(prompt -> requestMap.put("prompt", prompt));
        request.responseFormat().ifPresent(format -> requestMap.put("response_format", format.getValue()));
        request.temperature().ifPresent(temp -> requestMap.put("temperature", temp));
        
        return JsonUtils.toJsonString(requestMap);
    }
    
    private String buildSpeechRequestJson(SpeechRequest request) {
        var requestMap = new HashMap<String, Object>();
        
        requestMap.put("model", request.model());
        requestMap.put("input", request.input());
        requestMap.put("voice", request.voice());
        
        request.responseFormat().ifPresent(format -> requestMap.put("response_format", format.getValue()));
        request.sampleRate().ifPresent(rate -> requestMap.put("sample_rate", rate.getValue()));
        request.speed().ifPresent(speed -> requestMap.put("speed", speed));
        
        return JsonUtils.toJsonString(requestMap);
    }
    
    private TranscriptionResponse parseTranscriptionResponse(String responseJson) {
        try {
            String text = ResponseParser.getRequiredString(responseJson, "text");
            
            var xGroq = ResponseParser.getOptionalString(responseJson, "x_groq")
                .map(xGroqJson -> new XGroq(
                    ResponseParser.getRequiredString(xGroqJson, "id")
                ));
            
            return new TranscriptionResponse(text, xGroq);
            
        } catch (Exception e) {
            throw new groq4j.exceptions.GroqSerializationException("Failed to parse transcription response: " + e.getMessage(), e);
        }
    }
    
    private TranslationResponse parseTranslationResponse(String responseJson) {
        try {
            String text = ResponseParser.getRequiredString(responseJson, "text");
            
            var xGroq = ResponseParser.getOptionalString(responseJson, "x_groq")
                .map(xGroqJson -> new XGroq(
                    ResponseParser.getRequiredString(xGroqJson, "id")
                ));
            
            return new TranslationResponse(text, xGroq);
            
        } catch (Exception e) {
            throw new groq4j.exceptions.GroqSerializationException("Failed to parse translation response: " + e.getMessage(), e);
        }
    }
}
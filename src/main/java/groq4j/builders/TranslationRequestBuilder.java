package groq4j.builders;

import groq4j.enums.ResponseFormat;
import groq4j.models.audio.TranslationRequest;
import groq4j.utils.FileUtils;
import groq4j.utils.ValidationUtils;

import java.util.Optional;

/**
 * Builder for creating TranslationRequest objects with fluent API.
 * 
 * Supports both file-based and URL-based translation requests.
 * Translation always converts input audio to English text.
 */
public class TranslationRequestBuilder {
    private String model;
    private Optional<byte[]> file = Optional.empty();
    private Optional<String> url = Optional.empty();
    private Optional<String> prompt = Optional.empty();
    private Optional<ResponseFormat> responseFormat = Optional.empty();
    private Optional<Double> temperature = Optional.empty();
    
    private TranslationRequestBuilder() {
        // Private constructor - use static factory methods
    }
    
    /**
     * Creates a new builder for translation.
     * 
     * @param model the model to use for translation (required)
     * @return new builder instance
     */
    public static TranslationRequestBuilder create(String model) {
        var builder = new TranslationRequestBuilder();
        builder.model = model;
        return builder;
    }
    
    /**
     * Creates a new builder with file data for translation.
     * 
     * @param model the model to use for translation (required)
     * @param audioData the audio file data as byte array
     * @return new builder instance with file data set
     */
    public static TranslationRequestBuilder withFile(String model, byte[] audioData) {
        return create(model).file(audioData);
    }
    
    /**
     * Creates a new builder with URL for translation.
     * 
     * @param model the model to use for translation (required)
     * @param audioUrl the URL of the audio file
     * @return new builder instance with URL set
     */
    public static TranslationRequestBuilder withUrl(String model, String audioUrl) {
        return create(model).url(audioUrl);
    }
    
    /**
     * Sets the audio file data for translation.
     * 
     * @param audioData the audio file data as byte array
     * @return this builder for method chaining
     */
    public TranslationRequestBuilder file(byte[] audioData) {
        ValidationUtils.requireNonNull(audioData, "audioData");
        if (audioData.length == 0) {
            throw new IllegalArgumentException("Audio data cannot be empty");
        }
        this.file = Optional.of(audioData);
        this.url = Optional.empty(); // Clear URL if file is set
        return this;
    }
    
    /**
     * Sets the audio file data with filename for validation.
     * 
     * @param audioData the audio file data as byte array
     * @param filename the filename for format validation
     * @return this builder for method chaining
     */
    public TranslationRequestBuilder file(byte[] audioData, String filename) {
        if (filename != null && !filename.isEmpty()) {
            FileUtils.validateAudioFile(audioData, filename);
        }
        return file(audioData);
    }
    
    /**
     * Sets the URL of the audio file for translation.
     * 
     * @param audioUrl the URL of the audio file
     * @return this builder for method chaining
     */
    public TranslationRequestBuilder url(String audioUrl) {
        ValidationUtils.requireNonNull(audioUrl, "audioUrl");
        if (audioUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Audio URL cannot be empty");
        }
        this.url = Optional.of(audioUrl.trim());
        this.file = Optional.empty(); // Clear file if URL is set
        return this;
    }
    
    /**
     * Sets an optional text prompt to guide the model's style or continue a previous audio segment.
     * 
     * @param prompt the prompt text
     * @return this builder for method chaining
     */
    public TranslationRequestBuilder prompt(String prompt) {
        if (prompt != null && !prompt.trim().isEmpty()) {
            this.prompt = Optional.of(prompt.trim());
        }
        return this;
    }
    
    /**
     * Sets the response format for the translation.
     * 
     * @param format the response format (json, text, verbose_json)
     * @return this builder for method chaining
     */
    public TranslationRequestBuilder responseFormat(ResponseFormat format) {
        if (format != null) {
            this.responseFormat = Optional.of(format);
        }
        return this;
    }
    
    /**
     * Sets the temperature for the translation (0.0 to 1.0).
     * 
     * @param temperature the temperature value
     * @return this builder for method chaining
     */
    public TranslationRequestBuilder temperature(double temperature) {
        ValidationUtils.validateRange(temperature, groq4j.utils.Constants.MIN_TEMPERATURE, groq4j.utils.Constants.MAX_TEMPERATURE, "temperature");
        this.temperature = Optional.of(temperature);
        return this;
    }
    
    /**
     * Sets JSON response format.
     * 
     * @return this builder for method chaining
     */
    public TranslationRequestBuilder asJson() {
        return responseFormat(ResponseFormat.JSON);
    }
    
    /**
     * Sets text response format.
     * 
     * @return this builder for method chaining
     */
    public TranslationRequestBuilder asText() {
        return responseFormat(ResponseFormat.TEXT);
    }
    
    /**
     * Sets verbose JSON response format.
     * 
     * @return this builder for method chaining
     */
    public TranslationRequestBuilder asVerboseJson() {
        return responseFormat(ResponseFormat.VERBOSE_JSON);
    }
    
    /**
     * Builds the TranslationRequest.
     * 
     * @return the configured TranslationRequest
     * @throws IllegalArgumentException if required fields are missing or invalid
     */
    public TranslationRequest build() {
        ValidationUtils.validateModel(model);
        
        if (file.isEmpty() && url.isEmpty()) {
            throw new IllegalArgumentException("Either file data or URL must be provided");
        }
        
        if (file.isPresent() && url.isPresent()) {
            throw new IllegalArgumentException("Cannot provide both file data and URL");
        }
        
        return new TranslationRequest(
            model,
            file,
            url,
            prompt,
            responseFormat,
            temperature
        );
    }
}
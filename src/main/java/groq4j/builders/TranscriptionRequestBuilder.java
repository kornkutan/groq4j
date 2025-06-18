package groq4j.builders;

import groq4j.enums.ResponseFormat;
import groq4j.enums.TimestampGranularity;
import groq4j.models.audio.TranscriptionRequest;
import groq4j.utils.FileUtils;
import groq4j.utils.ValidationUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Builder for creating TranscriptionRequest objects with fluent API.
 * 
 * Supports both file-based and URL-based transcription requests with
 * various configuration options like language, response format, and
 * timestamp granularities.
 */
public class TranscriptionRequestBuilder {
    private String model;
    private Optional<byte[]> file = Optional.empty();
    private Optional<String> url = Optional.empty();
    private Optional<String> language = Optional.empty();
    private Optional<String> prompt = Optional.empty();
    private Optional<ResponseFormat> responseFormat = Optional.empty();
    private Optional<Double> temperature = Optional.empty();
    private Optional<List<TimestampGranularity>> timestampGranularities = Optional.empty();
    
    private TranscriptionRequestBuilder() {
        // Private constructor - use static factory methods
    }
    
    /**
     * Creates a new builder for file-based transcription.
     * 
     * @param model the model to use for transcription (required)
     * @return new builder instance
     */
    public static TranscriptionRequestBuilder create(String model) {
        var builder = new TranscriptionRequestBuilder();
        builder.model = model;
        return builder;
    }
    
    /**
     * Creates a new builder with file data for transcription.
     * 
     * @param model the model to use for transcription (required)
     * @param audioData the audio file data as byte array
     * @return new builder instance with file data set
     */
    public static TranscriptionRequestBuilder withFile(String model, byte[] audioData) {
        return create(model).file(audioData);
    }
    
    /**
     * Creates a new builder with URL for transcription.
     * 
     * @param model the model to use for transcription (required)
     * @param audioUrl the URL of the audio file
     * @return new builder instance with URL set
     */
    public static TranscriptionRequestBuilder withUrl(String model, String audioUrl) {
        return create(model).url(audioUrl);
    }
    
    /**
     * Sets the audio file data for transcription.
     * 
     * @param audioData the audio file data as byte array
     * @return this builder for method chaining
     */
    public TranscriptionRequestBuilder file(byte[] audioData) {
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
    public TranscriptionRequestBuilder file(byte[] audioData, String filename) {
        if (filename != null && !filename.isEmpty()) {
            FileUtils.validateAudioFile(audioData, filename);
        }
        return file(audioData);
    }
    
    /**
     * Sets the URL of the audio file for transcription.
     * 
     * @param audioUrl the URL of the audio file
     * @return this builder for method chaining
     */
    public TranscriptionRequestBuilder url(String audioUrl) {
        ValidationUtils.requireNonNull(audioUrl, "audioUrl");
        if (audioUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Audio URL cannot be empty");
        }
        this.url = Optional.of(audioUrl.trim());
        this.file = Optional.empty(); // Clear file if URL is set
        return this;
    }
    
    /**
     * Sets the language of the input audio (optional).
     * 
     * @param language the language code (e.g., "en", "es", "fr")
     * @return this builder for method chaining
     */
    public TranscriptionRequestBuilder language(String language) {
        if (language != null && !language.trim().isEmpty()) {
            this.language = Optional.of(language.trim());
        }
        return this;
    }
    
    /**
     * Sets an optional text prompt to guide the model's style or continue a previous audio segment.
     * 
     * @param prompt the prompt text
     * @return this builder for method chaining
     */
    public TranscriptionRequestBuilder prompt(String prompt) {
        if (prompt != null && !prompt.trim().isEmpty()) {
            this.prompt = Optional.of(prompt.trim());
        }
        return this;
    }
    
    /**
     * Sets the response format for the transcription.
     * 
     * @param format the response format (json, text, verbose_json)
     * @return this builder for method chaining
     */
    public TranscriptionRequestBuilder responseFormat(ResponseFormat format) {
        if (format != null) {
            this.responseFormat = Optional.of(format);
        }
        return this;
    }
    
    /**
     * Sets the temperature for the transcription (0.0 to 1.0).
     * 
     * @param temperature the temperature value
     * @return this builder for method chaining
     */
    public TranscriptionRequestBuilder temperature(double temperature) {
        ValidationUtils.validateRange(temperature, groq4j.utils.Constants.MIN_TEMPERATURE, groq4j.utils.Constants.MAX_TEMPERATURE, "temperature");
        this.temperature = Optional.of(temperature);
        return this;
    }
    
    /**
     * Sets the timestamp granularities for the transcription.
     * 
     * @param granularities the list of timestamp granularities
     * @return this builder for method chaining
     */
    public TranscriptionRequestBuilder timestampGranularities(List<TimestampGranularity> granularities) {
        if (granularities != null && !granularities.isEmpty()) {
            this.timestampGranularities = Optional.of(new ArrayList<>(granularities));
        }
        return this;
    }
    
    /**
     * Adds a single timestamp granularity.
     * 
     * @param granularity the timestamp granularity to add
     * @return this builder for method chaining
     */
    public TranscriptionRequestBuilder addTimestampGranularity(TimestampGranularity granularity) {
        if (granularity != null) {
            if (this.timestampGranularities.isEmpty()) {
                this.timestampGranularities = Optional.of(new ArrayList<>());
            }
            this.timestampGranularities.get().add(granularity);
        }
        return this;
    }
    
    /**
     * Convenience method to request word-level timestamps.
     * 
     * @return this builder for method chaining
     */
    public TranscriptionRequestBuilder withWordTimestamps() {
        return addTimestampGranularity(TimestampGranularity.WORD);
    }
    
    /**
     * Convenience method to request segment-level timestamps.
     * 
     * @return this builder for method chaining
     */
    public TranscriptionRequestBuilder withSegmentTimestamps() {
        return addTimestampGranularity(TimestampGranularity.SEGMENT);
    }
    
    /**
     * Convenience method to request both word and segment timestamps.
     * 
     * @return this builder for method chaining
     */
    public TranscriptionRequestBuilder withAllTimestamps() {
        return withWordTimestamps().withSegmentTimestamps();
    }
    
    /**
     * Sets JSON response format.
     * 
     * @return this builder for method chaining
     */
    public TranscriptionRequestBuilder asJson() {
        return responseFormat(ResponseFormat.JSON);
    }
    
    /**
     * Sets text response format.
     * 
     * @return this builder for method chaining
     */
    public TranscriptionRequestBuilder asText() {
        return responseFormat(ResponseFormat.TEXT);
    }
    
    /**
     * Sets verbose JSON response format.
     * 
     * @return this builder for method chaining
     */
    public TranscriptionRequestBuilder asVerboseJson() {
        return responseFormat(ResponseFormat.VERBOSE_JSON);
    }
    
    /**
     * Builds the TranscriptionRequest.
     * 
     * @return the configured TranscriptionRequest
     * @throws IllegalArgumentException if required fields are missing or invalid
     */
    public TranscriptionRequest build() {
        ValidationUtils.validateModel(model);
        
        if (file.isEmpty() && url.isEmpty()) {
            throw new IllegalArgumentException("Either file data or URL must be provided");
        }
        
        if (file.isPresent() && url.isPresent()) {
            throw new IllegalArgumentException("Cannot provide both file data and URL");
        }
        
        return new TranscriptionRequest(
            model,
            file,
            url,
            language,
            prompt,
            responseFormat,
            temperature,
            timestampGranularities
        );
    }
}
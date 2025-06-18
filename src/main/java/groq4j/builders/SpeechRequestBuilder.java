package groq4j.builders;

import groq4j.enums.AudioFormat;
import groq4j.enums.SampleRate;
import groq4j.models.audio.SpeechRequest;
import groq4j.utils.ValidationUtils;

import java.util.Optional;

/**
 * Builder for creating SpeechRequest objects with fluent API.
 * 
 * Supports text-to-speech synthesis with configurable voice,
 * audio format, sample rate, and speech speed.
 */
public class SpeechRequestBuilder {
    private String model;
    private String input;
    private String voice;
    private Optional<AudioFormat> responseFormat = Optional.empty();
    private Optional<SampleRate> sampleRate = Optional.empty();
    private Optional<Double> speed = Optional.empty();
    
    private SpeechRequestBuilder() {
        // Private constructor - use static factory methods
    }
    
    /**
     * Creates a new builder for speech synthesis.
     * 
     * @param model the model to use for speech synthesis (required)
     * @return new builder instance
     */
    public static SpeechRequestBuilder create(String model) {
        var builder = new SpeechRequestBuilder();
        builder.model = model;
        return builder;
    }
    
    /**
     * Creates a new builder with all required parameters.
     * 
     * @param model the model to use for speech synthesis (required)
     * @param input the text to convert to speech (required)
     * @param voice the voice to use for synthesis (required)
     * @return new builder instance with required fields set
     */
    public static SpeechRequestBuilder create(String model, String input, String voice) {
        return create(model).input(input).voice(voice);
    }
    
    /**
     * Sets the text input to convert to speech.
     * 
     * @param input the text to convert to speech
     * @return this builder for method chaining
     */
    public SpeechRequestBuilder input(String input) {
        ValidationUtils.requireNonNull(input, "input");
        if (input.trim().isEmpty()) {
            throw new IllegalArgumentException("Input text cannot be empty");
        }
        this.input = input.trim();
        return this;
    }
    
    /**
     * Sets the voice to use for speech synthesis.
     * 
     * @param voice the voice identifier
     * @return this builder for method chaining
     */
    public SpeechRequestBuilder voice(String voice) {
        ValidationUtils.requireNonNull(voice, "voice");
        if (voice.trim().isEmpty()) {
            throw new IllegalArgumentException("Voice cannot be empty");
        }
        this.voice = voice.trim();
        return this;
    }
    
    /**
     * Sets the audio format for the response.
     * 
     * @param format the audio format (flac, mp3, mulaw, ogg, wav)
     * @return this builder for method chaining
     */
    public SpeechRequestBuilder responseFormat(AudioFormat format) {
        if (format != null) {
            this.responseFormat = Optional.of(format);
        }
        return this;
    }
    
    /**
     * Sets the sample rate for the audio.
     * 
     * @param sampleRate the sample rate in Hz
     * @return this builder for method chaining
     */
    public SpeechRequestBuilder sampleRate(SampleRate sampleRate) {
        if (sampleRate != null) {
            this.sampleRate = Optional.of(sampleRate);
        }
        return this;
    }
    
    /**
     * Sets the speech speed (0.5 to 5.0).
     * 
     * @param speed the speech speed multiplier
     * @return this builder for method chaining
     */
    public SpeechRequestBuilder speed(double speed) {
        if (speed < 0.5 || speed > 5.0) {
            throw new IllegalArgumentException("Speed must be between 0.5 and 5.0");
        }
        this.speed = Optional.of(speed);
        return this;
    }
    
    /**
     * Sets MP3 audio format.
     * 
     * @return this builder for method chaining
     */
    public SpeechRequestBuilder asMp3() {
        return responseFormat(AudioFormat.MP3);
    }
    
    /**
     * Sets WAV audio format.
     * 
     * @return this builder for method chaining
     */
    public SpeechRequestBuilder asWav() {
        return responseFormat(AudioFormat.WAV);
    }
    
    /**
     * Sets FLAC audio format.
     * 
     * @return this builder for method chaining
     */
    public SpeechRequestBuilder asFlac() {
        return responseFormat(AudioFormat.FLAC);
    }
    
    /**
     * Sets OGG audio format.
     * 
     * @return this builder for method chaining
     */
    public SpeechRequestBuilder asOgg() {
        return responseFormat(AudioFormat.OGG);
    }
    
    /**
     * Sets mulaw audio format.
     * 
     * @return this builder for method chaining
     */
    public SpeechRequestBuilder asMulaw() {
        return responseFormat(AudioFormat.MULAW);
    }
    
    /**
     * Sets standard quality sample rate (22050 Hz).
     * 
     * @return this builder for method chaining
     */
    public SpeechRequestBuilder standardQuality() {
        return sampleRate(SampleRate.RATE_22050);
    }
    
    /**
     * Sets high quality sample rate (44100 Hz).
     * 
     * @return this builder for method chaining
     */
    public SpeechRequestBuilder highQuality() {
        return sampleRate(SampleRate.RATE_44100);
    }
    
    /**
     * Sets maximum quality sample rate (48000 Hz).
     * 
     * @return this builder for method chaining
     */
    public SpeechRequestBuilder maxQuality() {
        return sampleRate(SampleRate.RATE_48000);
    }
    
    /**
     * Sets normal speech speed (1.0x).
     * 
     * @return this builder for method chaining
     */
    public SpeechRequestBuilder normalSpeed() {
        return speed(1.0);
    }
    
    /**
     * Sets slow speech speed (0.75x).
     * 
     * @return this builder for method chaining
     */
    public SpeechRequestBuilder slowSpeed() {
        return speed(0.75);
    }
    
    /**
     * Sets fast speech speed (1.25x).
     * 
     * @return this builder for method chaining
     */
    public SpeechRequestBuilder fastSpeed() {
        return speed(1.25);
    }
    
    /**
     * Sets very fast speech speed (1.5x).
     * 
     * @return this builder for method chaining
     */
    public SpeechRequestBuilder veryFastSpeed() {
        return speed(1.5);
    }
    
    /**
     * Builds the SpeechRequest.
     * 
     * @return the configured SpeechRequest
     * @throws IllegalArgumentException if required fields are missing or invalid
     */
    public SpeechRequest build() {
        ValidationUtils.validateModel(model);
        ValidationUtils.requireNonNull(input, "input");
        ValidationUtils.requireNonNull(voice, "voice");
        
        if (input.trim().isEmpty()) {
            throw new IllegalArgumentException("Input text cannot be empty");
        }
        
        if (voice.trim().isEmpty()) {
            throw new IllegalArgumentException("Voice cannot be empty");
        }
        
        return new SpeechRequest(
            model,
            input,
            voice,
            responseFormat,
            sampleRate,
            speed
        );
    }
}
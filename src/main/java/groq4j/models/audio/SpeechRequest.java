package groq4j.models.audio;

import groq4j.enums.AudioFormat;
import groq4j.enums.SampleRate;

import java.util.Optional;

public record SpeechRequest(
    String model,
    String input,
    String voice,
    Optional<AudioFormat> responseFormat,
    Optional<SampleRate> sampleRate,
    Optional<Double> speed
) {
    public SpeechRequest {
        if (model == null || model.trim().isEmpty()) {
            throw new IllegalArgumentException("model cannot be null or empty");
        }
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("input cannot be null or empty");
        }
        if (voice == null || voice.trim().isEmpty()) {
            throw new IllegalArgumentException("voice cannot be null or empty");
        }
        
        speed.ifPresent(s -> {
            if (s < 0.5 || s > 5.0) {
                throw new IllegalArgumentException("speed must be between 0.5 and 5.0");
            }
        });
    }
    
    public boolean hasCustomResponseFormat() {
        return responseFormat.isPresent();
    }
    
    public boolean hasCustomSampleRate() {
        return sampleRate.isPresent();
    }
    
    public boolean hasCustomSpeed() {
        return speed.isPresent();
    }
    
    public static SpeechRequest simple(String model, String input, String voice) {
        return new SpeechRequest(
            model,
            input,
            voice,
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        );
    }
}
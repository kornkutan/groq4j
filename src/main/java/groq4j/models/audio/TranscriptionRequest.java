package groq4j.models.audio;

import groq4j.enums.ResponseFormat;
import groq4j.enums.TimestampGranularity;

import java.util.List;
import java.util.Optional;

public record TranscriptionRequest(
    String model,
    Optional<byte[]> file,
    Optional<String> url,
    Optional<String> language,
    Optional<String> prompt,
    Optional<ResponseFormat> responseFormat,
    Optional<Double> temperature,
    Optional<List<TimestampGranularity>> timestampGranularities
) {
    public TranscriptionRequest {
        if (model == null || model.trim().isEmpty()) {
            throw new IllegalArgumentException("model cannot be null or empty");
        }
        
        // Either file or url must be provided
        if (file.isEmpty() && url.isEmpty()) {
            throw new IllegalArgumentException("Either file or url must be provided");
        }
        
        // Both file and url cannot be provided at the same time
        if (file.isPresent() && url.isPresent()) {
            throw new IllegalArgumentException("Cannot provide both file and url");
        }
        
        temperature.ifPresent(t -> {
            if (t < 0.0 || t > 1.0) {
                throw new IllegalArgumentException("temperature must be between 0.0 and 1.0");
            }
        });
        
        language.ifPresent(lang -> {
            if (lang.trim().isEmpty()) {
                throw new IllegalArgumentException("language cannot be empty if provided");
            }
        });
        
        prompt.ifPresent(p -> {
            if (p.trim().isEmpty()) {
                throw new IllegalArgumentException("prompt cannot be empty if provided");
            }
        });
        
        timestampGranularities.ifPresent(tg -> {
            if (tg.isEmpty()) {
                throw new IllegalArgumentException("timestampGranularities cannot be empty if provided");
            }
        });
    }
    
    public boolean hasFile() {
        return file.isPresent();
    }
    
    public boolean hasUrl() {
        return url.isPresent();
    }
    
    public boolean hasTimestampGranularities() {
        return timestampGranularities.isPresent() && !timestampGranularities.get().isEmpty();
    }
    
    public static TranscriptionRequest withFile(String model, byte[] file) {
        return new TranscriptionRequest(
            model,
            Optional.of(file),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        );
    }
    
    public static TranscriptionRequest withUrl(String model, String url) {
        return new TranscriptionRequest(
            model,
            Optional.empty(),
            Optional.of(url),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        );
    }
}
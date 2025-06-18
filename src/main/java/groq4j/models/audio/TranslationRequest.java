package groq4j.models.audio;

import groq4j.enums.ResponseFormat;

import java.util.Optional;

public record TranslationRequest(
    String model,
    Optional<byte[]> file,
    Optional<String> url,
    Optional<String> prompt,
    Optional<ResponseFormat> responseFormat,
    Optional<Double> temperature
) {
    public TranslationRequest {
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
        
        prompt.ifPresent(p -> {
            if (p.trim().isEmpty()) {
                throw new IllegalArgumentException("prompt cannot be empty if provided");
            }
        });
    }
    
    public boolean hasFile() {
        return file.isPresent();
    }
    
    public boolean hasUrl() {
        return url.isPresent();
    }
    
    public static TranslationRequest withFile(String model, byte[] file) {
        return new TranslationRequest(
            model,
            Optional.of(file),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        );
    }
    
    public static TranslationRequest withUrl(String model, String url) {
        return new TranslationRequest(
            model,
            Optional.empty(),
            Optional.of(url),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        );
    }
}
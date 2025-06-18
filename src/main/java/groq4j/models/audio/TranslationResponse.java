package groq4j.models.audio;

import groq4j.models.common.XGroq;

import java.util.Optional;

public record TranslationResponse(
    String text,
    Optional<XGroq> xGroq
) {
    public TranslationResponse {
        if (text == null) {
            throw new IllegalArgumentException("text cannot be null");
        }
    }
    
    public boolean hasGroqMetadata() {
        return xGroq.isPresent();
    }
    
    public String getRequestId() {
        return xGroq.map(XGroq::id).orElse(null);
    }
    
    public Optional<String> getRequestIdSafe() {
        return xGroq.map(XGroq::id);
    }
    
    public boolean isEmpty() {
        return text.trim().isEmpty();
    }
    
    public int getTextLength() {
        return text.length();
    }
    
    public static TranslationResponse of(String text) {
        return new TranslationResponse(text, Optional.empty());
    }
    
    public static TranslationResponse withMetadata(String text, XGroq xGroq) {
        return new TranslationResponse(text, Optional.of(xGroq));
    }
}
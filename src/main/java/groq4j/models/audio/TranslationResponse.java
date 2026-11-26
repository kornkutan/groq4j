package groq4j.models.audio;

import groq4j.models.common.XGroq;

import java.util.List;
import java.util.Optional;

/** A translation result. Segments are present only for {@code verbose_json}. */
public record TranslationResponse(
    String text,
    Optional<XGroq> xGroq,
    Optional<String> language,
    Optional<Double> duration,
    List<TranscriptionSegment> segments
) {
    public TranslationResponse {
        if (text == null) {
            throw new IllegalArgumentException("text cannot be null");
        }
    }

    public TranslationResponse(String text, Optional<XGroq> xGroq) {
        this(text, xGroq, Optional.empty(), Optional.empty(), List.of());
    }

    public boolean hasGroqMetadata() {
        return xGroq.isPresent();
    }

    public boolean isEmpty() {
        return text.trim().isEmpty();
    }

    public int getTextLength() {
        return text.length();
    }

    public Optional<String> getRequestIdSafe() {
        return xGroq.map(XGroq::id);
    }

    public String getRequestId() {
        return xGroq.map(XGroq::id).orElse(null);
    }

    public static TranslationResponse of(String text) {
        return new TranslationResponse(text, Optional.empty());
    }
}

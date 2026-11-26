package groq4j.models.audio;

import groq4j.models.common.XGroq;

import java.util.List;
import java.util.Optional;

/**
 * A transcription result. {@code text} is always present; segments and words are only filled in
 * when {@code verbose_json} was requested.
 */
public record TranscriptionResponse(
    String text,
    Optional<XGroq> xGroq,
    Optional<String> language,
    Optional<Double> duration,
    List<TranscriptionSegment> segments,
    List<TranscriptionWord> words
) {
    public TranscriptionResponse {
        if (text == null) {
            throw new IllegalArgumentException("text cannot be null");
        }
    }

    public TranscriptionResponse(String text, Optional<XGroq> xGroq) {
        this(text, xGroq, Optional.empty(), Optional.empty(), List.of(), List.of());
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

    public boolean hasTimestamps() {
        return !segments.isEmpty() || !words.isEmpty();
    }

    public static TranscriptionResponse of(String text) {
        return new TranscriptionResponse(text, Optional.empty());
    }

    public static TranscriptionResponse withMetadata(String text, XGroq xGroq) {
        return new TranscriptionResponse(text, Optional.of(xGroq));
    }
}

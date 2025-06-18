package groq4j.models.models;

import java.util.Optional;

public record Model(
    String id,
    String object,
    long created,
    String ownedBy,
    boolean active,
    int contextWindow,
    Optional<Object> publicApps,
    Optional<Integer> maxCompletionTokens
) {
    public Model {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("id cannot be null or empty");
        }
        if (object == null || object.trim().isEmpty()) {
            throw new IllegalArgumentException("object cannot be null or empty");
        }
        if (ownedBy == null || ownedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("ownedBy cannot be null or empty");
        }
        if (contextWindow < 0) {
            throw new IllegalArgumentException("contextWindow cannot be negative");
        }
        if (created < 0) {
            throw new IllegalArgumentException("created timestamp cannot be negative");
        }
        
        maxCompletionTokens.ifPresent(mct -> {
            if (mct < 1) {
                throw new IllegalArgumentException("maxCompletionTokens must be positive if present");
            }
        });
    }
    
    public boolean isActive() {
        return active;
    }
    
    public boolean hasMaxCompletionTokens() {
        return maxCompletionTokens.isPresent();
    }
    
    public boolean hasPublicApps() {
        return publicApps.isPresent();
    }
    
    public int getEffectiveMaxTokens() {
        return maxCompletionTokens.orElse(contextWindow);
    }
    
    public String getDisplayName() {
        return id.replace("-", " ").toUpperCase();
    }
    
    public boolean isWhisperModel() {
        return id.toLowerCase().contains("whisper");
    }
    
    public boolean isChatModel() {
        return !isWhisperModel() && !id.toLowerCase().contains("tts");
    }
    
    public boolean isTtsModel() {
        return id.toLowerCase().contains("tts") || id.toLowerCase().contains("speech");
    }
    
    public static Model simple(String id, String ownedBy, boolean active, int contextWindow) {
        return new Model(
            id,
            "model",
            System.currentTimeMillis() / 1000,
            ownedBy,
            active,
            contextWindow,
            Optional.empty(),
            Optional.empty()
        );
    }
}
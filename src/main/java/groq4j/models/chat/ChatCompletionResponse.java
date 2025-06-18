package groq4j.models.chat;

import groq4j.models.common.Usage;
import groq4j.models.common.XGroq;

import java.util.List;
import java.util.Optional;

public record ChatCompletionResponse(
    String id,
    String object,
    long created,
    String model,
    List<Choice> choices,
    Usage usage,
    Optional<String> systemFingerprint,
    Optional<XGroq> xGroq
) {
    public ChatCompletionResponse {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("id cannot be null or empty");
        }
        if (object == null || object.trim().isEmpty()) {
            throw new IllegalArgumentException("object cannot be null or empty");
        }
        if (model == null || model.trim().isEmpty()) {
            throw new IllegalArgumentException("model cannot be null or empty");
        }
        if (choices == null || choices.isEmpty()) {
            throw new IllegalArgumentException("choices cannot be null or empty");
        }
        if (usage == null) {
            throw new IllegalArgumentException("usage cannot be null");
        }
        if (created < 0) {
            throw new IllegalArgumentException("created timestamp cannot be negative");
        }
    }
    
    public String getFirstChoiceContent() {
        return choices.getFirst().message().content().orElse("");
    }
    
    public Optional<String> getFirstChoiceContentSafe() {
        return choices.isEmpty() ? Optional.empty() : 
               choices.getFirst().message().content();
    }
    
    public String getFinishReason() {
        return choices.getFirst().finishReason();
    }
    
    public Optional<String> getFinishReasonSafe() {
        return choices.isEmpty() ? Optional.empty() : 
               Optional.ofNullable(choices.getFirst().finishReason());
    }
    
    public boolean hasToolCalls() {
        return !choices.isEmpty() && 
               choices.getFirst().message().toolCalls().isPresent() &&
               !choices.getFirst().message().toolCalls().get().isEmpty();
    }
    
    public int getTotalTokens() {
        return usage.totalTokens();
    }
    
    public int getPromptTokens() {
        return usage.promptTokens();
    }
    
    public int getCompletionTokens() {
        return usage.completionTokens();
    }
    
    public double getTotalTime() {
        return usage.totalTime();
    }
    
    public double getTokensPerSecond() {
        return getTotalTokens() / getTotalTime();
    }
    
}
package groq4j.models.chat;

import groq4j.models.common.Message;

import java.util.Optional;

public record Choice(
    int index,
    Message message,
    Optional<LogProbs> logprobs,
    String finishReason
) {
    public Choice {
        if (index < 0) {
            throw new IllegalArgumentException("index cannot be negative");
        }
        if (message == null) {
            throw new IllegalArgumentException("message cannot be null");
        }
        if (finishReason == null || finishReason.trim().isEmpty()) {
            throw new IllegalArgumentException("finishReason cannot be null or empty");
        }
    }
    
    public boolean hasLogProbs() {
        return logprobs.isPresent();
    }
    
    public boolean isFinishedNormally() {
        return "stop".equals(finishReason);
    }
    
    public boolean wasFinishedByLength() {
        return "length".equals(finishReason);
    }
    
    public boolean wasContentFiltered() {
        return "content_filter".equals(finishReason);
    }
    
    public boolean hasToolCalls() {
        return "tool_calls".equals(finishReason) ||
               (message.toolCalls().isPresent() && !message.toolCalls().get().isEmpty());
    }
    
    public String getContent() {
        return message.content().orElse("");
    }
    
    public Optional<String> getContentSafe() {
        return message.content();
    }
    
    public static Choice of(int index, Message message, String finishReason) {
        return new Choice(index, message, Optional.empty(), finishReason);
    }
    
    public static Choice withLogProbs(int index, Message message, LogProbs logprobs, String finishReason) {
        return new Choice(index, message, Optional.of(logprobs), finishReason);
    }
}
package groq4j.models.responses;

import java.util.Map;

/**
 * One server-sent event of a streamed response. The {@code type} matches OpenAI event names,
 * for example {@code response.output_text.delta} or {@code response.completed}.
 */
public record ResponseStreamEvent(String type, String delta, Map<String, Object> raw) {

    public boolean isTextDelta() {
        return "response.output_text.delta".equals(type);
    }

    public boolean isCompleted() {
        return "response.completed".equals(type);
    }

    public boolean isFailed() {
        return "response.failed".equals(type) || "error".equals(type);
    }

    public boolean isTerminal() {
        return isCompleted() || isFailed() || "response.incomplete".equals(type);
    }
}

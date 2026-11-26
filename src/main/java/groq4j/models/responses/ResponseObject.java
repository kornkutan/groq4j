package groq4j.models.responses;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The result of {@code POST /openai/v1/responses}.
 *
 * <p>{@code raw} keeps the full decoded body so fields this record does not model, such as
 * {@code incomplete_details} or per-response settings, stay reachable.
 */
public record ResponseObject(
    String id,
    String object,
    String status,
    long createdAt,
    String model,
    List<ResponseOutputItem> output,
    Optional<ResponseUsage> usage,
    Optional<String> errorMessage,
    Optional<String> instructions,
    Map<String, Object> metadata,
    Map<String, Object> raw
) {

    public boolean hasError() {
        return errorMessage.isPresent();
    }

    public String getOutputText() {
        var sb = new StringBuilder();
        for (ResponseOutputItem item : output) {
            if (item.isMessage()) {
                item.text().ifPresent(sb::append);
            }
        }
        return sb.toString();
    }

    public String getReasoningText() {
        var sb = new StringBuilder();
        for (ResponseOutputItem item : output) {
            if (item.isReasoning()) {
                item.text().ifPresent(sb::append);
            }
        }
        return sb.toString();
    }

    public List<ResponseOutputItem> getFunctionCalls() {
        return output.stream().filter(ResponseOutputItem::isFunctionCall).toList();
    }

    public boolean isCompleted() {
        return "completed".equals(status);
    }
}

package groq4j.models.responses;

import groq4j.utils.JsonUtils;

import java.util.Map;
import java.util.Optional;

/**
 * One entry of {@code output}. Three types exist: {@code message} with text content,
 * {@code function_call} for tool use, and {@code reasoning} which holds the reasoning trace.
 */
public record ResponseOutputItem(
    String type,
    String id,
    Optional<String> role,
    Optional<String> status,
    Optional<String> text,
    Optional<String> name,
    Optional<String> callId,
    Optional<String> arguments,
    Map<String, Object> raw
) {
    public static final String TYPE_MESSAGE = "message";
    public static final String TYPE_FUNCTION_CALL = "function_call";
    public static final String TYPE_REASONING = "reasoning";

    public static ResponseOutputItem fromMap(Map<String, Object> map) {
        String type = JsonUtils.stringValue(map.get("type"));
        Optional<String> text = Optional.empty();

        if (map.get("content") instanceof java.util.List<?> parts) {
            var sb = new StringBuilder();
            for (Object raw : parts) {
                Map<String, Object> part = JsonUtils.asMap(raw);
                if ("output_text".equals(JsonUtils.stringValue(part.get("type")))) {
                    sb.append(JsonUtils.stringValue(part.get("text")));
                }
            }
            if (!sb.isEmpty()) {
                text = Optional.of(sb.toString());
            }
        }
        if (TYPE_REASONING.equals(type)) {
            text = Optional.ofNullable(reasoningText(map));
        }

        return new ResponseOutputItem(
            type,
            JsonUtils.stringValue(map.get("id")),
            Optional.ofNullable(JsonUtils.stringValue(map.get("role"))),
            Optional.ofNullable(JsonUtils.stringValue(map.get("status"))),
            text,
            Optional.ofNullable(JsonUtils.stringValue(map.get("name"))),
            Optional.ofNullable(JsonUtils.stringValue(map.get("call_id"))),
            Optional.ofNullable(JsonUtils.stringValue(map.get("arguments"))),
            map);
    }

    private static String reasoningText(Map<String, Object> map) {
        var sb = new StringBuilder();
        for (Object raw : JsonUtils.asList(map.get("content"))) {
            Map<String, Object> part = JsonUtils.asMap(raw);
            if ("reasoning_text".equals(JsonUtils.stringValue(part.get("type")))) {
                String value = JsonUtils.stringValue(part.get("text"));
                if (value != null) {
                    sb.append(value);
                }
            }
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    public boolean isMessage() {
        return TYPE_MESSAGE.equals(type);
    }

    public boolean isFunctionCall() {
        return TYPE_FUNCTION_CALL.equals(type);
    }

    public boolean isReasoning() {
        return TYPE_REASONING.equals(type);
    }
}

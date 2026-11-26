package groq4j.models.chat;

import java.util.Map;
import java.util.Optional;

/**
 * A partial tool call inside a streamed chunk. The name usually arrives in the first delta and
 * the arguments accumulate across chunks, indexed by {@code index}.
 */
public record ToolCallDelta(
    int index,
    Optional<String> id,
    Optional<String> type,
    Optional<String> functionName,
    Optional<String> arguments
) {
    public static ToolCallDelta fromMap(Map<String, Object> map) {
        Double index = groq4j.utils.JsonUtils.doubleValue(map.get("index"));
        Map<String, Object> function = groq4j.utils.JsonUtils.asMap(map.get("function"));
        return new ToolCallDelta(
            index == null ? 0 : index.intValue(),
            Optional.ofNullable(groq4j.utils.JsonUtils.stringValue(map.get("id"))),
            Optional.ofNullable(groq4j.utils.JsonUtils.stringValue(map.get("type"))),
            Optional.ofNullable(groq4j.utils.JsonUtils.stringValue(function.get("name"))),
            Optional.ofNullable(groq4j.utils.JsonUtils.stringValue(function.get("arguments"))));
    }
}

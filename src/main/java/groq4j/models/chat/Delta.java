package groq4j.models.chat;

import groq4j.utils.JsonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** The incremental message fragment carried by one streamed chunk. */
public record Delta(
    Optional<String> role,
    Optional<String> content,
    Optional<String> reasoning,
    Optional<String> refusal,
    List<ToolCallDelta> toolCalls,
    List<Annotation> annotations
) {
    public static Delta fromMap(Map<String, Object> map) {
        List<ToolCallDelta> toolCalls = new ArrayList<>();
        for (Object raw : JsonUtils.asList(map.get("tool_calls"))) {
            toolCalls.add(ToolCallDelta.fromMap(JsonUtils.asMap(raw)));
        }
        List<Annotation> annotations = new ArrayList<>();
        for (Object raw : JsonUtils.asList(map.get("annotations"))) {
            annotations.add(Annotation.fromMap(JsonUtils.asMap(raw)));
        }
        return new Delta(
            Optional.ofNullable(JsonUtils.stringValue(map.get("role"))),
            Optional.ofNullable(JsonUtils.stringValue(map.get("content"))),
            Optional.ofNullable(JsonUtils.stringValue(map.get("reasoning"))),
            Optional.ofNullable(JsonUtils.stringValue(map.get("refusal"))),
            List.copyOf(toolCalls),
            List.copyOf(annotations));
    }
}

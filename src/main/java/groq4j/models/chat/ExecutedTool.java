package groq4j.models.chat;

import groq4j.utils.JsonUtils;

import java.util.Map;
import java.util.Optional;

/**
 * A tool Groq ran on the server, such as browser search or code execution, when the model is a
 * compound system. The payload shapes differ per tool, so {@code details} keeps the raw object.
 */
public record ExecutedTool(
    int index,
    String type,
    String arguments,
    Optional<String> output,
    Map<String, Object> details
) {
    public static ExecutedTool fromMap(Map<String, Object> map) {
        Double index = JsonUtils.doubleValue(map.get("index"));
        return new ExecutedTool(
            index == null ? 0 : index.intValue(),
            JsonUtils.stringValue(map.get("type")),
            JsonUtils.stringValue(map.get("arguments")),
            Optional.ofNullable(JsonUtils.stringValue(map.get("output"))),
            map);
    }

    public Map<String, Object> searchResults() {
        return JsonUtils.asMap(details.get("search_results"));
    }

    public java.util.List<Object> codeResults() {
        return JsonUtils.asList(details.get("code_results"));
    }

    public java.util.List<Object> browserResults() {
        return JsonUtils.asList(details.get("browser_results"));
    }
}

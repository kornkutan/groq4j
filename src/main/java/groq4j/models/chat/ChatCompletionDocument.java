package groq4j.models.chat;

import groq4j.utils.JsonUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A document supplied to the model as context. Citations in the answer point back to the
 * optional id, which makes this the supported way to do retrieval without a separate RAG model.
 */
public record ChatCompletionDocument(String id, String type, Optional<String> text, Optional<Map<String, Object>> data) {

    public static ChatCompletionDocument text(String id, String text) {
        return new ChatCompletionDocument(id, "text", Optional.of(text), Optional.empty());
    }

    public static ChatCompletionDocument json(String id, Map<String, Object> data) {
        return new ChatCompletionDocument(id, "json", Optional.empty(), Optional.of(data));
    }

    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        if (id != null) {
            map.put("id", id);
        }
        var source = new LinkedHashMap<String, Object>();
        source.put("type", type);
        text.ifPresent(value -> source.put("text", value));
        data.ifPresent(value -> source.put("data", value));
        map.put("source", source);
        return map;
    }

    public static ChatCompletionDocument fromMap(Map<String, Object> map) {
        Map<String, Object> source = JsonUtils.asMap(map.get("source"));
        return new ChatCompletionDocument(
            JsonUtils.stringValue(map.get("id")),
            JsonUtils.stringValue(source.get("type")),
            Optional.ofNullable(JsonUtils.stringValue(source.get("text"))),
            Optional.ofNullable(source.get("data")).map(JsonUtils::asMap));
    }
}

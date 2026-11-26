package groq4j.models.responses;

import groq4j.models.chat.ContentPart;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One item in the {@code input} array of a response request. Groq supports plain messages,
 * function call echoes and function call outputs. Stateful conversations are not supported, so
 * the client keeps and resends the history.
 */
public record ResponseInputItem(String type, Map<String, Object> fields) {

    public static ResponseInputItem message(String role, String text) {
        var map = new LinkedHashMap<String, Object>();
        map.put("type", "message");
        map.put("role", role);
        map.put("content", List.of(Map.of("type", "input_text", "text", text)));
        return new ResponseInputItem("message", map);
    }

    /** Message with text and image parts; images use {@code input_image}. */
    public static ResponseInputItem messageWithParts(String role, List<ContentPart> parts) {
        var content = new java.util.ArrayList<Map<String, Object>>();
        for (ContentPart part : parts) {
            if (part.isImage()) {
                var image = new LinkedHashMap<String, Object>();
                image.put("type", "input_image");
                image.put("image_url", part.imageUrl().orElse(null));
                part.detail().ifPresent(detail -> image.put("detail", detail));
                content.add(image);
            } else {
                content.add(Map.of("type", "input_text", "text", part.text().orElse("")));
            }
        }
        var map = new LinkedHashMap<String, Object>();
        map.put("type", "message");
        map.put("role", role);
        map.put("content", content);
        return new ResponseInputItem("message", map);
    }

    public static ResponseInputItem functionCall(String id, String callId, String name, String arguments) {
        var map = new LinkedHashMap<String, Object>();
        map.put("type", "function_call");
        map.put("id", id);
        map.put("call_id", callId);
        map.put("name", name);
        map.put("arguments", arguments);
        return new ResponseInputItem("function_call", map);
    }

    public static ResponseInputItem functionCallOutput(String callId, String output) {
        var map = new LinkedHashMap<String, Object>();
        map.put("type", "function_call_output");
        map.put("call_id", callId);
        map.put("output", output);
        return new ResponseInputItem("function_call_output", map);
    }

    public Map<String, Object> toMap() {
        return fields;
    }
}

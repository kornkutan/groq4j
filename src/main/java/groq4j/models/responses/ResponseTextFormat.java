package groq4j.models.responses;

import java.util.LinkedHashMap;
import java.util.Map;

/** The {@code text.format} block: plain text, JSON mode, or a JSON schema. */
public record ResponseTextFormat(String type, String name, Map<String, Object> schema, boolean strict) {

    public static ResponseTextFormat text() {
        return new ResponseTextFormat("text", null, null, false);
    }

    public static ResponseTextFormat jsonObject() {
        return new ResponseTextFormat("json_object", null, null, false);
    }

    public static ResponseTextFormat jsonSchema(String name, Map<String, Object> schema) {
        return new ResponseTextFormat("json_schema", name, schema, true);
    }

    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("type", type);
        if (name != null) {
            map.put("name", name);
        }
        if (schema != null) {
            map.put("schema", schema);
        }
        if (strict) {
            map.put("strict", true);
        }
        return map;
    }
}

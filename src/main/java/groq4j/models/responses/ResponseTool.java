package groq4j.models.responses;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A tool offered to the Responses API. Functions use a flat shape, unlike chat completions which
 * nest the definition under {@code function}.
 */
public record ResponseTool(String name, Optional<String> description,
                           Optional<Map<String, Object>> parameters, Optional<Boolean> strict) {

    public static ResponseTool function(String name, String description, Map<String, Object> parameters) {
        return new ResponseTool(name, Optional.ofNullable(description),
            Optional.ofNullable(parameters), Optional.empty());
    }

    public static ResponseTool strictFunction(String name, String description, Map<String, Object> parameters) {
        return new ResponseTool(name, Optional.ofNullable(description),
            Optional.ofNullable(parameters), Optional.of(true));
    }

    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("type", "function");
        map.put("name", name);
        description.ifPresent(value -> map.put("description", value));
        parameters.ifPresent(value -> map.put("parameters", value));
        strict.ifPresent(value -> map.put("strict", value));
        return map;
    }
}

package groq4j.models.common;

import groq4j.enums.ToolType;

import java.util.Map;
import java.util.Optional;

public record Tool(
    ToolType type,
    FunctionDefinition function
) {
    public static Tool function(String name, String description, Optional<Map<String, Object>> parameters) {
        return new Tool(ToolType.FUNCTION, new FunctionDefinition(name, description, parameters));
    }

    public static Tool function(String name, String description) {
        return function(name, description, Optional.empty());
    }

    public Map<String, Object> toMap() {
        var map = new java.util.HashMap<String, Object>();
        map.put("type", type.getValue());
        map.put("function", function.toMap());
        return map;
    }

    public static Tool fromJson(String json, String path) {
        String typeStr = groq4j.utils.ResponseParser.getRequiredString(json, path + ".type");
        ToolType type = ToolType.fromValue(typeStr);
        FunctionDefinition function = FunctionDefinition.fromJson(json, path + ".function");
        
        return new Tool(type, function);
    }

    public record FunctionDefinition(
        String name,
        String description,
        Optional<Map<String, Object>> parameters
    ) {
        public Map<String, Object> toMap() {
            var map = new java.util.HashMap<String, Object>();
            map.put("name", name);
            map.put("description", description);
            parameters.ifPresent(p -> map.put("parameters", p));
            return map;
        }

        public static FunctionDefinition fromJson(String json, String path) {
            String name = groq4j.utils.ResponseParser.getRequiredString(json, path + ".name");
            String description = groq4j.utils.ResponseParser.getRequiredString(json, path + ".description");
            
            // TODO: Parse parameters object when needed
            Optional<Map<String, Object>> parameters = Optional.empty();
            
            return new FunctionDefinition(name, description, parameters);
        }
    }
}
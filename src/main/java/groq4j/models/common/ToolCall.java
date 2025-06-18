package groq4j.models.common;

import groq4j.enums.ToolType;

import java.util.Map;

public record ToolCall(
    String id,
    ToolType type,
    FunctionCall function
) {
    public static ToolCall function(String id, String name, String arguments) {
        return new ToolCall(id, ToolType.FUNCTION, new FunctionCall(name, arguments));
    }

    public Map<String, Object> toMap() {
        var map = new java.util.HashMap<String, Object>();
        map.put("id", id);
        map.put("type", type.getValue());
        map.put("function", function.toMap());
        return map;
    }

    public static ToolCall fromJson(String json, String path) {
        String id = groq4j.utils.ResponseParser.getRequiredString(json, path + ".id");
        String typeStr = groq4j.utils.ResponseParser.getRequiredString(json, path + ".type");
        ToolType type = ToolType.fromValue(typeStr);
        FunctionCall function = FunctionCall.fromJson(json, path + ".function");
        
        return new ToolCall(id, type, function);
    }

    public record FunctionCall(String name, String arguments) {
        public Map<String, Object> toMap() {
            var map = new java.util.HashMap<String, Object>();
            map.put("name", name);
            map.put("arguments", arguments);
            return map;
        }

        public static FunctionCall fromJson(String json, String path) {
            String name = groq4j.utils.ResponseParser.getRequiredString(json, path + ".name");
            String arguments = groq4j.utils.ResponseParser.getRequiredString(json, path + ".arguments");
            return new FunctionCall(name, arguments);
        }
    }
}
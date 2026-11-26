package groq4j.models.chat;

import groq4j.utils.JsonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Tools discovered on a remote MCP server, returned when MCP connectors are in use. */
public record McpToolList(
    String id,
    Optional<String> serverLabel,
    List<McpTool> tools
) {
    public static McpToolList fromMap(Map<String, Object> map) {
        List<McpTool> tools = new ArrayList<>();
        for (Object raw : JsonUtils.asList(map.get("tools"))) {
            tools.add(McpTool.fromMap(JsonUtils.asMap(raw)));
        }
        return new McpToolList(
            JsonUtils.stringValue(map.get("id")),
            Optional.ofNullable(JsonUtils.stringValue(map.get("server_label"))),
            tools);
    }

    /** One discovered MCP tool. */
    public record McpTool(String name, Optional<String> description, Map<String, Object> inputSchema) {
        public static McpTool fromMap(Map<String, Object> map) {
            return new McpTool(
                JsonUtils.stringValue(map.get("name")),
                Optional.ofNullable(JsonUtils.stringValue(map.get("description"))),
                JsonUtils.asMap(map.get("input_schema")));
        }
    }
}

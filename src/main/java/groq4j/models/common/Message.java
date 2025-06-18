package groq4j.models.common;

import groq4j.enums.MessageRole;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record Message(
    MessageRole role,
    Optional<String> content,
    Optional<String> name,
    Optional<List<ToolCall>> toolCalls,
    Optional<String> toolCallId,
    Optional<String> reasoning
) {
    public static Message system(String content) {
        return new Message(MessageRole.SYSTEM, Optional.of(content), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static Message system(String content, String name) {
        return new Message(MessageRole.SYSTEM, Optional.of(content), Optional.of(name), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static Message user(String content) {
        return new Message(MessageRole.USER, Optional.of(content), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static Message user(String content, String name) {
        return new Message(MessageRole.USER, Optional.of(content), Optional.of(name), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static Message assistant(String content) {
        return new Message(MessageRole.ASSISTANT, Optional.of(content), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static Message assistant(String content, List<ToolCall> toolCalls) {
        return new Message(MessageRole.ASSISTANT, Optional.of(content), Optional.empty(), Optional.of(toolCalls), Optional.empty(), Optional.empty());
    }

    public static Message assistantWithToolCalls(List<ToolCall> toolCalls) {
        return new Message(MessageRole.ASSISTANT, Optional.empty(), Optional.empty(), Optional.of(toolCalls), Optional.empty(), Optional.empty());
    }

    public static Message assistantWithReasoning(String content, String reasoning) {
        return new Message(MessageRole.ASSISTANT, Optional.of(content), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(reasoning));
    }

    public static Message tool(String content, String toolCallId) {
        return new Message(MessageRole.TOOL, Optional.of(content), Optional.empty(), Optional.empty(), Optional.of(toolCallId), Optional.empty());
    }
    
    // Factory method for builder compatibility
    public static Message of(MessageRole role, String content) {
        return new Message(role, Optional.of(content), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public Map<String, Object> toMap() {
        var map = new java.util.HashMap<String, Object>();
        map.put("role", role.getValue());
        
        // Content is optional (can be null for assistant messages with tool_calls)
        content.ifPresent(c -> map.put("content", c));
        
        // Name is optional for all message types
        name.ifPresent(n -> map.put("name", n));
        
        // Tool calls for assistant messages
        toolCalls.ifPresent(tc -> map.put("tool_calls", tc.stream().map(ToolCall::toMap).toList()));
        
        // Tool call ID for tool messages (not name field)
        toolCallId.ifPresent(tcId -> map.put("tool_call_id", tcId));
        
        // Reasoning for assistant messages (qwen3 models)
        reasoning.ifPresent(r -> map.put("reasoning", r));
        
        return map;
    }

    public static Message fromJson(String json, String path) {
        String roleStr = groq4j.utils.ResponseParser.getRequiredString(json, path + ".role");
        MessageRole role = MessageRole.fromValue(roleStr);
        
        // Content is optional for assistant messages with tool_calls
        Optional<String> content = groq4j.utils.ResponseParser.getOptionalString(json, path + ".content");
        Optional<String> name = groq4j.utils.ResponseParser.getOptionalString(json, path + ".name");
        Optional<String> toolCallId = groq4j.utils.ResponseParser.getOptionalString(json, path + ".tool_call_id");
        Optional<String> reasoning = groq4j.utils.ResponseParser.getOptionalString(json, path + ".reasoning");
        
        // TODO: Parse tool_calls when ToolCall is implemented
        Optional<List<ToolCall>> toolCalls = Optional.empty();
        
        return new Message(role, content, name, toolCalls, toolCallId, reasoning);
    }

    public boolean isFromUser() {
        return role == MessageRole.USER;
    }

    public boolean isFromAssistant() {
        return role == MessageRole.ASSISTANT;
    }

    public boolean isFromSystem() {
        return role == MessageRole.SYSTEM;
    }

    public boolean isFromTool() {
        return role == MessageRole.TOOL;
    }

    public boolean hasToolCalls() {
        return toolCalls.isPresent() && !toolCalls.get().isEmpty();
    }

    public int getContentLength() {
        return content.map(String::length).orElse(0);
    }
    
    public String getContentOrEmpty() {
        return content.orElse("");
    }
    
    public boolean hasContent() {
        return content.isPresent() && !content.get().isEmpty();
    }
    
    public boolean hasReasoning() {
        return reasoning.isPresent() && !reasoning.get().isEmpty();
    }
    
    public boolean hasToolCallId() {
        return toolCallId.isPresent() && !toolCallId.get().isEmpty();
    }
}
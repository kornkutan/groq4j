package groq4j.models.common;

import groq4j.enums.MessageRole;
import groq4j.models.chat.Annotation;
import groq4j.models.chat.ContentPart;
import groq4j.utils.JsonUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * One message in a chat conversation.
 *
 * <p>Content is either a plain string ({@code content}) or a list of parts ({@code contentParts})
 * when images are involved. Both are optional because an assistant message may carry only tool
 * calls, and a tool message carries only its result.
 */
public record Message(
    MessageRole role,
    Optional<String> content,
    Optional<String> name,
    Optional<List<ToolCall>> toolCalls,
    Optional<String> toolCallId,
    Optional<String> reasoning,
    Optional<List<ContentPart>> contentParts,
    Optional<List<Annotation>> annotations,
    Optional<String> refusal
) {

    public Message(MessageRole role, Optional<String> content, Optional<String> name,
                   Optional<List<ToolCall>> toolCalls, Optional<String> toolCallId, Optional<String> reasoning) {
        this(role, content, name, toolCalls, toolCallId, reasoning,
            Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static Message system(String content) {
        return of(MessageRole.SYSTEM, content);
    }

    public static Message system(String content, String name) {
        return new Message(MessageRole.SYSTEM, Optional.of(content), Optional.of(name), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static Message user(String content) {
        return of(MessageRole.USER, content);
    }

    public static Message user(String content, String name) {
        return new Message(MessageRole.USER, Optional.of(content), Optional.of(name), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    /** User message carrying text plus one or more images (public URL or {@code data:} URI). */
    public static Message userWithImages(String text, String... imageUrls) {
        var parts = new java.util.ArrayList<ContentPart>();
        if (text != null && !text.isEmpty()) {
            parts.add(ContentPart.text(text));
        }
        for (String url : imageUrls) {
            parts.add(ContentPart.image(url));
        }
        return userWithParts(parts);
    }

    public static Message userWithParts(List<ContentPart> parts) {
        return new Message(MessageRole.USER, Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.of(List.copyOf(parts)), Optional.empty(), Optional.empty());
    }

    public static Message assistant(String content) {
        return new Message(MessageRole.ASSISTANT, Optional.of(content), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static Message assistant(String content, List<ToolCall> toolCalls) {
        return new Message(MessageRole.ASSISTANT, Optional.of(content), Optional.empty(), Optional.of(toolCalls),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static Message assistantWithToolCalls(List<ToolCall> toolCalls) {
        return new Message(MessageRole.ASSISTANT, Optional.empty(), Optional.empty(), Optional.of(toolCalls),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static Message assistantWithReasoning(String content, String reasoning) {
        return new Message(MessageRole.ASSISTANT, Optional.of(content), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.of(reasoning), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static Message tool(String content, String toolCallId) {
        return new Message(MessageRole.TOOL, Optional.of(content), Optional.empty(), Optional.empty(),
            Optional.of(toolCallId), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static Message of(MessageRole role, String content) {
        return new Message(role, Optional.ofNullable(content), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("role", role.getValue());

        if (contentParts.isPresent() && !contentParts.get().isEmpty()) {
            map.put("content", contentParts.get().stream().map(ContentPart::toMap).toList());
        } else {
            content.ifPresent(value -> map.put("content", value));
        }

        name.ifPresent(value -> map.put("name", value));
        toolCalls.ifPresent(calls -> map.put("tool_calls", calls.stream().map(ToolCall::toMap).toList()));
        toolCallId.ifPresent(value -> map.put("tool_call_id", value));
        reasoning.ifPresent(value -> map.put("reasoning", value));

        return map;
    }

    public static Message fromMap(Map<String, Object> map) {
        MessageRole role = MessageRole.fromValue(JsonUtils.stringValue(map.get("role")));

        Optional<String> content = Optional.ofNullable(JsonUtils.stringValue(map.get("content")));
        Optional<List<ContentPart>> parts = Optional.empty();
        if (map.get("content") instanceof List<?> rawParts) {
            var parsed = new java.util.ArrayList<ContentPart>();
            for (Object rawPart : rawParts) {
                Map<String, Object> part = JsonUtils.asMap(rawPart);
                String type = JsonUtils.stringValue(part.get("type"));
                if (ContentPart.TYPE_IMAGE_URL.equals(type)) {
                    Map<String, Object> image = JsonUtils.asMap(part.get("image_url"));
                    parsed.add(new ContentPart(type, Optional.empty(),
                        Optional.ofNullable(JsonUtils.stringValue(image.get("url"))),
                        Optional.ofNullable(JsonUtils.stringValue(image.get("detail")))));
                } else {
                    parsed.add(ContentPart.text(JsonUtils.stringValue(part.get("text"))));
                }
            }
            parts = parsed.isEmpty() ? Optional.empty() : Optional.of(parsed);
        }

        Optional<List<ToolCall>> toolCalls = Optional.empty();
        if (map.get("tool_calls") instanceof List<?> rawCalls) {
            var parsed = new java.util.ArrayList<ToolCall>();
            for (Object rawCall : rawCalls) {
                ToolCall toolCall = ToolCall.fromMap(JsonUtils.asMap(rawCall));
                if (toolCall != null) {
                    parsed.add(toolCall);
                }
            }
            toolCalls = parsed.isEmpty() ? Optional.empty() : Optional.of(parsed);
        }

        Optional<List<Annotation>> annotations = Optional.empty();
        if (map.get("annotations") instanceof List<?> rawAnnotations) {
            var parsed = new java.util.ArrayList<Annotation>();
            for (Object rawAnnotation : rawAnnotations) {
                parsed.add(Annotation.fromMap(JsonUtils.asMap(rawAnnotation)));
            }
            annotations = parsed.isEmpty() ? Optional.empty() : Optional.of(parsed);
        }

        return new Message(
            role,
            content,
            Optional.ofNullable(JsonUtils.stringValue(map.get("name"))),
            toolCalls,
            Optional.ofNullable(JsonUtils.stringValue(map.get("tool_call_id"))),
            Optional.ofNullable(JsonUtils.stringValue(map.get("reasoning"))),
            parts,
            annotations,
            Optional.ofNullable(JsonUtils.stringValue(map.get("refusal"))));
    }

    public static Message fromJson(String json, String path) {
        Object node = JsonUtils.node(json, path);
        return fromMap(JsonUtils.asMap(node));
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

    public boolean hasImages() {
        return contentParts.map(parts -> parts.stream().anyMatch(ContentPart::isImage)).orElse(false);
    }
}

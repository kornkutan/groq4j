package groq4j.models.chat;

import groq4j.utils.JsonUtils;

import java.util.Map;
import java.util.Optional;

/** A citation annotation attached to a chat message. */
public record Annotation(
    String type,
    Optional<DocumentCitation> documentCitation,
    Optional<FunctionCitation> functionCitation
) {
    public static final String TYPE_DOCUMENT = "document_citation";
    public static final String TYPE_FUNCTION = "function_citation";

    public static Annotation fromMap(Map<String, Object> map) {
        String type = JsonUtils.stringValue(map.get("type"));
        Optional<DocumentCitation> documentCitation = Optional.empty();
        Optional<FunctionCitation> functionCitation = Optional.empty();

        if (map.get("document_citation") instanceof Map<?, ?> citation) {
            Map<String, Object> c = JsonUtils.asMap(citation);
            documentCitation = Optional.of(new DocumentCitation(
                intOf(c.get("start_index")),
                intOf(c.get("end_index")),
                JsonUtils.stringValue(c.get("document_id"))));
        }
        if (map.get("function_citation") instanceof Map<?, ?> citation) {
            Map<String, Object> c = JsonUtils.asMap(citation);
            functionCitation = Optional.of(new FunctionCitation(
                intOf(c.get("start_index")),
                intOf(c.get("end_index")),
                JsonUtils.stringValue(c.get("tool_call_id"))));
        }
        return new Annotation(type, documentCitation, functionCitation);
    }

    private static int intOf(Object value) {
        Double number = JsonUtils.doubleValue(value);
        return number == null ? 0 : number.intValue();
    }

    public boolean isDocumentCitation() {
        return TYPE_DOCUMENT.equals(type);
    }

    public boolean isFunctionCitation() {
        return TYPE_FUNCTION.equals(type);
    }
}

package groq4j.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JsonUtils parsing and serialization")
class JsonUtilsTest {

    @Test
    void parsesNestedObjectsAndArrays() {
        String json = """
            {"id":"chatcmpl-1","n":42,"ratio":0.25,"ok":true,"none":null,
             "items":[{"a":1},{"a":2}],"nested":{"deep":{"value":"x"}}}
            """;

        Map<String, Object> root = JsonUtils.parseObject(json);

        assertEquals("chatcmpl-1", JsonUtils.stringValue(root.get("id")));
        assertEquals(42, JsonUtils.doubleValue(root.get("n")).intValue());
        assertEquals(0.25, JsonUtils.doubleValue(root.get("ratio")));
        assertEquals(Boolean.TRUE, root.get("ok"));
        assertNull(root.get("none"));
        assertEquals(2, JsonUtils.asList(root.get("items")).size());
        assertEquals("x", JsonUtils.stringValue(JsonUtils.node(root, "nested.deep.value")));
    }

    @Test
    void resolvesBracketPathsForArrays() {
        Map<String, Object> root = JsonUtils.parseObject("{\"choices\":[{\"message\":{\"content\":\"hi\"}}]}");

        assertEquals("hi", JsonUtils.stringValue(JsonUtils.node(root, "choices[0].message.content")));
        assertNull(JsonUtils.node(root, "choices[5].message.content"));
    }

    @Test
    void handlesEscapesAndUnicode() {
        String json = "{\"text\":\"line\\nquote\\\" slash\\\\ tab\\t caf\\u00e9 \\ud83d\\ude00\"}";
        Map<String, Object> root = JsonUtils.parseObject(json);

        assertEquals("line\nquote\" slash\\ tab\t caf\u00e9 \ud83d\ude00", root.get("text"));
    }

    @Test
    void escapesWhenWriting() {
        String json = JsonUtils.toJsonString(Map.of(
            "text", "a\"b\\c\nd",
            "list", List.of("x", "y"),
            "flag", true));

        Map<String, Object> parsed = JsonUtils.parseObject(json);

        assertEquals("a\"b\\c\nd", parsed.get("text"));
        assertEquals(List.of("x", "y"), parsed.get("list"));
        assertEquals(Boolean.TRUE, parsed.get("flag"));
    }

    @Test
    void rejectsMalformedInput() {
        assertThrows(RuntimeException.class, () -> JsonUtils.parseObject("{\"a\":}"));
        assertThrows(RuntimeException.class, () -> JsonUtils.parseObject("{\"a\":1"));
        assertThrows(RuntimeException.class, () -> JsonUtils.parseObject(""));
    }

    @Test
    void parsesTopLevelArray() {
        List<Object> values = JsonUtils.parseArray("[1,\"two\",false,null]");

        assertEquals(4, values.size());
        assertEquals(1, JsonUtils.doubleValue(values.get(0)).intValue());
        assertEquals("two", values.get(1));
        assertNull(values.get(3));
    }

    @Test
    void emptyCollectionsRoundTrip() {
        assertEquals(List.of(), JsonUtils.parseArray("[]"));
        assertTrue(JsonUtils.parseObject("{}").isEmpty());
    }

    @Test
    void parsesLargeNumbersAndExponents() {
        Map<String, Object> root = JsonUtils.parseObject("{\"big\":1234567890123,\"exp\":1.5e3}");

        assertEquals(1234567890123L, ((Number) root.get("big")).longValue());
        assertEquals(1500.0, JsonUtils.doubleValue(root.get("exp")));
    }
}

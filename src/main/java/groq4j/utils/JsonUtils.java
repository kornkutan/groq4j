package groq4j.utils;

import groq4j.exceptions.GroqSerializationException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Minimal, dependency-free JSON reader and writer.
 *
 * <p>Reading produces plain Java values: {@link Map} for objects, {@link List} for arrays,
 * {@link String}, {@link Long} or {@link Double} for numbers, {@link Boolean} and {@code null}.
 * Writing accepts the same shapes plus enums and {@link Optional}.
 */
public final class JsonUtils {
    private JsonUtils() {
        // Prevent instantiation
    }

    // ---------------------------------------------------------------- writing

    public static String toJsonString(Object obj) {
        if (obj == null) {
            return "null";
        } else if (obj instanceof String s) {
            return quote(s);
        } else if (obj instanceof Boolean || obj instanceof Number) {
            return obj.toString();
        } else if (obj instanceof Optional<?> optional) {
            return optional.map(JsonUtils::toJsonString).orElse("null");
        } else if (obj instanceof Map<?, ?> map) {
            return mapToJson(map);
        } else if (obj instanceof Iterable<?> iterable) {
            return iterableToJson(iterable);
        } else if (obj instanceof Object[] array) {
            return iterableToJson(List.of(array));
        } else if (obj instanceof Enum<?> enumValue) {
            return quote(enumValue.toString());
        }
        throw new GroqSerializationException("Unsupported object type for JSON serialization: " + obj.getClass());
    }

    private static String mapToJson(Map<?, ?> map) {
        var sb = new StringBuilder("{");
        boolean first = true;
        for (var entry : map.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            if (!first) {
                sb.append(',');
            }
            sb.append(quote(entry.getKey().toString())).append(':').append(toJsonString(entry.getValue()));
            first = false;
        }
        return sb.append('}').toString();
    }

    private static String iterableToJson(Iterable<?> items) {
        var sb = new StringBuilder("[");
        boolean first = true;
        for (Object item : items) {
            if (!first) {
                sb.append(',');
            }
            sb.append(toJsonString(item));
            first = false;
        }
        return sb.append(']').toString();
    }

    public static String quote(String value) {
        return "\"" + escapeJson(value) + "\"";
    }

    public static String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        var sb = new StringBuilder(str.length() + 16);
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------- reading

    /** Parses any JSON document into plain Java values. */
    public static Object parse(String json) {
        if (json == null || json.isBlank()) {
            throw GroqSerializationException.invalidResponseFormat("Cannot parse empty JSON");
        }
        return new Reader(json).readDocument();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String json) {
        Object parsed = parse(json);
        if (parsed instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        throw GroqSerializationException.invalidResponseFormat("Expected a JSON object but got " + describe(parsed));
    }

    @SuppressWarnings("unchecked")
    public static List<Object> parseArray(String json) {
        Object parsed = parse(json);
        if (parsed instanceof List<?> list) {
            return (List<Object>) list;
        }
        throw GroqSerializationException.invalidResponseFormat("Expected a JSON array but got " + describe(parsed));
    }

    /**
     * Resolves a dot separated path such as {@code usage.prompt_tokens} or
     * {@code choices[0].message.content} against a raw JSON document.
     *
     * @return the value at the path, or {@code null} when any segment is missing
     */
    public static Object node(String json, String path) {
        if (json == null) {
            return null;
        }
        return node(parse(json), path);
    }

    public static Object node(Object root, String path) {
        if (root == null) {
            return null;
        }
        if (path == null || path.isEmpty()) {
            return root;
        }

        Object current = root;
        for (String segment : path.split("\\.")) {
            if (current == null) {
                return null;
            }
            String key = segment;
            Integer index = null;
            int bracket = segment.indexOf('[');
            if (bracket >= 0) {
                key = segment.substring(0, bracket);
                int close = segment.indexOf(']', bracket);
                if (close < 0) {
                    return null;
                }
                try {
                    index = Integer.valueOf(segment.substring(bracket + 1, close));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            if (!key.isEmpty()) {
                if (!(current instanceof Map<?, ?> map)) {
                    return null;
                }
                current = map.get(key);
            }
            if (index != null) {
                if (!(current instanceof List<?> list) || index < 0 || index >= list.size()) {
                    return null;
                }
                current = list.get(index);
            }
        }
        return current;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> asMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    @SuppressWarnings("unchecked")
    public static List<Object> asList(Object value) {
        return value instanceof List<?> list ? (List<Object>) list : List.of();
    }

    // ------------------------------------------------- path based accessors

    public static String extractStringValue(String json, String path) {
        return stringValue(node(json, path));
    }

    public static Integer extractIntValue(String json, String path) {
        Double value = doubleValue(node(json, path));
        return value == null ? null : value.intValue();
    }

    public static Long extractLongValue(String json, String path) {
        Object value = node(json, path);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    public static Double extractDoubleValue(String json, String path) {
        return doubleValue(node(json, path));
    }

    public static Boolean extractBooleanValue(String json, String path) {
        Object value = node(json, path);
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof String s) {
            return Boolean.parseBoolean(s);
        }
        return null;
    }

    public static int extractIntValuePrimitive(String json, String path) {
        Integer value = extractIntValue(json, path);
        return value != null ? value : 0;
    }

    public static long extractLongValuePrimitive(String json, String path) {
        Long value = extractLongValue(json, path);
        return value != null ? value : 0L;
    }

    public static boolean extractBooleanValuePrimitive(String json, String path) {
        Boolean value = extractBooleanValue(json, path);
        return value != null && value;
    }

    /** Returns the JSON text of the array at the path, or {@code "[]"} when absent. */
    public static String extractArrayValue(String json, String path) {
        Object value = node(json, path);
        return value instanceof List<?> ? toJsonString(value) : "[]";
    }

    /** Returns the JSON text of the object at the path, or {@code null} when absent. */
    public static String extractObjectValue(String json, String path) {
        Object value = node(json, path);
        return value instanceof Map<?, ?> ? toJsonString(value) : null;
    }

    public static Optional<Object> extractOptionalValue(String json, String path) {
        return Optional.ofNullable(node(json, path));
    }

    public static Optional<Integer> extractOptionalIntValue(String json, String path) {
        return Optional.ofNullable(extractIntValue(json, path));
    }

    public static Optional<Long> extractOptionalLongValue(String json, String path) {
        return Optional.ofNullable(extractLongValue(json, path));
    }

    public static Optional<String> extractOptionalStringValue(String json, String path) {
        return Optional.ofNullable(extractStringValue(json, path));
    }

    public static Optional<Map<String, String>> extractOptionalMapValue(String json, String path) {
        Object value = node(json, path);
        if (!(value instanceof Map<?, ?> map) || map.isEmpty()) {
            return Optional.empty();
        }
        var result = new LinkedHashMap<String, String>();
        map.forEach((k, v) -> result.put(String.valueOf(k), v == null ? null : String.valueOf(v)));
        return Optional.of(result);
    }

    // ------------------------------------------------------------ conversion

    public static String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String s) {
            return s;
        }
        if (value instanceof Map<?, ?> || value instanceof List<?>) {
            return toJsonString(value);
        }
        return String.valueOf(value);
    }

    public static Double doubleValue(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String s) {
            try {
                return Double.valueOf(s);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private static String describe(Object value) {
        return value == null ? "null" : value.getClass().getSimpleName();
    }

    // ---------------------------------------------------------------- helpers

    @FunctionalInterface
    public interface JsonParser<T> {
        T parse(String json);
    }

    public static <T> List<T> parseJsonArray(String jsonArray, JsonParser<T> parser) {
        if (jsonArray == null || jsonArray.isBlank()) {
            return List.of();
        }
        List<Object> elements = parseArray(jsonArray);
        List<T> result = new ArrayList<>(elements.size());
        for (Object element : elements) {
            result.add(parser.parse(toJsonString(element)));
        }
        return result;
    }

    /** Recursive descent reader for RFC 8259 JSON. */
    private static final class Reader {
        private final String source;
        private int pos;

        Reader(String source) {
            this.source = source;
        }

        Object readDocument() {
            skipWhitespace();
            Object value = readValue();
            skipWhitespace();
            if (pos < source.length()) {
                throw error("Unexpected trailing content");
            }
            return value;
        }

        private Object readValue() {
            skipWhitespace();
            if (pos >= source.length()) {
                throw error("Unexpected end of input");
            }
            char c = source.charAt(pos);
            return switch (c) {
                case '{' -> readObject();
                case '[' -> readArray();
                case '"' -> readString();
                case 't' -> readLiteral("true", Boolean.TRUE);
                case 'f' -> readLiteral("false", Boolean.FALSE);
                case 'n' -> readLiteral("null", null);
                default -> readNumber();
            };
        }

        private Map<String, Object> readObject() {
            var map = new LinkedHashMap<String, Object>();
            expect('{');
            skipWhitespace();
            if (peek() == '}') {
                pos++;
                return map;
            }
            while (true) {
                skipWhitespace();
                String key = readString();
                skipWhitespace();
                expect(':');
                map.put(key, readValue());
                skipWhitespace();
                char c = next();
                if (c == '}') {
                    return map;
                }
                if (c != ',') {
                    throw error("Expected ',' or '}' in object");
                }
            }
        }

        private List<Object> readArray() {
            var list = new ArrayList<>();
            expect('[');
            skipWhitespace();
            if (peek() == ']') {
                pos++;
                return list;
            }
            while (true) {
                list.add(readValue());
                skipWhitespace();
                char c = next();
                if (c == ']') {
                    return list;
                }
                if (c != ',') {
                    throw error("Expected ',' or ']' in array");
                }
            }
        }

        private String readString() {
            expect('"');
            var sb = new StringBuilder();
            while (true) {
                if (pos >= source.length()) {
                    throw error("Unterminated string");
                }
                char c = source.charAt(pos++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c != '\\') {
                    sb.append(c);
                    continue;
                }
                char escaped = next();
                switch (escaped) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        if (pos + 4 > source.length()) {
                            throw error("Truncated unicode escape");
                        }
                        sb.append((char) Integer.parseInt(source.substring(pos, pos + 4), 16));
                        pos += 4;
                    }
                    default -> throw error("Invalid escape sequence \\" + escaped);
                }
            }
        }

        private Object readNumber() {
            int start = pos;
            if (peek() == '-' || peek() == '+') {
                pos++;
            }
            boolean fractional = false;
            while (pos < source.length()) {
                char c = source.charAt(pos);
                if (c >= '0' && c <= '9') {
                    pos++;
                } else if (c == '.' || c == 'e' || c == 'E' || c == '-' || c == '+') {
                    fractional = fractional || c == '.' || c == 'e' || c == 'E';
                    pos++;
                } else {
                    break;
                }
            }
            String text = source.substring(start, pos);
            if (text.isEmpty()) {
                throw error("Invalid number");
            }
            try {
                if (fractional) {
                    return Double.valueOf(text);
                }
                long value = Long.parseLong(text);
                return value;
            } catch (NumberFormatException e) {
                try {
                    return Double.valueOf(text);
                } catch (NumberFormatException ignored) {
                    throw error("Invalid number: " + text);
                }
            }
        }

        private Object readLiteral(String literal, Object value) {
            if (!source.startsWith(literal, pos)) {
                throw error("Invalid literal");
            }
            pos += literal.length();
            return value;
        }

        private void skipWhitespace() {
            while (pos < source.length() && Character.isWhitespace(source.charAt(pos))) {
                pos++;
            }
        }

        private char peek() {
            return pos < source.length() ? source.charAt(pos) : '\0';
        }

        private char next() {
            if (pos >= source.length()) {
                throw error("Unexpected end of input");
            }
            return source.charAt(pos++);
        }

        private void expect(char expected) {
            if (next() != expected) {
                throw error("Expected '" + expected + "'");
            }
        }

        private GroqSerializationException error(String message) {
            return GroqSerializationException.invalidResponseFormat(message + " at offset " + pos);
        }
    }
}

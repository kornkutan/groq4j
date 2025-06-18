package groq4j.utils;

import groq4j.exceptions.GroqSerializationException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class JsonUtils {
    private JsonUtils() {
        // Prevent instantiation
    }

    public static String toJsonString(Object obj) {
        if (obj == null) {
            return "null";
        } else if (obj instanceof String) {
            return "\"" + escapeJson((String) obj) + "\"";
        } else if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        } else if (obj instanceof Optional<?> optional) {
            return optional.map(JsonUtils::toJsonString).orElse("null");
        } else if (obj instanceof Map<?, ?> map) {
            return mapToJson(map);
        } else if (obj instanceof List<?> list) {
            return listToJson(list);
        } else if (obj instanceof Enum<?> enumValue) {
            return toJsonString(enumValue.toString());
        }
        throw new GroqSerializationException("Unsupported object type for JSON serialization: " + obj.getClass());
    }

    private static String mapToJson(Map<?, ?> map) {
        if (map.isEmpty()) {
            return "{}";
        }
        
        var sb = new StringBuilder("{");
        boolean first = true;
        for (var entry : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(escapeJson(entry.getKey().toString())).append("\":");
            sb.append(toJsonString(entry.getValue()));
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    private static String listToJson(List<?> list) {
        if (list.isEmpty()) {
            return "[]";
        }
        
        var sb = new StringBuilder("[");
        boolean first = true;
        for (var item : list) {
            if (!first) sb.append(",");
            sb.append(toJsonString(item));
            first = false;
        }
        sb.append("]");
        return sb.toString();
    }

    public static String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t")
                  .replace("\b", "\\b")
                  .replace("\f", "\\f");
    }

    public static String extractStringValue(String json, String path) {
        try {
            return extractValue(json, path, true);
        } catch (Exception e) {
            throw GroqSerializationException.jsonParsingError("Failed to extract string value at path: " + path, e);
        }
    }

    public static Integer extractIntValue(String json, String path) {
        try {
            String value = extractValue(json, path, false);
            return value != null ? Integer.parseInt(value) : null;
        } catch (NumberFormatException e) {
            throw GroqSerializationException.jsonParsingError("Failed to parse integer at path: " + path, e);
        }
    }

    public static Double extractDoubleValue(String json, String path) {
        try {
            String value = extractValue(json, path, false);
            return value != null ? Double.parseDouble(value) : null;
        } catch (NumberFormatException e) {
            throw GroqSerializationException.jsonParsingError("Failed to parse double at path: " + path, e);
        }
    }

    public static Boolean extractBooleanValue(String json, String path) {
        try {
            String value = extractValue(json, path, false);
            return value != null ? Boolean.parseBoolean(value) : null;
        } catch (Exception e) {
            throw GroqSerializationException.jsonParsingError("Failed to parse boolean at path: " + path, e);
        }
    }

    public static Long extractLongValue(String json, String path) {
        try {
            String value = extractValue(json, path, false);
            return value != null ? Long.parseLong(value) : null;
        } catch (NumberFormatException e) {
            throw GroqSerializationException.jsonParsingError("Failed to parse long at path: " + path, e);
        }
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
        return value != null ? value : false;
    }

    public static String extractArrayValue(String json, String path) {
        try {
            String keyPattern = "\"" + path.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
            int keyIndex = json.indexOf(keyPattern);
            if (keyIndex == -1) {
                return "[]";
            }
            
            int colonIndex = json.indexOf(":", keyIndex + keyPattern.length());
            if (colonIndex == -1) {
                return "[]";
            }
            
            int valueStart = colonIndex + 1;
            while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
                valueStart++;
            }
            
            if (valueStart >= json.length() || json.charAt(valueStart) != '[') {
                return "[]";
            }
            
            int arrayEnd = findArrayEnd(json, valueStart + 1);
            if (arrayEnd == -1) {
                return "[]";
            }
            
            return json.substring(valueStart, arrayEnd + 1);
        } catch (Exception e) {
            throw GroqSerializationException.jsonParsingError("Failed to extract array at path: " + path, e);
        }
    }

    public static Optional<Object> extractOptionalValue(String json, String path) {
        String value = extractValue(json, path, false);
        return value != null && !value.equals("null") ? Optional.of(value) : Optional.empty();
    }

    public static Optional<Integer> extractOptionalIntValue(String json, String path) {
        try {
            String value = extractValue(json, path, false);
            return (value != null && !value.equals("null")) ? Optional.of(Integer.parseInt(value)) : Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public static Optional<Long> extractOptionalLongValue(String json, String path) {
        try {
            String value = extractValue(json, path, false);
            return (value != null && !value.equals("null")) ? Optional.of(Long.parseLong(value)) : Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public static Optional<String> extractOptionalStringValue(String json, String path) {
        try {
            String value = extractValue(json, path, true);
            return (value != null && !value.equals("null")) ? Optional.of(value) : Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static Optional<Map<String, String>> extractOptionalMapValue(String json, String path) {
        try {
            String value = extractValue(json, path, false);
            if (value == null || value.equals("null") || value.trim().isEmpty()) {
                return Optional.empty();
            }
            
            // Simple map parsing for metadata - assumes string key-value pairs
            Map<String, String> map = new java.util.HashMap<>();
            if (value.startsWith("{") && value.endsWith("}")) {
                String content = value.substring(1, value.length() - 1).trim();
                if (!content.isEmpty()) {
                    String[] pairs = content.split(",");
                    for (String pair : pairs) {
                        String[] keyValue = pair.split(":", 2);
                        if (keyValue.length == 2) {
                            String key = keyValue[0].trim().replaceAll("^\"|\"+$", "");
                            String val = keyValue[1].trim().replaceAll("^\"|\"+$", "");
                            map.put(key, val);
                        }
                    }
                }
            }
            return map.isEmpty() ? Optional.empty() : Optional.of(map);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static String extractObjectValue(String json, String path) {
        try {
            String keyPattern = "\"" + path.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
            int keyIndex = json.indexOf(keyPattern);
            if (keyIndex == -1) {
                return null;
            }
            
            int colonIndex = json.indexOf(":", keyIndex + keyPattern.length());
            if (colonIndex == -1) {
                return null;
            }
            
            int valueStart = colonIndex + 1;
            while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
                valueStart++;
            }
            
            if (valueStart >= json.length() || json.charAt(valueStart) != '{') {
                return null;
            }
            
            int objectEnd = findObjectEnd(json, valueStart + 1);
            if (objectEnd == -1) {
                return null;
            }
            
            return json.substring(valueStart, objectEnd + 1);
        } catch (Exception e) {
            throw GroqSerializationException.jsonParsingError("Failed to extract object at path: " + path, e);
        }
    }

    private static int findObjectEnd(String json, int start) {
        int braceCount = 1;
        boolean inString = false;
        
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            
            if (!inString) {
                if (c == '"') {
                    inString = true;
                } else if (c == '{') {
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                    if (braceCount == 0) {
                        return i;
                    }
                }
            } else {
                if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                    inString = false;
                }
            }
        }
        
        return -1;
    }

    @FunctionalInterface
    public interface JsonParser<T> {
        T parse(String json);
    }

    public static <T> List<T> parseJsonArray(String jsonArray, JsonParser<T> parser) {
        if (jsonArray == null || jsonArray.trim().isEmpty() || jsonArray.equals("[]")) {
            return List.of();
        }
        
        String content = jsonArray.trim();
        if (content.startsWith("[") && content.endsWith("]")) {
            content = content.substring(1, content.length() - 1).trim();
        }
        
        if (content.isEmpty()) {
            return List.of();
        }
        
        String[] elements = parseArrayElements(content);
        return java.util.Arrays.stream(elements)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(parser::parse)
                .collect(java.util.stream.Collectors.toList());
    }

    private static String extractValue(String json, String path, boolean isString) {
        String[] parts = path.split("\\.");
        String currentJson = json;
        
        for (String part : parts) {
            if (part.contains("[") && part.contains("]")) {
                String arrayKey = part.substring(0, part.indexOf("["));
                int index = Integer.parseInt(part.substring(part.indexOf("[") + 1, part.indexOf("]")));
                currentJson = extractArrayElement(currentJson, arrayKey, index);
            } else {
                currentJson = extractObjectValue(currentJson, part, isString && part.equals(parts[parts.length - 1]));
            }
            
            if (currentJson == null) {
                return null;
            }
        }
        
        return currentJson;
    }

    private static String extractObjectValue(String json, String key, boolean isStringValue) {
        // Look for the exact key pattern: "key"
        String keyPattern = "\"" + key.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        int keyIndex = json.indexOf(keyPattern);
        if (keyIndex == -1) {
            return null;
        }
        
        // Find the colon after the key
        int colonIndex = json.indexOf(":", keyIndex + keyPattern.length());
        if (colonIndex == -1) {
            return null;
        }
        
        // Skip whitespace after colon
        int valueStart = colonIndex + 1;
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }
        
        if (valueStart >= json.length()) {
            return null;
        }
        
        if (isStringValue) {
            if (json.charAt(valueStart) == '"') {
                int valueEnd = findStringEnd(json, valueStart + 1);
                if (valueEnd != -1) {
                    return json.substring(valueStart + 1, valueEnd);
                }
            }
        } else {
            int valueEnd = findValueEnd(json, valueStart);
            if (valueEnd != -1) {
                return json.substring(valueStart, valueEnd).trim();
            }
        }
        
        return null;
    }

    private static String extractArrayElement(String json, String arrayKey, int index) {
        // Look for the exact key pattern: "key"
        String keyPattern = "\"" + arrayKey.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        int keyIndex = json.indexOf(keyPattern);
        if (keyIndex == -1) {
            return null;
        }
        
        // Find the colon after the key
        int colonIndex = json.indexOf(":", keyIndex + keyPattern.length());
        if (colonIndex == -1) {
            return null;
        }
        
        // Skip whitespace after colon
        int valueStart = colonIndex + 1;
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }
        
        // Should be opening bracket [
        if (valueStart >= json.length() || json.charAt(valueStart) != '[') {
            return null;
        }
        
        int arrayEnd = findArrayEnd(json, valueStart + 1);
        if (arrayEnd == -1) {
            return null;
        }
        
        String arrayContent = json.substring(valueStart + 1, arrayEnd);
        String[] elements = parseArrayElements(arrayContent);
        
        if (index >= 0 && index < elements.length) {
            return elements[index].trim();
        }
        
        return null;
    }

    private static String[] parseArrayElements(String arrayContent) {
        if (arrayContent.trim().isEmpty()) {
            return new String[0];
        }
        
        List<String> elements = new java.util.ArrayList<>();
        int start = 0;
        int braceCount = 0;
        int bracketCount = 0;
        boolean inString = false;
        
        for (int i = 0; i < arrayContent.length(); i++) {
            char c = arrayContent.charAt(i);
            
            if (!inString) {
                if (c == '"') {
                    inString = true;
                } else if (c == '{') {
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                } else if (c == '[') {
                    bracketCount++;
                } else if (c == ']') {
                    bracketCount--;
                } else if (c == ',' && braceCount == 0 && bracketCount == 0) {
                    // Found a top-level comma - this is an element separator
                    elements.add(arrayContent.substring(start, i).trim());
                    start = i + 1;
                }
            } else {
                if (c == '"' && (i == 0 || arrayContent.charAt(i - 1) != '\\')) {
                    inString = false;
                }
            }
        }
        
        // Add the last element
        if (start < arrayContent.length()) {
            elements.add(arrayContent.substring(start).trim());
        }
        
        return elements.toArray(new String[0]);
    }

    private static int findStringEnd(String json, int start) {
        for (int i = start; i < json.length(); i++) {
            if (json.charAt(i) == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                return i;
            }
        }
        return -1;
    }

    private static int findValueEnd(String json, int start) {
        int braceCount = 0;
        int bracketCount = 0;
        boolean inString = false;
        
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            
            if (!inString) {
                if (c == '"') {
                    inString = true;
                } else if (c == '{') {
                    braceCount++;
                } else if (c == '}') {
                    if (braceCount > 0) {
                        braceCount--;
                    } else {
                        return i;
                    }
                } else if (c == '[') {
                    bracketCount++;
                } else if (c == ']') {
                    if (bracketCount > 0) {
                        bracketCount--;
                    } else {
                        return i;
                    }
                } else if (c == ',' && braceCount == 0 && bracketCount == 0) {
                    return i;
                }
            } else {
                if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                    inString = false;
                }
            }
        }
        
        return json.length();
    }

    private static int findArrayEnd(String json, int start) {
        int bracketCount = 1;
        boolean inString = false;
        
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            
            if (!inString) {
                if (c == '"') {
                    inString = true;
                } else if (c == '[') {
                    bracketCount++;
                } else if (c == ']') {
                    bracketCount--;
                    if (bracketCount == 0) {
                        return i;
                    }
                }
            } else {
                if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                    inString = false;
                }
            }
        }
        
        return -1;
    }
}
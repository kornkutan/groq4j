package groq4j.utils;

import groq4j.exceptions.GroqSerializationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Typed accessors over {@link JsonUtils} paths, with the error handling policy used by
 * response models: required lookups throw, optional lookups return empty.
 */
public final class ResponseParser {
    private ResponseParser() {
        // Prevent instantiation
    }

    public static String getRequiredString(String json, String path) {
        String value = JsonUtils.extractStringValue(json, path);
        if (value == null) {
            throw GroqSerializationException.invalidResponseFormat("Required field missing: " + path);
        }
        return value;
    }

    public static Optional<String> getOptionalString(String json, String path) {
        return JsonUtils.extractOptionalStringValue(json, path);
    }

    public static int getRequiredInt(String json, String path) {
        Integer value = JsonUtils.extractIntValue(json, path);
        if (value == null) {
            throw GroqSerializationException.invalidResponseFormat("Required integer field missing: " + path);
        }
        return value;
    }

    public static Optional<Integer> getOptionalInt(String json, String path) {
        return JsonUtils.extractOptionalIntValue(json, path);
    }

    public static long getRequiredLong(String json, String path) {
        Long value = JsonUtils.extractLongValue(json, path);
        if (value == null) {
            throw GroqSerializationException.invalidResponseFormat("Required long field missing: " + path);
        }
        return value;
    }

    public static Optional<Long> getOptionalLong(String json, String path) {
        return JsonUtils.extractOptionalLongValue(json, path);
    }

    public static double getRequiredDouble(String json, String path) {
        Double value = JsonUtils.extractDoubleValue(json, path);
        if (value == null) {
            throw GroqSerializationException.invalidResponseFormat("Required double field missing: " + path);
        }
        return value;
    }

    public static Optional<Double> getOptionalDouble(String json, String path) {
        return Optional.ofNullable(JsonUtils.extractDoubleValue(json, path));
    }

    public static boolean getRequiredBoolean(String json, String path) {
        Boolean value = JsonUtils.extractBooleanValue(json, path);
        if (value == null) {
            throw GroqSerializationException.invalidResponseFormat("Required boolean field missing: " + path);
        }
        return value;
    }

    public static Optional<Boolean> getOptionalBoolean(String json, String path) {
        return Optional.ofNullable(JsonUtils.extractBooleanValue(json, path));
    }

    public static Object getNode(String json, String path) {
        return JsonUtils.node(json, path);
    }

    public static Optional<Map<String, Object>> getOptionalObject(String json, String path) {
        Object value = JsonUtils.node(json, path);
        return value instanceof Map<?, ?> ? Optional.of(JsonUtils.asMap(value)) : Optional.empty();
    }

    public static List<Map<String, Object>> getObjectArray(String json, String path) {
        Object value = JsonUtils.node(json, path);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object element : JsonUtils.asList(value)) {
            if (element instanceof Map<?, ?>) {
                result.add(JsonUtils.asMap(element));
            }
        }
        return result;
    }

    public static <T extends Enum<T>> T getRequiredEnum(String json, String path, Class<T> enumClass) {
        String value = getRequiredString(json, path);
        try {
            return Enum.valueOf(enumClass, value.toUpperCase().replace("-", "_"));
        } catch (IllegalArgumentException e) {
            throw GroqSerializationException.invalidResponseFormat(
                "Invalid enum value for " + path + ": " + value);
        }
    }

    public static <T extends Enum<T>> Optional<T> getOptionalEnum(String json, String path, Class<T> enumClass) {
        return getOptionalString(json, path).flatMap(v -> {
            try {
                return Optional.of(Enum.valueOf(enumClass, v.toUpperCase().replace("-", "_")));
            } catch (IllegalArgumentException e) {
                return Optional.empty();
            }
        });
    }

    public static List<String> getStringArray(String json, String path) {
        List<String> result = new ArrayList<>();
        for (Object element : JsonUtils.asList(JsonUtils.node(json, path))) {
            String value = JsonUtils.stringValue(element);
            if (value != null) {
                result.add(value);
            }
        }
        return result;
    }

    public static Optional<List<String>> getOptionalStringArray(String json, String path) {
        List<String> array = getStringArray(json, path);
        return array.isEmpty() ? Optional.empty() : Optional.of(array);
    }

    public static String extractChoiceContent(String json, int choiceIndex) {
        return getOptionalString(json, "choices[" + choiceIndex + "].message.content").orElse("");
    }

    public static String extractFirstChoiceContent(String json) {
        return extractChoiceContent(json, 0);
    }

    public static Optional<String> extractFinishReason(String json, int choiceIndex) {
        return getOptionalString(json, "choices[" + choiceIndex + "].finish_reason");
    }

    public static Optional<String> extractFirstFinishReason(String json) {
        return extractFinishReason(json, 0);
    }

    public static int extractChoiceCount(String json) {
        return JsonUtils.asList(JsonUtils.node(json, "choices")).size();
    }

    public static boolean isErrorResponse(String json) {
        return JsonUtils.node(json, "error") != null;
    }

    public static Optional<String> extractErrorMessage(String json) {
        return getOptionalString(json, "error.message");
    }
}

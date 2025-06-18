package groq4j.utils;

import groq4j.exceptions.GroqSerializationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
        try {
            return Optional.ofNullable(JsonUtils.extractStringValue(json, path));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static int getRequiredInt(String json, String path) {
        Integer value = JsonUtils.extractIntValue(json, path);
        if (value == null) {
            throw GroqSerializationException.invalidResponseFormat("Required integer field missing: " + path);
        }
        return value;
    }

    public static Optional<Integer> getOptionalInt(String json, String path) {
        try {
            return Optional.ofNullable(JsonUtils.extractIntValue(json, path));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static long getRequiredLong(String json, String path) {
        Integer value = JsonUtils.extractIntValue(json, path);
        if (value == null) {
            throw GroqSerializationException.invalidResponseFormat("Required long field missing: " + path);
        }
        return value.longValue();
    }

    public static Optional<Long> getOptionalLong(String json, String path) {
        try {
            Integer value = JsonUtils.extractIntValue(json, path);
            return Optional.ofNullable(value != null ? value.longValue() : null);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static double getRequiredDouble(String json, String path) {
        Double value = JsonUtils.extractDoubleValue(json, path);
        if (value == null) {
            throw GroqSerializationException.invalidResponseFormat("Required double field missing: " + path);
        }
        return value;
    }

    public static Optional<Double> getOptionalDouble(String json, String path) {
        try {
            return Optional.ofNullable(JsonUtils.extractDoubleValue(json, path));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static boolean getRequiredBoolean(String json, String path) {
        Boolean value = JsonUtils.extractBooleanValue(json, path);
        if (value == null) {
            throw GroqSerializationException.invalidResponseFormat("Required boolean field missing: " + path);
        }
        return value;
    }

    public static Optional<Boolean> getOptionalBoolean(String json, String path) {
        try {
            return Optional.ofNullable(JsonUtils.extractBooleanValue(json, path));
        } catch (Exception e) {
            return Optional.empty();
        }
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
        try {
            Optional<String> value = getOptionalString(json, path);
            return value.map(v -> {
                try {
                    return Enum.valueOf(enumClass, v.toUpperCase().replace("-", "_"));
                } catch (IllegalArgumentException e) {
                    return null;
                }
            });
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static List<String> getStringArray(String json, String path) {
        List<String> result = new ArrayList<>();
        try {
            // Extract array content
            String arrayContent = JsonUtils.extractStringValue(json, path);
            if (arrayContent != null) {
                // Parse array elements
                String[] elements = arrayContent.split(",");
                for (String element : elements) {
                    String cleaned = element.trim().replaceAll("^\"|\"$", "");
                    if (!cleaned.isEmpty()) {
                        result.add(cleaned);
                    }
                }
            }
        } catch (Exception e) {
            // Return empty list if parsing fails
        }
        return result;
    }

    public static Optional<List<String>> getOptionalStringArray(String json, String path) {
        try {
            List<String> array = getStringArray(json, path);
            return array.isEmpty() ? Optional.empty() : Optional.of(array);
        } catch (Exception e) {
            return Optional.empty();
        }
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
        try {
            // This is a simplified implementation
            // In a real implementation, you'd properly parse the JSON array
            String choicesSection = JsonUtils.extractStringValue(json, "choices");
            if (choicesSection == null) return 0;
            
            // Count occurrences of choice objects
            int count = 0;
            int index = 0;
            while ((index = choicesSection.indexOf("{\"index\":", index)) != -1) {
                count++;
                index++;
            }
            return count;
        } catch (Exception e) {
            return 0;
        }
    }

    public static boolean isErrorResponse(String json) {
        return json.contains("\"error\":");
    }

    public static Optional<String> extractErrorMessage(String json) {
        if (!isErrorResponse(json)) {
            return Optional.empty();
        }
        return getOptionalString(json, "error.message");
    }
}
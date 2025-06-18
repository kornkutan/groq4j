package groq4j.utils;

import groq4j.exceptions.GroqValidationException;

import java.util.Collection;
import java.util.Optional;

public final class ValidationUtils {
    private ValidationUtils() {
        // Prevent instantiation
    }

    public static void requireNonNull(Object value, String fieldName) {
        if (value == null) {
            throw GroqValidationException.requiredFieldMissing(fieldName);
        }
    }

    public static void requireNonEmpty(String value, String fieldName) {
        requireNonNull(value, fieldName);
        if (value.trim().isEmpty()) {
            throw GroqValidationException.invalidFieldValue(fieldName, value, "cannot be empty");
        }
    }

    public static void requireNonEmpty(Collection<?> value, String fieldName) {
        requireNonNull(value, fieldName);
        if (value.isEmpty()) {
            throw GroqValidationException.invalidFieldValue(fieldName, value, "cannot be empty");
        }
    }

    public static void validateRange(double value, double min, double max, String fieldName) {
        if (value < min || value > max) {
            throw GroqValidationException.fieldOutOfRange(fieldName, value, min, max);
        }
    }

    public static void validateRange(int value, int min, int max, String fieldName) {
        if (value < min || value > max) {
            throw GroqValidationException.fieldOutOfRange(fieldName, value, min, max);
        }
    }

    public static void validateOptionalRange(Optional<Double> value, double min, double max, String fieldName) {
        value.ifPresent(v -> validateRange(v, min, max, fieldName));
    }

    public static void validateOptionalRange(Optional<Integer> value, int min, int max, String fieldName) {
        value.ifPresent(v -> validateRange(v, min, max, fieldName));
    }

    public static void validateTemperature(Optional<Double> temperature) {
        validateOptionalRange(temperature, Constants.MIN_TEMPERATURE, Constants.MAX_TEMPERATURE, "temperature");
    }

    public static void validateTopP(Optional<Double> topP) {
        validateOptionalRange(topP, Constants.MIN_TOP_P, Constants.MAX_TOP_P, "topP");
    }

    public static void validateFrequencyPenalty(Optional<Double> frequencyPenalty) {
        validateOptionalRange(frequencyPenalty, Constants.MIN_FREQUENCY_PENALTY, Constants.MAX_FREQUENCY_PENALTY, "frequencyPenalty");
    }

    public static void validatePresencePenalty(Optional<Double> presencePenalty) {
        validateOptionalRange(presencePenalty, Constants.MIN_PRESENCE_PENALTY, Constants.MAX_PRESENCE_PENALTY, "presencePenalty");
    }

    public static void validateSpeed(Optional<Double> speed) {
        validateOptionalRange(speed, Constants.MIN_SPEED, Constants.MAX_SPEED, "speed");
    }

    public static void validateTopLogprobs(Optional<Integer> topLogprobs) {
        validateOptionalRange(topLogprobs, Constants.MIN_TOP_LOGPROBS, Constants.MAX_TOP_LOGPROBS, "topLogprobs");
    }

    public static void validateMaxTokens(Optional<Integer> maxTokens) {
        maxTokens.ifPresent(tokens -> {
            if (tokens <= 0) {
                throw GroqValidationException.invalidFieldValue("maxTokens", tokens, "must be positive");
            }
            if (tokens > Constants.MAX_TOKENS_LIMIT) {
                throw GroqValidationException.fieldOutOfRange("maxTokens", tokens, 1, Constants.MAX_TOKENS_LIMIT);
            }
        });
    }

    public static void validateN(Optional<Integer> n) {
        n.ifPresent(value -> {
            if (value != 1) {
                throw GroqValidationException.invalidFieldValue("n", value, "only n=1 is currently supported");
            }
        });
    }

    public static void validateStopSequences(Optional<? extends Collection<String>> stop) {
        stop.ifPresent(sequences -> {
            if (sequences.size() > Constants.MAX_STOP_SEQUENCES) {
                throw GroqValidationException.fieldOutOfRange("stop", sequences.size(), 0, Constants.MAX_STOP_SEQUENCES);
            }
        });
    }

    public static void validateApiKey(String apiKey) {
        requireNonEmpty(apiKey, "apiKey");
        if (!apiKey.startsWith("gsk_")) {
            throw GroqValidationException.invalidFieldValue("apiKey", apiKey, "must start with 'gsk_'");
        }
    }

    public static void validateModel(String model) {
        requireNonEmpty(model, "model");
    }

    public static void validateFileSize(byte[] fileData) {
        requireNonNull(fileData, "fileData");
        if (fileData.length > Constants.MAX_FILE_SIZE_BYTES) {
            throw GroqValidationException.fieldOutOfRange("fileSize", fileData.length, 0, Constants.MAX_FILE_SIZE_BYTES);
        }
    }

    public static void validateUrl(String url) {
        requireNonEmpty(url, "url");
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw GroqValidationException.invalidFieldValue("url", url, "must be a valid HTTP or HTTPS URL");
        }
    }

    public static void validateBatchCompletionWindow(String completionWindow) {
        requireNonEmpty(completionWindow, "completionWindow");
        if (!completionWindow.matches("^\\d+[hd]$")) {
            throw GroqValidationException.invalidFieldValue("completionWindow", completionWindow, 
                "must be in format like '24h' or '7d'");
        }
    }

    public static void validateBatchEndpoint(String endpoint) {
        requireNonEmpty(endpoint, "endpoint");
        if (!"/v1/chat/completions".equals(endpoint)) {
            throw GroqValidationException.invalidFieldValue("endpoint", endpoint, 
                "only '/v1/chat/completions' is currently supported");
        }
    }
}
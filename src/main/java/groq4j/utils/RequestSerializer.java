package groq4j.utils;

import java.util.Map;
import java.util.Optional;

public final class RequestSerializer {
    private RequestSerializer() {
        // Prevent instantiation
    }

    public static String serializeToJson(Map<String, Object> data) {
        return JsonUtils.toJsonString(data);
    }

    public static void addIfPresent(Map<String, Object> map, String key, Optional<?> value) {
        value.ifPresent(v -> map.put(key, v));
    }

    public static void addIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    public static void addRequiredField(Map<String, Object> map, String key, Object value) {
        ValidationUtils.requireNonNull(value, key);
        map.put(key, value);
    }

    public static void addOptionalString(Map<String, Object> map, String key, Optional<String> value) {
        value.ifPresent(v -> map.put(key, v));
    }

    public static void addOptionalNumber(Map<String, Object> map, String key, Optional<? extends Number> value) {
        value.ifPresent(v -> map.put(key, v));
    }

    public static void addOptionalBoolean(Map<String, Object> map, String key, Optional<Boolean> value) {
        value.ifPresent(v -> map.put(key, v));
    }

    public static void addOptionalEnum(Map<String, Object> map, String key, Optional<? extends Enum<?>> value) {
        value.ifPresent(v -> map.put(key, v.toString()));
    }

    public static void addStringArray(Map<String, Object> map, String key, Optional<? extends Iterable<String>> value) {
        value.ifPresent(v -> map.put(key, v));
    }

    public static void addObjectArray(Map<String, Object> map, String key, Optional<? extends Iterable<?>> value) {
        value.ifPresent(v -> map.put(key, v));
    }

    public static Map<String, Object> createBaseRequest(String model) {
        ValidationUtils.validateModel(model);
        return Map.of("model", model);
    }

    public static String buildChatCompletionRequest(
        String model,
        Object messages,
        Optional<Double> temperature,
        Optional<Integer> maxCompletionTokens,
        Map<String, Object> additionalFields
    ) {
        var request = new java.util.HashMap<String, Object>();
        
        addRequiredField(request, "model", model);
        addRequiredField(request, "messages", messages);
        addOptionalNumber(request, "temperature", temperature);
        addOptionalNumber(request, "max_completion_tokens", maxCompletionTokens);
        
        if (additionalFields != null) {
            request.putAll(additionalFields);
        }
        
        return serializeToJson(request);
    }

    public static String buildAudioTranscriptionRequest(
        String model,
        Optional<String> language,
        Optional<String> prompt,
        Optional<String> responseFormat,
        Optional<Double> temperature
    ) {
        var request = new java.util.HashMap<String, Object>();
        
        addRequiredField(request, "model", model);
        addOptionalString(request, "language", language);
        addOptionalString(request, "prompt", prompt);
        addOptionalString(request, "response_format", responseFormat);
        addOptionalNumber(request, "temperature", temperature);
        
        return serializeToJson(request);
    }

    public static String buildAudioTranslationRequest(
        String model,
        Optional<String> prompt,
        Optional<String> responseFormat,
        Optional<Double> temperature
    ) {
        var request = new java.util.HashMap<String, Object>();
        
        addRequiredField(request, "model", model);
        addOptionalString(request, "prompt", prompt);
        addOptionalString(request, "response_format", responseFormat);
        addOptionalNumber(request, "temperature", temperature);
        
        return serializeToJson(request);
    }

    public static String buildSpeechRequest(
        String model,
        String input,
        String voice,
        Optional<String> responseFormat,
        Optional<Integer> sampleRate,
        Optional<Double> speed
    ) {
        var request = new java.util.HashMap<String, Object>();
        
        addRequiredField(request, "model", model);
        addRequiredField(request, "input", input);
        addRequiredField(request, "voice", voice);
        addOptionalString(request, "response_format", responseFormat);
        addOptionalNumber(request, "sample_rate", sampleRate);
        addOptionalNumber(request, "speed", speed);
        
        return serializeToJson(request);
    }

    public static String buildBatchRequest(
        String inputFileId,
        String endpoint,
        String completionWindow,
        Optional<Map<String, Object>> metadata
    ) {
        var request = new java.util.HashMap<String, Object>();
        
        addRequiredField(request, "input_file_id", inputFileId);
        addRequiredField(request, "endpoint", endpoint);
        addRequiredField(request, "completion_window", completionWindow);
        addIfPresent(request, "metadata", metadata);
        
        return serializeToJson(request);
    }

    public static Map<String, Object> buildFileUploadFormData(
        byte[] fileData,
        String filename,
        String purpose
    ) {
        var formData = new java.util.HashMap<String, Object>();
        formData.put("file", fileData);
        formData.put("purpose", purpose);
        
        if (filename != null && !filename.isEmpty()) {
            formData.put("filename", filename);
        }
        
        return formData;
    }
}
package groq4j.utils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class UrlUtils {
    private UrlUtils() {
        // Prevent instantiation
    }

    public static String buildUrl(String baseUrl, String endpoint) {
        return HttpUtils.buildUrl(baseUrl, endpoint);
    }

    public static String buildUrl(String baseUrl, String endpoint, String pathParam) {
        return HttpUtils.buildUrl(baseUrl, endpoint, pathParam);
    }

    public static String buildUrlWithQuery(String baseUrl, String endpoint, Map<String, String> queryParams) {
        String url = buildUrl(baseUrl, endpoint);
        
        if (queryParams == null || queryParams.isEmpty()) {
            return url;
        }
        
        var queryString = new StringBuilder();
        boolean first = true;
        
        for (var entry : queryParams.entrySet()) {
            if (entry.getValue() != null) {
                if (!first) {
                    queryString.append("&");
                }
                queryString.append(urlEncode(entry.getKey()))
                          .append("=")
                          .append(urlEncode(entry.getValue()));
                first = false;
            }
        }
        
        if (!queryString.isEmpty()) {
            return url + "?" + queryString;
        }
        
        return url;
    }

    public static String urlEncode(String value) {
        if (value == null) {
            return "";
        }
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public static String buildChatCompletionsUrl(String baseUrl) {
        return buildUrl(baseUrl, Constants.CHAT_COMPLETIONS);
    }

    public static String buildAudioTranscriptionsUrl(String baseUrl) {
        return buildUrl(baseUrl, Constants.AUDIO_TRANSCRIPTIONS);
    }

    public static String buildAudioTranslationsUrl(String baseUrl) {
        return buildUrl(baseUrl, Constants.AUDIO_TRANSLATIONS);
    }

    public static String buildAudioSpeechUrl(String baseUrl) {
        return buildUrl(baseUrl, Constants.AUDIO_SPEECH);
    }

    public static String buildModelsUrl(String baseUrl) {
        return buildUrl(baseUrl, Constants.MODELS);
    }

    public static String buildModelUrl(String baseUrl, String modelId) {
        return buildUrl(baseUrl, Constants.MODELS, modelId);
    }

    // Convenience methods using default base URL
    public static String buildModelsUrl() {
        return buildModelsUrl(Constants.BASE_URL);
    }

    public static String buildModelUrl(String modelId) {
        return buildModelUrl(Constants.BASE_URL, modelId);
    }

    // Batch URL convenience methods using default base URL
    public static String buildBatchesUrl() {
        return buildBatchesUrl(Constants.BASE_URL);
    }

    public static String buildBatchUrl(String batchId) {
        return buildBatchUrl(Constants.BASE_URL, batchId);
    }

    public static String buildBatchCancelUrl(String batchId) {
        return buildBatchCancelUrl(Constants.BASE_URL, batchId);
    }

    public static String buildBatchesUrl(String baseUrl) {
        return buildUrl(baseUrl, Constants.BATCHES);
    }

    public static String buildBatchUrl(String baseUrl, String batchId) {
        return buildUrl(baseUrl, Constants.BATCHES, batchId);
    }

    public static String buildBatchCancelUrl(String baseUrl, String batchId) {
        return buildUrl(baseUrl, Constants.BATCHES, batchId + "/cancel");
    }

    public static String buildFilesUrl(String baseUrl) {
        return buildUrl(baseUrl, Constants.FILES);
    }

    public static String buildFileUrl(String baseUrl, String fileId) {
        return buildUrl(baseUrl, Constants.FILES, fileId);
    }

    public static String buildFileContentUrl(String baseUrl, String fileId) {
        return buildUrl(baseUrl, Constants.FILES, fileId + "/content");
    }

    public static boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        
        try {
            java.net.URI.create(url); // Hack: throws an exception if the URL is invalid
            return url.startsWith("http://") || url.startsWith("https://");
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static String sanitizeUrl(String url) {
        if (url == null) {
            return "";
        }
        
        // Remove trailing slashes
        while (url.endsWith("/") && url.length() > 1) {
            url = url.substring(0, url.length() - 1);
        }
        
        return url;
    }
}
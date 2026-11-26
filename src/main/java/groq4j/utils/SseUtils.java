package groq4j.utils;

import java.util.stream.Stream;

/**
 * Helpers for the data-only server-sent event format used by Groq streaming endpoints.
 * Every event is one {@code data:} line, and the stream ends with {@code data: [DONE]}.
 */
public final class SseUtils {
    private SseUtils() {
        // Prevent instantiation
    }

    /** Strips the {@code data:} prefix and drops blank lines, comments and the terminator. */
    public static Stream<String> payloads(Stream<String> lines) {
        return lines
            .map(String::trim)
            .filter(line -> !line.isEmpty())
            .filter(line -> !line.startsWith(":"))
            .filter(line -> line.startsWith(Constants.SSE_DATA_PREFIX))
            .map(line -> line.substring(Constants.SSE_DATA_PREFIX.length()).trim())
            .takeWhile(payload -> !Constants.SSE_DONE.equals(payload));
    }
}

package groq4j.exceptions;

/** Raised for {@code 413 Request Entity Too Large}. */
public class GroqRequestTooLargeException extends GroqApiException {

    public GroqRequestTooLargeException(String message, String errorType, String errorCode) {
        super(message, 413, errorType, errorCode);
    }

    public GroqRequestTooLargeException(String message) {
        super(message, 413, "invalid_request_error", "request_too_large");
    }
}

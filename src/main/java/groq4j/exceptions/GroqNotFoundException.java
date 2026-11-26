package groq4j.exceptions;

public class GroqNotFoundException extends GroqApiException {

    public GroqNotFoundException(String message, String errorType, String errorCode) {
        super(message, 404, errorType, errorCode);
    }

    public GroqNotFoundException(String message) {
        super(message, 404, "invalid_request_error", "not_found");
    }
}

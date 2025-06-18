package groq4j.exceptions;

public class GroqBadRequestException extends GroqApiException {
    
    public GroqBadRequestException(String message) {
        super(message, 400, "invalid_request_error", "bad_request");
    }

    public GroqBadRequestException(String message, Throwable cause) {
        super(message, cause, 400, "invalid_request_error", "bad_request");
    }

    public GroqBadRequestException(String message, String errorType, String errorCode) {
        super(message, 400, errorType, errorCode);
    }
}
package groq4j.exceptions;

public class GroqForbiddenException extends GroqApiException {

    public GroqForbiddenException(String message, String errorType, String errorCode) {
        super(message, 403, errorType, errorCode);
    }

    public GroqForbiddenException(String message) {
        super(message, 403, "permission_error", "forbidden");
    }
}

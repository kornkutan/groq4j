package groq4j.exceptions;

public class GroqServerException extends GroqApiException {
    
    public GroqServerException(String message, int statusCode) {
        super(message, statusCode, "server_error", "internal_server_error");
    }

    public GroqServerException(String message, Throwable cause, int statusCode) {
        super(message, cause, statusCode, "server_error", "internal_server_error");
    }

    public GroqServerException(String message, int statusCode, String errorType, String errorCode) {
        super(message, statusCode, errorType, errorCode);
    }
}
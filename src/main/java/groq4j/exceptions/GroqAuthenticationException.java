package groq4j.exceptions;

public class GroqAuthenticationException extends GroqApiException {
    
    public GroqAuthenticationException(String message) {
        super(message, 401, "authentication_error", "invalid_api_key");
    }

    public GroqAuthenticationException(String message, Throwable cause) {
        super(message, cause, 401, "authentication_error", "invalid_api_key");
    }

    public GroqAuthenticationException(String message, String errorType, String errorCode) {
        super(message, 401, errorType, errorCode);
    }
}
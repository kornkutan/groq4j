package groq4j.exceptions;

public class GroqNetworkException extends GroqApiException {
    
    public GroqNetworkException(String message) {
        super(message);
    }

    public GroqNetworkException(String message, Throwable cause) {
        super(message, cause);
    }

    public static GroqNetworkException connectionTimeout(String message) {
        return new GroqNetworkException("Connection timeout: " + message);
    }

    public static GroqNetworkException connectionFailed(String message, Throwable cause) {
        return new GroqNetworkException("Connection failed: " + message, cause);
    }

    public static GroqNetworkException readTimeout(String message) {
        return new GroqNetworkException("Read timeout: " + message);
    }
}
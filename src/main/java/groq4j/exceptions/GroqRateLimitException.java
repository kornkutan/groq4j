package groq4j.exceptions;

public class GroqRateLimitException extends GroqApiException {
    private final Integer retryAfterSeconds;

    public GroqRateLimitException(String message) {
        super(message, 429, "rate_limit_error", "rate_limit_exceeded");
        this.retryAfterSeconds = null;
    }

    public GroqRateLimitException(String message, Integer retryAfterSeconds) {
        super(message, 429, "rate_limit_error", "rate_limit_exceeded");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public GroqRateLimitException(String message, Throwable cause) {
        super(message, cause, 429, "rate_limit_error", "rate_limit_exceeded");
        this.retryAfterSeconds = null;
    }

    public GroqRateLimitException(String message, String errorType, String errorCode) {
        super(message, 429, errorType, errorCode);
        this.retryAfterSeconds = null;
    }

    public Integer getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
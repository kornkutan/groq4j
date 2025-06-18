package groq4j.exceptions;

public class GroqApiException extends RuntimeException {
    private final int statusCode;
    private final String errorType;
    private final String errorCode;

    public GroqApiException(String message) {
        super(message);
        this.statusCode = 0;
        this.errorType = null;
        this.errorCode = null;
    }

    public GroqApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 0;
        this.errorType = null;
        this.errorCode = null;
    }

    public GroqApiException(String message, int statusCode, String errorType, String errorCode) {
        super(message);
        this.statusCode = statusCode;
        this.errorType = errorType;
        this.errorCode = errorCode;
    }

    public GroqApiException(String message, Throwable cause, int statusCode, String errorType, String errorCode) {
        super(message, cause);
        this.statusCode = statusCode;
        this.errorType = errorType;
        this.errorCode = errorCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getErrorType() {
        return errorType;
    }

    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public String toString() {
        return String.format("GroqApiException{statusCode=%d, errorType='%s', errorCode='%s', message='%s'}", 
                statusCode, errorType, errorCode, getMessage());
    }
}
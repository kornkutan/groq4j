package groq4j.exceptions;

/**
 * Raised for {@code 424 Failed Dependency}, which Groq returns when a remote dependency such as
 * a remote MCP server fails, usually because of an authentication problem on that server.
 */
public class GroqFailedDependencyException extends GroqApiException {

    public GroqFailedDependencyException(String message, String errorType, String errorCode) {
        super(message, 424, errorType, errorCode);
    }

    public GroqFailedDependencyException(String message) {
        super(message, 424, "dependency_error", "failed_dependency");
    }
}

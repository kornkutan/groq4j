package groq4j.models.chat;

/** Character range in the message content that cites the result of a tool call. */
public record FunctionCitation(int startIndex, int endIndex, String toolCallId) {
}

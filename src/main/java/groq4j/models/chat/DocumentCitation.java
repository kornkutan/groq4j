package groq4j.models.chat;

/** Character range in the message content that cites a document supplied in the request. */
public record DocumentCitation(int startIndex, int endIndex, String documentId) {
}

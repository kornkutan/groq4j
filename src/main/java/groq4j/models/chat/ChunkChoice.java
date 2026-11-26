package groq4j.models.chat;

import java.util.Optional;

/** One streamed choice. {@code finishReason} is only present on the final chunk. */
public record ChunkChoice(
    int index,
    Delta delta,
    Optional<String> finishReason
) {
}

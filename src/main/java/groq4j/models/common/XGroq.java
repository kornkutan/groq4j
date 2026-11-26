package groq4j.models.common;

import java.util.Optional;

/**
 * Groq specific metadata. The request id is sent on every response and is the value to quote
 * when contacting support; the seed appears when one was requested.
 */
public record XGroq(String id, Optional<Integer> seed) {

    public XGroq(String id) {
        this(id, Optional.empty());
    }
}

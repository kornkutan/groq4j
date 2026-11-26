package groq4j.models.responses;

/** Token accounting for a response, including cached and reasoning token breakdowns. */
public record ResponseUsage(
    int inputTokens,
    int outputTokens,
    int totalTokens,
    int cachedTokens,
    int reasoningTokens
) {
}

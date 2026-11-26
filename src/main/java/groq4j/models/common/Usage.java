package groq4j.models.common;

import java.util.Optional;

/**
 * Token accounting for a completion. Groq adds timing fields on top of the OpenAI shape, plus
 * optional cache and reasoning token breakdowns.
 */
public record Usage(
    double queueTime,
    int promptTokens,
    double promptTime,
    int completionTokens,
    double completionTime,
    int totalTokens,
    double totalTime,
    Optional<Integer> cachedTokens,
    Optional<Integer> reasoningTokens
) {

    public Usage(double queueTime, int promptTokens, double promptTime, int completionTokens,
                 double completionTime, int totalTokens, double totalTime) {
        this(queueTime, promptTokens, promptTime, completionTokens, completionTime, totalTokens, totalTime,
            Optional.empty(), Optional.empty());
    }

    public double getCostEstimate(double inputTokenPrice, double outputTokenPrice) {
        return (promptTokens * inputTokenPrice) + (completionTokens * outputTokenPrice);
    }

    public double getTokensPerSecond() {
        return totalTime > 0 ? totalTokens / totalTime : 0.0;
    }

    public double getEfficiency() {
        return queueTime > 0 ? (promptTime + completionTime) / (queueTime + promptTime + completionTime) : 1.0;
    }
}

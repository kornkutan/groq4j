package groq4j.models.common;

public record Usage(
    double queueTime,
    int promptTokens,
    double promptTime,
    int completionTokens,
    double completionTime,
    int totalTokens,
    double totalTime
) {
    public static Usage fromJson(String json) {
        return new Usage(
            groq4j.utils.ResponseParser.getOptionalDouble(json, "queue_time").orElse(0.0),
            groq4j.utils.ResponseParser.getOptionalInt(json, "prompt_tokens").orElse(0),
            groq4j.utils.ResponseParser.getOptionalDouble(json, "prompt_time").orElse(0.0),
            groq4j.utils.ResponseParser.getOptionalInt(json, "completion_tokens").orElse(0),
            groq4j.utils.ResponseParser.getOptionalDouble(json, "completion_time").orElse(0.0),
            groq4j.utils.ResponseParser.getOptionalInt(json, "total_tokens").orElse(0),
            groq4j.utils.ResponseParser.getOptionalDouble(json, "total_time").orElse(0.0)
        );
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
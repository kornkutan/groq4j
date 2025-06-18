package groq4j.models.common;

public record RequestCounts(
    int total,
    int completed,
    int failed
) {
    public static RequestCounts fromJson(String json, String path) {
        return new RequestCounts(
            groq4j.utils.ResponseParser.getRequiredInt(json, path + ".total"),
            groq4j.utils.ResponseParser.getRequiredInt(json, path + ".completed"),
            groq4j.utils.ResponseParser.getRequiredInt(json, path + ".failed")
        );
    }

    public static RequestCounts fromJson(String json) {
        return fromJson(json, "request_counts");
    }

    public int getPending() {
        return total - completed - failed;
    }

    public double getCompletionRate() {
        return total > 0 ? (double) completed / total : 0.0;
    }

    public double getFailureRate() {
        return total > 0 ? (double) failed / total : 0.0;
    }

    public boolean isComplete() {
        return completed + failed == total;
    }

    public boolean hasFailures() {
        return failed > 0;
    }
}
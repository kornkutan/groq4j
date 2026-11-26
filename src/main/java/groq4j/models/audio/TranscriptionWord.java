package groq4j.models.audio;

/** One word with start and end timestamps, requested with {@code timestamp_granularities=["word"]}. */
public record TranscriptionWord(String word, double start, double end) {
}

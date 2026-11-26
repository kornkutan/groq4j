package groq4j.models.audio;

import java.util.List;

/** One timed segment of a transcription, available with {@code response_format=verbose_json}. */
public record TranscriptionSegment(
    int id,
    double start,
    double end,
    String text,
    double avgLogprob,
    double compressionRatio,
    double noSpeechProb,
    int seek,
    double temperature,
    List<Integer> tokens
) {
}

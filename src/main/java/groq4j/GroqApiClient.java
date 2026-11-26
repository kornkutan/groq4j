package groq4j;

import groq4j.services.AudioService;
import groq4j.services.BatchService;
import groq4j.services.ChatService;
import groq4j.services.EmbeddingsService;
import groq4j.services.FilesService;
import groq4j.services.ModelsService;
import groq4j.services.RerankingService;
import groq4j.services.ResponsesService;

/**
 * Entry point for the Groq API. Each accessor returns a service bound to the same API key and
 * HTTP client.
 *
 * {@snippet :
 * var client = GroqApiClientImpl.create(System.getenv("GROQ_API_KEY"));
 * var answer = client.chat().simple("openai/gpt-oss-20b", "Hello");
 * }
 */
public interface GroqApiClient {

    /** Chat completions, blocking and streamed. */
    ChatService chat();

    /** The Responses API, blocking and streamed. */
    ResponsesService responses();

    /** Text embeddings. */
    EmbeddingsService embeddings();

    /** Document reranking. */
    RerankingService reranking();

    /** Transcription, translation and speech synthesis. */
    AudioService audio();

    /** Model listing and lookup. */
    ModelsService models();

    /** Batch jobs. */
    BatchService batch();

    /** File upload and download. */
    FilesService files();
}

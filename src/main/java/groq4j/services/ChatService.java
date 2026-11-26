package groq4j.services;

import groq4j.models.chat.ChatCompletionChunk;
import groq4j.models.chat.ChatCompletionRequest;
import groq4j.models.chat.ChatCompletionResponse;

import java.util.stream.Stream;

/**
 * Chat completions against {@code POST /openai/v1/chat/completions}.
 *
 * <p>Both blocking and streaming forms are supported. Streaming returns a lazily consumed
 * {@link Stream} of chunks that must be closed, for example with try-with-resources.
 */
public interface ChatService {

    /**
     * Creates a chat completion.
     *
     * @param request the full request, see {@link groq4j.builders.ChatCompletionRequestBuilder}
     * @return the completion, including tool calls and citations when present
     * @throws groq4j.exceptions.GroqApiException if the API call fails
     * @throws groq4j.exceptions.GroqValidationException if the request is invalid
     */
    ChatCompletionResponse createCompletion(ChatCompletionRequest request);

    /**
     * Creates a chat completion and delivers the answer as server-sent events.
     *
     * <p>The returned stream holds an open HTTP connection. Close it, either by exhausting the
     * stream or with try-with-resources.
     *
     * {@snippet :
     * try (var chunks = chat.streamCompletion(request)) {
     *     chunks.forEach(chunk -> System.out.print(chunk.getContent()));
     * }
     * }
     *
     * @param request the full request; {@code stream} and {@code stream_options} are set for you
     * @return a stream of chunks, ending when the server sends {@code data: [DONE]}
     * @throws groq4j.exceptions.GroqApiException if the API call fails
     */
    Stream<ChatCompletionChunk> streamCompletion(ChatCompletionRequest request);

    /**
     * Convenience call with one user message.
     *
     * @param model the model id, for example {@code openai/gpt-oss-20b}
     * @param message the user message content
     * @return the completion
     */
    ChatCompletionResponse simple(String model, String message);

    /**
     * Convenience call with a system prompt and one user message.
     *
     * @param model the model id
     * @param systemPrompt the system message that sets behaviour
     * @param userMessage the user message content
     * @return the completion
     */
    ChatCompletionResponse simple(String model, String systemPrompt, String userMessage);
}

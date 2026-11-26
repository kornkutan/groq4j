package groq4j.services;

import groq4j.models.responses.ResponseObject;
import groq4j.models.responses.ResponseRequest;
import groq4j.models.responses.ResponseStreamEvent;

import java.util.stream.Stream;

/**
 * The Responses API at {@code POST /openai/v1/responses}, compatible with OpenAI's Responses API.
 *
 * <p>Groq does not support stateful conversations, {@code previous_response_id}, {@code store} or
 * {@code truncation} yet, so the caller resends the conversation on each call.
 */
public interface ResponsesService {

    /**
     * Creates a response.
     *
     * @param request the request, see {@link groq4j.builders.ResponseRequestBuilder}
     * @return the response with output items and usage
     * @throws groq4j.exceptions.GroqApiException if the API call fails
     */
    ResponseObject createResponse(ResponseRequest request);

    /**
     * Creates a response and delivers events as they are generated.
     *
     * <p>The returned stream holds an open HTTP connection and must be closed.
     *
     * @param request the request
     * @return a stream of events, ending after {@code response.completed}
     */
    Stream<ResponseStreamEvent> streamResponse(ResponseRequest request);
}

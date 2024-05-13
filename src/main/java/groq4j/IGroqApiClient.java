package groq4j;

import javax.json.JsonObject;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.Observable;

public interface IGroqApiClient {
    /**
     * Creates a chat completion asynchronously and returns a Single that emits the result.
     * @param request The JSON object containing the request parameters.
     * @return A Single that emits the resulting JSON object.
     */
    Single<JsonObject> createChatCompletionAsync(JsonObject request);

    /**
     * Creates a chat completion stream and returns an Observable that emits each JSON object
     * as they are received from the server.
     * @param request The JSON object containing the request parameters.
     * @return An Observable that emits JSON objects as they are streamed from the server.
     */
    Observable<JsonObject> createChatCompletionStreamAsync(JsonObject request);
}
package groq4j;

import groq4j.exceptions.GroqApiException;
import groq4j.exceptions.GroqAuthenticationException;
import groq4j.exceptions.GroqBadRequestException;
import groq4j.exceptions.GroqFailedDependencyException;
import groq4j.exceptions.GroqForbiddenException;
import groq4j.exceptions.GroqNotFoundException;
import groq4j.exceptions.GroqRateLimitException;
import groq4j.exceptions.GroqRequestTooLargeException;
import groq4j.exceptions.GroqServerException;
import groq4j.exceptions.GroqUnprocessableEntityException;
import groq4j.exceptions.GroqValidationException;
import groq4j.support.FakeGroqServer;
import groq4j.support.FakeServerTestBase;
import groq4j.utils.HttpUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Client wiring and the documented error codes")
class ClientWireSpecTest extends FakeServerTestBase {

    private GroqApiClient client() {
        return GroqApiClientImpl.create(HttpUtils.createHttpClient(), "test-key", server.baseUrl());
    }

    @Test
    void exposesEveryServiceOnOneClient() {
        GroqApiClient client = client();

        assertNotNull(client.chat());
        assertNotNull(client.responses());
        assertNotNull(client.embeddings());
        assertNotNull(client.reranking());
        assertNotNull(client.audio());
        assertNotNull(client.models());
        assertNotNull(client.batch());
        assertNotNull(client.files());

        assertSame(client.chat(), client.chat());
        assertSame(client.files(), client.files());

        assertEquals(2, client.models().listModels().getModelCount());
        assertEquals("chatcmpl-fixture", client.chat().simple("openai/gpt-oss-20b", "pong").id());
    }

    @Test
    void rejectsMissingOrEmptyApiKey() {
        assertThrows(GroqValidationException.class, () -> GroqApiClientImpl.create(""));
        assertThrows(GroqValidationException.class, () -> GroqApiClientImpl.create("   "));
        assertThrows(GroqValidationException.class, () -> GroqApiClientImpl.create(null));
    }

    @Test
    void mapsEveryDocumentedErrorCode() {
        record Case(int status, Class<? extends GroqApiException> type) {
        }

        Stream.of(
            new Case(400, GroqBadRequestException.class),
            new Case(401, GroqAuthenticationException.class),
            new Case(403, GroqForbiddenException.class),
            new Case(404, GroqNotFoundException.class),
            new Case(413, GroqRequestTooLargeException.class),
            new Case(422, GroqUnprocessableEntityException.class),
            new Case(424, GroqFailedDependencyException.class),
            new Case(429, GroqRateLimitException.class),
            new Case(498, GroqApiException.class),
            new Case(499, GroqApiException.class),
            new Case(500, GroqServerException.class),
            new Case(502, GroqServerException.class),
            new Case(503, GroqServerException.class),
            new Case(504, GroqServerException.class)
        ).forEach(c -> {
            server.route("/models", request -> FakeGroqServer.Reply.error(
                c.status(), "boom " + c.status(), "test_error", "code_" + c.status()));

            var thrown = assertThrows(GroqApiException.class, () -> client().models().listModels());
            assertEquals(c.type(), thrown.getClass(), "status " + c.status());
            assertEquals(c.status(), thrown.getStatusCode());
            assertEquals("code_" + c.status(), thrown.getErrorCode());
        });
    }

    @Test
    void keepsRetryAfterOnRateLimits() {
        server.route("/models", request -> new FakeGroqServer.Reply(429, "application/json",
            "{\"error\":{\"message\":\"slow down\",\"type\":\"rate_limit_error\",\"code\":\"rate_limit\"}}"));

        var thrown = assertThrows(GroqRateLimitException.class, () -> client().models().listModels());

        assertEquals("slow down", thrown.getMessage());
        assertEquals("rate_limit_error", thrown.getErrorType());
    }
}

package groq4j.support;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base class for tests that drive the client against a fresh {@link FakeGroqServer}. No network
 * access outside localhost, no API key, no Groq account.
 */
public abstract class FakeServerTestBase {

    protected FakeGroqServer server;

    @BeforeEach
    void startFakeServer() {
        server = new FakeGroqServer().withDefaultRoutes();
        server.start();
    }

    @AfterEach
    void stopFakeServer() {
        server.close();
    }
}

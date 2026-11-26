package groq4j.utils;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class SseUtilsTest {

    @Test
    void stripsDataPrefixAndStopsAtDone() {
        Stream<String> lines = Stream.of(
            "data: {\"a\":1}",
            "",
            ": keep-alive comment",
            "data: {\"a\":2}",
            "data: [DONE]",
            "data: {\"a\":3}");

        List<String> payloads = SseUtils.payloads(lines).toList();

        assertEquals(List.of("{\"a\":1}", "{\"a\":2}"), payloads);
    }

    @Test
    void handlesStreamWithoutDone() {
        List<String> payloads = SseUtils.payloads(Stream.of("data: [1]")).toList();

        assertEquals(List.of("[1]"), payloads);
    }

    @Test
    void handlesCarriageReturns() {
        List<String> payloads = SseUtils.payloads(Stream.of("data: {\"a\":1}\r")).toList();

        assertEquals(List.of("{\"a\":1}"), payloads);
    }

    @Test
    void ignoresEmptyStream() {
        assertTrue(SseUtils.payloads(Stream.<String>empty()).toList().isEmpty());
    }
}

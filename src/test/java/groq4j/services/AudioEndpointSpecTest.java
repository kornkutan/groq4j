package groq4j.services;

import groq4j.builders.SpeechRequestBuilder;
import groq4j.builders.TranscriptionRequestBuilder;
import groq4j.builders.TranslationRequestBuilder;
import groq4j.enums.AudioFormat;
import groq4j.enums.ResponseFormat;
import groq4j.enums.TimestampGranularity;
import groq4j.enums.TtsVoice;
import groq4j.models.audio.SpeechRequest;
import groq4j.models.audio.TranscriptionResponse;
import groq4j.models.audio.TranslationResponse;
import groq4j.support.FakeGroqServer;
import groq4j.support.FakeServerTestBase;
import groq4j.support.GroqFixtures;
import groq4j.utils.HttpUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Audio endpoints against a fake Groq server")
class AudioEndpointSpecTest extends FakeServerTestBase {

    private static final byte[] WAV = "RIFFwavdata".getBytes();

    private AudioService audio() {
        return new AudioServiceImpl(HttpUtils.createHttpClient(), "test-key", server.baseUrl());
    }

    @Test
    void transcribesUploadedFileAsMultipart() {
        TranscriptionResponse response = audio().createTranscription(
            TranscriptionRequestBuilder.withFile("whisper-large-v3", WAV, "speech.wav")
                .language("en")
                .prompt("technical talk")
                .responseFormat(ResponseFormat.VERBOSE_JSON)
                .temperature(0.2)
                .timestampGranularities(List.of(TimestampGranularity.WORD, TimestampGranularity.SEGMENT))
                .build());

        assertEquals("hello world", response.text());
        assertEquals("en", response.language().orElse(null));
        assertEquals(2.5, response.duration().orElseThrow(), 1e-9);
        assertEquals(2, response.segments().size());
        assertEquals("hello", response.segments().getFirst().text());
        assertEquals(List.of(1, 2), response.segments().getFirst().tokens());
        assertEquals(2, response.words().size());
        assertEquals("world", response.words().getLast().word());
        assertTrue(response.hasTimestamps());
        assertEquals("req_audio", response.getRequestId());

        var request = server.requestAt("/openai/v1/audio/transcriptions");
        assertTrue(request.header("Content-Type").startsWith("multipart/form-data"));
        assertTrue(request.body().contains("filename=\"speech.wav\""));
        assertTrue(request.body().contains("Content-Type: audio/wav"));
        assertTrue(request.body().contains("name=\"response_format\""));
        assertTrue(request.body().contains("verbose_json"));
        assertEquals(2, request.body().split("name=\"timestamp_granularities\\[\\]\"", -1).length - 1,
            "each granularity must be its own form field: " + request.body());
    }

    @Test
    void transcribesRemoteUrlAsJson() {
        audio().createTranscription(TranscriptionRequestBuilder.withUrl(
            "whisper-large-v3", "https://example.com/speech.wav").build());

        var request = server.requestAt("/openai/v1/audio/transcriptions");
        assertTrue(request.header("Content-Type").startsWith("application/json"));
        assertTrue(request.body().contains("\"url\":\"https://example.com/speech.wav\""));
    }

    @Test
    void simpleTranscriptionSendsFilename() {
        TranscriptionResponse response = audio().simpleTranscription("whisper-large-v3", WAV, "clip.wav");

        assertEquals("hello world", response.text());
        assertTrue(server.requestAt("/openai/v1/audio/transcriptions").body().contains("filename=\"clip.wav\""));
    }

    @Test
    void translatesUploadedFileAndUrl() {
        TranslationResponse file = audio().createTranslation(
            TranslationRequestBuilder.withFile("whisper-large-v3", WAV, "foreign.mp3")
                .prompt("translate")
                .responseFormat(ResponseFormat.VERBOSE_JSON)
                .build());
        audio().createTranslation(TranslationRequestBuilder.withUrl(
            "whisper-large-v3", "https://example.com/foreign.mp3").build());
        TranslationResponse simple = audio().simpleTranslation("whisper-large-v3", WAV, "foreign.mp3");

        assertEquals("hello world", file.text());
        assertEquals(1, file.segments().size());
        assertEquals("hello world", simple.text());
        assertTrue(server.requests().stream()
            .filter(request -> request.path().endsWith("/audio/translations"))
            .anyMatch(request -> request.body().contains("\"url\"")));
    }

    @Test
    void synthesizesSpeechAndReturnsBytes() {
        SpeechRequest request = SpeechRequestBuilder.create()
            .input("[cheerful] Groq is fast.")
            .voice(TtsVoice.TROY)
            .responseFormat(AudioFormat.WAV)
            .build();

        byte[] audio = audio().createSpeech(request);

        assertEquals("RIFF", new String(audio));
        String body = server.requestAt("/openai/v1/audio/speech").body();
        assertTrue(body.contains("\"model\":\"canopylabs/orpheus-v1-english\""));
        assertTrue(body.contains("\"voice\":\"troy\""));
        assertTrue(body.contains("\"response_format\":\"wav\""));
    }

    @Test
    void simpleSpeechUsesTheGivenModel() {
        byte[] audio = audio().simpleSpeech("canopylabs/orpheus-v1-english", "hello", "hannah");

        assertEquals("RIFF", new String(audio));
        assertTrue(server.requestAt("/openai/v1/audio/speech").body().contains("\"voice\":\"hannah\""));
    }

    @Test
    void rejectsOversizedInputForOrpheus() {
        assertThrows(IllegalArgumentException.class, () -> SpeechRequestBuilder.create()
            .input("x".repeat(201))
            .voice(TtsVoice.TROY)
            .build());

        SpeechRequestBuilder.create()
            .input("x".repeat(200))
            .voice(TtsVoice.ABDULLAH)
            .build();
    }

    @Test
    void surfacesAudioErrors() {
        server.route("/audio/transcriptions", request -> FakeGroqServer.Reply.error(
            400, "invalid audio", "invalid_request_error", "invalid_file"));

        var thrown = assertThrows(groq4j.exceptions.GroqBadRequestException.class,
            () -> audio().simpleTranscription("whisper-large-v3", WAV, "broken.wav"));

        assertEquals(400, thrown.getStatusCode());
        assertEquals("invalid_file", thrown.getErrorCode());
    }
}

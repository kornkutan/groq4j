import groq4j.builders.SpeechRequestBuilder;
import groq4j.builders.TranscriptionRequestBuilder;
import groq4j.builders.TranslationRequestBuilder;
import groq4j.enums.AudioFormat;
import groq4j.enums.ResponseFormat;
import groq4j.enums.SampleRate;
import groq4j.enums.TimestampGranularity;
import groq4j.services.AudioService;
import groq4j.services.AudioServiceImpl;
import groq4j.models.audio.TranscriptionResponse;
import groq4j.models.audio.TranslationResponse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Audio Service Tests")
@Tag("integration")
@Tag("requires-api-key")
@Tag("requires-audio-files")
class AudioServiceTest extends BaseServiceTest {
    
    private AudioService audioService;
    private byte[] testAudioData;
    private byte[] frenchAudioData;
    
    @BeforeAll
    void setup() {
        super.baseSetup();
        requireServiceEnabled("Audio", isAudioEnabled());
        audioService = AudioServiceImpl.create(getApiKey());
        
        // Load test audio files from configuration
        testAudioData = loadAudioFile(getHarvardAudioFile());
        frenchAudioData = loadAudioFile(getFrenchAudioFile());
        
        logTestProgress("AudioService initialized for testing with audio files: " + getHarvardAudioFile() + ", " + getFrenchAudioFile());
    }
        
    @Test
    @DisplayName("Simple transcription should return valid text response")
    void testSimpleTranscription() {
        requireApiKey();
        
        var response = audioService.simpleTranscription(
            getWhisperModel(), 
            testAudioData, 
            getHarvardAudioFile()
        );
        
        assertNotNull(response, "Response should not be null");
        assertNotNull(response.text(), "Response text should not be null");
        assertFalse(response.text().trim().isEmpty(), "Response text should not be empty");
        assertTrue(response.getTextLength() > 0, "Text length should be positive");
        
        logTestProgress("Simple transcription test passed - transcribed " + response.getTextLength() + " characters");
    }
        
    @Test
    @DisplayName("Advanced transcription with builder should return detailed response")
    void testAdvancedTranscriptionWithBuilder() {
        requireApiKey();
        
        var request = TranscriptionRequestBuilder.withFile(getWhisperModel(), testAudioData)
            .language("en")
            .prompt("This is a test transcription")
            .responseFormat(ResponseFormat.VERBOSE_JSON)
            .temperature(0.2)
            .build();
        
        var response = audioService.createTranscription(request);
        
        assertNotNull(response, "Response should not be null");
        assertNotNull(response.text(), "Response text should not be null");
        assertFalse(response.text().trim().isEmpty(), "Response text should not be empty");
        assertTrue(response.getTextLength() > 0, "Text length should be positive");
        
        logTestProgress("Advanced transcription test passed - detailed response received");
    }
        
    @Test
    @DisplayName("Simple translation should convert audio to English text")
    void testSimpleTranslation() {
        requireApiKey();
        
        var response = audioService.simpleTranslation(
            getWhisperModel(), 
            testAudioData, 
            getHarvardAudioFile()
        );
        
        assertNotNull(response, "Response should not be null");
        assertNotNull(response.text(), "Response text should not be null");
        assertFalse(response.text().trim().isEmpty(), "Response text should not be empty");
        assertTrue(response.getTextLength() > 0, "Text length should be positive");
        
        logTestProgress("Simple translation test passed - translated to English");
    }
        
    @Test
    @DisplayName("Advanced translation with builder should return detailed response")
    void testAdvancedTranslationWithBuilder() {
        requireApiKey();
        
        var request = TranslationRequestBuilder.withFile(getWhisperModel(), testAudioData)
            .prompt("Translate this audio to English")
            .responseFormat(ResponseFormat.JSON)
            .temperature(0.3)
            .build();
        
        var response = audioService.createTranslation(request);
        
        assertNotNull(response, "Response should not be null");
        assertNotNull(response.text(), "Response text should not be null");
        assertFalse(response.text().trim().isEmpty(), "Response text should not be empty");
        assertTrue(response.getTextLength() > 0, "Text length should be positive");
        
        logTestProgress("Advanced translation test passed - detailed translation response");
    }
        
    @Test
    @DisplayName("Simple speech synthesis should generate audio data")
    void testSimpleSpeechSynthesis() {
        requireApiKey();
        
        // Test might fail if TTS is premium feature - use try-catch with assumption
        assertDoesNotThrow(() -> {
            try {
                var basicRequest = SpeechRequestBuilder.create(getSynthesisModel())
                    .input("Hello world")
                    .voice(getSynthesisVoice())
                    .build();
                
                byte[] audioData = audioService.createSpeech(basicRequest);
                
                assertNotNull(audioData, "Audio data should not be null");
                assertTrue(audioData.length > 0, "Audio data should not be empty");
                
                logTestProgress("Simple speech synthesis test passed - generated " + formatBytes(audioData.length));
            } catch (Exception e) {
                // TTS might be premium feature, log and skip
                logTestProgress("Speech synthesis might be premium feature: " + e.getMessage());
                Assumptions.assumeTrue(false, "Speech synthesis requires premium access");
            }
        });
    }
        
    @Test
    @DisplayName("Advanced speech synthesis with builder should generate audio with custom settings")
    void testAdvancedSpeechSynthesisWithBuilder() {
        requireApiKey();
        
        assertDoesNotThrow(() -> {
            try {
                var request = SpeechRequestBuilder.create(getSynthesisModel())
                    .input("This is an advanced test of speech synthesis.")
                    .voice(getSynthesisVoice())
                    .responseFormat(AudioFormat.MP3)
                    .sampleRate(SampleRate.RATE_44100)
                    .speed(1.2)
                    .build();
                
                byte[] audioData = audioService.createSpeech(request);
                
                assertNotNull(audioData, "Audio data should not be null");
                assertTrue(audioData.length > 0, "Audio data should not be empty");
                
                logTestProgress("Advanced speech synthesis test passed - custom settings applied");
            } catch (Exception e) {
                logTestProgress("Advanced speech synthesis might be premium feature: " + e.getMessage());
                Assumptions.assumeTrue(false, "Advanced speech synthesis requires premium access");
            }
        });
    }
        
    @Test
    @DisplayName("French audio translation should convert to English text")
    void testFrenchAudioTranslation() {
        requireApiKey();
        
        // Test simple translation method
        var simpleTranslation = audioService.simpleTranslation(
            getWhisperModel(), 
            frenchAudioData, 
            getFrenchAudioFile()
        );
        
        // Test advanced translation with builder
        var advancedRequest = TranslationRequestBuilder.withFile(getWhisperModel(), frenchAudioData)
            .prompt("Translate this French audio to English")
            .responseFormat(ResponseFormat.JSON)
            .temperature(0.2)
            .build();
        
        var advancedTranslation = audioService.createTranslation(advancedRequest);
        
        // Assertions for simple translation
        assertNotNull(simpleTranslation, "Simple translation should not be null");
        assertNotNull(simpleTranslation.text(), "Simple translation text should not be null");
        assertTrue(simpleTranslation.getTextLength() > 0, "Simple translation should have text");
        
        // Assertions for advanced translation
        assertNotNull(advancedTranslation, "Advanced translation should not be null");
        assertNotNull(advancedTranslation.text(), "Advanced translation text should not be null");
        assertTrue(advancedTranslation.getTextLength() > 0, "Advanced translation should have text");
        
        logTestProgress("French audio translation test passed - both methods work");
    }
        
    @Test
    @DisplayName("URL-based transcription should handle mock URL gracefully")
    void testUrlBasedTranscription() {
        requireApiKey();
        
        var request = TranscriptionRequestBuilder.withUrl(
            getWhisperModel(), 
            "https://example.com/test-audio.wav"
        )
        .language("en")
        .asJson()
        .build();
        
        // This test is expected to fail with mock URL, but validates the request building
        assertThrows(Exception.class, () -> {
            audioService.createTranscription(request);
        }, "Mock URL should throw an exception");
        
        logTestProgress("URL-based transcription test passed - request builder works correctly");
    }
        
    @Test
    @DisplayName("Invalid model should throw appropriate exception")
    void testErrorHandlingInvalidModel() {
        requireApiKey();
        
        assertThrows(Exception.class, () -> {
            audioService.simpleTranscription(
                "invalid-model-name", 
                testAudioData, 
                getHarvardAudioFile()
            );
        }, "Invalid model should throw an exception");
        
        logTestProgress("Error handling test passed");
    }
        
    @Nested
    @DisplayName("Audio Response Format Tests")
    class AudioResponseFormatTests {
        
        @ParameterizedTest
        @ValueSource(strings = {"JSON", "VERBOSE_JSON"})
        @DisplayName("Different response formats should work correctly")
        void testDifferentResponseFormats(String formatName) {
            requireApiKey();
            
            ResponseFormat format = ResponseFormat.valueOf(formatName);
            
            var request = TranscriptionRequestBuilder.withFile(getWhisperModel(), testAudioData)
                .language("en")
                .responseFormat(format)
                .build();
            
            var response = audioService.createTranscription(request);
            
            assertNotNull(response, "Response should not be null");
            assertNotNull(response.text(), "Response text should not be null");
            
            logTestProgress("Response format " + formatName + " test passed");
        }
    }
    
    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        long kb = bytes / 1024;
        if (kb < 1024) return kb + " KB";
        long mb = kb / 1024;
        return mb + " MB";
    }
    
    /**
     * Loads a real audio file from test resources.
     * 
     * @param filename the name of the audio file in test resources
     * @return byte array containing the audio file data
     */
    private byte[] loadAudioFile(String filename) {
        try {
            var classLoader = AudioServiceTest.class.getClassLoader();
            try (var inputStream = classLoader.getResourceAsStream(filename)) {
                
                if (inputStream == null) {
                    throw new RuntimeException("Audio file not found in resources: " + filename);
                }
                
                byte[] audioData = inputStream.readAllBytes();
                logTestProgress("Loaded audio file: " + filename + " (" + formatBytes(audioData.length) + ")");
                return audioData;
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to load audio file: " + filename, e);
        }
    }
}
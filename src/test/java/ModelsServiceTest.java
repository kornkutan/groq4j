import groq4j.models.models.Model;
import groq4j.models.models.ModelListResponse;
import groq4j.services.ModelsService;
import groq4j.services.ModelsServiceImpl;

import java.util.List;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Models Service Tests")
@Tag("integration")
@Tag("requires-api-key")
class ModelsServiceTest extends BaseServiceTest {
    
    private ModelsService modelsService;
    private ModelListResponse cachedResponse;
    
    @BeforeAll
    void setup() {
        super.baseSetup();
        requireServiceEnabled("Models", isModelsEnabled());
        modelsService = ModelsServiceImpl.create(getApiKey());
        logTestProgress("ModelsService initialized for testing");
    }
        
    @Test
    @DisplayName("List all models should return valid response with data")
    void testListAllModels() {
        requireApiKey();
        
        ModelListResponse response = modelsService.listModels();
        cachedResponse = response; // Cache for other tests
        
        assertNotNull(response, "Response should not be null");
        assertNotNull(response.object(), "Response object should not be null");
        assertFalse(response.isEmpty(), "Response should not be empty");
        assertTrue(response.getModelCount() > 0, "Should have at least one model");
        assertNotNull(response.data(), "Data list should not be null");
        
        logTestProgress("List all models test passed - found " + response.getModelCount() + " models");
    }
        
    @Test
    @DisplayName("Get active chat models should return only chat models")
    void testGetActiveChatModels() {
        requireApiKey();
        
        ModelListResponse response = getCachedResponse();
        List<Model> chatModels = response.getChatModels();
        
        assertNotNull(chatModels, "Chat models list should not be null");
        assertTrue(chatModels.size() > 0, "Should have at least one chat model");
        
        // Verify all returned models are actually chat models
        for (Model model : chatModels) {
            assertTrue(model.isChatModel(), "Model " + model.id() + " should be a chat model");
            assertTrue(model.isActive(), "Model " + model.id() + " should be active");
        }
        
        logTestProgress("Active chat models test passed - found " + chatModels.size() + " chat models");
    }
        
    @Test
    @DisplayName("Get Whisper models should return only Whisper models")
    void testGetWhisperModels() {
        requireApiKey();
        
        ModelListResponse response = getCachedResponse();
        List<Model> whisperModels = response.getWhisperModels();
        
        assertNotNull(whisperModels, "Whisper models list should not be null");
        
        // Verify all returned models are actually Whisper models
        for (Model model : whisperModels) {
            assertTrue(model.isWhisperModel(), "Model " + model.id() + " should be a Whisper model");
        }
        
        logTestProgress("Whisper models test passed - found " + whisperModels.size() + " Whisper models");
    }
        
    @Test
    @DisplayName("Get TTS models should return only TTS models")
    void testGetTtsModels() {
        requireApiKey();
        
        ModelListResponse response = getCachedResponse();
        List<Model> ttsModels = response.getTtsModels();
        
        assertNotNull(ttsModels, "TTS models list should not be null");
        
        // Verify all returned models are actually TTS models
        for (Model model : ttsModels) {
            assertTrue(model.isTtsModel(), "Model " + model.id() + " should be a TTS model");
        }
        
        logTestProgress("TTS models test passed - found " + ttsModels.size() + " TTS models");
    }
        
    @Test
    @DisplayName("Retrieve specific chat model should return model details")
    void testRetrieveSpecificChatModel() {
        requireApiKey();
        
        Model model = modelsService.retrieveModel(getDefaultModel());
        
        assertNotNull(model, "Model should not be null");
        assertEquals(getDefaultModel(), model.id(), "Model ID should match request");
        assertNotNull(model.object(), "Model object should not be null");
        assertTrue(model.contextWindow() > 0, "Context window should be positive");
        assertNotNull(model.ownedBy(), "ownedBy should not be null");
        assertTrue(model.isChatModel(), "Should be identified as chat model");
        
        logTestProgress("Retrieve specific chat model test passed - " + model.id());
    }
        
    @Test
    @DisplayName("Retrieve specific Whisper model should return model details")
    void testRetrieveSpecificWhisperModel() {
        requireApiKey();
        
        Model model = modelsService.retrieveModel(TestConstants.AUDIO_TRANSCRIPTION_MODEL);
        
        assertNotNull(model, "Model should not be null");
        assertEquals(TestConstants.AUDIO_TRANSCRIPTION_MODEL, model.id(), "Model ID should match request");
        assertTrue(model.isWhisperModel(), "Should be identified as Whisper model");
        assertNotNull(model.ownedBy(), "ownedBy should not be null");
        
        logTestProgress("Retrieve specific Whisper model test passed - " + model.id());
    }
        
    @Test
    @DisplayName("Check model existence should correctly identify configured models")
    void testConfiguredModelExistence() {
        requireApiKey();
        
        ModelListResponse response = getCachedResponse();
        
        assertTrue(response.hasModel(getDefaultModel()), "Default model " + getDefaultModel() + " should exist");
        assertTrue(response.hasModel(getWhisperModel()), "Whisper model " + getWhisperModel() + " should exist");
        
        logTestProgress("Model existence test passed for configured models");
    }
    
    @Test
    @DisplayName("Check non-existent model should return false")
    void testNonExistentModel() {
        requireApiKey();
        
        ModelListResponse response = getCachedResponse();
        
        assertFalse(response.hasModel("non-existent-model-12345"), "Non-existent model should not be found");
        
        logTestProgress("Non-existent model test passed");
    }
        
    @Test
    @DisplayName("Find existing model should return model object")
    void testFindExistingModel() {
        requireApiKey();
        
        ModelListResponse response = getCachedResponse();
        
        Model foundModel = response.findModel(getDefaultModel());
        
        assertNotNull(foundModel, "Found model should not be null");
        assertEquals(getDefaultModel(), foundModel.id(), "Found model ID should match");
        assertTrue(foundModel.isChatModel(), "Found model should be a chat model");
        
        logTestProgress("Find existing model test passed - " + foundModel.id());
    }
    
    @Test
    @DisplayName("Find non-existent model should throw IllegalArgumentException")
    void testFindNonExistentModel() {
        requireApiKey();
        
        ModelListResponse response = getCachedResponse();
        
        assertThrows(IllegalArgumentException.class, () -> {
            response.findModel("definitely-non-existent-model");
        }, "Finding non-existent model should throw IllegalArgumentException");
        
        logTestProgress("Find non-existent model test passed");
    }
        
    @Test
    @DisplayName("Retrieve invalid model should throw appropriate exception")
    void testErrorHandlingInvalidModel() {
        requireApiKey();
        
        assertThrows(Exception.class, () -> {
            modelsService.retrieveModel("definitely-non-existent-model-12345");
        }, "Retrieving invalid model should throw an exception");
        
        logTestProgress("Error handling test passed");
    }
        
    @Nested
    @DisplayName("Utility Methods Tests")
    class UtilityMethodsTests {
        
        @Test
        @DisplayName("Performance and utility methods should work correctly")
        void testPerformanceAndUtilityMethods() {
            requireApiKey();
            
            long startTime = System.currentTimeMillis();
            ModelListResponse response = modelsService.listModels();
            long endTime = System.currentTimeMillis();
            
            // Performance assertions
            long duration = endTime - startTime;
            assertTrue(duration < 10000, "API call should complete within 10 seconds");
            
            // Utility method assertions
            assertTrue(response.getModelCount() > 0, "Should have at least one model");
            assertFalse(response.isEmpty(), "Response should not be empty");
            assertNotNull(response.getActiveModels(), "Active models list should not be null");
            assertNotNull(response.getChatModels(), "Chat models list should not be null");
            assertNotNull(response.getWhisperModels(), "Whisper models list should not be null");
            assertNotNull(response.getTtsModels(), "TTS models list should not be null");
            
            List<String> modelIds = response.getModelIds();
            assertNotNull(modelIds, "Model IDs list should not be null");
            assertEquals(response.getModelCount(), modelIds.size(), "Model IDs count should match total count");
            
            logTestProgress("Performance and utility methods test passed - API call took " + duration + "ms");
        }
    }
    
    private ModelListResponse getCachedResponse() {
        if (cachedResponse == null) {
            cachedResponse = modelsService.listModels();
        }
        return cachedResponse;
    }
        
}
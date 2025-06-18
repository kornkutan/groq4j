package groq4j;

import groq4j.services.ChatService;
import groq4j.services.AudioService;
import groq4j.services.ModelsService;
import groq4j.services.BatchService;
import groq4j.services.FilesService;

/**
 * Main interface for the Groq API client providing unified access to all Groq services.
 * This interface offers both legacy async methods and modern service access.
 */
public interface GroqApiClient {
    
    /**
     * Gets the chat service for chat completions and conversations.
     * @return ChatService instance for chat operations
     */
    ChatService chat();
    
    /**
     * Gets the audio service for transcription, translation, and speech synthesis.
     * @return AudioService instance for audio operations
     */
    AudioService audio();
    
    /**
     * Gets the models service for listing and retrieving model information.
     * @return ModelsService instance for model operations
     */
    ModelsService models();
    
    /**
     * Gets the batch service for batch processing operations.
     * @return BatchService instance for batch operations
     */
    BatchService batch();
    
    /**
     * Gets the files service for file management operations.
     * @return FilesService instance for file operations
     */
    FilesService files();
}
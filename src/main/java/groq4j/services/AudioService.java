package groq4j.services;

import groq4j.models.audio.SpeechRequest;
import groq4j.models.audio.TranscriptionRequest;
import groq4j.models.audio.TranscriptionResponse;
import groq4j.models.audio.TranslationRequest;
import groq4j.models.audio.TranslationResponse;

/**
 * Service interface for audio operations with the Groq API.
 * 
 * Provides functionality for:
 * - Audio transcription (speech-to-text)
 * - Audio translation (speech-to-text in English)
 * - Text-to-speech synthesis
 */
public interface AudioService {
    
    /**
     * Creates a transcription of audio data.
     * 
     * @param request the transcription request containing audio data and parameters
     * @return transcription response with the transcribed text
     * @throws groq4j.exceptions.GroqApiException if the API request fails
     * @throws groq4j.exceptions.GroqValidationException if the request parameters are invalid
     */
    TranscriptionResponse createTranscription(TranscriptionRequest request);
    
    /**
     * Creates a translation of audio data to English.
     * 
     * @param request the translation request containing audio data and parameters
     * @return translation response with the translated text
     * @throws groq4j.exceptions.GroqApiException if the API request fails
     * @throws groq4j.exceptions.GroqValidationException if the request parameters are invalid
     */
    TranslationResponse createTranslation(TranslationRequest request);
    
    /**
     * Creates speech audio from text input.
     * 
     * @param request the speech synthesis request containing text and voice parameters
     * @return byte array containing the generated audio data
     * @throws groq4j.exceptions.GroqApiException if the API request fails
     * @throws groq4j.exceptions.GroqValidationException if the request parameters are invalid
     */
    byte[] createSpeech(SpeechRequest request);
    
    /**
     * Convenience method for simple transcription with file data.
     * 
     * @param model the model to use for transcription (e.g., "whisper-large-v3")
     * @param audioData the audio file data as byte array
     * @param filename optional filename for the audio data (used for format detection)
     * @return transcription response with the transcribed text
     * @throws groq4j.exceptions.GroqApiException if the API request fails
     * @throws groq4j.exceptions.GroqValidationException if the parameters are invalid
     */
    TranscriptionResponse simpleTranscription(String model, byte[] audioData, String filename);
    
    /**
     * Convenience method for simple translation with file data.
     * 
     * @param model the model to use for translation (e.g., "whisper-large-v3")
     * @param audioData the audio file data as byte array
     * @param filename optional filename for the audio data (used for format detection)
     * @return translation response with the translated text
     * @throws groq4j.exceptions.GroqApiException if the API request fails
     * @throws groq4j.exceptions.GroqValidationException if the parameters are invalid
     */
    TranslationResponse simpleTranslation(String model, byte[] audioData, String filename);
    
    /**
     * Convenience method for simple speech synthesis.
     * 
     * @param model the model to use for speech synthesis
     * @param text the text to convert to speech
     * @param voice the voice to use for synthesis
     * @return byte array containing the generated audio data
     * @throws groq4j.exceptions.GroqApiException if the API request fails
     * @throws groq4j.exceptions.GroqValidationException if the parameters are invalid
     */
    byte[] simpleSpeech(String model, String text, String voice);
}
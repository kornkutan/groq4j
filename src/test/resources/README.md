# Groq4J Test Configuration Guide

This directory contains comprehensive test configuration files for the Groq4J library test suite, featuring a flexible configuration system that allows you to customize test behavior through `application.properties`.

## 🚀 Quick Start

1. **Use Default Configuration**: Tests work out-of-the-box with fallback settings
2. **Set Your API Key**: Export `GROQ_API_KEY` environment variable or edit `application.properties`
3. **Run Tests**: Execute `mvn test` to run the complete test suite

## 📁 Configuration Files

### application.properties
The main configuration file containing:
- **API Key Management**: Multi-source key resolution with priority order
- **Model Configuration**: Configurable AI models for different test scenarios
- **Service Controls**: Enable/disable specific test categories
- **Timeout Settings**: Customizable test execution timeouts
- **Audio File Paths**: Configurable test audio file references

### Audio Test Files
- `harvard.wav` - English audio sample (3MB) for transcription testing
- `french_audio.wav` - French audio sample (1MB) for translation testing

## API Key Configuration

The test suite uses a **3-tier priority system** for API key resolution:

### Priority 1: Environment Variable
```bash
export GROQ_API_KEY="gsk_your_actual_api_key_here"
mvn test
```

### Priority 2: Properties File
Edit `src/test/resources/application.properties`:
```properties
groq.api.key=gsk_your_actual_api_key_here
```

## Test Configuration Options

### Model Configuration
```properties
# Primary models used across test suites
test.model.default=llama-3.1-8b-instant
test.model.alternative=deepseek-r1-distill-llama-70b
test.model.whisper=whisper-large-v3
test.model.synthesis=playai-tts
test.model.synthesis.voice=Fritz-PlayAI
```

### Service Control Switches
```properties
# Enable/disable entire test categories
test.chat.enabled=true          # Chat completion tests
test.models.enabled=true        # Model listing and retrieval tests  
test.audio.enabled=true         # Audio transcription/translation tests
test.batch.enabled=true         # Batch processing tests (premium)
test.files.enabled=true         # File management tests (premium)
```

**Use Cases:**
- **Skip Premium Features**: Set `test.batch.enabled=false` and `test.files.enabled=false` for free accounts
- **Focus Testing**: Enable only specific services during development
- **CI/CD Optimization**: Disable time-intensive audio tests in fast pipelines

### Timeout Configuration
```properties
# Customize test execution timeouts (in seconds)
test.timeout.short=10           # Quick API calls
test.timeout.medium=30          # Standard operations
test.timeout.long=60            # File uploads, audio processing
```

### Audio File Configuration
```properties
# Configurable test audio files
test.audio.harvard.file=harvard.wav
test.audio.french.file=french_audio.wav
```

## Running Tests

### Complete Test Suite
```bash
# Run all enabled tests with full configuration
mvn test
```

**Sample Output:**
```
[TEST] Test configuration loaded: model=llama-3.1-8b-instant, timeouts=10s/30s/60s
[TEST] ChatService initialized for testing with model: llama-3.1-8b-instant
[TEST] AudioService initialized for testing with audio files: harvard.wav, french_audio.wav
Tests run: 50, Failures: 0, Errors: 0, Skipped: 12
```

### Service-Specific Testing
```bash
# Test individual services
mvn test -Dtest=ChatServiceTest
mvn test -Dtest=ModelsServiceTest  
mvn test -Dtest=AudioServiceTest
mvn test -Dtest=BatchServiceTest
mvn test -Dtest=FilesServiceTest
```

### Individual Test Methods
```bash
# Test specific functionality
mvn test -Dtest=ChatServiceTest#testSimpleChatCompletion
mvn test -Dtest=AudioServiceTest#testSimpleTranscription
mvn test -Dtest=ModelsServiceTest#testConfiguredModelExistence
```

### Category-Based Testing
```bash
# Run specific test categories
mvn test -Dgroups="integration"
mvn test -Dgroups="requires-api-key"
mvn test -DexcludedGroups="premium-feature"
mvn test -DexcludedGroups="requires-audio-files"
```

## 🎯 Configuration Examples

### Example 1: Free Account Setup
```properties
# Optimal configuration for free Groq accounts
groq.api.key=your_free_api_key_here
test.batch.enabled=false        # Skip premium batch tests
test.files.enabled=false        # Skip premium file tests
test.audio.enabled=true         # Audio works on free tier
test.chat.enabled=true          # Chat works on free tier
test.models.enabled=true        # Models listing works on free tier
```

### Example 2: Premium Account Setup
```properties
# Full feature testing for premium accounts
groq.api.key=your_premium_api_key_here
test.batch.enabled=true         # Enable batch processing tests
test.files.enabled=true         # Enable file management tests
test.audio.enabled=true         # Enable audio processing tests
test.model.default=llama-3.1-8b-instant
test.model.alternative=deepseek-r1-distill-llama-70b
```

### Example 3: CI/CD Pipeline Setup
```properties
# Fast pipeline configuration
test.timeout.short=5            # Faster timeouts
test.timeout.medium=15
test.timeout.long=30
test.audio.enabled=false        # Skip large audio files in CI
test.batch.enabled=false        # Skip premium features
test.files.enabled=false        # Skip premium features
```

### Example 4: Model Testing Setup
```properties
# Testing with specific models
test.model.default=gemma-7b-it
test.model.alternative=mixtral-8x7b-32768
test.model.whisper=whisper-large-v3-turbo
# Run: mvn test -Dtest=ChatServiceTest
```

## Test Categories and Expected Results

### Core Services (Always Available)
- **ChatServiceTest**: 8 tests - Chat completions with different models
- **ModelsServiceTest**: 12 tests - Model listing, retrieval, validation
- **Expected Result**: All tests pass with valid API key

### Audio Services (Free Tier Compatible)  
- **AudioServiceTest**: 11 tests - Transcription, translation, synthesis
- **Expected Result**: Transcription/translation pass, synthesis may require premium
- **Audio Files Required**: `harvard.wav`, `french_audio.wav`

### Premium Services (Paid Accounts Only)
- **BatchServiceTest**: 8 tests - Batch job management
- **FilesServiceTest**: 10 tests - File upload, management, deletion
- **Expected Result**: Tests gracefully skip with 403 errors on free accounts

## Troubleshooting

### Common Issues and Solutions

#### Issue: Tests Skipped with "Service Disabled"
```
org.opentest4j.TestAbortedException: Chat tests are disabled in configuration
```
**Solution**: Check `application.properties` and set `test.chat.enabled=true`

#### Issue: Model Decommissioned Error
```
model_decommissioned: The model `llama-3.1-70b-versatile` has been decommissioned
```
**Solution**: Update model in `application.properties`:
```properties
test.model.alternative=deepseek-r1-distill-llama-70b
```

#### Issue: Premium Feature Errors (403)
```
GroqApiException{statusCode=403, message='Not available for your plan'}
```
**Solution**: Disable premium features:
```properties
test.batch.enabled=false
test.files.enabled=false
```

#### Issue: Audio Files Not Found
```
RuntimeException: Audio file not found in resources: harvard.wav
```
**Solution**: Ensure audio files exist in `src/test/resources/` or update paths:
```properties
test.audio.harvard.file=your_audio_file.wav
```

## Test Architecture Overview

### Configuration Loading Flow
1. **BaseServiceTest.baseSetup()** loads `application.properties`
2. **Configuration Resolution**: Properties → Defaults → Validation
3. **Service Initialization**: Each test service uses resolved configuration
4. **Test Execution**: Tests use configuration values via getter methods

### Key Classes
- **BaseServiceTest**: Base class providing configuration loading and service enablement
- **TestConstants**: Static constants and utility methods
- **Individual Test Classes**: Use configuration via inherited methods

### Configuration Methods Available in Tests
```java
// Model configuration
protected String getDefaultModel()      // test.model.default
protected String getAlternativeModel()  // test.model.alternative  
protected String getWhisperModel()      // test.model.whisper
protected String getSynthesisModel()    // test.model.synthesis
protected String getSynthesisVoice()    // test.model.synthesis.voice

// Timeout configuration
protected int getShortTimeout()         // test.timeout.short
protected int getMediumTimeout()        // test.timeout.medium
protected int getLongTimeout()          // test.timeout.long

// Audio file configuration
protected String getHarvardAudioFile()  // test.audio.harvard.file
protected String getFrenchAudioFile()   // test.audio.french.file

// Service enablement
protected boolean isChatEnabled()       // test.chat.enabled
protected boolean isAudioEnabled()      // test.audio.enabled
protected void requireServiceEnabled(String name, boolean enabled)
```

## Performance and Optimization

### Test Execution Times (Typical)
- **ChatServiceTest**: ~6 seconds (8 tests)
- **ModelsServiceTest**: ~3 seconds (12 tests)  
- **AudioServiceTest**: ~16 seconds (11 tests, includes large audio files)
- **BatchServiceTest**: ~2 seconds (8 tests, mostly skipped on free tier)
- **FilesServiceTest**: ~2 seconds (10 tests, mostly skipped on free tier)

### Optimization Tips
1. **Use Service Toggles**: Disable unused services for faster execution
2. **Adjust Timeouts**: Reduce timeouts for CI/CD pipelines
3. **Skip Audio Tests**: Disable `test.audio.enabled=false` for faster feedback
4. **Parallel Execution**: Use `mvn test -T 4` for parallel test execution
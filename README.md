# Groq4J - Unofficial Java Client for GroqCloud AI API

[![Maven Central](https://img.shields.io/maven-central/v/io.github.kornkutan/groq4j.svg)](https://search.maven.org/artifact/io.github.kornkutan/groq4j)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java 21+](https://img.shields.io/badge/Java-21%2B-blue.svg)](https://openjdk.java.net/projects/jdk/21/)

**Groq4J** is an unofficial Java library that provides a simple and intuitive interface to interact with the [GroqCloud AI API](https://console.groq.com/).

> **Note**: This is an unofficial library and is not affiliated with Groq Inc. It is intended primarily for server-side applications.

## ⚡ Features

- **Chat Completions** - Advanced conversational AI with multiple models
- **Audio Processing** - Transcription, translation, and text-to-speech synthesis  
- **Model Management** - List, retrieve, and validate available AI models
- **File Operations** - Upload, manage, and process files (Premium feature)
- **Batch Processing** - Handle bulk AI operations efficiently (Premium feature)
- **No External Dependencies** - Pure Java implementation with minimal footprint
- **Builder Pattern** - Intuitive request building with fluent APIs
- **Comprehensive Testing** - 50+ JUnit 5 tests with configurable test framework


## 🚧 Current Limitations

- **No Server-Sent Events (SSE) Streaming**: Streaming responses are not yet supported
- **Server-Side Focus**: Designed primarily for backend/server applications
- **Premium Features**: Some features require paid GroqCloud plans

## 🚀 Supported APIs

| Service | Description | Status |
|---------|-------------|--------|
| **Chat Completions** | Multi-turn conversations, system prompts | ✅ Full Support |
| **Audio Transcription** | Speech-to-text conversion | ✅ Full Support |
| **Audio Translation** | Audio translation to English | ✅ Full Support |
| **Text-to-Speech** | Voice synthesis from text | ✅ Full Support |
| **Models** | List and retrieve model information | ✅ Full Support |
| **Files** | File upload and management | ⚠️ Premium Only |
| **Batch** | Bulk processing operations | ⚠️ Premium Only |

## 📦 Installation

### Maven

```xml
<dependency>
    <groupId>io.github.kornkutan</groupId>
    <artifactId>groq4j</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle

```gradle
implementation 'io.github.kornkutan:groq4j:1.0.0'
```

### Requirements

- **Java 21** or higher
- **GroqCloud API Key** - Get yours at [console.groq.com](https://console.groq.com/)

## 🔧 Quick Start

```java
import groq4j.services.ChatServiceImpl;

// Initialize the client
var chatService = ChatServiceImpl.create("your-api-key-here");

// Simple chat completion
var response = chatService.simple("llama-3.1-8b-instant", "Hello, how are you?");
System.out.println(response.choices().getFirst().message().content().orElse(""));
```

## 📚 Usage Examples

### 🗣️ Chat Completions

#### Simple Chat
```java
import groq4j.services.ChatServiceImpl;

var chatService = ChatServiceImpl.create("your-api-key");

// Basic chat completion
var response = chatService.simple(
    "llama-3.1-8b-instant", 
    "Explain quantum computing in simple terms."
);
System.out.println(response.choices().getFirst().message().content().orElse(""));
```

#### Advanced Chat with System Prompt
```java
import groq4j.builders.ChatCompletionRequestBuilder;
import groq4j.models.common.Message;

var request = ChatCompletionRequestBuilder.create("llama-3.1-8b-instant")
    .systemMessage("You are a helpful coding assistant specializing in Java.")
    .userMessage("Show me how to implement a singleton pattern.")
    .temperature(0.7)
    .maxCompletionTokens(500)
    .build();

var response = chatService.createCompletion(request);
```

#### Multi-turn Conversation
```java
var messages = List.of(
    Message.system("You are a travel guide."),
    Message.user("I'm planning a trip to Japan. What should I visit?"),
    Message.assistant("Japan offers amazing destinations! Consider Tokyo for modern culture..."),
    Message.user("What about traditional temples?")
);

var request = ChatCompletionRequestBuilder.create("llama-3.1-8b-instant")
    .messages(messages)
    .temperature(0.8)
    .build();
```

### 🎵 Audio Processing

#### Speech Transcription
```java
import groq4j.services.AudioServiceImpl;
import groq4j.builders.TranscriptionRequestBuilder;

var audioService = AudioServiceImpl.create("your-api-key");

// Simple transcription
byte[] audioData = Files.readAllBytes(Path.of("speech.wav"));
var transcription = audioService.simpleTranscription(
    "whisper-large-v3", 
    audioData, 
    "speech.wav"
);
System.out.println("Transcription: " + transcription.text());
```

#### Advanced Transcription
```java
var request = TranscriptionRequestBuilder.withFile("whisper-large-v3", audioData)
    .language("en")
    .prompt("This is a technical presentation about AI")
    .responseFormat(ResponseFormat.VERBOSE_JSON)
    .temperature(0.2)
    .build();

var response = audioService.createTranscription(request);
```

#### Audio Translation
```java
// Translate foreign language audio to English
var translation = audioService.simpleTranslation(
    "whisper-large-v3",
    foreignAudioData,
    "foreign-speech.wav"
);
System.out.println("English translation: " + translation.text());
```

#### Text-to-Speech Synthesis
```java
import groq4j.builders.SpeechRequestBuilder;
import groq4j.enums.AudioFormat;

var speechRequest = SpeechRequestBuilder.create("playai-tts")
    .input("Hello, this is a test of text-to-speech synthesis.")
    .voice("Fritz-PlayAI")
    .responseFormat(AudioFormat.MP3)
    .speed(1.0)
    .build();

byte[] audioData = audioService.createSpeech(speechRequest);
Files.write(Path.of("output.mp3"), audioData);
```

### 🤖 Model Management

```java
import groq4j.services.ModelsServiceImpl;

var modelsService = ModelsServiceImpl.create("your-api-key");

// List all available models
var models = modelsService.listModels();
System.out.println("Available models: " + models.getModelCount());

// Get chat models only
var chatModels = models.getActiveChatModels();
chatModels.forEach(model -> 
    System.out.println(model.id() + " - " + model.contextWindow() + " tokens")
);

// Retrieve specific model details
var model = modelsService.retrieveModel("llama-3.1-8b-instant");
System.out.println("Model: " + model.displayName());
System.out.println("Context window: " + model.contextWindow());
System.out.println("Owner: " + model.ownedBy());
```

### 📁 File Operations (Premium)

```java
import groq4j.services.FilesServiceImpl;
import groq4j.builders.FileUploadRequestBuilder;
import groq4j.enums.FilePurpose;

var filesService = FilesServiceImpl.create("your-api-key");

// Upload file for batch processing
byte[] fileData = createBatchFile(); // Your JSONL batch data
var request = FileUploadRequestBuilder.createBatchFile(fileData, "batch-requests.jsonl")
    .build();

var uploadedFile = filesService.uploadFile(request);
System.out.println("Uploaded file ID: " + uploadedFile.id());

// List all files
var filesList = filesService.listFiles();
System.out.println("Total files: " + filesList.getTotalCount());

// Download file content
byte[] content = filesService.downloadFileContent(uploadedFile.id());
```

### ⚡ Batch Processing (Premium)

```java
import groq4j.services.BatchServiceImpl;
import groq4j.builders.BatchRequestBuilder;

var batchService = BatchServiceImpl.create("your-api-key");

// Create batch request
var batchRequest = BatchRequestBuilder.create("file-batch-123")
    .completionWindow("24h")
    .metadata("project", "ai-analysis")
    .description("Batch analysis of customer feedback")
    .build();

var batch = batchService.createBatch(batchRequest);
System.out.println("Batch created: " + batch.id());

// Monitor batch status
var batches = batchService.listBatches();
var inProgress = batches.getInProgressBatches();
var completed = batches.getCompletedBatches();

System.out.println("In progress: " + inProgress.size());
System.out.println("Completed: " + completed.size());
```

## 🔧 Configuration

### Environment Variables
```bash
export GROQ_API_KEY="your-api-key-here"
```

### Programmatic Configuration
```java
// Pass API key directly to service constructors
var chatService = ChatServiceImpl.create("gsk_your_api_key_here");
```

## 🧪 Testing

Groq4J includes a comprehensive test suite with configurable settings:

```bash
# Run all tests
mvn test

# Run specific service tests
mvn test -Dtest=ChatServiceTest
mvn test -Dtest=AudioServiceTest

# Skip premium feature tests (for free accounts)
# Edit src/test/resources/application.properties:
# test.batch.enabled=false
# test.files.enabled=false
```

## 🤝 Contributing

We welcome contributions! Here's how you can help:

### 🐛 Issues & Bug Reports
- Found a bug? [Open an issue](https://github.com/kornkutan/groq4j/issues)
- Include steps to reproduce, expected vs actual behavior
- Provide code samples when possible

### ✨ Feature Requests
- Have an idea? [Submit a feature request](https://github.com/kornkutan/groq4j/issues)
- Describe the use case and expected behavior
- Check existing issues to avoid duplicates

### 🔧 Pull Requests
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes and add tests
4. Ensure all tests pass (`mvn test`)
5. Commit your changes (`git commit -m 'Add amazing feature'`)
6. Push to the branch (`git push origin feature/amazing-feature`)
7. Open a Pull Request

### 📋 Development Guidelines
- Follow existing code style and patterns
- Add comprehensive tests for new features
- Update documentation for public APIs
- Use the existing builder pattern for new request types

## 📄 License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

```
MIT License

Copyright (c) 2024 Korn Kutan

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## 👨‍💻 Developer

**Korn Kutan**  
📧 Email: [korn@kutan.me](mailto:korn@kutan.me)  
🐙 GitHub: [@kornkutan](https://github.com/kornkutan)

## 🙏 Acknowledgments

- [GroqCloud](https://groq.com/) for providing the amazing AI infrastructure
- The Java community for excellent tooling and libraries

## ⚠️ Disclaimer

This is an unofficial library and is not affiliated with Groq Inc. Use at your own discretion. Always refer to the official [GroqCloud API documentation](https://console.groq.com/docs) for the most up-to-date API specifications.

---

**Made with ❤️ for the Java community**
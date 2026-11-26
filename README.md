# groq4j

[![build](https://github.com/kornkutan/groq4j/actions/workflows/build.yml/badge.svg)](https://github.com/kornkutan/groq4j/actions/workflows/build.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.kornkutan/groq4j.svg)](https://central.sonatype.com/artifact/io.github.kornkutan/groq4j)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java 21+](https://img.shields.io/badge/Java-21%2B-blue.svg)](https://openjdk.java.net/projects/jdk/21/)

Java client for the GroqCloud API. Zero runtime dependencies, built on `java.net.http`. Unofficial, not affiliated with Groq Inc.

## Install

Java 21 or newer.

```xml
<dependency>
    <groupId>io.github.kornkutan</groupId>
    <artifactId>groq4j</artifactId>
    <version>1.1.0</version>
</dependency>
```

```gradle
implementation 'io.github.kornkutan:groq4j:1.1.0'
```

If that version is not on Central yet, the same jar is attached to the [releases page](https://github.com/kornkutan/groq4j/releases). It has no transitive dependencies, so the jar alone is enough.

## Endpoints

| Endpoint | Service | Notes |
|----------|---------|-------|
| `/openai/v1/chat/completions` | `chat()` | blocking and `stream` |
| `/openai/v1/responses` | `responses()` | blocking and `stream` |
| `/openai/v1/embeddings` | `embeddings()` | `nomic-embed-text-v1_5` |
| `/openai/v1/reranking` | `reranking()` | `qwen3-reranker-4b` |
| `/openai/v1/audio/transcriptions` | `audio()` | file upload or `url` |
| `/openai/v1/audio/translations` | `audio()` | file upload or `url` |
| `/openai/v1/audio/speech` | `audio()` | returns raw audio bytes |
| `/openai/v1/models` | `models()` | |
| `/openai/v1/files` | `files()` | upload, list, fetch, delete, content |
| `/openai/v1/batches` | `batch()` | create, list, fetch, cancel |

## Quick start

```java
import groq4j.GroqApiClientImpl;

var client = GroqApiClientImpl.create(System.getenv("GROQ_API_KEY"));

var response = client.chat().simple("openai/gpt-oss-20b", "Hello, how are you?");
System.out.println(response.choices().getFirst().message().content().orElse(""));
```

## Chat completions

```java
import groq4j.builders.ChatCompletionRequestBuilder;

var request = ChatCompletionRequestBuilder.create("openai/gpt-oss-20b")
    .systemMessage("You write tight technical answers.")
    .userMessage("What is a Groq LPU?")
    .temperature(0.3)
    .maxCompletionTokens(400)
    .build();

var response = client.chat().createCompletion(request);
```

### Streaming

```java
try (var chunks = client.chat().streamCompletion(request)) {
    chunks.forEach(chunk -> System.out.print(chunk.getContent()));
}
```

This holds an HTTP connection open, so always close the stream. The final chunk carries usage, because `stream_options.include_usage` is sent for you.

### Reasoning

```java
import groq4j.enums.ReasoningEffort;

var request = ChatCompletionRequestBuilder.create("openai/gpt-oss-120b")
    .userMessage("Prove that the square root of 2 is irrational.")
    .reasoningEffort(ReasoningEffort.HIGH)
    .includeReasoning(true)
    .build();
```

Effort accepts `none`, `default`, `minimal`, `low`, `medium`, `high`, `xhigh`, `max`. Use `includeReasoning(true)` for raw text or `reasoningFormat(...)` for parsed reasoning, not both.

### Tool calling

```java
import groq4j.models.common.Message;
import groq4j.models.common.Tool;

var tool = Tool.function("get_weather", "Current weather for a city", Map.of(
    "type", "object",
    "properties", Map.of("city", Map.of("type", "string")),
    "required", List.of("city")));

var first = client.chat().createCompletion(
    ChatCompletionRequestBuilder.create("openai/gpt-oss-20b")
        .userMessage("What is the weather in Bangkok?")
        .addTool(tool)
        .build());

var toolCalls = first.choices().getFirst().message().toolCalls().orElse(List.of());
var call = toolCalls.getFirst();
String result = callWeatherApi();  // your code

var second = client.chat().createCompletion(
    ChatCompletionRequestBuilder.create("openai/gpt-oss-20b")
        .userMessage("What is the weather in Bangkok?")
        .addMessage(Message.assistantWithToolCalls(List.of(call)))
        .addMessage(Message.tool(result, call.id()))
        .build());
```

### Structured output

```java
var request = ChatCompletionRequestBuilder.create("openai/gpt-oss-20b")
    .userMessage("Describe Bangkok as JSON with fields city and country.")
    .jsonSchema("city", "City description", Map.of(
        "type", "object",
        "properties", Map.of(
            "city", Map.of("type", "string"),
            "country", Map.of("type", "string")),
        "required", List.of("city", "country")))
    .build();
```

### Citations over your own documents

```java
var request = ChatCompletionRequestBuilder.create("openai/gpt-oss-120b")
    .userMessage("Which chip is deterministic? Cite the document id.")
    .addTextDocument("doc-1", "Groq LPUs are deterministic.")
    .addTextDocument("doc-2", "GPUs are throughput oriented.")
    .build();

client.chat().createCompletion(request).choices().getFirst().message().annotations()
    .orElse(List.of())
    .forEach(a -> a.documentCitation().ifPresent(c -> System.out.println(c.documentId())));
```

Documents are plain text (`addTextDocument`) or JSON (`addJsonDocument`), matched by `id` in the returned annotations.

## Responses API

Groq keeps no conversation state yet, so send the full input on every call.

```java
import groq4j.builders.ResponseRequestBuilder;

var request = ResponseRequestBuilder.create("openai/gpt-oss-20b")
    .input("Explain prompt caching in two sentences.")
    .instructions("Answer in plain English.")
    .maxOutputTokens(200)
    .build();

var response = client.responses().createResponse(request);
System.out.println(response.getOutputText());

try (var events = client.responses().streamResponse(request)) {
    events.filter(e -> e.isTextDelta()).forEach(e -> System.out.print(e.delta()));
}
```

`ResponseObject.raw()` holds the full decoded body for anything the library does not model.

## Embeddings and reranking

```java
import groq4j.builders.EmbeddingRequestBuilder;
import groq4j.builders.RerankRequestBuilder;

var embeddings = client.embeddings().createEmbeddings(
    EmbeddingRequestBuilder.create().input(List.of("first passage", "second passage")).build());

double[] vector = embeddings.getFirst().vector().orElseThrow();

var reranked = client.reranking().rerank(
    RerankRequestBuilder.create()
        .query("what is an LPU?")
        .documents("LPUs are deterministic chips.", "A recipe for bread.")
        .build());

reranked.results().forEach(r -> System.out.println(r.score() + " " + r.document()));
```

Add `base64Encoding()` to receive vectors as base64 strings instead of float arrays.

## Audio

### Transcription and translation

```java
import groq4j.builders.TranscriptionRequestBuilder;
import groq4j.enums.ResponseFormat;
import groq4j.enums.TimestampGranularity;

byte[] audio = Files.readAllBytes(Path.of("speech.wav"));

var detailed = client.audio().createTranscription(
    TranscriptionRequestBuilder.withFile("whisper-large-v3", audio, "speech.wav")
        .language("en")
        .responseFormat(ResponseFormat.VERBOSE_JSON)
        .timestampGranularities(List.of(TimestampGranularity.WORD, TimestampGranularity.SEGMENT))
        .build());

detailed.segments().forEach(s -> System.out.println(s.start() + "-" + s.end() + " " + s.text()));

var translation = client.audio().simpleTranslation("whisper-large-v3", foreignAudio, "speech.mp3");
```

Passing the filename keeps format validation and the content type correct. A hosted file works too: `TranscriptionRequestBuilder.withUrl("whisper-large-v3", url)`.

### Text to speech

```java
import groq4j.builders.SpeechRequestBuilder;
import groq4j.enums.AudioFormat;
import groq4j.enums.TtsVoice;

var speech = SpeechRequestBuilder.create()
    .input("[cheerful] Groq serves models fast.")
    .voice(TtsVoice.TROY)
    .responseFormat(AudioFormat.WAV)
    .build();

Files.write(Path.of("speech.wav"), client.audio().createSpeech(speech));
```

Groq hosts `canopylabs/orpheus-v1-english` and `canopylabs/orpheus-arabic-saudi`. Both return `wav`, cap input at 200 characters, and the English model understands direction tags such as `[whispering]`.

## Files and batches

```java
import groq4j.builders.BatchRequestBuilder;
import groq4j.enums.FilePurpose;

var uploaded = client.files().uploadFile(jsonlBytes, "requests.jsonl", FilePurpose.BATCH);
var batch = client.batch().createBatch(BatchRequestBuilder.create(uploaded.id()).in24Hours().build());

var status = client.batch().retrieveBatch(batch.id());
client.batch().cancelBatch(batch.id());

var files = client.files().listFiles();
byte[] output = client.files().downloadFileContent(status.outputFileId().orElseThrow());
```

Limits: 200 MB and 50,000 requests per batch file, 100 MB per audio file. Batches accept `/v1/chat/completions`, `/v1/audio/transcriptions` and `/v1/audio/translations`.

## Models

```java
var models = client.models().listModels();
models.data().forEach(m -> System.out.println(m.id() + " " + m.contextWindow()));

var model = client.models().retrieveModel("openai/gpt-oss-20b");
```

## Errors

Every failure throws a subclass of `GroqApiException` carrying status code, error type, error code and request id.

| Status | Exception |
|--------|-----------|
| 400 | `GroqBadRequestException` |
| 401 | `GroqAuthenticationException` |
| 403 | `GroqForbiddenException` |
| 404 | `GroqNotFoundException` |
| 413 | `GroqRequestTooLargeException` |
| 422 | `GroqUnprocessableEntityException` |
| 424 | `GroqFailedDependencyException` |
| 429 | `GroqRateLimitException` |
| 498, 499 | `GroqApiException` |
| 5xx | `GroqServerException` |

Local problems throw `GroqValidationException`, `GroqSerializationException` or `GroqNetworkException`.

```java
try {
    client.chat().simple("openai/gpt-oss-20b", "hello");
} catch (GroqRateLimitException e) {
    System.out.println("retry after " + e.getRetryAfterSeconds() + "s, code " + e.getErrorCode());
}
```

## Configuration

Each client owns one `HttpClient` and reuses it. Pass your own for a proxy, custom executor or timeouts:

```java
var httpClient = groq4j.utils.HttpUtils.createHttpClient();
var client = GroqApiClientImpl.create(httpClient, System.getenv("GROQ_API_KEY"));
```

Point the library at a proxy or mock server with `-Dgroq4j.baseUrl=http://localhost:8080/openai/v1`.

## Tests

The offline suite needs no API key. It runs every service method against an in-process fake Groq server and covers request building, response parsing, streaming, every documented error status and transport failures.

```bash
mvn test
mvn test jacoco:report    # coverage report in target/site/jacoco
```

Live API tests are tagged `integration` and skipped by default:

```bash
GROQ_API_KEY=... mvn test -Pintegration
```

## Contributing

Bugs and feature requests go to [issues](https://github.com/kornkutan/groq4j/issues), code goes to [pull requests](https://github.com/kornkutan/groq4j/pulls).

- Keep the library free of runtime dependencies.
- Add an offline test for anything that parses or serializes. `FakeServerTestBase` gives each test its own fake server.
- Match the existing builder pattern for new request types.

## License

MIT. See [LICENSE](LICENSE).

Unofficial library, not affiliated with Groq Inc. [console.groq.com/docs](https://console.groq.com/docs) is the source of truth for API behaviour.

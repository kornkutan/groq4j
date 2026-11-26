# groq4j

[![build](https://github.com/kornkutan/groq4j/actions/workflows/build.yml/badge.svg)](https://github.com/kornkutan/groq4j/actions/workflows/build.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.kornkutan/groq4j.svg)](https://search.maven.org/artifact/io.github.kornkutan/groq4j)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java 21+](https://img.shields.io/badge/Java-21%2B-blue.svg)](https://openjdk.java.net/projects/jdk/21/)

Java client for the GroqCloud API. Zero runtime dependencies, built on `java.net.http`.

Unofficial library, not affiliated with Groq Inc. Meant for server-side code.

## Features

- Chat completions, blocking and streamed over server-sent events
- Responses API, blocking and streamed
- Text embeddings and document reranking
- Speech to text (transcription and translation) with word and segment timestamps
- Text to speech with the Orpheus models
- Batch jobs and file uploads
- Tool calling, structured output, reasoning effort, citations over documents
- JSON parsing and serialization written from scratch, so no Jackson, Gson or OkHttp on your classpath
- Builder pattern for every request, typed exceptions for every documented error code
- Offline unit tests that need no API key

## Supported endpoints

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

## Install

1.1.0 is the current release and is not on Maven Central yet. Grab the jar from the [release page](https://github.com/kornkutan/groq4j/releases/tag/v1.1.0) and put it on your classpath. It has no transitive dependencies, so the jar is all you need:

```bash
curl -LO https://github.com/kornkutan/groq4j/releases/download/v1.1.0/groq4j-1.1.0.jar
```

Or build from source:

```bash
git clone https://github.com/kornkutan/groq4j.git
cd groq4j && mvn install -DskipTests
```

Maven Central still serves 1.0.1, which predates the Responses, Embeddings and Reranking services. Use it only if that is what you want:

```xml
<dependency>
    <groupId>io.github.kornkutan</groupId>
    <artifactId>groq4j</artifactId>
    <version>1.0.1</version>
</dependency>
```

## Quick start

```java
import groq4j.GroqApiClient;

var client = GroqApiClient.create(System.getenv("GROQ_API_KEY"));

var response = client.chat().simple("openai/gpt-oss-20b", "Hello, how are you?");
System.out.println(response.choices().getFirst().message().content().orElse(""));
```

## Chat completions

### System prompt and sampling

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
import groq4j.builders.ChatCompletionRequestBuilder;

var request = ChatCompletionRequestBuilder.create("openai/gpt-oss-20b")
    .userMessage("Count from 1 to 20.")
    .build();

try (var chunks = client.chat().streamCompletion(request)) {
    chunks.forEach(chunk -> System.out.print(chunk.getContent()));
}
```

`streamCompletion` keeps an HTTP connection open, so always close the stream. The last chunk carries usage because `stream_options.include_usage` is sent for you.

### Reasoning control

```java
import groq4j.enums.ReasoningEffort;

var request = ChatCompletionRequestBuilder.create("openai/gpt-oss-120b")
    .userMessage("Prove that the square root of 2 is irrational.")
    .reasoningEffort(ReasoningEffort.HIGH)
    .includeReasoning(true)
    .build();
```

`reasoningEffort` accepts `none`, `default`, `minimal`, `low`, `medium`, `high`, `xhigh`, `max`. Use either `includeReasoning(true)` (raw reasoning text) or `reasoningFormat(...)` (parsed reasoning), not both.

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

var toolCalls = first.choices().getFirst().message().toolCalls();
if (!toolCalls.isEmpty()) {
    var call = toolCalls.getFirst();
    String result = callWeatherApi();  // your code

    var second = client.chat().createCompletion(
        ChatCompletionRequestBuilder.create("openai/gpt-oss-20b")
            .userMessage("What is the weather in Bangkok?")
            .addMessage(Message.assistantWithToolCalls(toolCalls))
            .addMessage(Message.tool(result, call.id()))
            .build());

    System.out.println(second.choices().getFirst().message().content().orElse(""));
}
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

var response = client.chat().createCompletion(request);
response.choices().getFirst().message().annotations().forEach(annotation ->
    annotation.documentCitation().ifPresent(citation ->
        System.out.println(citation.documentId())));
```

Documents are either plain text (`addTextDocument`) or JSON (`addJsonDocument`), matched by `id` in the returned annotations.

## Responses API

The Responses API is the OpenAI compatible alternative to chat completions. Groq does not keep conversation state yet, so send the full input each call.

```java
import groq4j.builders.ResponseRequestBuilder;

var request = ResponseRequestBuilder.create("openai/gpt-oss-20b")
    .input("Explain prompt caching in two sentences.")
    .instructions("Answer in plain English.")
    .maxOutputTokens(200)
    .build();

var response = client.responses().createResponse(request);
System.out.println(response.getOutputText());
System.out.println(response.usage().map(u -> u.totalTokens()).orElse(0));
```

Streaming:

```java
try (var events = client.responses().streamResponse(request)) {
    events.filter(e -> e.isTextDelta()).forEach(e -> System.out.print(e.delta()));
}
```

`ResponseObject.raw()` holds the full decoded body for any field this library does not model.

## Embeddings and reranking

```java
import groq4j.builders.EmbeddingRequestBuilder;
import groq4j.builders.RerankRequestBuilder;

var embeddings = client.embeddings().createEmbeddings(
    EmbeddingRequestBuilder.create()
        .input(List.of("first passage", "second passage"))
        .build());

double[] vector = embeddings.getFirst().vector().orElseThrow();

var reranked = client.reranking().rerank(
    RerankRequestBuilder.create()
        .query("what is an LPU?")
        .documents("LPUs are deterministic chips.", "A recipe for bread.")
        .build());

reranked.results().forEach(r -> System.out.println(r.score() + " " + r.document()));
```

Use `base64Encoding()` on the embedding builder to receive vectors as base64 strings instead of float arrays.

## Audio

### Transcription

```java
import groq4j.builders.TranscriptionRequestBuilder;
import groq4j.enums.ResponseFormat;
import groq4j.enums.TimestampGranularity;

byte[] audio = Files.readAllBytes(Path.of("speech.wav"));

var plain = client.audio().simpleTranscription("whisper-large-v3", audio, "speech.wav");

var detailed = client.audio().createTranscription(
    TranscriptionRequestBuilder.withFile("whisper-large-v3", audio, "speech.wav")
        .language("en")
        .responseFormat(ResponseFormat.VERBOSE_JSON)
        .timestampGranularities(List.of(TimestampGranularity.WORD, TimestampGranularity.SEGMENT))
        .build());

detailed.segments().forEach(s -> System.out.println(s.start() + "-" + s.end() + " " + s.text()));
```

Passing the filename keeps the format validation accurate and sends the right content type. A hosted file works too: `TranscriptionRequestBuilder.withUrl("whisper-large-v3", url)`.

### Translation

```java
var translation = client.audio().simpleTranslation("whisper-large-v3", foreignAudio, "speech.mp3");
System.out.println(translation.text());
```

### Text to speech

```java
import groq4j.builders.SpeechRequestBuilder;
import groq4j.enums.AudioFormat;
import groq4j.enums.TtsVoice;

var request = SpeechRequestBuilder.create()
    .input("[cheerful] Groq serves models fast.")
    .voice(TtsVoice.TROY)
    .responseFormat(AudioFormat.WAV)
    .build();

byte[] wav = client.audio().createSpeech(request);
Files.write(Path.of("speech.wav"), wav);
```

Groq hosts `canopylabs/orpheus-v1-english` and `canopylabs/orpheus-arabic-saudi`. Both return `wav`, accept at most 200 characters of input, and the English model understands vocal direction tags such as `[cheerful]` or `[whispering]`.

## Files and batches

```java
import groq4j.enums.FilePurpose;

var uploaded = client.files().uploadFile(jsonlBytes, "requests.jsonl", FilePurpose.BATCH);

import groq4j.builders.BatchRequestBuilder;

var batch = client.batch().createBatch(
    BatchRequestBuilder.create(uploaded.id()).in24Hours().build());
var status = client.batch().retrieveBatch(batch.id());
client.batch().cancelBatch(batch.id());

var files = client.files().listFiles();
byte[] content = client.files().downloadFileContent(uploaded.id());
byte[] output = client.files().downloadFileContent(client.batch().retrieveBatch(batch.id()).outputFileId().orElseThrow());
```

Limits: 200 MB and 50,000 requests per batch file, 100 MB per audio file. Batch endpoint accepts `/v1/chat/completions`, `/v1/audio/transcriptions` and `/v1/audio/translations`.

## Models

```java
var models = client.models().listModels();
models.data().forEach(m -> System.out.println(m.id() + " " + m.contextWindow()));

var model = client.models().retrieveModel("openai/gpt-oss-20b");
```

## Errors

Every failure throws a subclass of `GroqApiException` with status code, error type, error code and request id.

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

Local problems throw `GroqValidationException` (bad input), `GroqSerializationException` (bad JSON) or `GroqNetworkException` (connection failure).

```java
try {
    client.chat().simple("openai/gpt-oss-20b", "hello");
} catch (groq4j.exceptions.GroqRateLimitException e) {
    System.out.println("retry after " + e.getRetryAfterSeconds() + "s, request " + e.getRequestId());
}
```

## Configuration

The client owns one `HttpClient` per instance and reuses it. Pass your own when you need a proxy, custom executor or timeouts:

```java
var httpClient = groq4j.utils.HttpUtils.createHttpClient();
var client = GroqApiClient.create(httpClient, System.getenv("GROQ_API_KEY"));
```

Point the library at a proxy or a mock server with `-Dgroq4j.baseUrl=http://localhost:8080/openai/v1`.

## Testing

Offline tests need no API key. They run every service method against an in-process fake Groq server on 127.0.0.1 and cover request building, response parsing, streaming, every documented error status and transport failure:

```bash
mvn test
```

Coverage report:

```bash
mvn test jacoco:report
open target/site/jacoco/index.html
```

Live API tests are tagged `integration` and skipped by default. To run them, set `GROQ_API_KEY` and enable the profile:

```bash
mvn test -Pintegration
```

## Build

```bash
mvn compile
mvn test
mvn package
```

## Contributing

Issues and pull requests go to [github.com/kornkutan/groq4j](https://github.com/kornkutan/groq4j).

- Keep the library free of runtime dependencies.
- Add an offline test for anything that parses or serializes. `FakeServerTestBase` gives each test its own fake server.
- Match the existing builder pattern for new request types.

## License

MIT. See [LICENSE](LICENSE).

## Disclaimer

Unofficial library, not affiliated with Groq Inc. The official documentation at [console.groq.com/docs](https://console.groq.com/docs) is the source of truth for API behaviour.

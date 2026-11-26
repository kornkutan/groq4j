package groq4j.services;

import groq4j.builders.SpeechRequestBuilder;
import groq4j.builders.TranscriptionRequestBuilder;
import groq4j.builders.TranslationRequestBuilder;
import groq4j.enums.HttpMethod;
import groq4j.exceptions.GroqSerializationException;
import groq4j.models.audio.SpeechRequest;
import groq4j.models.audio.TranscriptionRequest;
import groq4j.models.audio.TranscriptionResponse;
import groq4j.models.audio.TranscriptionSegment;
import groq4j.models.audio.TranscriptionWord;
import groq4j.models.audio.TranslationRequest;
import groq4j.models.audio.TranslationResponse;
import groq4j.models.common.XGroq;
import groq4j.utils.Constants;
import groq4j.utils.FileUtils;
import groq4j.utils.HttpUtils;
import groq4j.utils.JsonUtils;
import groq4j.utils.UrlUtils;
import groq4j.utils.ValidationUtils;

import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Transcription, translation and speech synthesis. */
public class AudioServiceImpl implements AudioService {
    private final HttpClient httpClient;
    private final String apiKey;

    public AudioServiceImpl(String apiKey) {
        this.httpClient = HttpUtils.createHttpClient();
        this.apiKey = apiKey;
        ValidationUtils.validateApiKey(apiKey);
    }

    public AudioServiceImpl(HttpClient httpClient, String apiKey) {
        ValidationUtils.requireNonNull(httpClient, "httpClient");
        this.httpClient = httpClient;
        this.apiKey = apiKey;
        ValidationUtils.validateApiKey(apiKey);
    }

    public static AudioService create(HttpClient httpClient, String apiKey) {
        return new AudioServiceImpl(httpClient, apiKey);
    }

    public static AudioService create(String apiKey) {
        return new AudioServiceImpl(apiKey);
    }

    @Override
    public TranscriptionResponse createTranscription(TranscriptionRequest request) {
        ValidationUtils.requireNonNull(request, "request");

        if (request.hasFile()) {
            var httpRequest = HttpUtils.createMultipartRequest(
                UrlUtils.buildAudioTranscriptionsUrl(Constants.BASE_URL), apiKey, transcriptionForm(request));
            return HttpUtils.await(HttpUtils.executeRequest(httpClient, httpRequest)
                    .thenApply(this::parseTranscriptionResponse));
        }

        var httpRequest = HttpUtils.createJsonRequest(
            UrlUtils.buildAudioTranscriptionsUrl(Constants.BASE_URL), apiKey, HttpMethod.POST,
            JsonUtils.toJsonString(transcriptionBody(request)));
        return HttpUtils.await(HttpUtils.executeRequest(httpClient, httpRequest)
                .thenApply(this::parseTranscriptionResponse));
    }

    @Override
    public TranslationResponse createTranslation(TranslationRequest request) {
        ValidationUtils.requireNonNull(request, "request");

        if (request.hasFile()) {
            var httpRequest = HttpUtils.createMultipartRequest(
                UrlUtils.buildAudioTranslationsUrl(Constants.BASE_URL), apiKey, translationForm(request));
            return HttpUtils.await(HttpUtils.executeRequest(httpClient, httpRequest)
                    .thenApply(this::parseTranslationResponse));
        }

        var httpRequest = HttpUtils.createJsonRequest(
            UrlUtils.buildAudioTranslationsUrl(Constants.BASE_URL), apiKey, HttpMethod.POST,
            JsonUtils.toJsonString(translationBody(request)));
        return HttpUtils.await(HttpUtils.executeRequest(httpClient, httpRequest)
                .thenApply(this::parseTranslationResponse));
    }

    @Override
    public byte[] createSpeech(SpeechRequest request) {
        ValidationUtils.requireNonNull(request, "request");

        var body = new LinkedHashMap<String, Object>();
        body.put("model", request.model());
        body.put("input", request.input());
        body.put("voice", request.voice());
        request.responseFormat().ifPresent(v -> body.put("response_format", v.getValue()));
        request.sampleRate().ifPresent(v -> body.put("sample_rate", v.getValue()));
        request.speed().ifPresent(v -> body.put("speed", v));

        var httpRequest = HttpUtils.createJsonRequest(
            UrlUtils.buildAudioSpeechUrl(Constants.BASE_URL), apiKey, HttpMethod.POST,
            JsonUtils.toJsonString(body));

        return HttpUtils.await(HttpUtils.executeRequestForBytes(httpClient, httpRequest));
    }

    @Override
    public TranscriptionResponse simpleTranscription(String model, byte[] audioData, String filename) {
        ValidationUtils.validateModel(model);
        ValidationUtils.requireNonNull(audioData, "audioData");
        ValidationUtils.validateAudioFileSize(audioData);
        if (filename != null && !filename.isEmpty()) {
            FileUtils.validateAudioFile(audioData, filename);
        }
        return createTranscription(TranscriptionRequestBuilder.withFile(model, audioData, filename).build());
    }

    @Override
    public TranslationResponse simpleTranslation(String model, byte[] audioData, String filename) {
        ValidationUtils.validateModel(model);
        ValidationUtils.requireNonNull(audioData, "audioData");
        ValidationUtils.validateAudioFileSize(audioData);
        if (filename != null && !filename.isEmpty()) {
            FileUtils.validateAudioFile(audioData, filename);
        }
        return createTranslation(TranslationRequestBuilder.withFile(model, audioData, filename).build());
    }

    @Override
    public byte[] simpleSpeech(String model, String text, String voice) {
        ValidationUtils.validateModel(model);
        ValidationUtils.requireNonNull(text, "text");
        ValidationUtils.requireNonNull(voice, "voice");

        return createSpeech(SpeechRequestBuilder.create(model, text, voice).build());
    }

    private Map<String, Object> transcriptionForm(TranscriptionRequest request) {
        var form = new LinkedHashMap<String, Object>();
        form.put("model", request.model());
        form.put("file", toFormFile(request.file().orElseThrow(), request.filename().orElse(null)));
        request.language().ifPresent(v -> form.put("language", v));
        request.prompt().ifPresent(v -> form.put("prompt", v));
        request.responseFormat().ifPresent(v -> form.put("response_format", v.getValue()));
        request.temperature().ifPresent(v -> form.put("temperature", v.toString()));
        request.timestampGranularities().ifPresent(v ->
            form.put("timestamp_granularities[]", v.stream().map(g -> g.getValue()).toList()));
        return form;
    }

    private Map<String, Object> transcriptionBody(TranscriptionRequest request) {
        var body = new LinkedHashMap<String, Object>();
        body.put("model", request.model());
        request.url().ifPresent(v -> body.put("url", v));
        request.language().ifPresent(v -> body.put("language", v));
        request.prompt().ifPresent(v -> body.put("prompt", v));
        request.responseFormat().ifPresent(v -> body.put("response_format", v.getValue()));
        request.temperature().ifPresent(v -> body.put("temperature", v));
        request.timestampGranularities().ifPresent(v ->
            body.put("timestamp_granularities", v.stream().map(g -> g.getValue()).toList()));
        return body;
    }

    private Map<String, Object> translationForm(TranslationRequest request) {
        var form = new LinkedHashMap<String, Object>();
        form.put("model", request.model());
        form.put("file", toFormFile(request.file().orElseThrow(), request.filename().orElse(null)));
        request.prompt().ifPresent(v -> form.put("prompt", v));
        request.responseFormat().ifPresent(v -> form.put("response_format", v.getValue()));
        request.temperature().ifPresent(v -> form.put("temperature", v.toString()));
        return form;
    }

    private Map<String, Object> translationBody(TranslationRequest request) {
        var body = new LinkedHashMap<String, Object>();
        body.put("model", request.model());
        request.url().ifPresent(v -> body.put("url", v));
        request.prompt().ifPresent(v -> body.put("prompt", v));
        request.responseFormat().ifPresent(v -> body.put("response_format", v.getValue()));
        request.temperature().ifPresent(v -> body.put("temperature", v));
        return body;
    }

    private HttpUtils.FormFile toFormFile(byte[] data, String filename) {
        String resolvedName = filename == null || filename.isEmpty() ? "audio.wav" : filename;
        return new HttpUtils.FormFile(data, resolvedName, FileUtils.guessMimeType(resolvedName));
    }

    TranscriptionResponse parseTranscriptionResponse(String json) {
        try {
            Map<String, Object> root = JsonUtils.parseObject(json);
            return new TranscriptionResponse(
                JsonUtils.stringValue(root.get("text")),
                parseXGroq(root.get("x_groq")),
                Optional.ofNullable(JsonUtils.stringValue(root.get("language"))),
                Optional.ofNullable(JsonUtils.doubleValue(root.get("duration"))),
                parseSegments(root.get("segments")),
                parseWords(root.get("words")));
        } catch (GroqSerializationException e) {
            throw e;
        } catch (Exception e) {
            throw new GroqSerializationException("Failed to parse transcription response: " + e.getMessage(), e);
        }
    }

    TranslationResponse parseTranslationResponse(String json) {
        try {
            Map<String, Object> root = JsonUtils.parseObject(json);
            return new TranslationResponse(
                JsonUtils.stringValue(root.get("text")),
                parseXGroq(root.get("x_groq")),
                Optional.ofNullable(JsonUtils.stringValue(root.get("language"))),
                Optional.ofNullable(JsonUtils.doubleValue(root.get("duration"))),
                parseSegments(root.get("segments")));
        } catch (GroqSerializationException e) {
            throw e;
        } catch (Exception e) {
            throw new GroqSerializationException("Failed to parse translation response: " + e.getMessage(), e);
        }
    }

    private List<TranscriptionSegment> parseSegments(Object rawSegments) {
        List<TranscriptionSegment> segments = new ArrayList<>();
        for (Object raw : JsonUtils.asList(rawSegments)) {
            Map<String, Object> segment = JsonUtils.asMap(raw);
            List<Integer> tokens = new ArrayList<>();
            for (Object token : JsonUtils.asList(segment.get("tokens"))) {
                Double value = JsonUtils.doubleValue(token);
                if (value != null) {
                    tokens.add(value.intValue());
                }
            }
            segments.add(new TranscriptionSegment(
                intValue(segment.get("id")),
                doubleValue(segment.get("start")),
                doubleValue(segment.get("end")),
                JsonUtils.stringValue(segment.get("text")),
                doubleValue(segment.get("avg_logprob")),
                doubleValue(segment.get("compression_ratio")),
                doubleValue(segment.get("no_speech_prob")),
                intValue(segment.get("seek")),
                doubleValue(segment.get("temperature")),
                tokens));
        }
        return segments;
    }

    private List<TranscriptionWord> parseWords(Object rawWords) {
        List<TranscriptionWord> words = new ArrayList<>();
        for (Object raw : JsonUtils.asList(rawWords)) {
            Map<String, Object> word = JsonUtils.asMap(raw);
            words.add(new TranscriptionWord(
                JsonUtils.stringValue(word.get("word")),
                doubleValue(word.get("start")),
                doubleValue(word.get("end"))));
        }
        return words;
    }

    private Optional<XGroq> parseXGroq(Object rawXGroq) {
        if (!(rawXGroq instanceof Map<?, ?>)) {
            return Optional.empty();
        }
        String id = JsonUtils.stringValue(JsonUtils.asMap(rawXGroq).get("id"));
        return id == null ? Optional.empty() : Optional.of(new XGroq(id));
    }

    private int intValue(Object value) {
        Double number = JsonUtils.doubleValue(value);
        return number == null ? 0 : number.intValue();
    }

    private double doubleValue(Object value) {
        Double number = JsonUtils.doubleValue(value);
        return number == null ? 0.0 : number;
    }
}

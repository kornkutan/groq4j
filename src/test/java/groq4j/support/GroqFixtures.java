package groq4j.support;

/** Canned Groq API bodies used by the fake server. Shapes follow the published OpenAPI spec. */
public final class GroqFixtures {

    private GroqFixtures() {
    }

    public static final String MODELS = """
        {"object":"list","data":[
          {"id":"openai/gpt-oss-20b","object":"model","created":1700000000,"owned_by":"OpenAI",
           "active":true,"context_window":131072,"max_completion_tokens":32768},
          {"id":"whisper-large-v3","object":"model","created":1700000000,"owned_by":"OpenAI",
           "active":true,"context_window":448,"max_completion_tokens":448}
        ]}""";

    public static String model(String id) {
        return """
            {"id":"%s","object":"model","created":1700000000,"owned_by":"Meta",
             "active":true,"context_window":131072,"max_completion_tokens":8192}""".formatted(id);
    }

    public static final String CHAT_COMPLETION = """
        {"id":"chatcmpl-fixture","object":"chat.completion","created":1700000000,
         "model":"openai/gpt-oss-20b","service_tier":"on_demand","system_fingerprint":"fp_1",
         "choices":[{"index":0,"finish_reason":"stop","logprobs":null,
           "message":{"role":"assistant","content":"pong","reasoning":"because tests"}}],
         "usage":{"queue_time":0.01,"prompt_tokens":12,"prompt_time":0.02,
           "completion_tokens":4,"completion_time":0.03,"total_tokens":16,"total_time":0.05,
           "prompt_tokens_details":{"cached_tokens":6},
           "completion_tokens_details":{"reasoning_tokens":2}},
         "x_groq":{"id":"req_fixture"}}""";

    public static final String TOOL_CALL_COMPLETION = """
        {"id":"chatcmpl-tool","object":"chat.completion","created":1700000000,
         "model":"openai/gpt-oss-20b",
         "choices":[{"index":0,"finish_reason":"tool_calls",
           "message":{"role":"assistant","content":null,"tool_calls":[
             {"id":"call_1","type":"function","function":{"name":"get_weather","arguments":"{\\"city\\":\\"Bangkok\\"}"}}]}}],
         "usage":{"prompt_tokens":10,"completion_tokens":5,"total_tokens":15}}""";

    public static String chatStream() {
        return """
            data: {"id":"chatcmpl-stream","object":"chat.completion.chunk","created":1700000000,"model":"openai/gpt-oss-20b","choices":[{"index":0,"delta":{"role":"assistant"},"finish_reason":null}]}

            data: {"id":"chatcmpl-stream","object":"chat.completion.chunk","created":1700000000,"model":"openai/gpt-oss-20b","choices":[{"index":0,"delta":{"content":"po"},"finish_reason":null},{"index":1,"delta":{"content":"pong"},"finish_reason":null}]}

            data: {"id":"chatcmpl-stream","object":"chat.completion.chunk","created":1700000000,"model":"openai/gpt-oss-20b","choices":[{"index":0,"delta":{},"finish_reason":"stop"}],"usage":{"prompt_tokens":3,"completion_tokens":2,"total_tokens":5},"x_groq":{"id":"req_stream"}}

            data: [DONE]

            """;
    }

    public static final String RESPONSE = """
        {"id":"resp_fixture","object":"response","created_at":1700000000,"status":"completed",
         "model":"openai/gpt-oss-20b","instructions":"be brief","metadata":{"suite":"unit"},
         "output":[
           {"type":"reasoning","id":"rs_1","status":"completed",
            "content":[{"type":"reasoning_text","text":"short answer"}]},
           {"type":"message","id":"msg_1","role":"assistant","status":"completed",
            "content":[{"type":"output_text","text":"pong","annotations":[]}]}],
         "usage":{"input_tokens":20,"output_tokens":4,"total_tokens":24,
           "input_tokens_details":{"cached_tokens":0},
           "output_tokens_details":{"reasoning_tokens":7}}}""";

    public static final String RESPONSE_WITH_TOOL_CALL = """
        {"id":"resp_tool","object":"response","created_at":1700000000,"status":"completed",
         "model":"openai/gpt-oss-20b","output":[
           {"type":"function_call","id":"fc_1","call_id":"call_9","name":"get_weather",
            "arguments":"{\\"city\\":\\"Bangkok\\"}"}],
         "usage":{"input_tokens":10,"output_tokens":5,"total_tokens":15}}""";

    public static String responsesStream() {
        return """
            data: {"type":"response.created","response":{"id":"resp_stream","status":"in_progress"}}

            data: {"type":"response.output_text.delta","item_id":"msg_1","delta":"po"}

            data: {"type":"response.output_text.delta","item_id":"msg_1","delta":"ng"}

            data: {"type":"response.completed","response":{"id":"resp_stream","status":"completed","usage":{"input_tokens":3,"output_tokens":2,"total_tokens":5}}}

            data: [DONE]

            """;
    }

    public static final String EMBEDDINGS = """
        {"object":"list","model":"nomic-embed-text-v1_5","data":[
          {"object":"embedding","index":0,"embedding":[0.11,0.22,0.33]},
          {"object":"embedding","index":1,"embedding":[0.44,0.55,0.66]}],
         "usage":{"prompt_tokens":6,"total_tokens":6}}""";

    public static final String RERANKING = """
        {"results":[{"index":0,"doc":"LPUs are deterministic chips.","score":0.91},
                    {"index":1,"doc":"Bread needs yeast.","score":0.03}]}""";

    public static final String TRANSCRIPTION = """
        {"text":"hello world","language":"en","duration":2.5,
         "segments":[
           {"id":0,"seek":0,"start":0.0,"end":1.2,"text":"hello","avg_logprob":-0.2,
            "compression_ratio":1.1,"no_speech_prob":0.01,"temperature":0.0,"tokens":[1,2]},
           {"id":1,"seek":0,"start":1.2,"end":2.5,"text":" world","avg_logprob":-0.3,
            "compression_ratio":1.2,"no_speech_prob":0.02,"temperature":0.0,"tokens":[3]}],
         "words":[{"word":"hello","start":0.0,"end":1.2},{"word":"world","start":1.2,"end":2.5}],
         "x_groq":{"id":"req_audio"}}""";

    public static final String TRANSLATION = """
        {"text":"hello world","language":"en","duration":2.5,
         "segments":[{"id":0,"seek":0,"start":0.0,"end":2.5,"text":"hello world",
           "avg_logprob":-0.2,"compression_ratio":1.1,"no_speech_prob":0.01,
           "temperature":0.0,"tokens":[1]}],
         "x_groq":{"id":"req_translation"}}""";

    /** Four bytes of a WAV header, enough to assert binary passthrough. */
    public static final String SPEECH_BYTES = "RIFF";

    public static final String FILE = """
        {"id":"file-1","object":"file","bytes":1024,"created_at":1700000000,
         "filename":"requests.jsonl","purpose":"batch"}""";

    public static final String FILE_LIST = """
        {"object":"list","data":[
          {"id":"file-1","object":"file","bytes":1024,"created_at":1700000000,
           "filename":"requests.jsonl","purpose":"batch"},
          {"id":"file-2","object":"file","bytes":64,"created_at":1700000100,
           "filename":"output.jsonl","purpose":"batch_output"}]}""";

    public static final String FILE_DELETE = """
        {"id":"file-1","object":"file","deleted":true}""";

    public static final String FILE_CONTENT = """
        {"id":"batch_req_1","custom_id":"1","response":{"status_code":200}}""";

    public static String batch(String status) {
        return """
            {"id":"batch_1","object":"batch","endpoint":"/v1/chat/completions",
             "input_file_id":"file-1","completion_window":"24h","status":"%s",
             "output_file_id":"file-2","error_file_id":null,"created_at":1700000000,
             "in_progress_at":1700000001,"expires_at":1700086400,"finalizing_at":null,
             "completed_at":null,"failed_at":null,"expired_at":null,"cancelling_at":null,
             "cancelled_at":null,"errors":null,
             "request_counts":{"total":2,"completed":1,"failed":0},
             "metadata":{"project":"tests"}}""".formatted(status);
    }

    public static final String BATCH = batch("in_progress");

    public static final String BATCH_LIST = """
        {"object":"list","data":[%s]}""".formatted(batch("completed"));
}

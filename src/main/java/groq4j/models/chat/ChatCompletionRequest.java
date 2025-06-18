package groq4j.models.chat;

import groq4j.enums.ReasoningEffort;
import groq4j.enums.ReasoningFormat;
import groq4j.enums.ServiceTier;
import groq4j.models.common.Message;
import groq4j.models.common.Tool;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record ChatCompletionRequest(
    List<Message> messages,
    String model,
    Optional<Double> frequencyPenalty,
    Optional<Map<String, Integer>> logitBias,
    Optional<Boolean> logprobs,
    Optional<Integer> maxCompletionTokens,
    Optional<Integer> n,
    Optional<Boolean> parallelToolCalls,
    Optional<Double> presencePenalty,
    Optional<ReasoningEffort> reasoningEffort,
    Optional<ReasoningFormat> reasoningFormat,
    Optional<JsonSchemaResponseFormat> responseFormat,
    Optional<SearchSettings> searchSettings,
    Optional<Integer> seed,
    Optional<ServiceTier> serviceTier,
    Optional<List<String>> stop,
    Optional<Double> temperature,
    Optional<ToolChoice> toolChoice,
    Optional<List<Tool>> tools,
    Optional<Integer> topLogprobs,
    Optional<Double> topP,
    Optional<String> user
) {
    public ChatCompletionRequest {
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("messages cannot be null or empty");
        }
        if (model == null || model.trim().isEmpty()) {
            throw new IllegalArgumentException("model cannot be null or empty");
        }
        
        // Validate optional numeric ranges
        frequencyPenalty.ifPresent(fp -> {
            if (fp < -2.0 || fp > 2.0) {
                throw new IllegalArgumentException("frequencyPenalty must be between -2.0 and 2.0");
            }
        });
        
        presencePenalty.ifPresent(pp -> {
            if (pp < -2.0 || pp > 2.0) {
                throw new IllegalArgumentException("presencePenalty must be between -2.0 and 2.0");
            }
        });
        
        temperature.ifPresent(t -> {
            if (t < 0.0 || t > 2.0) {
                throw new IllegalArgumentException("temperature must be between 0.0 and 2.0");
            }
        });
        
        topP.ifPresent(tp -> {
            if (tp < 0.0 || tp > 1.0) {
                throw new IllegalArgumentException("topP must be between 0.0 and 1.0");
            }
        });
        
        n.ifPresent(nValue -> {
            if (nValue != 1) {
                throw new IllegalArgumentException("n must be 1 (only supported value)");
            }
        });
        
        topLogprobs.ifPresent(tlp -> {
            if (tlp < 0 || tlp > 20) {
                throw new IllegalArgumentException("topLogprobs must be between 0 and 20");
            }
        });
        
        maxCompletionTokens.ifPresent(mct -> {
            if (mct < 1) {
                throw new IllegalArgumentException("maxCompletionTokens must be positive");
            }
        });
        
        stop.ifPresent(stopList -> {
            if (stopList.size() > 4) {
                throw new IllegalArgumentException("stop can contain at most 4 sequences");
            }
        });
    }
    
}
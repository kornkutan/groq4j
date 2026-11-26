package groq4j.models.chat;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record LogProbs(
    List<TokenLogProb> content,
    Optional<List<TokenLogProb>> refusal
) {
    public LogProbs {
        if (content == null) {
            throw new IllegalArgumentException("content cannot be null");
        }
    }
    
    public boolean hasRefusal() {
        return refusal.isPresent() && !refusal.get().isEmpty();
    }
    
    public int getContentTokenCount() {
        return content.size();
    }
    
    public int getRefusalTokenCount() {
        return refusal.map(List::size).orElse(0);
    }
    
    public Optional<TokenLogProb> getFirstContentToken() {
        return content.isEmpty() ? Optional.empty() : Optional.of(content.get(0));
    }
    
    public static LogProbs of(List<TokenLogProb> content) {
        return new LogProbs(content, Optional.empty());
    }
    
    public static LogProbs withRefusal(List<TokenLogProb> content, List<TokenLogProb> refusal) {
        return new LogProbs(content, Optional.of(refusal));
    }

    public static LogProbs fromMap(java.util.Map<String, Object> map) {
        return new LogProbs(
            parseTokens(map.get("content")),
            Optional.of(parseTokens(map.get("refusal"))));
    }

    private static List<TokenLogProb> parseTokens(Object raw) {
        List<TokenLogProb> tokens = new java.util.ArrayList<>();
        for (Object element : groq4j.utils.JsonUtils.asList(raw)) {
            java.util.Map<String, Object> token = groq4j.utils.JsonUtils.asMap(element);
            List<Integer> bytes = new java.util.ArrayList<>();
            for (Object b : groq4j.utils.JsonUtils.asList(token.get("bytes"))) {
                Double value = groq4j.utils.JsonUtils.doubleValue(b);
                if (value != null) {
                    bytes.add(value.intValue());
                }
            }
            List<TopLogProb> top = new java.util.ArrayList<>();
            for (Object element2 : groq4j.utils.JsonUtils.asList(token.get("top_logprobs"))) {
                java.util.Map<String, Object> topToken = groq4j.utils.JsonUtils.asMap(element2);
                Double logprob = groq4j.utils.JsonUtils.doubleValue(topToken.get("logprob"));
                top.add(TopLogProb.of(groq4j.utils.JsonUtils.stringValue(topToken.get("token")),
                    logprob == null ? 0.0 : logprob));
            }
            Double logprob = groq4j.utils.JsonUtils.doubleValue(token.get("logprob"));
            tokens.add(new TokenLogProb(
                groq4j.utils.JsonUtils.stringValue(token.get("token")),
                logprob == null ? 0.0 : logprob,
                bytes.isEmpty() ? Optional.empty() : Optional.of(bytes),
                top.isEmpty() ? Optional.empty() : Optional.of(top)));
        }
        return tokens;
    }
    
    public record TokenLogProb(
        String token,
        double logprob,
        Optional<List<Integer>> bytes,
        Optional<List<TopLogProb>> topLogprobs
    ) {
        public TokenLogProb {
            if (token == null) {
                throw new IllegalArgumentException("token cannot be null");
            }
        }
        
        public boolean hasBytes() {
            return bytes.isPresent() && !bytes.get().isEmpty();
        }
        
        public boolean hasTopLogProbs() {
            return topLogprobs.isPresent() && !topLogprobs.get().isEmpty();
        }
        
        public double getProbability() {
            return Math.exp(logprob);
        }
        
        public static TokenLogProb of(String token, double logprob) {
            return new TokenLogProb(token, logprob, Optional.empty(), Optional.empty());
        }
        
        public static TokenLogProb withBytes(String token, double logprob, List<Integer> bytes) {
            return new TokenLogProb(token, logprob, Optional.of(bytes), Optional.empty());
        }
        
        public static TokenLogProb withTopLogProbs(String token, double logprob, List<TopLogProb> topLogprobs) {
            return new TokenLogProb(token, logprob, Optional.empty(), Optional.of(topLogprobs));
        }
        
        public static TokenLogProb full(String token, double logprob, List<Integer> bytes, List<TopLogProb> topLogprobs) {
            return new TokenLogProb(token, logprob, Optional.of(bytes), Optional.of(topLogprobs));
        }
    }
    
    public record TopLogProb(
        String token,
        double logprob,
        Optional<List<Integer>> bytes
    ) {
        public TopLogProb {
            if (token == null) {
                throw new IllegalArgumentException("token cannot be null");
            }
        }
        
        public boolean hasBytes() {
            return bytes.isPresent() && !bytes.get().isEmpty();
        }
        
        public double getProbability() {
            return Math.exp(logprob);
        }
        
        public static TopLogProb of(String token, double logprob) {
            return new TopLogProb(token, logprob, Optional.empty());
        }
        
        public static TopLogProb withBytes(String token, double logprob, List<Integer> bytes) {
            return new TopLogProb(token, logprob, Optional.of(bytes));
        }
    }
}
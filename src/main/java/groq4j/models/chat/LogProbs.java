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
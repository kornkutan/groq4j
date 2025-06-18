package groq4j.models.chat;

import java.util.List;
import java.util.Optional;

public record SearchSettings(
    Optional<List<String>> includeDomains,
    Optional<List<String>> excludeDomains,
    Optional<Boolean> includeImages,
    Optional<Integer> maxResults,
    Optional<String> region,
    Optional<String> timeRange,
    Optional<Boolean> safeSearch
) {
    public SearchSettings {
        // Validate domain lists if present
        includeDomains.ifPresent(domains -> {
            if (domains.isEmpty()) {
                throw new IllegalArgumentException("includeDomains cannot be empty if present");
            }
            domains.forEach(domain -> {
                if (domain == null || domain.trim().isEmpty()) {
                    throw new IllegalArgumentException("domain cannot be null or empty");
                }
            });
        });
        
        excludeDomains.ifPresent(domains -> {
            if (domains.isEmpty()) {
                throw new IllegalArgumentException("excludeDomains cannot be empty if present");
            }
            domains.forEach(domain -> {
                if (domain == null || domain.trim().isEmpty()) {
                    throw new IllegalArgumentException("domain cannot be null or empty");
                }
            });
        });
        
        maxResults.ifPresent(max -> {
            if (max < 1 || max > 100) {
                throw new IllegalArgumentException("maxResults must be between 1 and 100");
            }
        });
        
        region.ifPresent(r -> {
            if (r.trim().isEmpty()) {
                throw new IllegalArgumentException("region cannot be empty if present");
            }
        });
        
        timeRange.ifPresent(tr -> {
            if (tr.trim().isEmpty()) {
                throw new IllegalArgumentException("timeRange cannot be empty if present");
            }
        });
    }
    
    public boolean hasIncludeDomains() {
        return includeDomains.isPresent() && !includeDomains.get().isEmpty();
    }
    
    public boolean hasExcludeDomains() {
        return excludeDomains.isPresent() && !excludeDomains.get().isEmpty();
    }
    
    public boolean hasFilters() {
        return hasIncludeDomains() || hasExcludeDomains() || hasIncludeImages() ||
               maxResults.isPresent() || region.isPresent() || 
               timeRange.isPresent() || safeSearch.isPresent();
    }
    
    public boolean hasIncludeImages() {
        return includeImages.isPresent();
    }
    
    public static SearchSettings empty() {
        return new SearchSettings(
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        );
    }
    
    public static SearchSettings includeDomains(String... domains) {
        return new SearchSettings(
            Optional.of(List.of(domains)),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        );
    }
    
    public static SearchSettings excludeDomains(String... domains) {
        return new SearchSettings(
            Optional.empty(),
            Optional.of(List.of(domains)),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        );
    }
    
    public static SearchSettings withImages(boolean includeImages) {
        return new SearchSettings(
            Optional.empty(),
            Optional.empty(),
            Optional.of(includeImages),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        );
    }
}
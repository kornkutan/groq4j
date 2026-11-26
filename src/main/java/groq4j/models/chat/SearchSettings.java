package groq4j.models.chat;

import java.util.List;
import java.util.Optional;

/**
 * Web search settings used by search enabled models. Only the fields Groq documents are exposed;
 * the older {@code region}, {@code time_range}, {@code safe_search} and {@code max_results}
 * parameters are gone from the API.
 */
public record SearchSettings(
    Optional<List<String>> includeDomains,
    Optional<List<String>> excludeDomains,
    Optional<Boolean> includeImages,
    Optional<String> country
) {
    public SearchSettings {
        includeDomains.ifPresent(domains -> validateDomains(domains, "includeDomains"));
        excludeDomains.ifPresent(domains -> validateDomains(domains, "excludeDomains"));
        country.ifPresent(value -> {
            if (value.trim().isEmpty()) {
                throw new IllegalArgumentException("country cannot be empty if present");
            }
        });
    }

    private static void validateDomains(List<String> domains, String field) {
        if (domains.isEmpty()) {
            throw new IllegalArgumentException(field + " cannot be empty if present");
        }
        domains.forEach(domain -> {
            if (domain == null || domain.trim().isEmpty()) {
                throw new IllegalArgumentException("domain cannot be null or empty");
            }
        });
    }

    public boolean hasIncludeDomains() {
        return includeDomains.isPresent() && !includeDomains.get().isEmpty();
    }

    public boolean hasExcludeDomains() {
        return excludeDomains.isPresent() && !excludeDomains.get().isEmpty();
    }

    public boolean hasIncludeImages() {
        return includeImages.isPresent();
    }

    public boolean hasFilters() {
        return hasIncludeDomains() || hasExcludeDomains() || hasIncludeImages() || country.isPresent();
    }

    public static SearchSettings empty() {
        return new SearchSettings(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static SearchSettings includeDomains(String... domains) {
        return new SearchSettings(Optional.of(List.of(domains)), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static SearchSettings excludeDomains(String... domains) {
        return new SearchSettings(Optional.empty(), Optional.of(List.of(domains)), Optional.empty(), Optional.empty());
    }

    public static SearchSettings withImages(boolean includeImages) {
        return new SearchSettings(Optional.empty(), Optional.empty(), Optional.of(includeImages), Optional.empty());
    }

    public SearchSettings withCountry(String country) {
        return new SearchSettings(includeDomains, excludeDomains, includeImages, Optional.of(country));
    }
}

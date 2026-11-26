package groq4j.builders;

import groq4j.models.chat.SearchSettings;

import java.util.List;
import java.util.Optional;

public class SearchSettingsBuilder {
    private List<String> includeDomains;
    private List<String> excludeDomains;
    private Boolean includeImages;
    private String country;

    public static SearchSettingsBuilder create() {
        return new SearchSettingsBuilder();
    }

    public SearchSettingsBuilder includeDomains(List<String> includeDomains) {
        this.includeDomains = includeDomains;
        return this;
    }

    public SearchSettingsBuilder includeDomains(String... domains) {
        this.includeDomains = List.of(domains);
        return this;
    }

    public SearchSettingsBuilder excludeDomains(List<String> excludeDomains) {
        this.excludeDomains = excludeDomains;
        return this;
    }

    public SearchSettingsBuilder excludeDomains(String... domains) {
        this.excludeDomains = List.of(domains);
        return this;
    }

    public SearchSettingsBuilder includeImages(boolean includeImages) {
        this.includeImages = includeImages;
        return this;
    }

    public SearchSettingsBuilder country(String country) {
        this.country = country;
        return this;
    }

    public SearchSettings build() {
        return new SearchSettings(
            Optional.ofNullable(includeDomains),
            Optional.ofNullable(excludeDomains),
            Optional.ofNullable(includeImages),
            Optional.ofNullable(country)
        );
    }
}

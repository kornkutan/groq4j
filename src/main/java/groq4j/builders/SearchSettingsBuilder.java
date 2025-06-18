package groq4j.builders;

import groq4j.models.chat.SearchSettings;

import java.util.List;
import java.util.Optional;

public class SearchSettingsBuilder {
    private List<String> includeDomains;
    private List<String> excludeDomains;
    private Boolean includeImages;
    private Integer maxResults;
    private String region;
    private String timeRange;
    private Boolean safeSearch;
    
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
    
    public SearchSettingsBuilder maxResults(int maxResults) {
        this.maxResults = maxResults;
        return this;
    }
    
    public SearchSettingsBuilder region(String region) {
        this.region = region;
        return this;
    }
    
    public SearchSettingsBuilder timeRange(String timeRange) {
        this.timeRange = timeRange;
        return this;
    }
    
    public SearchSettingsBuilder safeSearch(boolean safeSearch) {
        this.safeSearch = safeSearch;
        return this;
    }
    
    public SearchSettings build() {
        return new SearchSettings(
            Optional.ofNullable(includeDomains),
            Optional.ofNullable(excludeDomains),
            Optional.ofNullable(includeImages),
            Optional.ofNullable(maxResults),
            Optional.ofNullable(region),
            Optional.ofNullable(timeRange),
            Optional.ofNullable(safeSearch)
        );
    }
}
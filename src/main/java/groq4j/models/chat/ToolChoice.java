package groq4j.models.chat;

import groq4j.enums.ToolChoiceType;

import java.util.Optional;

public record ToolChoice(
    ToolChoiceType type,
    Optional<SpecificTool> function
) {
    public ToolChoice {
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }
        
        // If type is specific function, function must be present
        if (type == ToolChoiceType.FUNCTION && function.isEmpty()) {
            throw new IllegalArgumentException("function must be present when type is FUNCTION");
        }
        
        // If type is not specific function, function should not be present
        if (type != ToolChoiceType.FUNCTION && function.isPresent()) {
            throw new IllegalArgumentException("function should not be present when type is not FUNCTION");
        }
    }
    
    public boolean isNone() {
        return type == ToolChoiceType.NONE;
    }
    
    public boolean isAuto() {
        return type == ToolChoiceType.AUTO;
    }
    
    public boolean isRequired() {
        return type == ToolChoiceType.REQUIRED;
    }
    
    public boolean isSpecificFunction() {
        return type == ToolChoiceType.FUNCTION && function.isPresent();
    }
    
    public String getFunctionName() {
        return function.map(f -> f.name())
                      .orElseThrow(() -> new IllegalStateException("No function specified"));
    }
    
    public Optional<String> getFunctionNameSafe() {
        return function.map(SpecificTool::name);
    }
    
    // Static factory methods
    public static ToolChoice none() {
        return new ToolChoice(ToolChoiceType.NONE, Optional.empty());
    }
    
    public static ToolChoice auto() {
        return new ToolChoice(ToolChoiceType.AUTO, Optional.empty());
    }
    
    public static ToolChoice required() {
        return new ToolChoice(ToolChoiceType.REQUIRED, Optional.empty());
    }
    
    public static ToolChoice function(String functionName) {
        return new ToolChoice(
            ToolChoiceType.FUNCTION, 
            Optional.of(new SpecificTool("function", functionName))
        );
    }
    
    public record SpecificTool(
        String type,
        String name
    ) {
        public SpecificTool {
            if (type == null || type.trim().isEmpty()) {
                throw new IllegalArgumentException("type cannot be null or empty");
            }
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("name cannot be null or empty");
            }
        }
        
        public static SpecificTool function(String name) {
            return new SpecificTool("function", name);
        }
    }
}
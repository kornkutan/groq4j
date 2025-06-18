package groq4j.builders;

import groq4j.enums.ToolChoiceType;
import groq4j.models.chat.ToolChoice;

import java.util.Optional;

public class ToolChoiceBuilder {
    private ToolChoiceType type;
    private ToolChoice.SpecificTool function;
    
    public static ToolChoiceBuilder create() {
        return new ToolChoiceBuilder();
    }
    
    public static ToolChoiceBuilder none() {
        return new ToolChoiceBuilder().type(ToolChoiceType.NONE);
    }
    
    public static ToolChoiceBuilder auto() {
        return new ToolChoiceBuilder().type(ToolChoiceType.AUTO);
    }
    
    public static ToolChoiceBuilder required() {
        return new ToolChoiceBuilder().type(ToolChoiceType.REQUIRED);
    }
    
    public static ToolChoiceBuilder function(String functionName) {
        return new ToolChoiceBuilder()
            .type(ToolChoiceType.FUNCTION)
            .specificTool(ToolChoice.SpecificTool.function(functionName));
    }
    
    public ToolChoiceBuilder type(ToolChoiceType type) {
        this.type = type;
        return this;
    }
    
    public ToolChoiceBuilder specificTool(ToolChoice.SpecificTool tool) {
        this.function = tool;
        return this;
    }
    
    public ToolChoice build() {
        return new ToolChoice(type, Optional.ofNullable(function));
    }
}
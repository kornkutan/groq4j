package groq4j.models.chat;

import groq4j.enums.ResponseFormat;

import java.util.Map;
import java.util.Optional;

public record JsonSchemaResponseFormat(
    ResponseFormat type,
    Optional<JsonSchema> jsonSchema
) {
    public JsonSchemaResponseFormat {
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }
        
        // If type is JSON_SCHEMA, jsonSchema must be present
        if (type == ResponseFormat.JSON_SCHEMA && jsonSchema.isEmpty()) {
            throw new IllegalArgumentException("jsonSchema must be present when type is JSON_SCHEMA");
        }
        
        // If type is not JSON_SCHEMA, jsonSchema should not be present
        if (type != ResponseFormat.JSON_SCHEMA && jsonSchema.isPresent()) {
            throw new IllegalArgumentException("jsonSchema should not be present when type is not JSON_SCHEMA");
        }
    }
    
    public boolean hasJsonSchema() {
        return jsonSchema.isPresent();
    }
    
    public Map<String, Object> toMap() {
        var map = new java.util.HashMap<String, Object>();
        map.put("type", type.getValue());
        if (jsonSchema.isPresent()) {
            map.put("json_schema", jsonSchema.get().toMap());
        }
        return map;
    }
    
    // Factory methods
    public static JsonSchemaResponseFormat text() {
        return new JsonSchemaResponseFormat(ResponseFormat.TEXT, Optional.empty());
    }
    
    public static JsonSchemaResponseFormat jsonObject() {
        return new JsonSchemaResponseFormat(ResponseFormat.JSON_OBJECT, Optional.empty());
    }
    
    public static JsonSchemaResponseFormat jsonSchema(JsonSchema schema) {
        return new JsonSchemaResponseFormat(ResponseFormat.JSON_SCHEMA, Optional.of(schema));
    }
    
    public static JsonSchemaResponseFormat jsonSchema(String name, String description, Map<String, Object> schema) {
        return jsonSchema(JsonSchema.of(name, description, schema, false));
    }
    
    public static JsonSchemaResponseFormat strictJsonSchema(String name, String description, Map<String, Object> schema) {
        return jsonSchema(JsonSchema.of(name, description, schema, true));
    }
    
    public record JsonSchema(
        String name,
        Optional<String> description,
        Map<String, Object> schema,
        Optional<Boolean> strict
    ) {
        public JsonSchema {
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("name cannot be null or empty");
            }
            if (name.length() > 64) {
                throw new IllegalArgumentException("name cannot exceed 64 characters");
            }
            if (!name.matches("[a-zA-Z0-9_-]+")) {
                throw new IllegalArgumentException("name must contain only a-z, A-Z, 0-9, underscores and dashes");
            }
            if (schema == null || schema.isEmpty()) {
                throw new IllegalArgumentException("schema cannot be null or empty");
            }
        }
        
        public boolean hasDescription() {
            return description.isPresent() && !description.get().isEmpty();
        }
        
        public boolean isStrict() {
            return strict.orElse(false);
        }
        
        public Map<String, Object> toMap() {
            var map = new java.util.HashMap<String, Object>();
            map.put("name", name);
            description.ifPresent(d -> map.put("description", d));
            map.put("schema", schema);
            strict.ifPresent(s -> map.put("strict", s));
            return map;
        }
        
        public static JsonSchema of(String name, String description, Map<String, Object> schema, boolean strict) {
            return new JsonSchema(
                name,
                Optional.ofNullable(description),
                schema,
                Optional.of(strict)
            );
        }
        
        public static JsonSchema simple(String name, Map<String, Object> schema) {
            return new JsonSchema(
                name,
                Optional.empty(),
                schema,
                Optional.empty()
            );
        }
    }
}
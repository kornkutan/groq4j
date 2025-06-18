package groq4j.models.models;

import java.util.List;
import java.util.stream.Collectors;

public record ModelListResponse(
    String object,
    List<Model> data
) {
    public ModelListResponse {
        if (object == null || object.trim().isEmpty()) {
            throw new IllegalArgumentException("object cannot be null or empty");
        }
        if (data == null) {
            throw new IllegalArgumentException("data cannot be null");
        }
    }
    
    public int getModelCount() {
        return data.size();
    }
    
    public boolean isEmpty() {
        return data.isEmpty();
    }
    
    public List<Model> getActiveModels() {
        return data.stream()
                  .filter(Model::isActive)
                  .collect(Collectors.toList());
    }
    
    public List<Model> getChatModels() {
        return data.stream()
                  .filter(Model::isChatModel)
                  .filter(Model::isActive)
                  .collect(Collectors.toList());
    }
    
    public List<Model> getWhisperModels() {
        return data.stream()
                  .filter(Model::isWhisperModel)
                  .filter(Model::isActive)
                  .collect(Collectors.toList());
    }
    
    public List<Model> getTtsModels() {
        return data.stream()
                  .filter(Model::isTtsModel)
                  .filter(Model::isActive)
                  .collect(Collectors.toList());
    }
    
    public List<String> getModelIds() {
        return data.stream()
                  .map(Model::id)
                  .collect(Collectors.toList());
    }
    
    public boolean hasModel(String modelId) {
        return data.stream()
                  .anyMatch(model -> model.id().equals(modelId));
    }
    
    public Model findModel(String modelId) {
        return data.stream()
                  .filter(model -> model.id().equals(modelId))
                  .findFirst()
                  .orElseThrow(() -> new IllegalArgumentException("Model not found: " + modelId));
    }
    
    public static ModelListResponse of(List<Model> models) {
        return new ModelListResponse("list", models);
    }
}
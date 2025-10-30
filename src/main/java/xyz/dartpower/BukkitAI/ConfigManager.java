package xyz.dartpower.BukkitAI;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class ConfigManager {

    private static final Path CONFIG_PATH = Path.of("config.yaml");
    private final Yaml yaml = new Yaml();

    public boolean configExists() {
        return Files.exists(CONFIG_PATH);
    }

    public ConfigData loadConfig() {
        try {
            String content = Files.readString(CONFIG_PATH);
            Map<String, Object> data = yaml.load(content);

            if (data == null || !data.containsKey("openrouter")) {
                System.err.println("Ошибка: config.yaml имеет неверную структуру.");
                return null;
            }

            Map<String, String> openrouterSettings = (Map<String, String>) data.get("openrouter");
            String apiKey = openrouterSettings.get("api-key");
            String model = openrouterSettings.get("model");

            if (apiKey == null || model == null) {
                System.err.println("Ошибка: в config.yaml отсутствуют 'api-key' или 'model'.");
                return null;
            }

            return new ConfigData(apiKey, model);

        } catch (IOException e) {
            System.err.println("Ошибка при чтении config.yaml: " + e.getMessage());
            return null;
        } catch (ClassCastException e) {
            System.err.println("Ошибка: неверный тип данных в config.yaml.");
            return null;
        }
    }

    public void saveConfig(String apiKey, String model) throws IOException {
        ConfigData data = new ConfigData(apiKey, model);
        String yamlContent = yaml.dump(Map.of("openrouter", Map.of(
                "api-key", data.apiKey(),
                "model", data.model()
        )));
        Files.writeString(CONFIG_PATH, yamlContent);
    }

    public record ConfigData(String apiKey, String model) {}
}
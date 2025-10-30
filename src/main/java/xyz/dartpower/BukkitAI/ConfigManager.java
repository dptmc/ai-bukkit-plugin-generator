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

            if (data == null || !data.containsKey("active-provider")) {
                System.err.println("Ошибка: config.yaml имеет неверную структуру (отсутствует 'active-provider').");
                return null;
            }

            String activeProvider = (String) data.get("active-provider");
            Map<String, String> providerSettings = (Map<String, String>) data.get(activeProvider);

            if (providerSettings == null) {
                System.err.println("Ошибка: в config.yaml отсутствуют настройки для провайдера '" + activeProvider + "'.");
                return null;
            }

            String apiKey = providerSettings.get("api-key");
            String model = providerSettings.get("model");

            if (apiKey == null || model == null) {
                System.err.println("Ошибка: в настройках провайдера '" + activeProvider + "' отсутствуют 'api-key' или 'model'.");
                return null;
            }
            
            String baseUrl = null;
            if ("openai".equals(activeProvider)) {
                baseUrl = providerSettings.getOrDefault("base-url", "https://api.openai.com/v1/");
            }

            return new ConfigData(activeProvider, apiKey, model, baseUrl);

        } catch (IOException | ClassCastException e) {
            System.err.println("Ошибка при чтении или парсинге config.yaml: " + e.getMessage());
            return null;
        }
    }

    public void saveConfig(ConfigData config) throws IOException {
        Map<String, Object> data = Map.of(
            "active-provider", config.provider(),
            "openrouter", Map.of(
                "api-key", config.openrouterApiKey(),
                "model", config.openrouterModel()
            ),
            "openai", Map.of(
                "api-key", config.openaiApiKey(),
                "model", config.openaiModel(),
                "base-url", config.openaiBaseUrl()
            )
        );
        String yamlContent = yaml.dump(data);
        Files.writeString(CONFIG_PATH, yamlContent);
    }
    
    // Запись для хранения всех настроек
    public record ConfigData(
        String provider,
        String apiKey, 
        String model, 
        String baseUrl,
        
        // Поля для сохранения всех настроек, даже неактивных
        String openrouterApiKey,
        String openrouterModel,
        String openaiApiKey,
        String openaiModel,
        String openaiBaseUrl
    ) {}
}
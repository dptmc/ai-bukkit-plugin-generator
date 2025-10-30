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

            // Загружаем настройки для ОБЕИХ провайдеров, используя значения по умолчанию
            Map<String, Object> orSettings = (Map<String, Object>) data.getOrDefault("openrouter", Map.of());
            Map<String, Object> oaiSettings = (Map<String, Object>) data.getOrDefault("openai", Map.of());

            String orApiKey = (String) orSettings.getOrDefault("api-key", "");
            String orModel = (String) orSettings.getOrDefault("model", "");

            String oaiApiKey = (String) oaiSettings.getOrDefault("api-key", "");
            String oaiModel = (String) oaiSettings.getOrDefault("model", "");
            String oaiBaseUrl = (String) oaiSettings.getOrDefault("base-url", "https://api.openai.com/v1/");

            // Определяем настройки АКТИВНОГО провайдера
            String activeApiKey = "";
            String activeModel = "";
            String activeBaseUrl = null;

            if ("openrouter".equals(activeProvider)) {
                activeApiKey = orApiKey;
                activeModel = orModel;
            } else if ("openai".equals(activeProvider)) {
                activeApiKey = oaiApiKey;
                activeModel = oaiModel;
                activeBaseUrl = oaiBaseUrl;
            }

            // Создаем объект ConfigData со ВСЕМИ 9 параметрами
            return new ConfigData(
                    activeProvider, activeApiKey, activeModel, activeBaseUrl,
                    orApiKey, orModel,
                    oaiApiKey, oaiModel, oaiBaseUrl
            );

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
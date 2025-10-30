package xyz.dartpower.BukkitAI;

import java.io.IOException;
import java.util.Scanner;

public class PluginGenerator {

    public static void main(String[] args) {
		System.out.println("Default Charset: " + java.nio.charset.Charset.defaultCharset());
		System.out.println("File Encoding: " + System.getProperty("file.encoding"));

        Scanner scanner = new Scanner(System.in);
        ConfigManager configManager = new ConfigManager();

        System.out.println("=====================================");
        System.out.println("  Генератор плагинов для Bukkit");
        System.out.println("  (Поддержка OpenRouter & OpenAI)");
        System.out.println("=====================================");

        ConfigManager.ConfigData config = promptForConfiguration(scanner, configManager);

        System.out.println("\n--- Режим генерации ---");
        System.out.println("1. Описать плагин самому");
        System.out.println("2. Удиви меня (идея от ИИ)");
        System.out.print("Выберите режим (1 или 2): ");
        String modeChoice = scanner.nextLine().trim();

        String pluginName;
        String prompt;

        try {
            AiClient client = createClient(config);

            if (modeChoice.equals("2")) {
                // Для случайной идеи используем уникальный идентификатор
                String ideaId = "RandomIdea_" + System.currentTimeMillis();
                String aiGeneratedIdea = client.generateRandomPluginIdea(ideaId);
                
                System.out.println("\n-> ИИ сгенерировал идею: \"" + aiGeneratedIdea + "\"");
                System.out.print("Введите название для этого плагина (или оставьте пустым для случайного): ");
                String nameInput = scanner.nextLine().trim();
                
                if (nameInput.isEmpty()) {
                    pluginName = "CustomPlugin" + (int)(Math.random() * 1000);
                } else {
                    pluginName = nameInput;
                }
                
                prompt = aiGeneratedIdea;
                
            } else {
                System.out.print("\nВведите название плагина (например, MyAwesomePlugin): ");
                pluginName = scanner.nextLine();

                System.out.print("Опишите функционал плагина: ");
                prompt = scanner.nextLine();
            }
            scanner.close();

            System.out.println("\n-> Начинаю генерацию кода...");
            // Передаем pluginName в метод генерации кода
            String generatedCode = client.generatePluginCode(prompt, pluginName);
            ProjectCreator creator = new ProjectCreator(".", pluginName);
            creator.createProject(generatedCode);

        } catch (IOException | InterruptedException e) {
            System.err.println("\n❌ Произошла ошибка во время генерации.");
            System.err.println("   Проверьте папку 'docs' - там сохранен полный ответ от API, который поможет понять причину сбоя.");
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("\n❌ Произошла непредвиденная ошибка.");
            e.printStackTrace();
        }
    }

    // ... (остальные методы: promptForConfiguration, createClient, promptForApiKey, promptForModel) ...
    // Они остаются без изменений, просто скопируйте их сюда
    private static ConfigManager.ConfigData promptForConfiguration(Scanner scanner, ConfigManager configManager) {
        ConfigManager.ConfigData currentConfig = null;
        if (configManager.configExists()) {
            currentConfig = configManager.loadConfig();
        }

        boolean useSaved = false;
        if (currentConfig != null) {
            System.out.println("-> Найден файл config.yaml с активным провайдером: " + currentConfig.provider());
            System.out.print("-> Использовать сохраненные настройки? (y/n): ");
            String choice = scanner.nextLine().trim().toLowerCase();
            if (choice.equals("y") || choice.equals("yes")) {
                useSaved = true;
            }
        }

        if (useSaved) {
            return currentConfig;
        }

        System.out.println("\n--- Настройка провайдера ---");
        System.out.println("1. OpenRouter");
        System.out.println("2. OpenAI (или совместимый сервис, например LM Studio)");
        System.out.print("Выберите провайдера (1 или 2): ");
        String providerChoice = scanner.nextLine().trim();

        String provider, apiKey, model, baseUrl = null;
        String orApiKey = "", orModel = "", oaiApiKey = "", oaiModel = "", oaiBaseUrl = "https://api.openai.com/v1/";

        if (providerChoice.equals("1")) {
            provider = "openrouter";
            apiKey = promptForApiKey(scanner, "OpenRouter");
            model = promptForModel(scanner, "OpenRouter", "meta-llama/llama-3.1-8b-instruct:free");
            orApiKey = apiKey; orModel = model;
        } else if (providerChoice.equals("2")) {
            provider = "openai";
            apiKey = promptForApiKey(scanner, "OpenAI");
            model = promptForModel(scanner, "OpenAI", "gpt-3.5-turbo");
            System.out.print("Введите Base URL (оставьте пустым для стандартного https://api.openai.com/v1/): ");
            baseUrl = scanner.nextLine().trim();
            if (baseUrl.isEmpty()) {
                baseUrl = "https://api.openai.com/v1/";
            }
            oaiApiKey = apiKey; oaiModel = model; oaiBaseUrl = baseUrl;
        } else {
            System.err.println("Неверный выбор. Выход.");
            System.exit(1);
            return null;
        }
        
        if (currentConfig != null) {
            orApiKey = currentConfig.openrouterApiKey();
            orModel = currentConfig.openrouterModel();
            oaiApiKey = currentConfig.openaiApiKey();
            oaiModel = currentConfig.openaiModel();
            oaiBaseUrl = currentConfig.openaiBaseUrl();
        }
        
        ConfigManager.ConfigData newConfig = new ConfigManager.ConfigData(provider, apiKey, model, baseUrl, orApiKey, orModel, oaiApiKey, oaiModel, oaiBaseUrl);

        try {
            configManager.saveConfig(newConfig);
            System.out.println("-> Настройки сохранены в config.yaml.");
        } catch (IOException e) {
            System.err.println("   [Предупреждение] Не удалось сохранить config.yaml: " + e.getMessage());
        }
        
        return newConfig;
    }
    
    private static AiClient createClient(ConfigManager.ConfigData config) {
        if ("openrouter".equals(config.provider())) {
            return new OpenRouterClient(config.apiKey(), config.model());
        } else if ("openai".equals(config.provider())) {
            return new OpenAiClient(config.apiKey(), config.model(), config.baseUrl());
        }
        throw new IllegalArgumentException("Неизвестный провайдер: " + config.provider());
    }

    private static String promptForApiKey(Scanner scanner, String providerName) {
        System.out.print("Введите ваш API ключ от " + providerName + ": ");
        return scanner.nextLine();
    }

    private static String promptForModel(Scanner scanner, String providerName, String defaultModel) {
        System.out.print("Введите имя модели для " + providerName + " (по умолчанию: " + defaultModel + "): ");
        String model = scanner.nextLine().trim();
        return model.isEmpty() ? defaultModel : model;
    }
}
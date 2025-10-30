package xyz.dartpower.BukkitAI;

import java.io.IOException;
import java.util.Scanner;

public class PluginGenerator {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        ConfigManager configManager = new ConfigManager();

        System.out.println("=====================================");
        System.out.println("  Генератор плагинов для Bukkit");
        System.out.println("  на базе OpenRouter API");
        System.out.println("=====================================");

        String apiKey;
        String model;

        // --- Логика работы с конфигурацией ---
        if (configManager.configExists()) {
            System.out.println("-> Найден файл config.yaml. Попытка загрузить настройки...");
            ConfigManager.ConfigData configData = configManager.loadConfig();

            if (configData != null) {
                System.out.println("   Настройки успешно загружены.");
                System.out.print("-> Использовать сохраненные настройки? (y/n): ");
                String choice = scanner.nextLine().trim().toLowerCase();
                if (choice.equals("y") || choice.equals("yes")) {
                    apiKey = configData.apiKey();
                    model = configData.model();
                } else {
                    apiKey = promptForApiKey(scanner);
                    model = promptForModel(scanner);
                }
            } else {
                System.out.println("-> Не удалось загрузить настройки. Будет запрошен ввод заново.");
                apiKey = promptForApiKey(scanner);
                model = promptForModel(scanner);
            }
        } else {
            System.out.println("-> Файл config.yaml не найден. Создадим его после ввода данных.");
            apiKey = promptForApiKey(scanner);
            model = promptForModel(scanner);
        }

        // Сохраняем введенные настройки для будущих запусков
        try {
            configManager.saveConfig(apiKey, model);
            System.out.println("-> Настройки сохранены в config.yaml.");
        } catch (IOException e) {
            System.err.println("   [Предупреждение] Не удалось сохранить config.yaml: " + e.getMessage());
        }
        // --- Конец логики работы с конфигурацией ---


        System.out.print("Введите название плагина (например, MyAwesomePlugin): ");
        String pluginName = scanner.nextLine();

        System.out.print("Опишите функционал плагина (например, 'Команда /heal, которая лечит игрока'): ");
        String prompt = scanner.nextLine();
        scanner.close();

        try {
            // Передаем apiKey и model в клиент
            OpenRouterClient client = new OpenRouterClient(apiKey, model);
            ProjectCreator creator = new ProjectCreator(".", pluginName);

            String generatedCode = client.generatePluginCode(prompt);
            creator.createProject(generatedCode);

        } catch (IOException | InterruptedException e) {
            System.err.println("\n❌ Произошла ошибка во время генерации проекта.");
            System.err.println("   Убедитесь, что API ключ верен и у вас есть доступ в интернет.");
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("\n❌ Произошла непредвиденная ошибка.");
            e.printStackTrace();
        }
    }

    private static String promptForApiKey(Scanner scanner) {
        System.out.print("Введите ваш API ключ от OpenRouter: ");
        return scanner.nextLine();
    }

    private static String promptForModel(Scanner scanner) {
        System.out.print("Введите имя модели (например, meta-llama/llama-3.1-8b-instruct:free): ");
        return scanner.nextLine();
    }
}
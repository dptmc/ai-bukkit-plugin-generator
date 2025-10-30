package xyz.dartpower.BukkitAI;

import java.io.IOException;
import java.util.Scanner;

public class PluginGenerator {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=====================================");
        System.out.println("  Генератор плагинов для Bukkit");
        System.out.println("  на базе OpenRouter API");
        System.out.println("=====================================");

        System.out.print("Введите ваш API ключ от OpenRouter: ");
        String apiKey = scanner.nextLine();

        if (apiKey.isEmpty() || apiKey.equals("YOUR_API_KEY")) {
            System.err.println("Ошибка: API ключ не был предоставлен.");
            return;
        }

        System.out.print("Введите название плагина (например, MyAwesomePlugin): ");
        String pluginName = scanner.nextLine();

        System.out.print("Опишите функционал плагина (например, 'Команда /heal, которая лечит игрока'): ");
        String prompt = scanner.nextLine();
        scanner.close();

        try {
            OpenRouterClient client = new OpenRouterClient(apiKey);
            ProjectCreator creator = new ProjectCreator(".", pluginName); // "." - текущая директория

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
}
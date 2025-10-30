package xyz.dartpower.BukkitAI;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Утилита для логирования запросов и ответов ИИ в файлы.
 */
public class LoggerUtil {

    private static final Path LOG_DIR = Paths.get("docs");
    private static final DateTimeFormatter DT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    // Статический блок для создания папки 'docs' при первом использовании класса
    static {
        try {
            if (!Files.exists(LOG_DIR)) {
                Files.createDirectories(LOG_DIR);
            }
        } catch (IOException e) {
            System.err.println("Не удалось создать папку для логов 'docs': " + e.getMessage());
        }
    }

    /**
     * Сохраняет промпт и сырой ответ от ИИ в лог-файл.
     * @param pluginName Название плагина или идентификатор запроса.
     * @param prompt Промпт, отправленный ИИ.
     * @param rawResponse Сырой ответ (тело) от API.
     */
    public static void log(String pluginName, String prompt, String rawResponse) {
        String timestamp = LocalDateTime.now().format(DT_FORMAT);
        // Очищаем имя файла от недопустимых символов
        String fileName = String.format("%s_%s.log", sanitizeFileName(pluginName), timestamp);
        Path logFile = LOG_DIR.resolve(fileName);

        String logContent = String.format(
                "Plugin Name: %s\nTimestamp: %s\n\n--- PROMPT ---\n%s\n\n--- RAW AI RESPONSE ---\n%s\n",
                pluginName,
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                prompt,
                rawResponse
        );

        try {
            Files.writeString(logFile, logContent);
            System.out.println("   [Лог] Взаимодействие сохранено в файл: " + logFile);
        } catch (IOException e) {
            System.err.println("   [Ошибка] Не удалось записать лог в файл: " + e.getMessage());
        }
    }

    private static String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9.-]", "_");
    }
}
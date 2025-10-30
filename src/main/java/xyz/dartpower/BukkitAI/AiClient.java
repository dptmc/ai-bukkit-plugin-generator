package xyz.dartpower.BukkitAI;

import java.io.IOException;

/**
 * Общий интерфейс для клиентов AI-провайдеров.
 */
public interface AiClient {
    /**
     * Генерирует код плагина на основе промпта.
     * @param userPrompt Описание функционала плагина.
     * @return Сгенерированный код в виде строки.
     * @throws IOException В случае ошибки сети или API.
     * @throws InterruptedException В случае прерывания запроса.
     */
    String generatePluginCode(String userPrompt, String pluginName) throws IOException, InterruptedException;

    /**
     * Генерирует случайную идею для плагина.
     * @return Строка с описанием идеи для плагина.
     * @throws IOException В случае ошибки сети или API.
     * @throws InterruptedException В случае прерывания запроса.
     */
    String generateRandomPluginIdea(String pluginName) throws IOException, InterruptedException;
}
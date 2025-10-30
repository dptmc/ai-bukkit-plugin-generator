package xyz.dartpower.BukkitAI;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class OpenRouterClient {

    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";
    private final String apiKey;
    private final String model; // Добавляем поле для модели
    private final Gson gson = new Gson();
    private final HttpClient httpClient;

    // Обновляем конструктор
    public OpenRouterClient(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    public String generatePluginCode(String userPrompt) throws IOException, InterruptedException {
        String systemPrompt = """
                You are an expert Bukkit/Spigot plugin developer. Your task is to generate a complete, compilable, and well-structured Bukkit plugin based on the user's request.
                
                Follow these instructions strictly:
                1.  The main class MUST be named `Main.java`.
                2.  The package for the main class MUST be `com.example.PLUGIN_NAME`, where PLUGIN_NAME is the plugin name in lowercase.
                3.  Provide the code for `pom.xml`, `plugin.yml`, and all Java classes in separate, clearly marked code blocks.
                4.  Use the format:
                    ```xml
                    // pom.xml content here
                    ```
                    ```yaml
                    // plugin.yml content here
                    ```
                    ```java
                    // Java class content here
                    ```
                5.  The `pom.xml` must use the Spigot API dependency, version 1.20.1, and include the maven-shade-plugin for a fat jar.
                6.  The `plugin.yml` must have `name`, `version`, `main`, and `api-version: 1.19` keys.
                7.  Do not include any explanations or text outside of the code blocks. Only provide the raw code.
                """;

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", this.model); // Используем модель из поля

        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", systemPrompt);

        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", "Generate a Bukkit plugin with the following functionality: " + userPrompt);

        requestBody.add("messages", gson.toJsonTree(new Object[]{systemMessage, userMessage}));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("HTTP-Referer", "https://github.com/your-repo")
                .header("X-Title", "Bukkit Plugin Generator")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        System.out.println("-> Отправка запроса к OpenRouter (модель: " + this.model + ")...");
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("OpenRouter API вернул ошибку: " + response.statusCode() + " " + response.body());
        }

        JsonObject responseBody = gson.fromJson(response.body(), JsonObject.class);
        return responseBody.getAsJsonArray("choices")
                .get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString();
    }
}
package xyz.dartpower.BukkitAI;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class OpenRouterClient implements AiClient {

    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";
    private final String apiKey;
    private final String model;
    private final Gson gson = new Gson();
    private final HttpClient httpClient;

    public OpenRouterClient(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }
	
    @Override
    public String generatePluginCode(String userPrompt, String pluginName) throws IOException, InterruptedException {
		String systemPrompt = """
		You are an expert Bukkit/Spigot plugin developer and a system analyst. Your task is to understand the user's intent, even if described in non-technical terms, and then write a COMPLETE, FULLY-FUNCTIONAL, and COMPILABLE Bukkit plugin.

		CRITICAL RULES - FOLLOW THEM STRICTLY:

		1.  **INTENT ANALYSIS:**
			-   Carefully analyze the user's request to understand the core functionality they want.
			-   Interpret the request creatively but accurately. If the user says "I want players to get a gift every day", you should implement a daily reward system with a GUI or a command. If they say "make mobs explode when they die", implement that exact event.
			-   The language of the user's prompt determines the language for ALL in-game text (messages, descriptions, etc.). Code syntax remains in English.

		2.  **NO PLACEHOLDERS OR STUBS:** You are FORBIDDEN from using placeholders, comments like "// TODO", "// implement logic", or any other form of incomplete code. Every method, event listener, and command executor must be fully implemented.

		3.  **COMPLETE IMPLEMENTATION:** If the user asks for a command, you MUST implement the entire logic inside the `onCommand` method, including argument checks, sender type checks, permission checks, and providing feedback to the player. If they ask for an event, you MUST write the full logic inside the event handler method.

		4.  **STRUCTURE:** The main class MUST be named `Main.java`. The package MUST be `com.example.PLUGIN_NAME`, where PLUGIN_NAME is the plugin name in lowercase.

		5.  **OUTPUT FORMAT:** Provide the code for `pom.xml`, `plugin.yml`, and all Java classes in separate, clearly marked code blocks.
			```xml
			// pom.xml content here
			```
			```yaml
			// plugin.yml content here
			```
			```java
			// Java class content here
			```

		6.  **STANDARD FILES:**
			-   The `pom.xml` must use the Spigot API dependency (version 1.20.1) and include the maven-shade-plugin.
			-   The `plugin.yml` must have `name`, `version`, `main`, `api-version: 1.19`, and the correct `commands` section if commands are used.

		7.  **NO EXPLANATIONS:** Do not include any explanations or text outside of the code blocks. Only provide the raw, complete code.

		Before outputting, double-check that your code has NO placeholders and FULLY implements the user's interpreted request.
		""";

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", this.model);

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

        String rawBody = response.body();
        // Логируем ВЕСЬ ответ, до любой обработки
        LoggerUtil.log(pluginName, userPrompt, rawBody);

        if (response.statusCode() != 200) {
            throw new IOException("OpenRouter API вернул ошибку: " + response.statusCode() + " " + rawBody);
        }

        JsonObject responseBody = gson.fromJson(rawBody, JsonObject.class);

        // --- ИСПРАВЛЕНИЕ ОШИБКИ ---
        // Проверяем, что JSON не null и содержит нужное поле
        if (responseBody == null || !responseBody.has("choices")) {
            throw new IOException("OpenRouter API вернул некорректный JSON-ответ. Тело ответа: " + rawBody);
        }

        return responseBody.getAsJsonArray("choices")
                .get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString();
    }
	
	@Override
	public String generateRandomPluginIdea(String pluginName) throws IOException, InterruptedException {
		String ideaPrompt = "Generate a short, creative, and interesting idea for a new Minecraft Bukkit plugin. The idea should be suitable for implementation. Respond with only the idea itself, no extra text.";

		JsonObject requestBody = new JsonObject();
		requestBody.addProperty("model", this.model);

		JsonObject userMessage = new JsonObject();
		userMessage.addProperty("role", "user");
		userMessage.addProperty("content", ideaPrompt);

		requestBody.add("messages", gson.toJsonTree(new Object[]{userMessage}));

		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("https://openrouter.ai/api/v1/chat/completions"))
				.header("Authorization", "Bearer " + apiKey)
				.header("Content-Type", "application/json")
				.header("HTTP-Referer", "https://github.com/your-repo")
				.header("X-Title", "Bukkit Plugin Generator")
				.POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
				.build();

		System.out.println("-> Запрос случайной идеи у OpenRouter...");
		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

		String rawBody = response.body();
		LoggerUtil.log(pluginName, "Generate a random plugin idea", rawBody);

		if (response.statusCode() != 200) {
			throw new IOException("OpenRouter API вернул ошибку при запросе идеи: " + response.statusCode() + " " + rawBody);
		}

		JsonObject responseBody = gson.fromJson(rawBody, JsonObject.class);

		// --- ИСПРАВЛЕНИЕ ОШИБКИ ---
		if (responseBody == null || !responseBody.has("choices")) {
			throw new IOException("OpenRouter API вернул некорректный JSON-ответ при запросе идеи. Тело ответа: " + rawBody);
		}

		return responseBody.getAsJsonArray("choices")
				.get(0).getAsJsonObject()
				.getAsJsonObject("message")
				.get("content").getAsString().trim();
	}
}
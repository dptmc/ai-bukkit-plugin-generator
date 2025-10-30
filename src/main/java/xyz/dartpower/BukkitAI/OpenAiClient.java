package xyz.dartpower.BukkitAI;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class OpenAiClient implements AiClient {

    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final Gson gson = new Gson();
    private final HttpClient httpClient;

    public OpenAiClient(String apiKey, String model, String baseUrl) {
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Override
    public String generatePluginCode(String userPrompt, String pluginName) throws IOException, InterruptedException {
        // Системный промпт можно оставить тем же, он универсален
        String systemPrompt = """
        You are an expert Bukkit/Spigot plugin developer and a system analyst. Your task is to understand the user's intent, even if described in non-technical terms, and then write a COMPLETE, FULLY-FUNCTIONAL, and COMPILABLE Bukkit plugin.

        CRITICAL RULES - FOLLOW THEM STRICTLY:

        1.  **INTENT ANALYSIS:**
            -   Carefully analyze the user's request to understand the core functionality they want.
            -   Interpret the request creatively but accurately. If the user says "I want players to get a gift every day", you should implement a daily reward system with a GUI or a command. If they say "make mobs explode when they die", implement that exact event.
            -   language of the user's prompt determines the language for ALL in-game text (messages, descriptions, etc.). Code syntax remains in English.

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

        String endpoint = baseUrl.endsWith("/") ? baseUrl + "chat/completions" : baseUrl + "/chat/completions";

		// Увеличьте таймаут и добавьте отладочную информацию
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(endpoint))
				.header("Authorization", "Bearer " + apiKey)
				.header("Content-Type", "application/json")
				.timeout(Duration.ofSeconds(120)) // Увеличили с 60 до 120 секунд
				.POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
				.build();

		System.out.println("-> Отправка запроса к OpenAI (модель: " + this.model + ", URL: " + baseUrl + ")...");
		System.out.println("-> Тело запроса: " + requestBody.toString()); // Добавьте эту строку для отладки

		try {
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			System.out.println("-> Код ответа: " + response.statusCode()); // Добавьте эту строку
			System.out.println("-> Заголовки ответа: " + response.headers()); // И эту
			
			String rawBody = response.body();
			System.out.println("-> Сырой ответ (первые 200 символов): " + 
							  (rawBody.length() > 200 ? rawBody.substring(0, 200) + "..." : rawBody)); // И эту

            LoggerUtil.log(pluginName, userPrompt, rawBody);

            if (response.statusCode() != 200) {
                throw new IOException("OpenAI API вернул ошибку: " + response.statusCode() + " " + rawBody);
            }

            JsonObject responseBody = gson.fromJson(rawBody, JsonObject.class);

            // --- ИСПРАВЛЕНИЕ ОШИБКИ ---
            if (responseBody == null || !responseBody.has("choices")) {
                throw new IOException("OpenAI API вернул некорректный JSON-ответ. Тело ответа: " + rawBody);
            }

            return responseBody.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();
        } catch (java.net.http.HttpTimeoutException e) {
            // Добавляем обработку специфического исключения таймаута
            throw new IOException("Сервер не ответил в течение 60 секунд. LM Studio可能 быть перегружен, не отвечает на запросы или выключен. Проверьте его состояние.", e);
        }
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

		String endpoint = baseUrl.endsWith("/") ? baseUrl + "chat/completions" : baseUrl + "/chat/completions";

		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(endpoint))
				.header("Authorization", "Bearer " + apiKey)
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
				.build();

		System.out.println("-> Запрос случайной идеи у OpenAI...");
		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

		String rawBody = response.body();
		LoggerUtil.log(pluginName, "Generate a random plugin idea", rawBody);

		if (response.statusCode() != 200) {
			throw new IOException("OpenAI API вернул ошибку при запросе идеи: " + response.statusCode() + " " + rawBody);
		}

		if (rawBody == null || rawBody.trim().isEmpty()) {
			throw new IOException("OpenAI API вернул пустой ответ. Это может быть связано со сложностью запроса или сбоем модели. Попробуйте упростить промпт или сменить модель.");
		}

		JsonObject responseBody = gson.fromJson(rawBody, JsonObject.class);

		// --- ИСПРАВЛЕНИЕ ОШИБКИ ---
		if (responseBody == null || !responseBody.has("choices")) {
			throw new IOException("OpenAI API вернул некорректный JSON-ответ при запросе идеи. Тело ответа: " + rawBody);
		}

		return responseBody.getAsJsonArray("choices")
				.get(0).getAsJsonObject()
				.getAsJsonObject("message")
				.get("content").getAsString().trim();
	}
}
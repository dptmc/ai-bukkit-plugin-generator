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
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Override
    public String generatePluginCode(String userPrompt, String pluginName) throws IOException, InterruptedException {
        // Системный промпт можно оставить тем же, он универсален
        String systemPrompt = """
		You are an expert Bukkit/Spigot plugin developer. Generate a complete, functional Bukkit plugin based on the user's request.
		Provide the code for pom.xml, plugin.yml, and all Java classes in separate code blocks.
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
				.header("Content-Type", "application/json; charset=UTF-8")  // Добавляем кодировку
				.timeout(Duration.ofSeconds(60))
				.POST(HttpRequest.BodyPublishers.ofString(requestBody.toString(), java.nio.charset.StandardCharsets.UTF_8))  // Указываем кодировку
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
				.header("Content-Type", "application/json; charset=UTF-8")  // Добавляем кодировку
				.POST(HttpRequest.BodyPublishers.ofString(requestBody.toString(), java.nio.charset.StandardCharsets.UTF_8))  // Указываем кодировку
				.build();

		System.out.println("-> Запрос случайной идеи у OpenAI...");
		System.out.println("-> URL: " + endpoint);
		System.out.println("-> Модель: " + this.model);

		try {
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			
			System.out.println("-> Код ответа: " + response.statusCode());
			System.out.println("-> Заголовки ответа: " + response.headers());
			
			String rawBody = response.body();
			System.out.println("-> Длина ответа: " + (rawBody != null ? rawBody.length() : 0));
			
			if (rawBody != null && rawBody.length() > 0) {
				System.out.println("-> Сырой ответ (первые 200 символов): " + 
								  (rawBody.length() > 200 ? rawBody.substring(0, 200) + "..." : rawBody));
			}

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
		} catch (Exception e) {
			System.err.println("-> Ошибка при отправке запроса: " + e.getMessage());
			throw e;
		}
	}
}
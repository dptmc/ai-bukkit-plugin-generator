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
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }
    
	@Override
	public String generatePluginCode(String userPrompt, String pluginName) throws IOException, InterruptedException {
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
		
		String requestPayload = requestBody.toString();
		System.out.println("\n--- DEBUG: Request Payload ---");
		System.out.println(requestPayload);
		System.out.println("----------------------------\n");

		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("https://openrouter.ai/api/v1/chat/completions"))
				.header("Authorization", "Bearer " + apiKey)
				.header("Content-Type", "application/json; charset=UTF-8")  // Добавляем явное указание кодировки
				.header("HTTP-Referer", "https://github.com/your-repo")
				.header("X-Title", "Bukkit Plugin Generator")
				.timeout(Duration.ofSeconds(120))
				.POST(HttpRequest.BodyPublishers.ofString(requestPayload, java.nio.charset.StandardCharsets.UTF_8))  // Указываем кодировку при отправке
				.build();

		System.out.println("-> Отправка запроса к OpenRouter (модель: " + this.model + ")...");
		try {
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			
			System.out.println("-> Код ответа: " + response.statusCode());
			System.out.println("-> Заголовки ответа: " + response.headers());
			
			String rawBody = response.body();
			System.out.println("-> Длина ответа: " + rawBody.length());
			System.out.println("-> Сырой ответ (первые 200 символов): " + 
							  (rawBody.length() > 200 ? rawBody.substring(0, 200) + "..." : rawBody));
			
			// Сохраняем лог до всех проверок
			LoggerUtil.log(pluginName, userPrompt, rawBody);
			
			// Проверяем код ответа
			if (response.statusCode() != 200) {
				throw new IOException("OpenRouter API вернул ошибку: " + response.statusCode() + " " + rawBody);
			}

			// Проверяем, не пустой ли ответ
			if (rawBody == null || rawBody.trim().isEmpty()) {
				throw new IOException("OpenRouter API вернул пустой ответ. Это может быть связано со сложностью запроса или сбоем модели.");
			}
			
			// Проверяем, является ли ответ валидным JSON
			JsonObject responseBody;
			try {
				responseBody = gson.fromJson(rawBody, JsonObject.class);
			} catch (Exception e) {
				System.err.println("Ошибка парсинга JSON: " + e.getMessage());
				System.err.println("Ответ от сервера: " + rawBody);
				throw new IOException("OpenRouter API вернул невалидный JSON. " + e.getMessage(), e);
			}

			// Проверяем наличие поля choices
			if (responseBody == null || !responseBody.has("choices")) {
				throw new IOException("OpenRouter API вернул некорректный JSON-ответ. Тело ответа: " + rawBody);
			}

			return responseBody.getAsJsonArray("choices")
					.get(0).getAsJsonObject()
					.getAsJsonObject("message")
					.get("content").getAsString();
		} catch (java.net.http.HttpTimeoutException e) {
			throw new IOException("Сервер OpenRouter не ответил в течение 120 секунд. Возможно, проблемы с сетью или API перегружен.", e);
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

		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("https://openrouter.ai/api/v1/chat/completions"))
				.header("Authorization", "Bearer " + apiKey)
				.header("Content-Type", "application/json; charset=UTF-8")  // Добавляем явное указание кодировки
				.header("HTTP-Referer", "https://github.com/your-repo")
				.header("X-Title", "Bukkit Plugin Generator")
				.POST(HttpRequest.BodyPublishers.ofString(requestBody.toString(), java.nio.charset.StandardCharsets.UTF_8))  // Указываем кодировку
				.build();

		System.out.println("-> Запрос случайной идеи у OpenRouter...");
		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

		String rawBody = response.body();
		LoggerUtil.log(pluginName, "Generate a random plugin idea", rawBody);

		if (response.statusCode() != 200) {
			throw new IOException("OpenRouter API вернул ошибку при запросе идеи: " + response.statusCode() + " " + rawBody);
		}

		JsonObject responseBody = gson.fromJson(rawBody, JsonObject.class);

		if (responseBody == null || !responseBody.has("choices")) {
			throw new IOException("OpenRouter API вернул некорректный JSON-ответ при запросе идеи. Тело ответа: " + rawBody);
		}

		return responseBody.getAsJsonArray("choices")
				.get(0).getAsJsonObject()
				.getAsJsonObject("message")
				.get("content").getAsString().trim();
	}
}
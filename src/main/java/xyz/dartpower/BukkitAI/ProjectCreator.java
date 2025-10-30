package xyz.dartpower.BukkitAI;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProjectCreator {

    private final String pluginName;
    private final String pluginNameLower;
    private final Path projectDir;
    private final Path srcMainJavaDir;
    private final Path srcMainResourcesDir;

    public ProjectCreator(String baseDir, String pluginName) {
        this.pluginName = pluginName;
        this.pluginNameLower = pluginName.toLowerCase();
        this.projectDir = Path.of(baseDir, pluginName);
        this.srcMainJavaDir = projectDir.resolve("src/main/java/com/example/" + pluginNameLower);
        this.srcMainResourcesDir = projectDir.resolve("src/main/resources");
    }

    public void createProject(String generatedContent) throws IOException {
        System.out.println("-> Создание структуры проекта...");
        createDirectoryStructure();

        System.out.println("-> Создание файлов из сгенерированного контента...");
        parseAndCreateFiles(generatedContent);

        System.out.println("\n✅ Проект '" + pluginName + "' успешно сгенерирован!");
        System.out.println("   Путь: " + projectDir.toAbsolutePath());
        System.out.println("\nДля сборки плагина перейдите в папку проекта и выполните:");
        System.out.println("   cd " + pluginName);
        System.out.println("   mvn clean package");
    }

    private void createDirectoryStructure() throws IOException {
        Files.createDirectories(srcMainJavaDir);
        Files.createDirectories(srcMainResourcesDir);
    }

    private void parseAndCreateFiles(String content) throws IOException {
        // Регулярное выражение для поиска блоков кода
        Pattern codeBlockPattern = Pattern.compile("```(\\w+)\\s*([\\s\\S]*?)```");
        Matcher matcher = codeBlockPattern.matcher(content);

        while (matcher.find()) {
            String language = matcher.group(1);
            String code = matcher.group(2).trim();

            switch (language.toLowerCase()) {
                case "xml" -> createFile("pom.xml", code);
                case "yaml" -> createFile("plugin.yml", code);
                case "java" -> createJavaFile(code);
                default -> System.out.println("   [Предупреждение] Неизвестный тип блока: " + language);
            }
        }
    }

    private void createJavaFile(String code) throws IOException {
        // Извлекаем имя класса из кода
        Pattern classNamePattern = Pattern.compile("public\\s+class\\s+(\\w+)");
        Matcher classNameMatcher = classNamePattern.matcher(code);
        if (classNameMatcher.find()) {
            String className = classNameMatcher.group(1);
            createFile(className + ".java", code);
        } else {
            System.out.println("   [Ошибка] Не удалось определить имя класса в Java-файле.");
        }
    }

    private void createFile(String fileName, String content) throws IOException {
        Path filePath;
        if (fileName.equals("pom.xml")) {
            filePath = projectDir.resolve(fileName);
        } else if (fileName.equals("plugin.yml")) {
            filePath = srcMainResourcesDir.resolve(fileName);
        } else { // Java файлы
            filePath = srcMainJavaDir.resolve(fileName);
        }
        Files.writeString(filePath, content);
        System.out.println("   Создан файл: " + filePath.getFileName());
    }
}
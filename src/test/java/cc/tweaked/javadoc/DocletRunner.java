package cc.tweaked.javadoc;

import javax.tools.*;
import javax.tools.DocumentationTool.Location;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DocletRunner {
    private static final File INPUT = new File("src/test/java").getAbsoluteFile();
    private static final Path GOLDEN = new File("src/test/resources").toPath();

    private final Path output;

    public DocletRunner() throws IOException {
        output = Files.createTempDirectory("cct_javadoc");
    }

    public void generate() throws IOException {
        DocumentationTool tool = ToolProvider.getSystemDocumentationTool();
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        StandardJavaFileManager fm = tool.getStandardFileManager(collector, Locale.ROOT, StandardCharsets.UTF_8);

        fm.setLocationFromPaths(Location.DOCUMENTATION_OUTPUT, Collections.singletonList(output));
        fm.setLocation(StandardLocation.SOURCE_PATH, Collections.singletonList(INPUT));

        Iterable<? extends JavaFileObject> files = fm.getJavaFileObjectsFromPaths(
            Files.walk(INPUT.toPath())
                .filter(Files::isRegularFile)
                .collect(Collectors.toList())
        );

        List<String> options = Arrays.asList("-d", output.toString());
        DocumentationTool.DocumentationTask task = tool.getTask(null, fm, collector, LuaDoclet.class, options, files);
        Boolean ok = task.call();

        for (Diagnostic<? extends JavaFileObject> diagnostic : collector.getDiagnostics()) {
            System.err.println(diagnostic.toString());
        }

        if (ok == null || !ok) throw new IllegalStateException("Generation failed (see above)");
    }

    public void compare(String name) throws IOException {
        assertEquals(
            read(GOLDEN.resolve(name)),
            read(output.resolve(name)),
            "Test outputs should be the same."
        );
    }

    public void update(String name) throws IOException {
        Files.copy(output.resolve(name), GOLDEN.resolve(name), StandardCopyOption.REPLACE_EXISTING);
    }

    public static String read(Path file) throws IOException {
        return Files.readString(file);
    }
}

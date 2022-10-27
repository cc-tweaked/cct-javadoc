package cc.tweaked.javadoc;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;

public class Tests {
    private static final boolean regenerate = "true".equalsIgnoreCase(System.getProperty("cc.regenerate"));
    private static DocletRunner runner;

    @BeforeAll
    public static void setup() throws IOException {
        runner = new DocletRunner();
        runner.generate();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "basic",
        "generic_peripheral",
        "types.One",
        "types.Two",
        "foo",
        "bar.Type",
    })
    public void checkEqual(String name) throws IOException {
        String fullName = name + ".lua";
        try {
            runner.compare(fullName);
        } finally {
            if (regenerate) runner.update(fullName);
        }
    }
}

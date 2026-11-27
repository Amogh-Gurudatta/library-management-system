import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * End-to-end tests. Every {@code <name>.in} in src/test/resources/golden is fed to the program and the
 * output must equal {@code <name>.expected} exactly. New pairs are picked up automatically.
 */
class GoldenTest {
    private static final Path DIR = Path.of("src", "test", "resources", "golden");

    @TestFactory
    Stream<DynamicTest> everyScriptProducesItsExpectedOutput() throws IOException {
        try (Stream<Path> files = Files.list(DIR)) {
            List<Path> inputs = files.filter(p -> p.getFileName().toString().endsWith(".in")).sorted().toList();
            return inputs.stream().map(input -> {
                String name = input.getFileName().toString().replaceFirst("\\.in$", "");
                return dynamicTest(name, () -> {
                    String expected = Files.readString(DIR.resolve(name + ".expected"));
                    assertEquals(expected, run(Files.readString(input)));
                });
            });
        }
    }

    /** Runs the whole program on the given input text and returns everything it prints. */
    static String run(String input) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream out = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            new CommandProcessor(new Library()).run(new Scanner(input), out);
        }
        return bytes.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
    }
}

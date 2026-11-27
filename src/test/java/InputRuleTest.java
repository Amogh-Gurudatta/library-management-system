import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Project rule: all input is read with java.util.Scanner, never BufferedReader, Console or similar. */
class InputRuleTest {
    private static final List<String> BANNED = List.of(
            "BufferedReader", "InputStreamReader", "System.console", "Console", "DataInputStream",
            "ObjectInputStream", "FileReader", "Files.read", "System.in.read");

    @Test
    void programCodeReadsInputOnlyThroughScanner() throws IOException {
        List<String> offences = new ArrayList<>();
        try (Stream<Path> files = Files.walk(Path.of("src", "main", "java"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String code = Files.readString(file);
                for (String banned : BANNED) {
                    if (code.contains(banned)) {
                        offences.add(file.getFileName() + " uses " + banned);
                    }
                }
            }
        }
        assertEquals(List.of(), offences);
    }

    @Test
    void mainCreatesTheOneScannerOverStandardInput() throws IOException {
        String main = Files.readString(Path.of("src", "main", "java", "Main.java"));
        assertTrue(main.contains("new Scanner(System.in)"));
    }
}

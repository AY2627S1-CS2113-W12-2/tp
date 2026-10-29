package seedu.tutorledger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Checks that commands typed into the application reach their feature handlers. */
class TutorLedgerCommandRoutingTest {
    @Test
    void main_routesCommandsToEveryFeatureHandler() {
        String output = runCommands(
                "help",
                "list",
                "view S99999",
                "lessons",
                "ls",
                "homework",
                "unpaid",
                "owed",
                "topics",
                "scores",
                "tasks",
                "filter",
                "exit");

        assertTrue(output.contains("Commands (UPPER_CASE = value you supply"));
        assertTrue(output.contains("Error: Use: topics STUDENT_ID"));
        assertTrue(output.contains("Error: Use: scores STUDENT_ID"));
        assertFalse(output.contains("Unknown command."),
                "A recognized feature command should not fall through to the unknown-command response");
    }

    @Test
    void main_routesStorageCommandsWithoutChangingTheDefaultDataFile(@TempDir Path tempDir) throws Exception {
        Path defaultDataFile = Path.of(Storage.DEFAULT_FILE_PATH);
        byte[] originalData = Files.readAllBytes(defaultDataFile);
        Path backup = tempDir.resolve("backup.txt");

        String output = runCommands("reload", "save " + backup, "exit");

        assertTrue(output.contains("Reloaded " + Storage.DEFAULT_FILE_PATH));
        assertTrue(output.contains("Saved a copy to " + backup));
        assertTrue(Files.exists(backup));
        assertArrayEquals(originalData, Files.readAllBytes(defaultDataFile));
    }

    private static String runCommands(String... commands) {
        InputStream originalInput = System.in;
        PrintStream originalOutput = System.out;
        Logger rootLogger = Logger.getLogger("");
        Handler[] handlers = rootLogger.getHandlers();
        Level[] originalHandlerLevels = new Level[handlers.length];
        for (int i = 0; i < handlers.length; i++) {
            originalHandlerLevels[i] = handlers[i].getLevel();
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PrintStream capturedOutput = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            String input = String.join(System.lineSeparator(), commands) + System.lineSeparator();
            System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
            System.setOut(capturedOutput);
            TutorLedger.main(new String[0]);
            capturedOutput.flush();
            return output.toString(StandardCharsets.UTF_8);
        } finally {
            System.setIn(originalInput);
            System.setOut(originalOutput);
            for (int i = 0; i < handlers.length; i++) {
                handlers[i].setLevel(originalHandlerLevels[i]);
            }
        }
    }
}

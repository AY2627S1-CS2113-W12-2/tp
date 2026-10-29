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
    void run_routesCommandsToEveryFeatureHandler(@TempDir Path tempDir) {
        String output = runCommands(tempDir.resolve("tutorledger.txt"),
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
    void run_reloadAndSaveCopy_dataFileUnchanged(@TempDir Path tempDir) throws Exception {
        // Each test gets its own data file, so the tests pass on a fresh clone with no data/ folder.
        Path dataFile = tempDir.resolve("tutorledger.txt");
        new Storage(dataFile.toString()).save(new TutorLedgerData());
        byte[] originalData = Files.readAllBytes(dataFile);
        Path backup = tempDir.resolve("backup.txt");

        String output = runCommands(dataFile, "reload", "save " + backup, "exit");

        assertTrue(output.contains("Reloaded " + dataFile));
        assertTrue(output.contains("Saved a copy to " + backup));
        assertTrue(Files.exists(backup));
        assertArrayEquals(originalData, Files.readAllBytes(dataFile));
    }

    /** Feeds the commands to the command loop, using the given data file, and returns what it printed. */
    private static String runCommands(Path dataFile, String... commands) {
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
            TutorLedger.run(new Storage(dataFile.toString()));
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

package seedu.tutorledger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises confirmed replay through the real input loop, using only temporary tutoring records. */
class CommandHistoryIntegrationTest {
    private static final String NL = System.lineSeparator();

    @TempDir
    Path tempDir;

    /** Creates a student and one unpaid past lesson for query and deletion scenarios. */
    private Path seededFile() throws IOException {
        TutorLedgerData data = new TutorLedgerData();
        data.addStudent("Alice Tan", "Pri 5", "91234567", List.of("Math"), null);
        data.addLesson("S1", "Math", "06-10-2020", "1600", "40", "present", "Fractions");
        Path file = tempDir.resolve("data.txt");
        new Storage(file.toString()).save(data);
        return file;
    }

    @Test
    void repeatQuery_afterPaymentShowsFreshResultsAndRecordsTheQueryOnce() throws IOException {
        Path file = seededFile();
        String query = "filter S1 from/01-01-2020 to/31-12-2020 pay/unpaid";

        String output = TutorLedgerCommandRoutingTest.runCommands(file,
                query, "mark L1 paid", "repeat 1", "  YeS  ", "history", "exit");

        assertTrue(output.contains("1 lesson (S1 Alice Tan, from Wed 01-01-2020"), output);
        assertTrue(output.contains("0 lessons (S1 Alice Tan, from Wed 01-01-2020"), output);
        assertTrue(output.contains("Selected command: " + query + NL + "Run this command? (yes/no):"), output);
        assertTrue(output.contains("Command history (3 commands)" + NL + "  1  " + query
                + NL + "  2  mark L1 paid" + NL + "  3  " + query), output);
        assertFalse(output.contains("Error:"), output);
        assertEquals(Lesson.PAYMENT_PAID, new Storage(file.toString()).load().data().getLesson("L1")
                .getPaymentStatus());
    }

    @Test
    void confirmation_yesAndYExecuteAMutationExactlyOnce() throws IOException {
        int index = 0;
        for (String answer : List.of("yes", "y", " YES ", "Y")) {
            Path file = tempDir.resolve("tasks" + index++ + ".txt");
            String output = TutorLedgerCommandRoutingTest.runCommands(file,
                    "task Print worksheets", "repeat 1", answer, "history", "exit");
            TutorLedgerData loaded = new Storage(file.toString()).load().data();
            assertEquals(2, loaded.getTasks().size(), output);
            assertTrue(output.contains("Command history (2 commands)"), output);
            assertFalse(output.contains("Error:"), output);
        }
    }

    @Test
    void confirmation_noAndNLeaveRecordsAndHistoryUntouched() throws IOException {
        Path file = seededFile();
        byte[] before = Files.readAllBytes(file);
        for (String answer : List.of("no", "n", " NO ", "N")) {
            String output = TutorLedgerCommandRoutingTest.runCommands(file,
                    "archive S1", "restore S1", "repeat 1", answer, "history", "exit");
            assertTrue(output.contains("Repeat cancelled."), output);
            assertTrue(output.contains("Command history (2 commands)"), output);
            assertArrayEquals(before, Files.readAllBytes(file));
        }
    }

    @Test
    void selectionAndInvalidReplies_neverDispatchBeforeConfirmation() throws IOException {
        Path file = seededFile();
        byte[] before = Files.readAllBytes(file);
        String output = TutorLedgerCommandRoutingTest.runCommands(file,
                "archive S1", "restore S1", "repeat 1", "task Must not run", "n", "history", "exit");
        assertTrue(output.contains("Selected command: archive S1"), output);
        assertTrue(output.contains("Please enter yes or no."), output);
        assertTrue(output.contains("Repeat cancelled."), output);
        assertTrue(output.contains("Command history (2 commands)"), output);
        assertArrayEquals(before, Files.readAllBytes(file));
        assertTrue(new Storage(file.toString()).load().data().getTasks().isEmpty());
    }

    @Test
    void readOnlyReplayAndInvalidRequests_preserveHandEditedFile() throws IOException {
        Path file = seededFile();
        Files.writeString(file, Files.readString(file) + "# Keep my hand edits\n");
        byte[] before = Files.readAllBytes(file);
        String output = TutorLedgerCommandRoutingTest.runCommands(file, "  v S1  ", "repeat 1", "yes",
                "history", "history extra", "repeat", "repeat 0", "repeat -1", "repeat 99",
                "repeat 1 extra", "repeat 999999999999999999999", "exit");
        assertTrue(output.contains("Selected command: v S1"), output);
        assertTrue(output.contains("Command history (2 commands)" + NL + "  1  v S1" + NL + "  2  v S1"), output);
        assertArrayEquals(before, Files.readAllBytes(file));
    }

    @Test
    void replay_revalidatesDeletedIdsAndKeepsDeletionConfirmation() throws IOException {
        Path file = seededFile();
        byte[] before = Files.readAllBytes(file);
        String warning = TutorLedgerCommandRoutingTest.runCommands(file, "delete S1", "repeat 1", "yes", "exit");
        assertEquals(2, warning.lines().filter(line -> line.startsWith("Warning:")).count(), warning);
        assertArrayEquals(before, Files.readAllBytes(file));

        String deleted = TutorLedgerCommandRoutingTest.runCommands(file,
                "delete S1 confirm", "repeat 1", "y", "history", "exit");
        assertTrue(deleted.contains("Error: No student found with ID S1."), deleted);
        assertTrue(deleted.contains("Command history (2 commands)"), deleted);
        assertTrue(new Storage(file.toString()).load().data().getStudents().isEmpty());
    }

    @Test
    void reloadRetainsHistoryAndExplicitStorageCommandsCanBeRepeated() throws IOException {
        Path file = seededFile();
        Path backup = tempDir.resolve("backup.txt");
        String output = TutorLedgerCommandRoutingTest.runCommands(file,
                "save " + backup, "repeat 1", "yes", "reload", "repeat 3", "yes", "history", "exit");
        assertTrue(Files.exists(backup));
        assertArrayEquals(Files.readAllBytes(file), Files.readAllBytes(backup));
        assertTrue(output.contains("Command history (4 commands)"), output);
        assertEquals(2, output.lines().filter(line -> line.startsWith("Reloaded ")).count(), output);
    }

    @Test
    void freshSessionHasNoHistoryAndEndOfInputCancelsSafely() throws IOException {
        Path file = seededFile();
        byte[] before = Files.readAllBytes(file);
        String first = TutorLedgerCommandRoutingTest.runCommands(file, "list", "repeat 1", "perhaps");
        assertTrue(first.contains("Please enter yes or no."), first);
        assertTrue(first.endsWith("Repeat cancelled." + NL), first);
        String second = TutorLedgerCommandRoutingTest.runCommands(file, "history", "repeat 1", "exit");
        assertTrue(second.contains("No commands in history."), second);
        assertTrue(second.contains("Error: No commands in history."), second);
        assertArrayEquals(before, Files.readAllBytes(file));
    }

    @Test
    void blankInputAndHistoryControlsAreNotRecordedButFailedCommandsAre() {
        Path file = tempDir.resolve("empty.txt");
        String output = TutorLedgerCommandRoutingTest.runCommands(file, " ", "history", "view S99",
                "repeat 1", "yes", "history", "exit");
        assertTrue(output.contains("Enter a command."), output);
        assertTrue(output.contains("Command history (2 commands)" + NL + "  1  view S99" + NL + "  2  view S99"),
                output);
        assertFalse(Files.exists(file));
        assertTrue(HelpCommand.getHelpText().contains("COMMAND HISTORY"));
        assertTrue(HelpCommand.getHelpText().contains("repeat NUMBER"));
    }
}

package seedu.tutorledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Checks US34's deletion warning, explicit confirmation and persistence without a pending action. */
class StudentDeletionWarningTest {
    private static final String NL = System.lineSeparator();
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-23T10:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path tempDir;

    private TutorLedgerData data;
    private StudentCommands commands;

    @BeforeEach
    void setUp() {
        data = new TutorLedgerData();
        data.addStudent("Alice Tan", "Pri 5", "91234567", List.of("Math"), "60");
        data.addStudent("Bob Tan", "JC 2", "81234567");
        commands = new StudentCommands(data, CLOCK);
    }

    @Test
    void delete_withHistory_warnsWithIdentityAndCountsAndPreservesEveryRecordAndCounter() {
        addHistory("S1", "15-09-2026");
        addHistory("S2", "16-09-2026");
        String before = Storage.encode(data);

        String output = commands.execute("delete s1");

        assertEquals("Warning: deleting S1: Alice Tan (Pri 5) will permanently remove:" + NL
                + "  1 lesson, 1 homework item and 1 score." + NL
                + "This cannot be undone. No records have been deleted." + NL
                + "To confirm, type: delete S1 confirm" + NL
                + "Otherwise, continue with another command.", output);
        assertEquals(before, Storage.encode(data), "The snapshot includes all records and four ID counters.");
    }

    @Test
    void delete_withOnlyOneLesson_showsZeroLinkedCounts() {
        data.addLesson("S1", "Math", "15-09-2026", "1600", "60");

        String output = commands.execute("delete S1");

        assertTrue(output.contains("1 lesson, 0 homework items and 0 scores."), output);
        assertEquals(2, data.getStudents().size());
        assertEquals(1, data.getLessons().size());
    }

    @Test
    void delete_withMultipleLinkedRecords_countsSubmittedHomeworkAndPluralises() {
        addHistory("S1", "15-09-2026");
        addHistory("S1", "16-09-2026");
        data.putHomework(new Homework("H3", "S1", "Old worksheet", "17-09-2026", "submitted"));
        String before = Storage.encode(data);

        String output = commands.execute("delete S1");

        assertTrue(output.contains("2 lessons, 3 homework items and 2 scores."), output);
        assertEquals(before, Storage.encode(data));
    }

    @Test
    void delete_pastFuturePaidAndCancelledLessons_eachRequireConfirmation() {
        Lesson past = new Lesson("L1", "S1", "Math", "15-09-2026", "1600", "present", "60", "Revision");
        Lesson future = new Lesson("L1", "S1", "Math", "01-12-2026", "1600", "not recorded", "60", "");
        for (Lesson lesson : List.of(past, past.withPaymentStatus("paid"), future,
                future.withCancellation("Student away"))) {
            data.putLesson(lesson);
            String before = Storage.encode(data);

            assertTrue(commands.execute("delete S1").startsWith("Warning:"));
            assertEquals(before, Storage.encode(data));
        }
    }

    @Test
    void delete_confirmed_cascadesOnlyForTargetAndNeverReusesIds() {
        addHistory("S1", "15-09-2026");
        addHistory("S2", "16-09-2026");
        Student other = data.getStudent("S2");
        Lesson otherLesson = data.getLesson("L2");
        Homework otherHomework = data.getHomeworkById("H2");
        Score otherScore = data.getScores().stream().filter(score -> score.getStudentId().equals("S2"))
                .findFirst().orElseThrow();
        assertTrue(commands.execute("delete S1").startsWith("Warning:"));

        String output = commands.execute("delete S1 confirm");

        assertEquals("Deleted S1: Alice Tan (Pri 5)" + NL
                + "Also deleted 1 lesson and 1 homework item." + NL + "Also deleted 1 score.", output);
        assertThrows(IllegalArgumentException.class, () -> data.getStudent("S1"));
        assertSame(other, data.getStudent("S2"));
        assertSame(otherLesson, data.getLessons().iterator().next());
        assertSame(otherHomework, data.getHomework().iterator().next());
        assertSame(otherScore, data.getScores().iterator().next());
        assertEquals("S3", data.addStudent("Carol Tan", "Sec 1", "87654321").getStudentId());
        assertEquals("L3", data.addLesson("S2", "Math", "17-09-2026", "1600", "60").getLessonId());
        assertEquals("H3", data.addHomework("S2", "New work", LocalDate.of(2026, 12, 1)).getHomeworkId());
        assertEquals("A3", data.addScore("S2", "WA2", "9", "10", "17-09-2026").getScoreId());
    }

    @Test
    void delete_explicitConfirmation_acceptsCaseAndWhitespaceWithoutAPriorWarning() {
        data.addLesson("S1", "Math", "15-09-2026", "1600", "60");

        String output = commands.execute("  DELETE   s1\tCoNfIrM  ");

        assertTrue(output.startsWith("Deleted S1:"), output);
        assertTrue(data.getLessons().isEmpty());
    }

    @Test
    void delete_withOnlyHomeworkScoresOrTasks_warnsBeforeDeleting() {
        data.addHomework("S1", "Worksheet", LocalDate.of(2026, 12, 1));
        data.addScore("S2", "WA1", "8", "10", "15-09-2026");
        data.addTask("S2", "Bring worksheet", null);

        // Each of these records would be lost with the student, so each one alone needs confirmation.
        String homeworkOnly = commands.execute("delete S1");
        assertTrue(homeworkOnly.startsWith("Warning: deleting S1:"), homeworkOnly);
        assertTrue(homeworkOnly.contains("0 lessons, 1 homework item and 0 scores."), homeworkOnly);
        String scoresAndTasks = commands.execute("delete S2");
        assertTrue(scoresAndTasks.contains("0 lessons, 0 homework items and 1 score." + NL
                + "  1 follow-up task." + NL), scoresAndTasks);
        assertEquals(2, data.getStudents().size());
        assertEquals(1, data.getHomework().size());

        String output = commands.execute("delete S1 confirm");

        assertTrue(output.startsWith("Deleted S1:"), output);
        assertTrue(output.contains("0 lessons and 1 homework item."), output);
        assertTrue(data.getHomework().isEmpty());
        assertEquals(1, data.getScores().size());
        assertEquals(1, data.getTasks().size());
    }

    @Test
    void delete_withNoLinkedRecords_deletesImmediately() {
        data.addLesson("S2", "Math", "15-09-2026", "1600", "60");

        String output = commands.execute("delete S1");

        assertEquals("Deleted S1: Alice Tan (Pri 5)" + NL + "Also deleted 0 lessons and 0 homework items.", output);
        assertEquals("S2", data.getStudents().iterator().next().getStudentId());
        assertEquals(1, data.getLessons().size());
    }

    @Test
    void delete_invalidArguments_neverChangeRecords() {
        addHistory("S1", "15-09-2026");
        String before = Storage.encode(data);
        for (String input : List.of("delete", "delete confirm", "delete S0", "delete Sx confirm",
                "delete L1", "delete S99", "delete S99 confirm", "delete S1 yes", "delete S1 no",
                "delete S1 confirmed", "delete S1 confirm/", "delete S1 S2", "delete S1 confirm extra",
                "delete S1 confirm confirm", "delete S1 S2 confirm", "delete S1 r/confirm")) {
            assertTrue(commands.execute(input).startsWith("Error:"), input);
            assertEquals(before, Storage.encode(data), input);
        }
    }

    @Test
    void delete_repeatedWarningsAndOtherCommands_neverArmDeletion() {
        addHistory("S1", "15-09-2026");
        String before = Storage.encode(data);
        assertTrue(commands.execute("delete S1").startsWith("Warning:"));
        assertTrue(commands.execute("view S1").contains("Alice Tan"));
        assertTrue(commands.execute("list").contains("Alice Tan"));
        commands.execute("help");
        assertTrue(commands.execute("yes").startsWith("Unknown command:"));
        assertTrue(commands.execute("confirm").startsWith("Unknown command:"));
        assertTrue(commands.execute("delete S1").startsWith("Warning:"));
        assertEquals(before, Storage.encode(data));
    }

    @Test
    void delete_afterChanges_usesCurrentIdentityAndLinkedRecords() {
        addHistory("S1", "15-09-2026");
        assertTrue(commands.execute("delete S1").startsWith("Warning:"));
        commands.execute("edit S1 n/Alice Lim l/JC1");
        addHistory("S1", "16-09-2026");

        String warning = commands.execute("delete S1");
        assertTrue(warning.contains("S1: Alice Lim (JC 1)"), warning);
        assertTrue(warning.contains("2 lessons, 2 homework items and 2 scores."), warning);
        String output = commands.execute("delete S1 confirm");
        assertTrue(output.startsWith("Deleted S1: Alice Lim (JC 1)"), output);
        assertTrue(output.contains("2 lessons and 2 homework items."), output);
        assertTrue(output.endsWith("Also deleted 2 scores."), output);
    }

    @Test
    void delete_warningAndInvalidConfirmation_doNotRewriteTheSavedFile() throws IOException {
        addHistory("S1", "15-09-2026");
        Path file = tempDir.resolve("tutorledger.txt");
        Storage storage = new Storage(file.toString());
        storage.save(data);
        // Hand edits make an accidental save observable even if the in-memory records did not change.
        String handEdited = Files.readString(file) + "# Preserve this hand-edited comment" + NL;
        Files.writeString(file, handEdited);
        StorageCommands persistence = new StorageCommands(data, storage);
        for (String input : List.of("delete S1", "delete S1 yes", "delete S1")) {
            String before = persistence.snapshot();
            commands.execute(input);
            assertEquals("", persistence.saveIfChanged(before));
            assertEquals(handEdited, Files.readString(file));
        }
    }

    @Test
    void delete_afterReload_checksCurrentDataAndPersistsOnlyConfirmedDeletion() throws IOException {
        addHistory("S1", "15-09-2026");
        Path file = tempDir.resolve("tutorledger.txt");
        Storage storage = new Storage(file.toString());
        storage.save(data);
        assertTrue(commands.execute("delete S1").startsWith("Warning:"));
        TutorLedgerData reloaded = storage.load().data();
        reloaded.replaceStudent(reloaded.getStudent("S1").withDetails("Alice Lim", "JC 1", "91234567",
                List.of("Math")));
        storage.save(reloaded);
        StorageCommands persistence = new StorageCommands(data, storage);
        assertTrue(persistence.execute("reload").startsWith("Reloaded "));
        assertTrue(commands.execute("delete S1").contains("Alice Lim (JC 1)"));

        String before = persistence.snapshot();
        assertTrue(commands.execute("delete S1 confirm").startsWith("Deleted S1: Alice Lim"));
        assertEquals("", persistence.saveIfChanged(before));

        Storage.LoadResult restarted = storage.load();
        assertTrue(restarted.problems().isEmpty());
        assertEquals("S2", restarted.data().getStudents().iterator().next().getStudentId());
        assertTrue(restarted.data().getLessons().isEmpty());
        assertTrue(restarted.data().getHomework().isEmpty());
        assertTrue(restarted.data().getScores().isEmpty());
        assertEquals(3, restarted.data().getNextStudentNumber());
        assertEquals(2, restarted.data().getNextLessonNumber());
        assertEquals(2, restarted.data().getNextHomeworkNumber());
        assertEquals(2, restarted.data().getNextScoreNumber());
    }

    @Test
    void delete_repeatedConfirmation_reportsMissingStudentWithoutFurtherChanges() {
        addHistory("S1", "15-09-2026");
        commands.execute("delete S1 confirm");
        String before = Storage.encode(data);

        assertEquals("Error: No student found with ID S1.", commands.execute("delete S1 confirm"));
        assertEquals(before, Storage.encode(data));
    }

    @Test
    void delete_lessonRoutingStillDeletesOnlyTheLesson() {
        addHistory("S1", "15-09-2026");
        assertFalse(LessonCommands.handles("delete S1 confirm"));
        assertTrue(LessonCommands.handles("delete L1"));

        String output = new LessonCommands(data, CLOCK).execute("delete L1");

        assertTrue(output.startsWith("Deleted L1"), output);
        assertEquals(2, data.getStudents().size());
        assertTrue(data.getLessons().isEmpty());
        assertEquals(1, data.getHomework().size());
        assertEquals(1, data.getScores().size());
    }

    /** Adds one record of each linked type, with different dates avoiding lesson-slot clashes. */
    private void addHistory(String studentId, String date) {
        data.addLesson(studentId, "Math", date, "1600", "60");
        data.addHomework(studentId, "Worksheet", LocalDate.of(2026, 12, 1));
        data.addScore(studentId, "WA1", "8", "10", date);
    }
}

package seedu.tutorledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;

/** Regression coverage for rejected imports, integer boundaries and allocation without partial changes. */
class RecordIdValidationTest {
    private static final String STUDENT = "S | S1 | Alice Tan | Pri5 | 91234567 | Math";

    @TempDir
    Path tempDir;

    /** Builds one row of every record type with the same numeric suffix. */
    private List<String> rows(String number) {
        return List.of(
                "S | S" + number + " | Bob Tan | Pri6 | 98765432",
                "L | L" + number + " | S1 | 01-01-2020 | 1600 | Math | 40 | present | unpaid | Revision",
                "H | H" + number + " | S1 | 01-01-2030 | outstanding | Revision",
                "A | A" + number + " | S1 | 01-01-2020 | 8 | 10 | Revision",
                "T | T" + number + " | pending | | S1 | Revision");
    }

    /** Checks the complete five-counter state, including counters for deleted records. */
    private void assertCounters(TutorLedgerData data, int student, int other) {
        assertEquals(student, data.getNextStudentNumber());
        assertEquals(other, data.getNextLessonNumber());
        assertEquals(other, data.getNextHomeworkNumber());
        assertEquals(other, data.getNextScoreNumber());
        assertEquals(other, data.getNextTaskNumber());
    }

    @Test
    void oversizedImports_allRecordTypesAreReportedAndAbsentFromMemoryAndSavedOutput() {
        for (String number : List.of("2147483647", "2147483648", "999999999999999999999999")) {
            List<String> input = new ArrayList<>(List.of(STUDENT));
            input.addAll(rows(number));
            Storage.LoadResult result = Storage.decode(input);
            TutorLedgerData data = result.data();
            assertEquals(5, result.problems().size(), result.problems().toString());
            assertTrue(result.problems().stream().allMatch(problem -> problem.contains("between 1 and 2147483646")));
            assertEquals(1, data.getStudents().size());
            assertTrue(data.getLessons().isEmpty());
            assertTrue(data.getHomework().isEmpty());
            assertTrue(data.getScores().isEmpty());
            assertTrue(data.getTasks().isEmpty());
            assertCounters(data, 2, 1);
            assertFalse(Storage.encode(data).contains(number));
            assertEquals("S2", data.addStudent("Carol Tan", "Pri5", "88888888").getStudentId());
        }
    }

    @Test
    void maximumRecordNumbers_roundTripAndPreserveExhaustedCounters() {
        List<String> input = new ArrayList<>(List.of(STUDENT));
        input.addAll(rows("2147483646"));
        Storage.LoadResult result = Storage.decode(input);
        assertTrue(result.problems().isEmpty(), result.problems().toString());
        TutorLedgerData data = result.data();
        assertCounters(data, Integer.MAX_VALUE, Integer.MAX_VALUE);
        assertEquals(List.of("S1", "S2147483646"),
                data.findStudents("").stream().map(Student::getStudentId).toList());
        String encoded = Storage.encode(data);
        Storage.LoadResult restored = Storage.decode(encoded.lines().toList());
        assertTrue(restored.problems().isEmpty(), restored.problems().toString());
        assertEquals(encoded, Storage.encode(restored.data()));
        // Even after deletion, saved NEXT values must prevent reusing the last allocated IDs.
        data.deleteStudent("S2147483646");
        data.deleteLesson("L2147483646");
        TutorLedgerData afterDeletion = Storage.decode(Storage.encode(data).lines().toList()).data();
        assertCounters(afterDeletion, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    @Test
    void exhaustedCounters_rejectEveryCreationWithoutChangingRecordsOrCounters() {
        TutorLedgerData data = Storage.decode(List.of(STUDENT,
                "NEXT | S2147483647 | L2147483647 | H2147483647 | A2147483647 | T2147483647")).data();
        String before = Storage.encode(data);
        List<Executable> creations = List.of(
                () -> data.addStudent("Bob Tan", "Pri6", "98765432"),
                () -> data.addLesson("S1", "Math", "01-01-2030", "1600", "40"),
                () -> data.addWeeklyLessons("S1", "Math", "01-01-2030", "1600", "40", 2),
                () -> data.addHomework("S1", "Revision", LocalDate.of(2030, 1, 1)),
                () -> data.addScore("S1", "Revision", "8", "10", "01-01-2020"),
                () -> data.addTask("S1", "Revision", null));
        for (Executable creation : creations) {
            assertTrue(assertThrows(IllegalArgumentException.class, creation).getMessage().contains("No more"));
            assertEquals(before, Storage.encode(data));
        }
    }

    @Test
    void finalAvailableIds_canBeAllocatedOnceForAllRecordTypes() {
        TutorLedgerData data = Storage.decode(List.of(STUDENT,
                "NEXT | S2147483646 | L2147483646 | H2147483646 | A2147483646 | T2147483646")).data();
        assertEquals("S2147483646", data.addStudent("Bob Tan", "Pri6", "98765432").getStudentId());
        assertEquals("L2147483646", data.addLesson("S1", "Math", "01-01-2030", "1600", "40").getLessonId());
        assertEquals("H2147483646", data.addHomework("S1", "Revision", LocalDate.of(2030, 1, 1)).getHomeworkId());
        assertEquals("A2147483646", data.addScore("S1", "Revision", "8", "10", "01-01-2020").getScoreId());
        assertEquals("T2147483646", data.addTask(null, "Revision", null).getTaskId());
        assertCounters(data, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    @Test
    void weeklySchedule_reservesEntireBatchBeforeInsertionAtTheBoundary() {
        TutorLedgerData data = Storage.decode(List.of(STUDENT, "NEXT | S2 | L2147483646 | H1")).data();
        String before = Storage.encode(data);
        assertThrows(IllegalArgumentException.class,
                () -> data.addWeeklyLessons("S1", "Math", "01-01-2030", "1600", "40", 2));
        assertEquals(before, Storage.encode(data));
        data.setNextLessonNumber(Integer.MAX_VALUE); // Cannot lower the exhausted counter afterwards.
        assertThrows(IllegalArgumentException.class, () -> data.setNextLessonNumber(1));

        TutorLedgerData enough = Storage.decode(List.of(STUDENT, "NEXT | S2 | L2147483645 | H1")).data();
        assertEquals(List.of("L2147483645", "L2147483646"), enough
                .addWeeklyLessons("S1", "Math", "01-01-2030", "1600", "40", 2)
                .stream().map(Lesson::getLessonId).toList());
        assertEquals(Integer.MAX_VALUE, enough.getNextLessonNumber());
    }

    @Test
    void invalidNextLines_doNotPartiallyApplyCountersAndLegacyCountersStillLoad() {
        for (String invalid : List.of("0", "2147483648", "999999999999999999999999")) {
            Storage.LoadResult result = Storage.decode(List.of(STUDENT,
                    "NEXT | S10 | L20 | H30 | A40 | T" + invalid));
            assertEquals(1, result.problems().size());
            assertCounters(result.data(), 2, 1);
        }
        for (String legacy : List.of("NEXT | s3 | l4 | h5", "NEXT | S3 | L4 | H5 | A6")) {
            Storage.LoadResult result = Storage.decode(List.of(STUDENT, legacy));
            assertTrue(result.problems().isEmpty());
            assertEquals(3, result.data().getNextStudentNumber());
            assertEquals(1, result.data().getNextTaskNumber());
        }
    }

    @Test
    void originalReproduction_listReloadSaveAndRestartNeverRetainRejectedStudent() throws IOException {
        Path file = tempDir.resolve("data.txt");
        Files.writeString(file, STUDENT + "\nS | S2147483648 | Bob Tan | Pri6 | 98765432\n");
        String output = TutorLedgerCommandRoutingTest.runCommands(file,
                "list", "reload", "view S2147483648", "save", "exit");
        assertTrue(output.contains("Skipped 1 line"), output);
        assertTrue(output.contains("1 student:"), output);
        assertTrue(output.contains("Error: No student found with ID S2147483648."), output);
        assertFalse(output.contains("For input string"), output);
        assertFalse(Files.readString(file).contains("2147483648"));
        String restarted = TutorLedgerCommandRoutingTest.runCommands(file, "list", "exit");
        assertFalse(restarted.contains("Skipped"), restarted);
        assertFalse(restarted.contains("Error:"), restarted);
    }
}

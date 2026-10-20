package seedu.tutorledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Checks the data file format: saving and loading give back the same records, and bad lines are skipped. */
class StorageTest {
    @TempDir
    Path tempDir;

    /** Builds records that use every field, including deleted IDs that must not be reused. */
    private static TutorLedgerData sampleData() {
        TutorLedgerData data = new TutorLedgerData();
        data.addStudent("Amirah Binte Rahman", "Sec 3", "91234567");
        data.addStudent("Tan Wei Ming", "Sec 4", "98765432");
        data.addStudent("Ryan Koh", "Sec 1", "87654321");
        data.replaceStudent(data.getStudent("S1").withDetails("Amirah Binte Rahman", "Sec 3", "91234567",
                List.of("E Math", "A Math")));
        data.addLesson("S1", "A Math", "15-09-2026", "1600", "60");
        data.addLesson("S1", "E Math", "22-09-2026", "1600", "62.50");
        data.addLesson("S3", "Science", "23-09-2026", "1000", "40");
        data.replaceLesson(data.getLesson("L1").withDetails(null, null, null, "present", null,
                "Quadratics | discriminant \\ recap"));
        data.updateLessonPaymentStatus("L1", Lesson.PAYMENT_PAID);
        data.addHomework("S1", "Worksheet 3B", LocalDate.of(2026, 9, 22));
        data.addHomework("S2", "TYS 2023 Paper 1, Q1-10", LocalDate.of(2026, 9, 26));
        data.putHomework(data.getHomeworkById("H1").asSubmitted());
        data.addScore("S1", "WA1 | retest", "17.5", "20", "07-09-2026");
        data.addScore("S3", "Quiz", "5", "10", "10-09-2026");
        // Deleting the newest records leaves gaps that the saved counters must remember.
        data.deleteStudent("S3");
        return data;
    }

    @Test
    void encodeThenDecode_allRecords_givesBackTheSameRecords() {
        TutorLedgerData original = sampleData();

        Storage.LoadResult result = Storage.decode(Storage.encode(original).lines().toList());

        assertTrue(result.problems().isEmpty());
        assertEquals(Storage.encode(original), Storage.encode(result.data()));
        TutorLedgerData loaded = result.data();
        assertEquals(List.of("E Math", "A Math"), loaded.getStudent("S1").getSubjects());
        assertEquals("Quadratics | discriminant \\ recap", loaded.getLesson("L1").getNotes());
        assertEquals(Lesson.Attendance.PRESENT, loaded.getLesson("L1").getAttendance());
        assertEquals(Lesson.PAYMENT_PAID, loaded.getLesson("L1").getPaymentStatus());
        assertEquals("62.50", loaded.getLesson("L2").getFee().toPlainString());
        assertTrue(loaded.getHomeworkById("H1").isSubmitted());
        // S3 and L3 were deleted, so their IDs must still not be handed out again.
        assertEquals(4, loaded.getNextStudentNumber());
        assertEquals(4, loaded.getNextLessonNumber());
        assertEquals(3, loaded.getNextHomeworkNumber());
    }

    @Test
    void decode_badLines_skipsOnlyThoseLinesAndReportsThemInOrder() {
        List<String> lines = List.of(
                "# comment",
                "S | S1 | Amirah Binte Rahman | Sec 3 | 91234567 | A Math",
                "L | L1 | S1 | 31-02-2026 | 1600 | A Math | 60 | not recorded | unpaid | ",
                "L | L2 | S9 | 22-09-2026 | 1600 | A Math | 60 | not recorded | unpaid | ",
                "S | S2 | Tan Wei Ming | Sec 9 | 98765432",
                "X | something",
                "H | H1 | S1 | 29-09-2026 | lost | Worksheet 4A",
                "L | L3 | S1 | 22-09-2026 | 1600 | A Math",
                "",
                "L | L4 | S1 | 22-09-2026 | 1600 | A Math | 60 | late | paid | Fine line");

        Storage.LoadResult result = Storage.decode(lines);

        assertEquals(List.of(
                "Line 3: Date must be a real date in DD-MM-YYYY format.",
                "Line 4: No student found with ID S9.",
                "Line 5: Level must be Sec 1 to Sec 5.",
                "Line 6: Unknown record type \"X\". A line must start with S, R, L, H, A or NEXT.",
                "Line 7: Homework status must be outstanding or submitted.",
                "Line 8: A lesson line needs 10 fields but has 6."), result.problems());
        assertEquals(1, result.data().getStudents().size());
        assertEquals(List.of("L4"), result.data().getLessons().stream().map(Lesson::getLessonId).toList());
        assertTrue(result.data().getHomework().isEmpty());
    }

    @Test
    void decode_handEditedFile_acceptsAnyOrderCaseAndSpacing() {
        List<String> lines = List.of(
                "h|h1|s1|29-09-2026|OUTSTANDING|Worksheet 4A",
                "l|l1|s1|22-09-2026|1600|A Math|60|PRESENT|Paid|",
                "s|s1|Amirah Binte Rahman|sec3|91234567|A Math|");

        Storage.LoadResult result = Storage.decode(lines);

        assertTrue(result.problems().isEmpty(), result.problems().toString());
        assertEquals("Sec 3", result.data().getStudent("S1").getLevel());
        assertEquals(List.of("A Math"), result.data().getStudent("S1").getSubjects());
        assertEquals("S1", result.data().getLesson("L1").getStudentId());
        assertEquals(Lesson.PAYMENT_PAID, result.data().getLesson("L1").getPaymentStatus());
        assertEquals("S1", result.data().getHomeworkById("H1").getStudentId());
    }

    @Test
    void decode_duplicateIdsOrSlots_skipsTheLaterLine() {
        List<String> lines = List.of(
                "S | S1 | Amirah Binte Rahman | Sec 3 | 91234567 | A Math",
                "S | S1 | Tan Wei Ming | Sec 4 | 98765432",
                "S | S2 | Amirah Binte Rahman | Sec 3 | 91234567",
                "L | L1 | S1 | 22-09-2026 | 1600 | A Math | 60 | not recorded | unpaid | ",
                "L | L1 | S1 | 23-09-2026 | 1600 | A Math | 60 | not recorded | unpaid | ",
                "L | L2 | S1 | 22-09-2026 | 1600 | A Math | 60 | not recorded | unpaid | ",
                "H | H1 | S1 | 29-09-2026 | outstanding | Worksheet 4A",
                "H | H1 | S1 | 30-09-2026 | outstanding | Worksheet 4B");

        Storage.LoadResult result = Storage.decode(lines);

        assertEquals(List.of(
                "Line 2: Duplicate student ID S1.",
                "Line 3: A student with that name and phone number already exists.",
                "Line 5: Duplicate lesson ID L1.",
                "Line 6: L1 already starts at that date and time.",
                "Line 8: Duplicate homework ID H1."), result.problems());
    }

    @Test
    void decode_nextLine_raisesCountersButNeverLowersThem() {
        Storage.LoadResult raised = Storage.decode(List.of("NEXT | S10 | L20 | H30"));
        assertEquals(10, raised.data().getNextStudentNumber());
        assertEquals(20, raised.data().getNextLessonNumber());
        assertEquals(30, raised.data().getNextHomeworkNumber());

        // A NEXT line that is too low (for example after hand-editing) must not let S5 be handed out twice.
        Storage.LoadResult tooLow = Storage.decode(List.of(
                "NEXT | S1 | L1 | H1",
                "S | S5 | Amirah Binte Rahman | Sec 3 | 91234567"));
        assertEquals(6, tooLow.data().getNextStudentNumber());

        Storage.LoadResult broken = Storage.decode(List.of("NEXT | 10 | L20", "NEXT | S1 | L1 | H1"));
        assertEquals(List.of("Line 1: A NEXT line must look like: NEXT | S1 | L1 | H1 | A1"), broken.problems());
    }

    @Test
    void decode_scores_roundTripAndOldNextLineStillLoads() {
        TutorLedgerData loaded = Storage.decode(Storage.encode(sampleData()).lines().toList()).data();
        Score score = loaded.getScores().iterator().next();
        assertEquals("A1", score.getScoreId());
        assertEquals("WA1 | retest", score.getTestName());
        assertEquals("17.5/20", score.getMarksText());
        // A2 belonged to the deleted student S3, so its ID must not be handed out again.
        assertEquals(1, loaded.getScores().size());
        assertEquals(3, loaded.getNextScoreNumber());

        // Files saved before scores were added have a NEXT line without a score counter.
        Storage.LoadResult old = Storage.decode(List.of("NEXT | S2 | L1 | H1",
                "S | S1 | Amirah Binte Rahman | Sec 3 | 91234567",
                "A | A4 | S1 | 07-09-2026 | 30 | 40 | WA1",
                "A | A5 | S1 | 07-09-2026 | 50 | 40 | WA2"));
        assertEquals(List.of("Line 4: Marks must be between 0 and the total marks."), old.problems());
        assertEquals(5, old.data().getNextScoreNumber());
    }

    @Test
    void saveThenLoad_newFolders_createsFoldersAndReadsTheFileBack() throws IOException {
        Storage storage = new Storage(tempDir.resolve("data").resolve("tutorledger.txt").toString());
        TutorLedgerData original = sampleData();

        storage.save(original);
        Storage.LoadResult result = storage.load();

        assertTrue(result.problems().isEmpty());
        assertEquals(Storage.encode(original), Storage.encode(result.data()));
    }

    @Test
    void load_fileWithByteOrderMark_ignoresTheMark() throws IOException {
        Path file = tempDir.resolve("bom.txt");
        Files.writeString(file, "﻿S | S1 | Amirah Binte Rahman | Sec 3 | 91234567", StandardCharsets.UTF_8);

        Storage.LoadResult result = new Storage(file.toString()).load();

        assertTrue(result.problems().isEmpty(), result.problems().toString());
        assertEquals(1, result.data().getStudents().size());
    }

    @Test
    void load_missingFile_throwsNoSuchFileException() {
        Storage storage = new Storage(tempDir.resolve("missing.txt").toString());

        assertThrows(NoSuchFileException.class, storage::load);
    }

    @Test
    void splitFields_escapedCharacters_areKeptInsideTheField() {
        assertEquals(List.of("L", "a | b", "c \\ d", "e"), Storage.splitFields("L | a \\| b | c \\\\ d | e"));
        assertEquals(List.of(""), Storage.splitFields("   # a comment"));
    }
}

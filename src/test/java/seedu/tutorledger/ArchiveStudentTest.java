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
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Checks US45's archive lifecycle, history preservation, scheduling boundaries and saved metadata. */
class ArchiveStudentTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-23T10:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path tempDir;

    private TutorLedgerData data;
    private StudentCommands commands;

    @BeforeEach
    void setUp() {
        data = new TutorLedgerData();
        data.addStudent("Alice Tan", "Pri 5", "91234567", List.of("Math"), "60");
        data.addStudent("Bob Tan", "JC 2", "81234567", List.of("Math"), "70");
        commands = new StudentCommands(data, CLOCK);
    }

    @Test
    void archiveAndRestore_keepProfileHistoryAndAllCounters() {
        Student original = data.getStudent("S1");
        Lesson lesson = data.addLesson("S1", "Math", "15-09-2026", "1600", "60");
        Homework homework = data.addHomework("S1", "Worksheet", LocalDate.of(2026, 12, 1));
        Score score = data.addScore("S1", "WA1", "8", "10", "15-09-2026");
        String before = Storage.encode(data);

        assertTrue(commands.execute("ARCHIVE s1").startsWith("Archived S1: Alice Tan"));
        Student archived = data.getStudent("S1");
        assertTrue(archived.isArchived());
        assertFalse(original.isArchived());
        assertEquals(original.getSubjects(), archived.getSubjects());
        assertEquals(original.getDefaultHourlyRate(), archived.getDefaultHourlyRate());
        assertEquals(original.getLevel(), archived.getLevel());
        assertEquals(original.getPhoneNumber(), archived.getPhoneNumber());
        assertSame(lesson, data.getLesson("L1"));
        assertSame(homework, data.getHomeworkById("H1"));
        assertSame(score, data.getScores().iterator().next());
        assertTrue(commands.execute("view S1").contains("Status: archived"));
        assertTrue(commands.execute("restore s1").startsWith("Restored S1:"));
        assertEquals(before, Storage.encode(data), "Restoration must preserve every record and ID counter.");
    }

    @Test
    void profileCopiesAndSubjectEdits_preserveArchiveStateAndRate() {
        commands.execute("archive S1");
        commands.execute("edit S1 n/Alice Lim l/JC1 p/87654321 r/80");
        commands.execute("subject S1 s/Physics");
        assertTrue(data.getStudent("S1").isArchived());
        assertEquals(List.of("Math", "Physics"), data.getStudent("S1").getSubjects());
        assertTrue(commands.execute("view S1").contains("$80.00/hour"));
        commands.execute("edit S1 r/");
        assertTrue(data.getStudent("S1").isArchived());
        assertTrue(data.getStudent("S1").getDefaultHourlyRate().isEmpty());
        assertFalse(data.getStudent("S1").withArchived(false).isArchived());
    }

    @Test
    void list_filtersStatusAndNameWithoutHidingSharedRecords() {
        commands.execute("archive S1");
        assertFalse(commands.execute("list").contains("Alice"));
        assertTrue(commands.execute("list").contains("Bob"));
        assertTrue(commands.execute("list ALICE status/ARCHIVED").contains("Alice Tan (Pri 5)"));
        assertTrue(commands.execute("list status/archived").contains("[archived]"));
        assertFalse(commands.execute("list status/archived").contains("Bob"));
        assertFalse(commands.execute("list status/active").contains("Alice"));
        String all = commands.execute("list tAn STATUS/all");
        assertTrue(all.indexOf("Alice") < all.indexOf("Bob"));
        assertEquals(2, data.findStudents("").size());
        assertEquals(2, data.getStudents().size());
        assertEquals("0 students match \"Alice\":", commands.execute("list Alice"));
        assertEquals("0 students (archived) match \"Nobody\":", commands.execute("list Nobody status/archived"));
    }

    @Test
    void repeatedActions_areNoOpsAndDoNotSaveOverHandEdits() throws IOException {
        commands.execute("archive S1");
        Path file = tempDir.resolve("tutorledger.txt");
        Storage storage = new Storage(file.toString());
        storage.save(data);
        String handEdited = Files.readString(file) + "# Keep this comment\n";
        Files.writeString(file, handEdited);
        StorageCommands persistence = new StorageCommands(data, storage);
        String before = persistence.snapshot();
        assertTrue(commands.execute("archive S1").contains("already archived"));
        assertTrue(commands.execute("restore S2").contains("already active"));
        assertEquals(before, persistence.snapshot());
        assertEquals("", persistence.saveIfChanged(before));
        assertEquals(handEdited, Files.readString(file));
    }

    @Test
    void add_withArchivedProfiles_explainsTotalAndActiveCounts() {
        commands.execute("archive S1");
        commands.execute("archive S2");
        String added = commands.execute("add n/izzaz l/sec 2 p/56781234 s/Chemistry r/50");
        assertTrue(added.endsWith("You now have 3 students in total (1 active, 2 archived)."), added);
        String list = commands.execute("list");
        assertTrue(list.startsWith("1 student:"), list);
        assertTrue(list.contains("izzaz"), list);
        assertFalse(list.contains("Alice"), list);
        commands.execute("restore S1");
        added = commands.execute("add n/Carol Tan l/Sec1 p/87654321");
        assertTrue(added.endsWith("You now have 4 students in total (3 active, 1 archived)."), added);
    }

    @Test
    void invalidCommands_doNotChangeAnyRecords() {
        String before = Storage.encode(data);
        for (String input : List.of("archive", "archive S0", "archive L1", "archive S99", "archive S1 extra",
                "restore", "restore S99", "restore S1 extra", "list status/", "list status/other",
                "list status/all status/archived", "list Alice kind/all", "list status/active s/Math")) {
            assertTrue(commands.execute(input).startsWith("Error:"), input);
            assertEquals(before, Storage.encode(data), input);
        }
    }

    @Test
    void archivedScheduling_rejectsSingleAndRecurringDataCallsWithoutConsumingIds() {
        commands.execute("archive S1");
        String before = Storage.encode(data);
        assertThrows(IllegalArgumentException.class,
                () -> data.addLesson("S1", "Math", "01-12-2026", "1600", "60"));
        assertThrows(IllegalArgumentException.class,
                () -> data.addLesson("S1", "Math", "01-12-2026", "1600", null, 90));
        assertThrows(IllegalArgumentException.class,
                () -> data.addWeeklyLessons("S1", "Math", "01-12-2026", "1600", "60", 3));
        assertThrows(IllegalArgumentException.class,
                () -> data.addWeeklyLessons("S1", "Math", "01-12-2026", "1600", null, 3, 90));
        LessonCommands lessons = new LessonCommands(data, CLOCK);
        assertTrue(lessons.execute("schedule S1 d/01-12-2026 t/1600 s/Math").contains("restore S1"));
        assertTrue(lessons.execute("schedule S1 d/01-12-2026 t/1600 s/Math w/3").contains("restore S1"));
        assertEquals(before, Storage.encode(data));
        commands.execute("restore S1");
        assertTrue(lessons.execute("schedule S1 d/01-12-2026 t/1600 s/Math w/2").startsWith("Scheduled 2"));
        assertEquals(3, data.getNextLessonNumber());
    }

    @Test
    void archive_keepsFutureLessonsVisibleAndPermitsHistoricalMaintenanceAndPayments() {
        data.addLesson("S1", "Math", "01-12-2026", "1600", "60");
        commands.execute("archive S1");
        LessonCommands lessons = new LessonCommands(data, CLOCK);
        assertTrue(lessons.execute("next").contains("S1 Alice Tan"));
        assertTrue(lessons.execute("reschedule L1 d/02-12-2026").startsWith("Rescheduled L1"));
        assertTrue(lessons.execute("log S1 d/15-09-2026 t/1600 s/Math f/60 a/present note/Vectors")
                .startsWith("Logged L2"));
        PaymentCommands payments = new PaymentCommands(data, CLOCK);
        assertTrue(payments.execute("owed").contains("S1 Alice Tan"));
        assertTrue(payments.execute("mark L2 paid").contains("paid"));
        assertTrue(payments.execute("fee_statement S1").contains("L2"));
        String monthlyIncome = payments.execute("income 09-2026");
        assertTrue(monthlyIncome.contains("S1 Alice Tan"), monthlyIncome);
        assertTrue(monthlyIncome.contains("$60.00"), monthlyIncome);
        assertTrue(payments.execute("income 2026").contains("September: $60.00"));
        assertTrue(lessons.execute("edit L2 note/More vectors").startsWith("Edited L2"));
        assertTrue(lessons.execute("cancel L1 r/Student stopped").startsWith("Cancelled L1"));
        assertTrue(data.getStudent("S1").isArchived());
    }

    @Test
    void duplicateIdentity_stillIncludesArchivedStudents() {
        commands.execute("archive S1");
        String before = Storage.encode(data);
        assertTrue(commands.execute("add n/Alice Tan l/Sec1 p/91234567").contains("already exists"));
        assertEquals(before, Storage.encode(data));
    }

    @Test
    void archiveMetadata_roundTripsAllHistoryAndRestorationRemovesMarker() throws IOException {
        data.addLesson("S1", "Math", "15-09-2026", "1600", "60", "present", "Vectors");
        data.addHomework("S1", "Worksheet", LocalDate.of(2026, 12, 1));
        data.addScore("S1", "WA1", "8", "10", "15-09-2026");
        commands.execute("archive S1");
        String before = Storage.encode(data);
        assertTrue(before.contains("ARCHIVED | S1"));
        assertFalse(before.contains("ARCHIVED | S2"));
        Path file = tempDir.resolve("data.txt");
        Storage storage = new Storage(file.toString());
        storage.save(data);
        Storage.LoadResult loaded = storage.load();
        assertTrue(loaded.problems().isEmpty());
        assertEquals(before, Storage.encode(loaded.data()));
        StudentCommands restarted = new StudentCommands(loaded.data(), CLOCK);
        assertTrue(restarted.execute("restore S1").startsWith("Restored S1"));
        assertFalse(Storage.encode(loaded.data()).contains("ARCHIVED | S1"));
    }

    @Test
    void legacyFiles_defaultToActiveAndMetadataCanAppearInAnyOrder() {
        String before = Storage.encode(data);
        Storage.LoadResult legacy = Storage.decode(before.lines().toList());
        assertTrue(legacy.problems().isEmpty());
        assertFalse(legacy.data().getStudent("S1").isArchived());
        for (List<String> metadata : List.of(List.of("ARCHIVED | s1", "R | S1 | 80"),
                List.of("R | S1 | 80", "ARCHIVED | s1"))) {
            List<String> lines = new ArrayList<>(metadata);
            lines.add("L | L1 | S1 | 15-09-2026 | 1600 | Math | 60 | present | unpaid | Vectors");
            lines.add("S | S1 | Alice Tan | Pri 5 | 91234567 | Math");
            Storage.LoadResult loaded = Storage.decode(lines);
            assertTrue(loaded.problems().isEmpty(), loaded.problems().toString());
            assertTrue(loaded.data().getStudent("S1").isArchived());
            assertEquals("80", loaded.data().getStudent("S1").getDefaultHourlyRate().orElseThrow().toPlainString());
            assertEquals(1, loaded.data().getLessons().size());
        }
    }

    @Test
    void malformedDuplicateAndOrphanArchiveMarkers_reportLinesWithoutLosingProfiles() {
        Storage.LoadResult loaded = Storage.decode(List.of(
                "ARCHIVED | S1 | true", "ARCHIVED | S99", "ARCHIVED | wrong", "ARCHIVED | S1",
                "ARCHIVED | s1", "ARCHIVED", "S | S1 | Alice Tan | Pri 5 | 91234567 | Math"));
        assertEquals(5, loaded.problems().size());
        for (int line : List.of(1, 2, 3, 5, 6)) {
            assertTrue(loaded.problems().stream().anyMatch(problem -> problem.startsWith("Line " + line + ":")));
        }
        assertTrue(loaded.data().getStudent("S1").isArchived());
        assertEquals(2, loaded.data().getNextStudentNumber());
    }

    @Test
    void reload_appliesArchiveStatusAndSharedCommandsSeeTheRestoredState() throws IOException {
        Path file = tempDir.resolve("data.txt");
        Storage storage = new Storage(file.toString());
        storage.save(data);
        Files.writeString(file, Files.readString(file) + "ARCHIVED | S1\n");
        StorageCommands persistence = new StorageCommands(data, storage);
        assertTrue(persistence.execute("reload").startsWith("Reloaded"));
        assertFalse(commands.execute("list").contains("Alice"));
        assertTrue(commands.execute("view S1").contains("Status: archived"));
        String before = persistence.snapshot();
        commands.execute("restore S1");
        assertEquals("", persistence.saveIfChanged(before));
        assertFalse(storage.load().data().getStudent("S1").isArchived());
    }

    @Test
    void deletingArchivedStudent_cascadesAndDoesNotReuseIds() {
        data.addLesson("S1", "Math", "15-09-2026", "1600", "60");
        commands.execute("archive S1");
        data.deleteStudent("S1");
        assertTrue(data.getLessons().isEmpty());
        assertFalse(Storage.encode(data).contains("ARCHIVED | S1"));
        assertEquals("S3", data.addStudent("Carol Tan", "Sec 1", "87654321").getStudentId());
    }

    @Test
    void deletingArchivedStudentWithHistory_stillRequiresExplicitConfirmation() {
        data.addLesson("S1", "Math", "15-09-2026", "1600", "60");
        commands.execute("archive S1");
        String before = Storage.encode(data);
        assertTrue(commands.execute("delete S1").startsWith("Warning:"));
        assertEquals(before, Storage.encode(data));
        assertTrue(data.getStudent("S1").isArchived());
        assertTrue(commands.execute("delete S1 confirm").startsWith("Deleted S1:"));
        assertEquals("S2", data.getStudents().iterator().next().getStudentId());
        assertTrue(data.getLessons().isEmpty());
    }

    @Test
    void help_listsLifecycleCommandsAndStatusChoices() {
        String help = commands.execute("help");
        assertTrue(help.contains("archive STUDENT_ID"));
        assertTrue(help.contains("restore STUDENT_ID"));
        assertTrue(help.contains("list [NAME] [status/STATUS]"));
        assertTrue(help.contains("status/active, status/archived or status/all"));
        assertTrue(help.contains("Restore before scheduling new lessons."));
    }
}

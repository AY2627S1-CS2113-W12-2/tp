package seedu.tutorledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Checks US12's derived topic history, date/attendance rules and read-only command behaviour. */
class TopicCommandsTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-23T10:00:00Z"), ZoneOffset.UTC);
    private static final String NL = System.lineSeparator();

    @TempDir
    Path tempDir;

    private TutorLedgerData data;
    private TopicCommands commands;

    @BeforeEach
    void setUp() {
        data = new TutorLedgerData();
        data.addStudent("Alice Tan", "Pri 5", "91234567", List.of("Math", "Physics"), "60");
        data.addStudent("Bob Tan", "JC 2", "81234567", List.of("Math"), "70");
        commands = new TopicCommands(data, CLOCK);
    }

    @Test
    void handles_acceptsOnlyTheTopicsCommandWord() {
        assertTrue(TopicCommands.handles("  TOPICS s1 s/Math  "));
        assertTrue(TopicCommands.handles("topics"));
        for (String input : List.of("", " ", "topic S1", "topicsExtra S1", "next S1", "view S1")) {
            assertFalse(TopicCommands.handles(input), input);
            assertEquals("Unknown topics command.", commands.execute(input));
        }
        assertFalse(TopicCommands.handles(null));
        assertEquals("Unknown topics command.", commands.execute(null));
    }

    @Test
    void topics_listsFullNotesWithDatesSubjectsAndIdsNewestFirst() {
        add("L1", "S1", "Math", "15-09-2026", "1600", "present", "Vectors; worked examples");
        add("L2", "S1", "Physics", "22-10-2026", "1700", "late", "Forces and motion");
        add("L3", "S1", "Math", "22-10-2026", "1800", "present", "Quadratics");
        String before = Storage.encode(data);

        String output = commands.execute("  TOPICS s1  ");

        assertEquals("Topics covered with S1 Alice Tan: 3 lessons" + NL
                + "  L3  Thu 22-10-2026 1800  Math" + NL + "    Quadratics" + NL
                + "  L2  Thu 22-10-2026 1700  Physics" + NL + "    Forces and motion" + NL
                + "  L1  Tue 15-09-2026 1600  Math" + NL + "    Vectors; worked examples", output);
        assertEquals(before, Storage.encode(data));
    }

    @Test
    void topics_onlyIncludesAttendedPastOrTodayLessonsWithNonblankNotes() {
        add("L1", "S1", "Math", "15-09-2026", "1600", "present", "Included paid");
        data.updateLessonPaymentStatus("L1", "paid");
        add("L2", "S1", "Math", "23-10-2026", "1600", "late", "Included today");
        add("L3", "S1", "Math", "15-09-2026", "1700", "absent", "Excluded absent");
        add("L4", "S1", "Math", "15-09-2026", "1800", "not recorded", "Excluded unrecorded");
        add("L5", "S1", "Math", "24-10-2026", "1600", "present", "Excluded future");
        add("L6", "S1", "Math", "15-09-2026", "1900", "present", "   ");
        add("L7", "S1", "Math", "15-09-2026", "2000", "not recorded", "Excluded cancelled");
        data.replaceLesson(data.getLesson("L7").withCancellation("Student away"));
        add("L8", "S2", "Math", "15-09-2026", "2100", "present", "Excluded other student");

        String output = commands.execute("topics S1");

        assertTrue(output.startsWith("Topics covered with S1 Alice Tan: 2 lessons"), output);
        assertTrue(output.contains("Included paid"), output);
        assertTrue(output.contains("Included today"), output);
        assertFalse(output.contains("Excluded"), output);
        assertFalse(output.contains("L6"), output);
    }

    @Test
    void subjectFilter_ignoresCaseAndCurrentEnrolmentButKeepsSubjectsSeparate() {
        add("L1", "S1", "A Math", "15-09-2026", "1600", "present", "Vectors");
        add("L2", "S1", "Physics", "16-09-2026", "1600", "present", "Forces");
        Student original = data.getStudent("S1");
        data.replaceStudent(original.withDetails(original.getName(), original.getLevel(), original.getPhoneNumber(),
                List.of()));

        String output = commands.execute("topics S1 S/a mAtH");

        assertTrue(output.startsWith("Topics covered with S1 Alice Tan (subject: a mAtH): 1 lesson"), output);
        assertTrue(output.contains("Vectors"), output);
        assertFalse(output.contains("Forces"), output);
        assertEquals("No topics recorded for S1 Alice Tan (subject: Biology).",
                commands.execute("topics S1 s/Biology"));
    }

    @Test
    void repeatedNotes_remainSeparateEntriesWithTheirOwnLessonIds() {
        add("L1", "S1", "Math", "15-09-2026", "1600", "present", "Vectors");
        add("L2", "S1", "Math", "16-09-2026", "1600", "present", "Vectors");

        String output = commands.execute("topics S1");

        assertTrue(output.contains("L1") && output.contains("L2"), output);
        assertEquals(2, output.lines().filter(line -> line.equals("    Vectors")).count());
    }

    @Test
    void emptyHistory_reportsStudentIdentityWithoutChangingRecords() {
        String before = Storage.encode(data);
        assertEquals("No topics recorded for S1 Alice Tan.", commands.execute("topics S1"));
        assertEquals(before, Storage.encode(data));
    }

    @Test
    void invalidArguments_reportErrorsAndPreserveRecordsAndCounters() {
        add("L1", "S1", "Math", "15-09-2026", "1600", "present", "Vectors");
        String before = Storage.encode(data);
        for (String input : List.of("topics", "topics S0", "topics S99", "topics L1", "topics S1 S2",
                "topics S1 s/", "topics S1 s/Math s/Physics", "topics S1 from/01-09-2026",
                "topics S1 s/Math pay/paid", "topics s/Math")) {
            assertTrue(commands.execute(input).startsWith("Error:"), input);
            assertEquals(before, Storage.encode(data), input);
        }
    }

    @Test
    void logRecordAndEdit_updateTheDerivedHistoryWithoutSeparateTopicStorage() {
        LessonCommands lessons = new LessonCommands(data, CLOCK);
        assertTrue(lessons.execute("log S1 d/15-09-2026 t/1600 s/Math f/60 a/present note/Vectors")
                .startsWith("Logged L1"));
        assertTrue(commands.execute("topics S1").contains("Vectors"));
        lessons.execute("record L1 a/late note/More vectors");
        assertTrue(commands.execute("topics S1").contains("More vectors"));
        lessons.execute("edit L1 note/");
        assertEquals("No topics recorded for S1 Alice Tan.", commands.execute("topics S1"));
        lessons.execute("edit L1 note/Revision a/present");
        assertTrue(commands.execute("topics S1").contains("Revision"));
        lessons.execute("edit L1 a/absent");
        assertEquals("No topics recorded for S1 Alice Tan.", commands.execute("topics S1"));
    }

    @Test
    void topicsNeverSaveOverHandEditedFiles() throws IOException {
        add("L1", "S1", "Math", "15-09-2026", "1600", "present", "Vectors");
        Path file = tempDir.resolve("data.txt");
        Storage storage = new Storage(file.toString());
        storage.save(data);
        String handEdited = Files.readString(file) + "# Keep this comment\n";
        Files.writeString(file, handEdited);
        StorageCommands persistence = new StorageCommands(data, storage);
        for (String input : List.of("topics S1", "topics S2", "topics S1 s/Math", "topics S1 extra")) {
            String before = persistence.snapshot();
            commands.execute(input);
            assertEquals("", persistence.saveIfChanged(before));
            assertEquals(handEdited, Files.readString(file));
        }
    }

    @Test
    void saveAndRestart_preserveNotesAndTopicOutputIncludingEscapedCharacters() throws IOException {
        add("L1", "S1", "Math", "15-09-2026", "1600", "present", "Vectors | recap \\ examples");
        String before = commands.execute("topics S1");
        Path file = tempDir.resolve("data.txt");
        Storage storage = new Storage(file.toString());
        storage.save(data);

        Storage.LoadResult loaded = storage.load();

        assertTrue(loaded.problems().isEmpty());
        assertEquals(before, new TopicCommands(loaded.data(), CLOCK).execute("topics S1"));
    }

    @Test
    void reloadAndDeletion_changeTheQueryImmediatelyWithoutCachedResults() throws IOException {
        add("L1", "S1", "Math", "15-09-2026", "1600", "present", "Vectors");
        Path file = tempDir.resolve("data.txt");
        Storage storage = new Storage(file.toString());
        storage.save(data);
        Files.writeString(file, Files.readString(file).replace("Vectors", "Quadratics"));
        StorageCommands persistence = new StorageCommands(data, storage);
        persistence.execute("reload");
        assertTrue(commands.execute("topics S1").contains("Quadratics"));
        assertFalse(commands.execute("topics S1").contains("Vectors"));
        data.deleteLesson("L1");
        assertEquals("No topics recorded for S1 Alice Tan.", commands.execute("topics S1"));
        data.deleteStudent("S1");
        assertEquals("Error: No student found with ID S1.", commands.execute("topics S1"));
    }

    @Test
    void help_listsTopicsAndItsSubjectFilter() {
        assertTrue(HelpCommand.getHelpText().contains("topics STUDENT_ID [s/SUBJECT]"));
        assertTrue(HelpCommand.getHelpText().contains("topics S1 s/A Math"));
    }

    /** Imports a lesson fixture without hiding malformed historical combinations behind command validation. */
    private void add(String id, String studentId, String subject, String date, String time, String attendance,
            String notes) {
        data.putLesson(new Lesson(id, studentId, subject, date, time, attendance, "60", notes));
    }
}

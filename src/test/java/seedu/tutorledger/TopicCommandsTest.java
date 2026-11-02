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

/** Checks US12's topic history, US22's topic search, date/attendance rules and read-only behaviour. */
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
        assertTrue(commands.handles("  TOPICS s1 s/Math  "));
        assertTrue(commands.handles("topics"));
        for (String input : List.of("", " ", "topic S1", "topicsExtra S1", "next S1", "view S1")) {
            assertFalse(commands.handles(input), input);
            assertEquals("Unknown command.", commands.execute(input));
        }
        assertFalse(commands.handles(null));
        assertEquals("Unknown command.", commands.execute(null));
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
        for (String input : List.of("topics S1", "topics S2", "topics S1 s/Math", "topics S1 extra",
                "topics k/Vectors", "topics S1 k/vectors s/Math", "topics k/", "topics k/none")) {
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
    void keyword_afterSaveAndRestart_findsTheSameLessonsIncludingEscapedCharacters() throws IOException {
        add("L1", "S1", "Math", "15-09-2026", "1600", "present", "Vectors | recap \\ examples");
        add("L2", "S2", "Math", "16-09-2026", "1600", "present", "Vectors");
        String before = commands.execute("topics k/vectors");
        Path file = tempDir.resolve("data.txt");
        Storage storage = new Storage(file.toString());
        storage.save(data);

        Storage.LoadResult loaded = storage.load();
        TopicCommands reloaded = new TopicCommands(loaded.data(), CLOCK);

        assertTrue(loaded.problems().isEmpty());
        assertEquals(before, reloaded.execute("topics k/vectors"));
        assertTrue(reloaded.execute("topics k/| recap \\").contains("L1"));
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
    void keyword_acrossStudents_listsEveryLessonThatCoveredTheTopicNewestFirst() {
        add("L1", "S1", "Math", "15-09-2026", "1600", "present", "Vectors; worked examples");
        add("L2", "S2", "Math", "20-10-2026", "1700", "late", "Revision of vectors");
        add("L3", "S1", "Physics", "21-10-2026", "1600", "present", "Forces");
        String before = Storage.encode(data);

        String output = commands.execute("topics k/vectors");

        assertEquals("Topics covered with any student (topic: vectors): 2 lessons" + NL
                + "  L2  Tue 20-10-2026 1700  S2 Bob Tan  Math" + NL + "    Revision of vectors" + NL
                + "  L1  Tue 15-09-2026 1600  S1 Alice Tan  Math" + NL + "    Vectors; worked examples", output);
        assertEquals(before, Storage.encode(data));
    }

    @Test
    void keyword_ignoresCaseAndSkipsLessonsThatDidNotCoverTheTopic() {
        add("L1", "S1", "Math", "15-09-2026", "1600", "present", "Completing the SQUARE");
        add("L2", "S1", "Math", "15-09-2026", "1700", "absent", "Completing the square, skipped absent");
        add("L3", "S2", "Math", "24-10-2026", "1600", "present", "Completing the square, skipped future");
        add("L4", "S2", "Math", "15-09-2026", "1800", "present", "Completing the square, skipped cancelled");
        data.replaceLesson(data.getLesson("L4").withCancellation("Teacher sick"));
        add("L5", "S2", "Math", "15-09-2026", "1900", "present", "Simultaneous equations");

        String output = commands.execute("TOPICS K/completing the square");

        assertTrue(output.startsWith("Topics covered with any student (topic: completing the square): 1 lesson"
                + NL), output);
        assertTrue(output.contains("L1") && output.contains("Completing the SQUARE"), output);
        assertFalse(output.contains("skipped") || output.contains("Simultaneous"), output);
    }

    @Test
    void keyword_withStudentAndSubject_narrowsTheSearchAndKeepsTheStudentLayout() {
        add("L1", "S1", "Math", "15-09-2026", "1600", "present", "Vectors");
        add("L2", "S1", "Physics", "16-09-2026", "1600", "present", "Vectors and forces");
        add("L3", "S2", "Math", "17-09-2026", "1600", "present", "Vectors");

        assertEquals("Topics covered with S1 Alice Tan (topic: vectors): 2 lessons" + NL
                + "  L2  Wed 16-09-2026 1600  Physics" + NL + "    Vectors and forces" + NL
                + "  L1  Tue 15-09-2026 1600  Math" + NL + "    Vectors", commands.execute("topics S1 k/vectors"));
        assertEquals("Topics covered with any student (subject: physics, topic: vectors): 1 lesson" + NL
                + "  L2  Wed 16-09-2026 1600  S1 Alice Tan  Physics" + NL + "    Vectors and forces",
                commands.execute("topics k/vectors s/physics"));
        assertEquals("No topics recorded for S2 Bob Tan (subject: Math, topic: forces).",
                commands.execute("topics S2 s/Math k/forces"));
    }

    @Test
    void keyword_noMatchOrSlashWords_reportsTheTopicAsTyped() {
        add("L1", "S1", "Math", "15-09-2026", "1600", "present", "Truth tables and/or logic gates");
        String before = Storage.encode(data);

        assertEquals("No topics recorded for any student (topic: integration).",
                commands.execute("topics k/integration"));
        assertTrue(commands.execute("topics k/and/or logic").startsWith(
                "Topics covered with any student (topic: and/or logic): 1 lesson"));
        assertEquals(before, Storage.encode(data));
    }

    @Test
    void keyword_includesTodayLateAndArchivedStudentsButNotUnrecordedLessons() {
        add("L1", "S1", "Math", "23-10-2026", "0900", "late", "Vectors today");
        add("L2", "S2", "Math", "15-09-2026", "1600", "present", "Vectors with an archived student");
        add("L3", "S1", "Math", "15-09-2026", "1700", "not recorded", "Vectors, skipped unrecorded");
        data.replaceStudent(data.getStudent("S2").withArchived(true));

        String output = commands.execute("topics k/vectors");

        assertEquals("Topics covered with any student (topic: vectors): 2 lessons" + NL
                + "  L1  Fri 23-10-2026 0900  S1 Alice Tan  Math" + NL + "    Vectors today" + NL
                + "  L2  Tue 15-09-2026 1600  S2 Bob Tan  Math" + NL + "    Vectors with an archived student",
                output);
    }

    @Test
    void keyword_lessonsAtTheSameTime_keepAStableLessonIdOrder() {
        add("L1", "S1", "Math", "15-09-2026", "1600", "present", "Vectors");
        add("L2", "S2", "Math", "15-09-2026", "1600", "present", "Vectors");
        add("L3", "S1", "Math", "15-09-2026", "1600", "present", "Vectors");

        List<String> lessonRows = commands.execute("topics k/vectors").lines()
                .filter(line -> line.startsWith("  L")).toList();

        assertEquals(List.of("  L3  Tue 15-09-2026 1600  S1 Alice Tan  Math",
                "  L2  Tue 15-09-2026 1600  S2 Bob Tan  Math",
                "  L1  Tue 15-09-2026 1600  S1 Alice Tan  Math"), lessonRows);
    }

    @Test
    void keyword_partOfAWordSpacesAndSymbols_matchTheNotesLiterally() {
        add("L1", "S1", "Math", "15-09-2026", "1600", "present", "Expanding (a+b)^2 | squares");
        add("L2", "S2", "Math", "16-09-2026", "1600", "present", "Vectors");

        assertTrue(commands.execute("topics s1 k/(A+B)^2").contains("L1"));
        assertTrue(commands.execute("topics k/^2 | sq").contains("L1"));
        assertTrue(commands.execute("topics k/   VECT   ").startsWith(
                "Topics covered with any student (topic: VECT): 1 lesson"));
        // Regular-expression symbols are plain text, so ".*" only matches notes that contain ".*".
        assertEquals("No topics recorded for any student (topic: .*).", commands.execute("topics k/.*"));
    }

    @Test
    void keyword_followsNotesWrittenByLessonCommands() {
        LessonCommands lessons = new LessonCommands(data, CLOCK);
        lessons.execute("log S1 d/15-09-2026 t/1600 s/Math f/60 a/present note/Vectors");
        lessons.execute("log S2 d/16-09-2026 t/1600 s/Math f/60 a/present note/Quadratics");
        assertTrue(commands.execute("topics k/vectors").contains("S1 Alice Tan"));

        lessons.execute("edit L2 note/Vectors recap");
        assertTrue(commands.execute("topics k/vectors").startsWith(
                "Topics covered with any student (topic: vectors): 2 lessons"));
        lessons.execute("edit L1 note/Matrices");
        assertFalse(commands.execute("topics k/vectors").contains("L1"));
    }

    @Test
    void keyword_invalidArguments_reportErrorsAndPreserveRecords() {
        add("L1", "S1", "Math", "15-09-2026", "1600", "present", "Vectors");
        String before = Storage.encode(data);
        for (String input : List.of("topics k/", "topics k/  ", "topics S1 k/", "topics k/a k/b",
                "topics s/Math", "topics vectors", "topics S99 k/vectors", "topics S1 S2 k/vectors",
                "topics pay/paid k/vectors")) {
            assertTrue(commands.execute(input).startsWith("Error:"), input);
            assertEquals(before, Storage.encode(data), input);
        }
        assertEquals("Error: Use: topics STUDENT_ID [s/SUBJECT] [k/KEYWORD], or topics k/KEYWORD [s/SUBJECT]"
                + " to search every student.", commands.execute("topics"));
    }

    @Test
    void help_listsTopicsWithItsSubjectFilterAndTopicSearch() {
        assertTrue(HelpCommand.getHelpText().contains("topics STUDENT_ID [s/SUBJECT] [k/KEYWORD]"));
        assertTrue(HelpCommand.getHelpText().contains("topics S1 s/A Math"));
        assertTrue(HelpCommand.getHelpText().contains("topics k/KEYWORD [s/SUBJECT]"));
        assertTrue(HelpCommand.getHelpText().contains("topics k/vectors"));
    }

    /** Imports a lesson fixture without hiding malformed historical combinations behind command validation. */
    private void add(String id, String studentId, String subject, String date, String time, String attendance,
            String notes) {
        data.putLesson(new Lesson(id, studentId, subject, date, time, attendance, "60", notes));
    }
}

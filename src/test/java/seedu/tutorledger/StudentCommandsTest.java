package seedu.tutorledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Checks the student command workflow and its links to other v1 records. */
class StudentCommandsTest {
    private TutorLedgerData data;
    private StudentCommands commands;

    @BeforeEach
    void setUp() {
        data = new TutorLedgerData();
        Clock clock = Clock.fixed(Instant.parse("2026-09-21T10:00:00Z"), ZoneOffset.UTC);
        commands = new StudentCommands(data, clock);
    }

    @Test
    void addAndListSupportPrimaryAndJuniorCollegeLevels() {
        assertTrue(commands.execute("add n/Alice Tan l/pRi5 p/91234567 r/40").contains("(Pri 5)"));
        assertTrue(commands.execute("add n/Bob Tan l/jc 2 p/81234567").contains("(JC 2)"));
        assertTrue(commands.execute("list").contains("Alice Tan (Pri 5)"));
        assertTrue(commands.execute("list").contains("Bob Tan (JC 2)"));
        assertTrue(commands.execute("view S1").contains("(Pri 5)"));
        assertTrue(commands.execute("view S2").contains("(JC 2)"));
    }

    @Test
    void editAcrossSchoolStagesPreservesStudentDetailsRateAndHistory() {
        commands.execute("add n/Alice Tan l/Pri6 p/91234567 s/Math r/40.50");
        Student original = data.getStudent("S1");
        Lesson lesson = data.addLesson("S1", "Math", "15-09-2026", "1600", "40.50");
        Homework homework = data.addHomework("S1", "Revision", LocalDate.of(2026, 9, 22));
        Score score = data.addScore("S1", "Test", "8", "10", "15-09-2026");
        for (String level : List.of("Sec1", "jC1", "pri2")) {
            assertTrue(commands.execute("edit S1 l/" + level).startsWith("Edited S1"));
            Student updated = data.getStudent("S1");
            assertEquals("S1", updated.getStudentId());
            assertEquals("Alice Tan", updated.getName());
            assertEquals("91234567", updated.getPhoneNumber());
            assertEquals(List.of("Math"), updated.getSubjects());
            assertEquals(new BigDecimal("40.50"), updated.getDefaultHourlyRate().orElseThrow());
            assertSame(lesson, data.getLesson("L1"));
            assertSame(homework, data.getHomeworkById("H1"));
            assertSame(score, data.getScores().iterator().next());
        }
        assertEquals("Pri 2", data.getStudent("S1").getLevel());
        assertEquals("Pri 6", original.getLevel());
    }

    @Test
    void invalidSchoolLevelsDoNotConsumeIdsOrPartiallyEditRecords() {
        commands.execute("add n/Alice Tan l/Pri 5 p/91234567 r/40");
        String before = Storage.encode(data);
        for (String level : List.of("", "Pri0", "Pri7", "Sec6", "JC0", "JC3", "P5", "Poly 1",
                "JC 1 extra", "Pri  5", "Pri5 l/JC1")) {
            assertTrue(commands.execute("add n/Bob Tan l/" + level + " p/81234567 r/60").startsWith("Error:"));
            assertEquals(before, Storage.encode(data));
            assertTrue(commands.execute("edit S1 l/" + level + " p/87654321 r/99").startsWith("Error:"));
            assertEquals(before, Storage.encode(data));
        }
        assertTrue(commands.execute("add n/Bob Tan l/JC2 p/81234567").startsWith("Added S2"));
    }

    @Test
    void addAndListAcceptPrefixesInAnyOrderAndFindNamesIgnoringCase() {
        assertTrue(commands.execute("ADD P/91234567 L/Sec3 N/Tan Wei Ming").contains("Added S1"));
        assertTrue(commands.execute("add n/Tan Wei Ling l/Sec 2 p/81234567").contains("Added S2"));
        assertTrue(commands.execute("list wEi").contains("2 students match"));
        assertEquals("Tan Wei Ming", data.getStudent("s1").getName());
        assertTrue(commands.execute("add n/tan wei ming l/Sec 4 p/91234567").contains("already exists"));
        assertEquals(3, data.getNextStudentNumber());
    }

    @Test
    void addWithSubjectsCreatesTheWholeRecordInOneLine() {
        String output = commands.execute("add n/Tan Wei Ming l/Sec 3 p/91234567 s/E Math s/A Math s/e math");
        assertEquals("Added S1: Tan Wei Ming (Sec 3), 91234567" + System.lineSeparator()
                + "Subjects: E Math, A Math" + System.lineSeparator()
                + "You now have 1 student.", output);
        assertEquals(2, data.getStudent("S1").getSubjects().size());
        // A blank subject is rejected before the student is added, so no half-entered record is left.
        assertTrue(commands.execute("add n/Tan Wei Ling l/Sec 2 p/81234567 s/").startsWith("Error:"));
        assertEquals(1, data.getStudents().size());
        assertEquals(2, data.getNextStudentNumber());
    }

    @Test
    void subjectAndEditAddReplaceAndClearWithoutPartialChanges() {
        commands.execute("add n/Tan Wei Ming l/Sec 3 p/91234567");
        commands.execute("subject s1 s/E Math S/A Math s/e math");
        assertEquals(2, data.getStudent("S1").getSubjects().size());
        commands.execute("edit S1 p/81234567 s/Pure Physics");
        assertEquals("81234567", data.getStudent("S1").getPhoneNumber());
        assertEquals("Pure Physics", data.getStudent("S1").getSubjects().getFirst());
        assertTrue(commands.execute("edit S1 l/Sec 6 s/Chemistry").startsWith("Error:"));
        assertEquals("Sec 3", data.getStudent("S1").getLevel());
        assertEquals("Pure Physics", data.getStudent("S1").getSubjects().getFirst());
        commands.execute("edit S1 s/");
        assertTrue(data.getStudent("S1").getSubjects().isEmpty());
    }

    @Test
    void viewShowsLinkedRecordsAndExcludesFutureFeesAndSubmittedHomework() {
        commands.execute("add n/Tan Wei Ming l/Sec 3 p/91234567");
        data.putLesson(new Lesson("L1", "S1", "E Math", "15-09-2026", "1600",
                "present", "60", "Quadratics"));
        data.putLesson(new Lesson("L2", "S1", "E Math", "22-09-2026", "1600",
                "not recorded", "70", ""));
        data.putHomework(new Homework("H1", "S1", "Worksheet 3B", "22-09-2026", "outstanding"));
        data.putHomework(new Homework("H2", "S1", "Old work", "20-09-2026", "submitted"));
        String view = commands.execute("view s1");
        assertTrue(view.indexOf("L2") < view.indexOf("L1"));
        assertTrue(view.contains("Quadratics"));
        assertTrue(view.contains("H1"));
        assertFalse(view.contains("H2"));
        assertTrue(view.contains("Amount owed: $60.00 (1 lesson)"));
    }

    @Test
    void deleteCascadesAndIdsRemainUnused() {
        commands.execute("add n/Tan Wei Ming l/Sec 3 p/91234567");
        data.putLesson(new Lesson("L1", "S1", "E Math", "15-09-2026", "1600",
                "present", "60", ""));
        data.putHomework(new Homework("H1", "S1", "Worksheet", "22-09-2026", "outstanding"));
        assertTrue(commands.execute("delete s1").contains("1 lesson and 1 homework"));
        assertTrue(data.getLessons().isEmpty());
        assertTrue(data.getHomework().isEmpty());
        assertTrue(commands.execute("add n/Tan Wei Ling l/Sec 2 p/81234567").contains("Added S2"));
    }

    @Test
    void missingValuesUnknownIdsAndDuplicateEditDoNotChangeRecords() {
        assertTrue(commands.execute("add n/Tan Wei Ming l/Sec 3").startsWith("Error:"));
        assertTrue(commands.execute("view S9").startsWith("Error:"));
        commands.execute("add n/Tan Wei Ming l/Sec 3 p/91234567");
        commands.execute("add n/Tan Wei Ling l/Sec 2 p/81234567");
        assertTrue(commands.execute("edit S2 n/tan wei ming p/91234567").contains("already exists"));
        assertEquals("Tan Wei Ling", data.getStudent("S2").getName());
        assertEquals("81234567", data.getStudent("S2").getPhoneNumber());
        assertTrue(commands.execute("subject S2 s/").startsWith("Error:"));
        assertTrue(commands.execute("delete S9").startsWith("Error:"));
    }

    @Test
    void importedCounterPreservesDeletedIdsAfterReload() {
        data.importStudent(new Student("S4", "Tan Wei Ming", "Sec 3", "91234567"));
        data.deleteStudent("S4");
        data.setNextStudentNumber(8);
        assertTrue(commands.execute("add n/Tan Wei Ling l/Sec 2 p/81234567").contains("Added S8"));
    }
}

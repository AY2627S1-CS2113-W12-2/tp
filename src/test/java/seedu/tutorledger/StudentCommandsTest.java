package seedu.tutorledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

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
    void addAndListAcceptPrefixesInAnyOrderAndFindNamesIgnoringCase() {
        assertTrue(commands.execute("ADD P/91234567 L/Sec3 N/Tan Wei Ming").contains("Added S1"));
        assertTrue(commands.execute("add n/Tan Wei Ling l/Sec 2 p/81234567").contains("Added S2"));
        assertTrue(commands.execute("list wEi").contains("2 students match"));
        assertEquals("Tan Wei Ming", data.getStudent("s1").getName());
        assertTrue(commands.execute("add n/tan wei ming l/Sec 4 p/91234567").contains("already exists"));
        assertEquals(3, data.getNextStudentNumber());
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

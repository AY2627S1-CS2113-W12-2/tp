package seedu.tutorledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Checks the assign, submit and homework commands. Today is fixed at Mon 21-09-2026. */
class HomeworkCommandsTest {
    private TutorLedgerData data;
    private HomeworkCommands commands;

    @BeforeEach
    void setUp() {
        data = new TutorLedgerData();
        Clock clock = Clock.fixed(Instant.parse("2026-09-21T10:00:00Z"), ZoneOffset.UTC);
        commands = new HomeworkCommands(data, clock);
        data.addStudent("Amirah Binte Rahman", "Sec 3", "91234567");
        data.addStudent("Tan Wei Ming", "Sec 4", "81234567");
    }

    @Test
    void assignCreatesOutstandingHomeworkWithNextId() {
        String output = commands.execute("assign s1 Worksheet 4A d/29-09-2026");
        assertEquals("Assigned H1 to S1 Amirah Binte Rahman" + System.lineSeparator()
                + "  Worksheet 4A, due Tue 29-09-2026", output);
        assertFalse(data.getHomeworkById("H1").isSubmitted());

        // The description may contain spaces and commas.
        assertTrue(commands.execute("assign S2 TYS 2023 Paper 1, Q1-10 d/26-09-2026").contains("Assigned H2"));
        assertEquals("TYS 2023 Paper 1, Q1-10", data.getHomeworkById("h2").getDescription());
    }

    @Test
    void assignRejectsBadInputWithoutAddingHomework() {
        assertTrue(commands.execute("assign S1 Worksheet").startsWith("Error:"));
        assertTrue(commands.execute("assign S1 Worksheet d/31-02-2026").startsWith("Error:"));
        assertTrue(commands.execute("assign S9 Worksheet d/29-09-2026").startsWith("Error:"));
        assertTrue(data.getHomework().isEmpty());
    }

    @Test
    void submitMarksHomeworkAndRejectsRepeats() {
        commands.execute("assign S1 Worksheet 3B d/22-09-2026");
        assertEquals("Marked H1 as submitted: Worksheet 3B (S1 Amirah Binte Rahman)", commands.execute("submit h1"));
        assertTrue(data.getHomeworkById("H1").isSubmitted());
        assertTrue(commands.execute("submit H1").contains("already submitted"));
        assertTrue(commands.execute("submit H5").startsWith("Error:"));
    }

    @Test
    void homeworkListsOutstandingSoonestFirstAndMarksOverdue() {
        commands.execute("assign S2 Mole concept Qs d/26-09-2026");
        commands.execute("assign S1 Worksheet 3B d/22-09-2026");
        commands.execute("assign S1 Algebra drill 2 d/18-09-2026");
        commands.execute("assign S1 Done already d/20-09-2026");
        commands.execute("submit H4");

        String output = commands.execute("homework");
        String[] lines = output.split(System.lineSeparator());
        assertEquals("3 homework items outstanding", lines[0]);
        assertTrue(lines[1].contains("H3") && lines[1].endsWith("OVERDUE"));
        assertTrue(lines[2].contains("H2") && !lines[2].contains("OVERDUE"));
        assertTrue(lines[3].contains("H1"));
        assertFalse(output.contains("Done already"));

        String forStudent = commands.execute("homework S2");
        assertTrue(forStudent.startsWith("1 homework item outstanding"));
        assertTrue(forStudent.contains("Mole concept Qs"));
        assertFalse(forStudent.contains("Worksheet 3B"));
    }

    @Test
    void handlesOnlyHomeworkCommandWords() {
        assertTrue(HomeworkCommands.handles("ASSIGN S1 x d/01-01-2027"));
        assertTrue(HomeworkCommands.handles("homework"));
        assertFalse(HomeworkCommands.handles("add n/Tan l/Sec 1 p/91234567"));
    }
}

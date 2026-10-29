package seedu.tutorledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
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

    @Test
    void handles_editOrDeleteWithHomeworkId_trueOnlyForHomework() {
        assertTrue(HomeworkCommands.handles("edit H1 d/30-09-2026"));
        assertTrue(HomeworkCommands.handles("DELETE h3"));
        assertFalse(HomeworkCommands.handles("edit S1 n/Tan Wei Ming"));
        assertFalse(HomeworkCommands.handles("delete L1"));
        assertFalse(HomeworkCommands.handles("edit"));
        // h/ would be a prefix, not a homework ID.
        assertFalse(HomeworkCommands.handles("edit h/x"));
        assertFalse(LessonCommands.handles("edit H1 d/30-09-2026"));
    }

    @Test
    void edit_someValues_changesOnlyThoseAndKeepsTheRest() {
        commands.execute("assign S1 Worksheet 4A d/29-09-2026");

        assertEquals("Edited H1 for S1 Amirah Binte Rahman" + System.lineSeparator()
                + "  Worksheet 4A and/or 4B, due Wed 30-09-2026, outstanding",
                commands.execute("edit h1 d/30-09-2026 desc/Worksheet 4A and/or 4B"));
        Homework item = data.getHomeworkById("H1");
        assertEquals("Worksheet 4A and/or 4B", item.getDescription());
        assertEquals(LocalDate.of(2026, 9, 30), item.getDueDate());
        assertEquals("S1", item.getStudentId());
    }

    @Test
    void edit_statusOutstanding_undoesAMistakenSubmit() {
        commands.execute("assign S1 Worksheet 4A d/29-09-2026");
        commands.execute("submit H1");

        assertTrue(commands.execute("edit H1 status/Outstanding").endsWith(", outstanding"));
        assertFalse(data.getHomeworkById("H1").isSubmitted());
        assertTrue(commands.execute("homework").contains("Worksheet 4A"));
        assertTrue(commands.execute("submit H1").startsWith("Marked H1"));
    }

    @Test
    void edit_invalidValues_changesNothing() {
        commands.execute("assign S1 Worksheet 4A d/29-09-2026");

        assertEquals("Error: Give at least one value to edit: desc/, d/ or status/.", commands.execute("edit H1"));
        assertEquals("Error: Due date must be a real date in DD-MM-YYYY format.",
                commands.execute("edit H1 d/31-02-2026"));
        assertEquals("Error: Status must be outstanding or submitted.", commands.execute("edit H1 status/done"));
        assertEquals("Error: Give a non-blank desc/ value.", commands.execute("edit H1 desc/"));
        assertEquals("Error: Unexpected prefix n/.", commands.execute("edit H1 n/Tan Wei Ming"));
        assertEquals("Error: No homework found with ID H9.", commands.execute("edit H9 d/30-09-2026"));
        // One bad value rejects the whole edit, so the valid date is not applied.
        assertTrue(commands.execute("edit H1 d/30-09-2026 status/done").startsWith("Error:"));

        Homework item = data.getHomeworkById("H1");
        assertEquals("Worksheet 4A", item.getDescription());
        assertEquals(LocalDate.of(2026, 9, 29), item.getDueDate());
        assertFalse(item.isSubmitted());
    }

    @Test
    void delete_homeworkId_removesItAndKeepsTheStudent() {
        commands.execute("assign S1 Worksheet 4A d/29-09-2026");

        assertEquals("Deleted H1 for S1 Amirah Binte Rahman: Worksheet 4A, due Tue 29-09-2026",
                commands.execute("delete H1"));
        assertTrue(data.getHomework().isEmpty());
        assertEquals("Amirah Binte Rahman", data.getStudent("S1").getName());
        assertEquals("Error: No homework found with ID H1.", commands.execute("delete H1"));
        assertEquals("Error: Use: delete HOMEWORK_ID", commands.execute("delete H1 confirm"));
    }
}

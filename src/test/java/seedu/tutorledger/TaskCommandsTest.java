package seedu.tutorledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Checks the task, tasks and done commands. Today is fixed at Wed 14-10-2026, 10:00. */
class TaskCommandsTest {
    private static final String NL = System.lineSeparator();

    private TutorLedgerData data;
    private Clock clock;
    private TaskCommands commands;

    @BeforeEach
    void setUp() {
        data = new TutorLedgerData();
        clock = Clock.fixed(Instant.parse("2026-10-14T10:00:00Z"), ZoneOffset.UTC);
        commands = new TaskCommands(data, clock);
        data.addStudent("Amirah Binte Rahman", "Sec 3", "91234567");
        data.addStudent("Tan Wei Ming", "Sec 4", "81234567");
    }

    @Test
    void task_withStudentAndDate_logsPendingTask() {
        assertEquals("Added T1: Bring Worksheet 4B, with answers" + NL
                        + "  for S1 Amirah Binte Rahman, by Tue 20-10-2026",
                commands.execute("task s1 Bring Worksheet 4B, with answers d/20-10-2026"));

        FollowUpTask task = data.getTask("t1");
        assertFalse(task.isDone());
        assertEquals("S1", task.getStudentId().orElseThrow());
    }

    @Test
    void task_studentAndDateAreOptional() {
        assertEquals("Added T1: Print term calendar", commands.execute("TASK Print term calendar"));
        assertEquals("Added T2: Mark S2's script" + NL + "  by Fri 16-10-2026",
                commands.execute("task Mark S2's script d/16-10-2026"));
        assertTrue(data.getTask("T1").getStudentId().isEmpty());
        assertTrue(data.getTask("T1").getDueDate().isEmpty());
    }

    @Test
    void task_invalidInput_showsErrorAndLogsNothing() {
        assertTrue(commands.execute("task").startsWith("Error: Use: task"));
        assertTrue(commands.execute("task S1").startsWith("Error: Use: task"));
        assertTrue(commands.execute("task d/20-10-2026").startsWith("Error: Use: task"));
        assertTrue(commands.execute("task S1 Bring notes d/31-02-2026").contains("real date"));
        assertTrue(commands.execute("task S1 Bring notes d/").contains("real date"));
        assertTrue(commands.execute("task S9 Bring notes").contains("No student found with ID S9"));
        assertTrue(data.getTasks().isEmpty());
    }

    @Test
    void tasks_listsPendingSoonestFirstUndatedLastAndMarksOverdue() {
        commands.execute("task Print term calendar");
        commands.execute("task S1 Bring Worksheet 4B d/20-10-2026");
        commands.execute("task S2 Return graded test d/12-10-2026");
        commands.execute("task S1 Already done d/13-10-2026");
        commands.execute("done T4");

        String[] lines = commands.execute("tasks").split(NL);

        assertEquals("3 follow-up tasks pending", lines[0]);
        assertTrue(lines[1].startsWith("  T3") && lines[1].endsWith("by Mon 12-10-2026  OVERDUE"), lines[1]);
        assertTrue(lines[2].startsWith("  T2") && lines[2].endsWith("by Tue 20-10-2026"), lines[2]);
        assertTrue(lines[3].startsWith("  T1") && lines[3].endsWith("no date"), lines[3]);
        assertEquals(4, lines.length);
    }

    @Test
    void tasks_forOneStudent_showsOnlyTheirPendingTasks() {
        commands.execute("task S1 Bring Worksheet 4B");
        commands.execute("task S2 Return graded test");
        commands.execute("task Print term calendar");

        String output = commands.execute("tasks s1");

        assertTrue(output.startsWith("1 follow-up task pending for S1 Amirah Binte Rahman"), output);
        assertTrue(output.contains("Bring Worksheet 4B"));
        assertFalse(output.contains("Return graded test") || output.contains("Print term calendar"));
        commands.execute("done T2");
        assertEquals("0 follow-up tasks pending for S2 Tan Wei Ming", commands.execute("tasks S2"));
        assertTrue(commands.execute("tasks S9").startsWith("Error:"));
        assertTrue(commands.execute("tasks S1 S2").startsWith("Error: Use: tasks"));
    }

    @Test
    void done_marksTaskDoneAndRejectsRepeatsAndUnknownIds() {
        commands.execute("task S1 Bring Worksheet 4B");

        assertEquals("Marked T1 as done: Bring Worksheet 4B", commands.execute("done t1"));
        assertTrue(data.getTask("T1").isDone());
        assertTrue(commands.execute("done T1").contains("already done"));
        assertTrue(commands.execute("done T9").contains("No follow-up task found with ID T9"));
        assertTrue(commands.execute("done 1").contains("must look like T1"));
        assertTrue(commands.execute("done").startsWith("Error: Use: done"));
        assertEquals("0 follow-up tasks pending", commands.execute("tasks"));
    }

    @Test
    void next_showsPendingFollowUpsForTheStudentOnlyWhenThereAreSome() {
        LessonCommands lessons = new LessonCommands(data, clock);
        data.addLesson("S1", "A Math", "15-10-2026", "1600", "60");
        assertFalse(lessons.execute("next").contains("Follow-ups"));

        commands.execute("task S1 Bring Worksheet 4B d/15-10-2026");
        commands.execute("task S1 Print past paper");
        commands.execute("task S2 Not for Amirah");
        commands.execute("task S1 Already done");
        commands.execute("done T4");

        assertTrue(lessons.execute("next").endsWith(
                "  Follow-ups: T1 Bring Worksheet 4B (by Thu 15-10-2026); T2 Print past paper"),
                lessons.execute("next"));
    }

    @Test
    void deleteStudent_alsoDeletesTheirTasksButKeepsOthers() {
        commands.execute("task S1 Bring Worksheet 4B");
        commands.execute("task S1 Print past paper");
        commands.execute("task Print term calendar");

        StudentCommands students = new StudentCommands(data, clock);
        // S1 has no lessons, but deleting them would lose two reminders, so a warning comes first.
        String warning = students.execute("delete S1");
        assertTrue(warning.startsWith("Warning: deleting S1:"), warning);
        assertTrue(warning.contains("  2 follow-up tasks." + System.lineSeparator()), warning);
        assertEquals(3, data.getTasks().size());

        String output = students.execute("delete S1 confirm");

        assertTrue(output.endsWith("Also deleted 2 follow-up tasks."), output);
        assertEquals(1, data.getTasks().size());
        // The task counter is not reset, so the deleted IDs are never handed out again.
        assertEquals("Added T4: Call parent", commands.execute("task Call parent"));
    }

    @Test
    void handles_onlyTaskCommandWords() {
        assertTrue(TaskCommands.handles("TASK Bring notes"));
        assertTrue(TaskCommands.handles("tasks"));
        assertTrue(TaskCommands.handles("done T1"));
        assertFalse(TaskCommands.handles("homework"));
        assertFalse(TaskCommands.handles(" "));
    }
}

package seedu.tutorledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Checks the flag, unflag and flagged commands (US14). Today is fixed at Wed 14-10-2026, 10:00. */
class StudentFlagTest {
    private static final String NL = System.lineSeparator();

    private TutorLedgerData data;
    private Clock clock;
    private StudentCommands commands;

    @BeforeEach
    void setUp() {
        data = new TutorLedgerData();
        clock = Clock.fixed(Instant.parse("2026-10-14T10:00:00Z"), ZoneOffset.UTC);
        commands = new StudentCommands(data, clock);
        data.addStudent("Amirah Binte Rahman", "Sec 3", "91234567");
        data.addStudent("Tan Wei Ming", "Sec 4", "81234567");
    }

    @Test
    void flag_withReason_flagsStudentAndKeepsOtherDetails() {
        data.replaceStudent(data.getStudent("S1").withDefaultHourlyRate("60"));

        assertEquals("Flagged S1: Amirah Binte Rahman (Sec 3) as needing extra attention." + NL
                        + "Reason: Struggling with vectors | mid-year",
                commands.execute("FLAG s1   Struggling with vectors | mid-year  "));

        Student student = data.getStudent("S1");
        assertTrue(student.isFlagged());
        assertEquals("Struggling with vectors | mid-year", student.getAttentionReason().orElseThrow());
        // Flagging is a separate state, so the rate set earlier must survive it.
        assertTrue(student.getDefaultHourlyRate().isPresent());
        assertFalse(data.getStudent("S2").isFlagged());
    }

    @Test
    void flag_withoutReasonOrAgain_isAcceptedAndReplacesTheReason() {
        assertTrue(commands.execute("flag S1").endsWith("Reason: (no reason given)"));
        assertEquals("", data.getStudent("S1").getAttentionReason().orElseThrow());

        String again = commands.execute("flag S1 Exam in 2 weeks");
        assertTrue(again.startsWith("Updated flag on S1"), again);
        assertEquals("Exam in 2 weeks", data.getStudent("S1").getAttentionReason().orElseThrow());
    }

    @Test
    void flag_invalidInput_showsErrorAndFlagsNobody() {
        assertEquals("Error: Use: flag STUDENT_ID [REASON]", commands.execute("flag"));
        assertEquals("Error: Give a student ID such as S1.", commands.execute("flag Amirah needs help"));
        assertEquals("Error: No student found with ID S9.", commands.execute("flag S9 Weak at algebra"));
        assertTrue(data.getStudents().stream().noneMatch(Student::isFlagged));
    }

    @Test
    void unflag_removesFlagAndReportsWhenThereIsNone() {
        commands.execute("flag S1 Weak at algebra");

        assertEquals("Removed the flag on S1: Amirah Binte Rahman (Sec 3).", commands.execute("unflag S1"));
        assertFalse(data.getStudent("S1").isFlagged());
        assertEquals("S1 Amirah Binte Rahman is not flagged.", commands.execute("unflag S1"));
        assertEquals("Error: Give a student ID such as S1.", commands.execute("unflag"));
    }

    @Test
    void flagged_listsActiveStudentsFirstAndArchivedOnesLast() {
        assertEquals("0 students flagged as needing extra attention.", commands.execute("flagged"));
        data.addStudent("Ryan Koh", "Sec 1", "87654321");
        commands.execute("flag S1 Weak at algebra");
        commands.execute("flag S3");
        commands.execute("archive S1");

        String[] lines = commands.execute("flagged").split(NL);

        assertEquals("2 students flagged as needing extra attention:", lines[0]);
        assertEquals("  S3  Ryan Koh (Sec 1)  (no reason given)", lines[1]);
        assertEquals("  S1  Amirah Binte Rahman (Sec 3)  Weak at algebra  [archived]", lines[2]);
        assertEquals(3, lines.length);
        assertEquals("Error: Use: flagged", commands.execute("flagged S1"));
    }

    @Test
    void listAndView_showTheFlagOnlyForFlaggedStudents() {
        commands.execute("flag S2 Exam in 2 weeks");

        String list = commands.execute("list");
        assertTrue(list.contains("Tan Wei Ming (Sec 4)  none  [flagged]"), list);
        assertFalse(list.contains("Amirah Binte Rahman (Sec 3)  none  [flagged]"), list);
        assertTrue(commands.execute("view S2").contains(NL + "Flagged: Exam in 2 weeks" + NL));
        assertFalse(commands.execute("view S1").contains("Flagged"));
    }

    @Test
    void next_showsTheFlagReasonForAFlaggedStudent() {
        LessonCommands lessons = new LessonCommands(data, clock);
        data.addLesson("S1", "A Math", "15-10-2026", "1600", "60");
        assertFalse(lessons.execute("next").contains("Flagged"));

        commands.execute("flag S1 Weak at algebra");

        assertTrue(lessons.execute("next").contains(NL + "  Flagged: Weak at algebra" + NL + "  Subject: A Math"),
                lessons.execute("next"));
    }

    @Test
    void editOrArchive_keepsTheFlag() {
        commands.execute("flag S1 Weak at algebra");
        commands.execute("edit S1 p/82345678 r/50");
        commands.execute("archive S1");
        commands.execute("restore S1");

        assertEquals("Weak at algebra", data.getStudent("S1").getAttentionReason().orElseThrow());
    }
}

package seedu.tutorledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Checks the score, scores and progress commands. Today is fixed at Wed 14-10-2026. */
class ScoreCommandsTest {
    private static final String NL = System.lineSeparator();

    private TutorLedgerData data;
    private ScoreCommands commands;

    @BeforeEach
    void setUp() {
        data = new TutorLedgerData();
        Clock clock = Clock.fixed(Instant.parse("2026-10-14T10:00:00Z"), ZoneOffset.UTC);
        commands = new ScoreCommands(data, clock);
        data.addStudent("Amirah Binte Rahman", "Sec 3", "91234567");
        data.addStudent("Tan Wei Ming", "Sec 4", "81234567");
    }

    /** Records three tests for S1: 70.0%, then 55.0%, then 82.5% today. */
    private void recordThreeScores() {
        commands.execute("score S1 WA1 m/28/40 d/07-09-2026");
        commands.execute("score S1 Mid-year exam m/55/100 d/21-09-2026");
        commands.execute("score S1 WA2 m/33/40");
    }

    @Test
    void score_validInput_recordsScoreAndComparesWithPreviousTest() {
        assertEquals("Recorded A1 for S1 Amirah Binte Rahman" + NL + "  WA1: 28/40 (70.0%) on Mon 07-09-2026",
                commands.execute("score s1 WA1 m/28/40 d/07-09-2026"));

        String second = commands.execute("SCORE S1 Mid-year exam d/21-09-2026 m/55/100");
        assertTrue(second.contains("Mid-year exam: 55/100 (55.0%)"), second);
        assertTrue(second.endsWith("Down 15.0 points from WA1 (70.0%)"), second);

        // Without d/, the score is dated today; half marks are kept exactly.
        String third = commands.execute("score S1 WA2 m/33.5/40");
        assertTrue(third.contains("(83.8%) on Wed 14-10-2026"), third);
        assertEquals(3, data.getScores().size());
    }

    @Test
    void score_invalidInput_showsErrorAndRecordsNothing() {
        assertTrue(commands.execute("score S1 WA1").startsWith("Error: Use: score"));
        assertTrue(commands.execute("score S1 m/28/40").startsWith("Error: Use: score"));
        assertTrue(commands.execute("score S1 WA1 m/28").contains("MARKS/TOTAL"));
        assertTrue(commands.execute("score S1 WA1 m/41/40").contains("between 0 and the total"));
        assertTrue(commands.execute("score S1 WA1 m/28/0").contains("more than 0"));
        assertTrue(commands.execute("score S1 WA1 m/abc/40").startsWith("Error:"));
        assertTrue(commands.execute("score S1 WA1 m/28/40 d/31-02-2026").startsWith("Error:"));
        assertTrue(commands.execute("score S9 WA1 m/28/40").contains("No student found with ID S9"));
        assertTrue(data.getScores().isEmpty());
    }

    @Test
    void scores_listsOldestFirstWithChangesAndTrend() {
        recordThreeScores();
        commands.execute("score S2 WA1 m/10/40 d/07-09-2026");

        String[] lines = commands.execute("scores S1").split(NL);

        assertEquals("3 scores for S1 Amirah Binte Rahman", lines[0]);
        assertTrue(lines[1].contains("A1") && lines[1].contains("70.0%"), lines[1]);
        assertTrue(lines[2].contains("A2") && lines[2].endsWith("-15.0"), lines[2]);
        assertTrue(lines[3].contains("A3") && lines[3].endsWith("+27.5"), lines[3]);
        assertEquals("Trend: improving, up 12.5 points from the first test to the latest", lines[4]);
        assertEquals(5, lines.length);
    }

    @Test
    void scores_fewerThanTwoScores_explainsHowToSeeATrend() {
        assertEquals("0 scores for S2 Tan Wei Ming", commands.execute("scores S2"));
        commands.execute("score S2 WA1 m/10/40");
        assertTrue(commands.execute("scores S2").endsWith("record at least 2 scores to see a trend"));
        assertTrue(commands.execute("scores").startsWith("Error:"));
    }

    @Test
    void progress_summarisesAttendanceAndScoresUpToToday() {
        recordThreeScores();
        addLesson("07-09-2026", "present");
        addLesson("14-09-2026", "late");
        addLesson("21-09-2026", "absent");
        addLesson("28-09-2026", null);
        // A lesson after today has not happened yet, so it must not count as unrecorded.
        addLesson("21-10-2026", null);

        String output = commands.execute("progress S1");

        assertTrue(output.startsWith("Progress for S1 Amirah Binte Rahman, up to Wed 14-10-2026"), output);
        assertTrue(output.contains("Attendance: attended 2 of 3 recorded lessons (66.7%)"), output);
        assertTrue(output.contains("1 present, 1 late, 1 absent"), output);
        assertTrue(output.contains("1 lesson not recorded yet"), output);
        assertTrue(output.contains("Scores: 3 tests, average 69.2%"), output);
        assertTrue(output.endsWith("Trend: improving, up 12.5 points from the first test to the latest"), output);
    }

    @Test
    void progress_withPeriod_countsOnlyRecordsInsideIt() {
        recordThreeScores();
        addLesson("07-09-2026", "present");
        addLesson("21-09-2026", "absent");

        String output = commands.execute("progress S1 from/15-09-2026 to/30-09-2026");

        assertTrue(output.startsWith("Progress for S1 Amirah Binte Rahman, Tue 15-09-2026 to Wed 30-09-2026"), output);
        assertTrue(output.contains("attended 0 of 1 recorded lesson (0.0%)"), output);
        assertTrue(output.contains("Scores: 1 test, average 55.0%"), output);
        assertFalse(output.contains("WA1"), output);
        assertTrue(output.contains("record at least 2 scores"), output);
    }

    @Test
    void progress_noRecordsOrBadPeriod_isHandled() {
        String empty = commands.execute("progress S2");
        assertTrue(empty.contains("Attendance: no recorded lessons in this period"), empty);
        assertTrue(empty.contains("Scores: none in this period"), empty);

        assertTrue(commands.execute("progress S1 from/30-09-2026 to/01-09-2026").contains("must not be after"));
        assertTrue(commands.execute("progress S1 week/1").startsWith("Error: Unexpected prefix"));
        assertTrue(commands.execute("progress").startsWith("Error: Use: progress"));
    }

    @Test
    void deleteStudent_alsoDeletesTheirScores() {
        recordThreeScores();
        commands.execute("score S2 WA1 m/10/40");

        Clock clock = Clock.fixed(Instant.parse("2026-10-14T10:00:00Z"), ZoneOffset.UTC);
        StudentCommands students = new StudentCommands(data, clock);
        // Scores alone are enough to need confirmation, because they are deleted with the student.
        assertTrue(students.execute("delete S1").contains("3 scores."));
        assertEquals(4, data.getScores().size());
        String output = students.execute("delete S1 confirm");

        assertTrue(output.endsWith("Also deleted 3 scores."), output);
        assertEquals(1, data.getScores().size());
        assertTrue(students.execute("delete S2 confirm").endsWith("Also deleted 1 score."));
        // A student without scores keeps the usual two-line message.
        data.addStudent("Ryan Koh", "Sec 1", "87654321");
        assertFalse(new StudentCommands(data, clock).execute("delete S3").contains("score"));
    }

    @Test
    void handles_onlyScoreCommandWords() {
        assertTrue(ScoreCommands.handles("SCORE S1 WA1 m/1/2"));
        assertTrue(ScoreCommands.handles("scores S1"));
        assertTrue(ScoreCommands.handles("progress S1"));
        assertFalse(ScoreCommands.handles("record L1 a/present"));
        assertFalse(ScoreCommands.handles(" "));
    }

    /** Adds an S1 A Math lesson at 1600 on {@code date}, recording attendance if it is not null. */
    private void addLesson(String date, String attendance) {
        Lesson lesson = data.addLesson("S1", "A Math", date, "1600", "60");
        if (attendance != null) {
            data.replaceLesson(lesson.withDetails(null, null, null, attendance, null, null));
        }
    }
}

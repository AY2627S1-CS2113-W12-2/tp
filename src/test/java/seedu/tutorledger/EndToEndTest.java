package seedu.tutorledger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Runs whole sessions through the real command loop, {@link TutorLedger#run(Storage)}, with a temporary data
 * file, the way a tutor would use the app: typed commands in, printed text out, and the file saved between
 * sessions. Unit and command tests check each class on its own; these tests check that the pieces work
 * together, including routing, aliases and automatic saving.
 *
 * <p>The command loop uses the real clock, so past lessons are dated in 2020 and future lessons in 2099.
 * That keeps every result the same whatever day the tests are run.
 */
class EndToEndTest {
    private static final String NL = System.lineSeparator();

    @TempDir
    Path tempDir;

    /** Runs one session: the commands, then exit. Returns everything printed. */
    private static String session(Path dataFile, String... commands) {
        String[] withExit = new String[commands.length + 1];
        System.arraycopy(commands, 0, withExit, 0, commands.length);
        withExit[commands.length] = "exit";
        return TutorLedgerCommandRoutingTest.runCommands(dataFile, withExit);
    }

    /**
     * Saves a small set of records that every kind of command can act on: S1 with a past lesson L1, a future
     * lesson L2, homework H1, score A1 and task T1, and S2 with no records.
     */
    private Path seededFile(String name) {
        Path dataFile = tempDir.resolve(name);
        String output = session(dataFile,
                "add n/Alice Tan l/Pri 5 p/91234567 s/Math r/40",
                "add n/Bob Lee l/JC 2 p/81234567 s/Chem",
                "log S1 d/06-10-2020 t/1600 s/Math f/40 a/present note/Fractions",
                "schedule S1 d/07-01-2099 t/1600 s/Math",
                "assign S1 Worksheet 4A d/10-01-2099",
                "score S1 WA1 m/28/40 d/07-09-2020",
                "task S1 Bring worksheet");
        assertFalse(output.contains("Error"), output);
        return dataFile;
    }

    @Test
    void fullWorkflow_everyCommandWorksTogetherAndSurvivesRestarts() throws IOException {
        Path dataFile = tempDir.resolve("tutorledger.txt");

        // Session 1: a tutor sets up two students and uses every feature once.
        String first = session(dataFile,
                "add n/Alice Tan l/Pri 5 p/91234567 s/Math r/40",
                "add n/Bob Lee l/JC 2 p/81234567",
                "subject S2 s/Chem",
                "edit S2 r/50",
                "log S1 d/06-10-2020 t/1600 s/Math f/40 a/present note/Fractions",
                "log S1 d/13-10-2020 t/1600 s/Math f/40 a/late",
                "schedule S1 d/07-01-2099 t/1600 s/Math w/2",
                "schedule S2 d/08-01-2099 t/1000 s/Chem dur/90",
                "reschedule L4 d/15-01-2099 t/1700",
                "cancel L5 r/Exam week",
                "uncancel L5",
                "cancel L5 r/Exam week",
                "edit L3 t/1630",
                "record L2 a/present note/Ratio",
                "delete L4",
                "filter S1",
                "next",
                "topics S1",
                "topics k/fractions",
                "assign S1 Worksheet 4A d/10-01-2099",
                "assign S2 Essay d/11-01-2099",
                "submit H2",
                "edit H1 desc/Worksheet 4B",
                "homework",
                "mark L1 paid",
                "pay S1 15 L2",
                "unpaid",
                "owed",
                "fee_statement S1",
                "income 10-2020",
                "score S1 WA1 m/28/40 d/07-09-2020",
                "score S1 WA2 m/33/40 d/07-10-2020",
                "scores S1",
                "progress S1 to/31-12-2020",
                "task S1 Bring worksheet d/09-01-2099",
                "task Print calendar",
                "done T2",
                "tasks",
                "flag S1 Weak at fractions",
                "archive S2",
                "list status/all");

        assertFalse(first.contains("Error"), first);
        assertFalse(first.contains("Unknown command"), first);
        assertTrue(first.contains("Scheduled 2 weekly lessons for S1 Alice Tan: L3 to L4"), first);
        // The rate set with edit is used for the 90-minute lesson: $50/hour for 1.5 hours.
        assertTrue(first.contains("Thu 08-01-2099 1000 (90 min), Chem, $75.00"), first);
        assertTrue(first.contains("To:   Thu 15-01-2099 1700"), first);
        assertTrue(first.contains("Deleted L4 for S1 Alice Tan"), first);
        // next skips the cancelled L5 and picks up the notes recorded for L2.
        assertTrue(first.contains("Next lesson: L3, Wed 07-01-2099 1630" + NL), first);
        assertTrue(first.contains("Last topic: Ratio (L2, Tue 13-10-2020)"), first);
        assertTrue(first.contains("Topics covered with any student (topic: fractions): 1 lesson" + NL
                + "  L1  Tue 06-10-2020 1600  S1 Alice Tan  Math" + NL + "    Fractions"), first);
        assertTrue(first.contains("1 homework item outstanding" + NL + "  H1  S1 Alice Tan  Worksheet 4B"), first);
        assertTrue(first.contains("L2  Tue 13-10-2020  S1 Alice Tan  Math  $25.00"), first);
        assertTrue(first.contains("S1 Alice Tan  $25.00  (1 lesson)"), first);
        assertTrue(first.contains("partial ($15.00 paid)"), first);
        assertTrue(first.contains("Income for October 2020: $55.00"), first);
        assertTrue(first.contains("Up 12.5 points from WA1 (70.0%)"), first);
        assertTrue(first.contains("Attendance: attended 2 of 2 recorded lessons (100.0%)"), first);
        assertTrue(first.contains("Scores: 2 tests, average 76.3%"), first);
        assertTrue(first.contains("1 follow-up task pending" + NL
                + "  T1  Bring worksheet  S1 Alice Tan  by Fri 09-01-2099"), first);
        assertTrue(first.contains("S1  Alice Tan (Pri 5)  Math  [flagged]"), first);
        assertTrue(first.contains("S2  Bob Lee (JC 2)  Chem  [archived]"), first);

        // Session 2: only display commands, so everything must come back from the file, and the file
        // must not be rewritten (a tutor's hand edits would otherwise be lost).
        byte[] savedAfterFirst = Files.readAllBytes(dataFile);
        String second = session(dataFile, "flagged", "view S1", "next S1", "tasks S1", "scores S1",
                "homework S1", "list status/archived", "fee_statement S1", "help", "topics k/RATIO");

        assertTrue(second.contains("Loaded 2 students, 4 lessons and 2 homework items"), second);
        assertTrue(second.contains("S1  Alice Tan (Pri 5)  Weak at fractions"), second);
        assertTrue(second.contains("Flagged: Weak at fractions"), second);
        assertTrue(second.contains("L2  Tue 13-10-2020 1600  Math  $40.00  present  partial  $15.00 paid"), second);
        assertTrue(second.contains("Follow-ups: T1 Bring worksheet (by Fri 09-01-2099)"), second);
        assertTrue(second.contains("2 scores for S1 Alice Tan"), second);
        assertTrue(second.contains("Topics covered with any student (topic: RATIO): 1 lesson" + NL
                + "  L2  Tue 13-10-2020 1600  S1 Alice Tan  Math" + NL + "    Ratio"), second);
        assertTrue(second.contains("S2  Bob Lee (JC 2)  Chem  [archived]"), second);
        assertFalse(second.contains("Error"), second);
        assertArrayEquals(savedAfterFirst, Files.readAllBytes(dataFile));

        // Session 3: changes after a restart, ending with a confirmed deletion.
        String third = session(dataFile, "unflag S1", "restore S2", "delete S1", "delete S1 confirm",
                "add n/Cara Ng l/Sec 1 p/87654321", "list status/all");

        assertTrue(third.contains("Warning: deleting S1: Alice Tan (Pri 5) will permanently remove:" + NL
                + "  3 lessons, 1 homework item and 2 scores." + NL + "  1 follow-up task."), third);
        assertTrue(third.contains("Also deleted 3 lessons and 1 homework item." + NL
                + "Also deleted 2 scores." + NL + "Also deleted 1 follow-up task."), third);
        // IDs are never reused, even across restarts: the new student is S3, not S1.
        assertTrue(third.contains("Added S3: Cara Ng (Sec 1), 87654321"), third);
        assertTrue(third.contains("2 students (all):" + NL + "  S2  Bob Lee (JC 2)  Chem" + NL
                + "  S3  Cara Ng (Sec 1)  none"), third);

        List<String> records = Files.readAllLines(dataFile).stream().filter(line -> !line.startsWith("#")).toList();
        assertEquals("NEXT | S4 | L6 | H3 | A3 | T3", records.getFirst());
        assertTrue(records.stream().noneMatch(line -> line.contains("| S1 |") || line.startsWith("FLAG")),
                String.join(NL, records));
    }

    /**
     * Equivalence partitioning over every command: each input below is invalid in a different way. Each one is
     * run in its own session, so a failure names the exact command. Every one must print an error and leave
     * the data file exactly as it was, because invalid input must never change a tutor's records.
     */
    @Test
    void invalidInput_everyCommandShowsAnErrorAndLeavesTheFileUnchanged() throws IOException {
        Path dataFile = seededFile("invalid.txt");
        byte[] before = Files.readAllBytes(dataFile);
        List<String> invalidCommands = List.of(
                // Students
                "add n/Alice Tan l/Pri 5 p/91234567",
                "add n/Ann Ho l/Sec 6 p/91234567",
                "add n/Ann Ho l/Sec 1 p/9123456",
                "add n/Ann Ho l/Sec 1 p/91234567 r/1.234",
                "add n/Ann Ho n/Bo Ho l/Sec 1 p/91234567",
                "add n/Ann Ho l/Sec 1 p/91234567 x/1",
                "add n/Ann2 l/Sec 1 p/91234567",
                "add l/Sec 1 p/91234567",
                "subject S1",
                "subject S9 s/Chem",
                "view S9",
                "view Alice",
                "list status/old",
                "edit S1",
                "edit S1 l/JC3",
                "edit S1 s/Math s/",
                "archive S9",
                "restore",
                "flag",
                "flag S9 Weak at algebra",
                "flag Alice Weak at algebra",
                "unflag S0",
                "flagged now",
                "delete S9",
                "delete S1 yes",
                // Lessons
                "schedule S1 d/31-02-2099 t/1600 s/Math",
                "schedule S1 d/08-01-2099 t/2400 s/Math",
                "schedule S1 d/08-01-2099 t/1600 s/Math dur/0",
                "schedule S1 d/08-01-2099 t/1600 s/Math dur/1441",
                "schedule S1 d/07-01-2099 t/1630 s/Math",
                "schedule S9 d/08-01-2099 t/1600 s/Math",
                "log S1 d/01-01-2099 t/1600 s/Math f/40 a/present",
                "log S1 d/01-01-2020 t/1600 s/Math f/40 a/sick",
                "log S1 d/01-01-2020 t/1600 s/Math a/present",
                "lessons month",
                "filter pay/maybe",
                "filter from/02-01-2020 to/01-01-2020",
                "next S9",
                "topics",
                "topics k/",
                "topics s/Math",
                "topics S9 k/fractions",
                "record L2 a/present",
                "record L1 a/maybe",
                "reschedule L1 d/01-02-2099",
                "reschedule L2",
                "cancel L1 r/Sick",
                "cancel L2",
                "uncancel L2",
                "edit L2 a/present",
                "edit L1 dur/-1",
                "delete L99",
                // Homework
                "assign S1 d/10-01-2099",
                "assign S1 Essay d/30-02-2099",
                "assign S9 Essay d/10-01-2099",
                "submit H9",
                "edit H1 status/maybe",
                "delete H9",
                // Payments
                "mark L1 maybe",
                "mark L99 paid",
                "pay S1 -1 L1",
                "pay S1 10",
                "fee_statement",
                "income 13-2020",
                "income 20",
                // Scores and progress
                "score S1 WA2 m/41/40",
                "score S1 WA2 m/1/0",
                "score S1 WA2 m/28",
                "scores",
                "progress S1 from/02-01-2020 to/01-01-2020",
                // Follow-up tasks
                "task",
                "task S9 Call parents",
                "done T9",
                "done 1",
                "tasks S1 S2");

        for (String command : invalidCommands) {
            String output = session(dataFile, command);
            assertTrue(output.contains("Error:"), "Expected an error for: " + command + NL + output);
            assertArrayEquals(before, Files.readAllBytes(dataFile), "The data file changed after: " + command);
        }
    }

    @Test
    void unknownCommands_areReportedAndChangeNothing() throws IOException {
        Path dataFile = seededFile("unknown.txt");
        byte[] before = Files.readAllBytes(dataFile);

        String output = session(dataFile, "xyz", "flags", "lesson", "taskss S1", "");

        assertTrue(output.contains("Unknown command: xyz."), output);
        assertTrue(output.contains("Unknown command: flags."), output);
        assertTrue(output.contains("Unknown command: lesson."), output);
        assertTrue(output.contains("Unknown command: taskss."), output);
        assertTrue(output.contains("Enter a command."), output);
        assertArrayEquals(before, Files.readAllBytes(dataFile));
    }

    /** Every shortcut must behave exactly like its full command, including the changes it saves. */
    @Test
    void aliases_giveTheSameOutputAndSavedDataAsTheFullCommands() throws IOException {
        Path fullFile = seededFile("full.txt");
        Path aliasFile = tempDir.resolve("alias.txt");
        Files.copy(fullFile, aliasFile);

        String full = session(fullFile, "view S1", "schedule S1 d/21-01-2099 t/1600 s/Math", "lessons",
                "record L1 a/late", "filter S1", "assign S1 Quiz prep d/22-01-2099", "submit H2", "homework",
                "mark L1 paid", "unpaid", "help");
        String alias = session(aliasFile, "v S1", "sch S1 d/21-01-2099 t/1600 s/Math", "ls",
                "rec L1 a/late", "fl S1", "as S1 Quiz prep d/22-01-2099", "sub H2", "hw",
                "m L1 paid", "up", "h");

        assertFalse(full.contains("Error"), full);
        // Only the file name in the start-up message differs between the two sessions.
        assertEquals(full.replace(fullFile.toString(), "FILE"), alias.replace(aliasFile.toString(), "FILE"));
        assertEquals(Files.readString(fullFile), Files.readString(aliasFile));
    }

    @Test
    void handEditedFile_reloadAppliesGoodLinesAndTheNextSaveDropsBadOnes() throws IOException {
        Path dataFile = seededFile("edited.txt");
        Files.writeString(dataFile, Files.readString(dataFile)
                + "FLAG | s1 | Hand edited" + NL
                + "FLAG | S9 | Nobody" + NL
                + "X | something" + NL);

        String output = session(dataFile, "reload", "flagged", "flag S1 Changed in the app");

        assertTrue(output.contains("Skipped 2 lines that could not be read:"), output);
        assertTrue(output.contains("No student found with ID S9."), output);
        assertTrue(output.contains("Unknown record type \"X\"."), output);
        assertTrue(output.contains("  S1  Alice Tan (Pri 5)  Hand edited"), output);
        String saved = Files.readString(dataFile);
        assertTrue(saved.contains("FLAG | S1 | Changed in the app"), saved);
        assertFalse(saved.contains("Nobody") || saved.contains("X | something"), saved);
    }

    @Test
    void unreadableDataFile_appStillStartsAndWarnsThatChangesAreNotSaved() throws IOException {
        // A folder where the data file should be cannot be read or written on any operating system.
        Path dataFile = tempDir.resolve("tutorledger.txt");
        Files.createDirectory(dataFile);

        String output = session(dataFile, "add n/Kim Ng l/Pri 1 p/91112222", "list");

        assertTrue(output.contains("Could not read " + dataFile), output);
        assertTrue(output.contains("Starting with no data."), output);
        assertTrue(output.contains("Added S1: Kim Ng (Pri 1), 91112222"), output);
        assertTrue(output.contains("Warning: your change could not be saved to " + dataFile), output);
        assertTrue(output.contains("1 student:" + NL + "  S1  Kim Ng (Pri 1)  none"), output);
        assertTrue(Files.isDirectory(dataFile));
    }

    @Test
    void inputWithExtraSpacesAndMixedCase_isAcceptedLikeTidyInput() {
        Path dataFile = tempDir.resolve("spaces.txt");

        String output = session(dataFile, "   ADD   n/Ann Ho   l/sec2   p/81234567  ", "  Flag   s1   Exam soon  ",
                "LIST", "Flagged");

        assertTrue(output.contains("Added S1: Ann Ho (Sec 2), 81234567"), output);
        assertTrue(output.contains("Reason: Exam soon" + NL), output);
        assertTrue(output.contains("  S1  Ann Ho (Sec 2)  none  [flagged]"), output);
        assertTrue(output.contains("  S1  Ann Ho (Sec 2)  Exam soon"), output);
    }

    /**
     * Checks the v2.1 student features together across a restart: names with s/o and d/o, flags with
     * characters the data file escapes, both list sorts, and history, which is never saved.
     */
    @Test
    void v21StudentFeatures_workTogetherAndSurviveARestart() throws IOException {
        Path dataFile = tempDir.resolve("v21.txt");
        String first = session(dataFile,
                "add n/Ravi s/o Kumar l/Sec 1 p/91234567 s/Math r/50",
                "add n/Priya d/o Raman l/Sec 2 p/81234567 s/Math r/40",
                "log S1 d/05-10-2020 t/1600 s/Math f/50 a/present",
                "log S2 d/12-10-2020 t/1600 s/Math f/90 a/present",
                "pay S2 30 L2",
                "flag S1 Fractions | ratios \\ exam soon",
                "list sort/owed",
                "repeat 7",
                "y");

        assertFalse(first.contains("Error"), first);
        assertTrue(first.contains("Added S1: Ravi s/o Kumar (Sec 1), 91234567"), first);
        String owedOrder = "2 students, most owed first:" + NL
                + "  S2  Priya d/o Raman (Sec 2)  Math  owes $60.00" + NL
                + "  S1  Ravi s/o Kumar (Sec 1)  Math  [flagged]  owes $50.00";
        // The query runs once when typed and once more when repeated.
        assertEquals(2, first.split(Pattern.quote(owedOrder), -1).length - 1, first);

        List<String> savedLines = Files.readAllLines(dataFile);
        String second = session(dataFile, "list ravi s/o", "list sort/last", "flagged", "history");

        assertTrue(second.contains("1 student match \"ravi s/o\":" + NL
                + "  S1  Ravi s/o Kumar (Sec 1)  Math  [flagged]"), second);
        assertTrue(second.contains("  S1  Ravi s/o Kumar (Sec 1)  Math  [flagged]  last lesson Mon 05-10-2020" + NL
                + "  S2  Priya d/o Raman (Sec 2)  Math  last lesson Mon 12-10-2020"), second);
        assertTrue(second.contains("  S1  Ravi s/o Kumar (Sec 1)  Fractions | ratios \\ exam soon"), second);
        // History belongs to one session, and the read-only session leaves the file as it was.
        assertTrue(second.contains("Command history (3 commands)"), second);
        assertFalse(second.contains("repeat"), second);
        assertEquals(savedLines, Files.readAllLines(dataFile));
    }
}


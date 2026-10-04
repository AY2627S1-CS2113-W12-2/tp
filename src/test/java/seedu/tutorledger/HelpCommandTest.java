package seedu.tutorledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Checks that the help command lists every command and ignores extra words. */
class HelpCommandTest {
    private StudentCommands commands;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-21T10:00:00Z"), ZoneOffset.UTC);
        commands = new StudentCommands(new TutorLedgerData(), clock);
    }

    @Test
    void execute_help_returnsHelpText() {
        assertEquals(HelpCommand.getHelpText(), commands.execute("help"));
    }

    @Test
    void execute_helpWithExtraWordsOrDifferentCase_ignoresExtras() {
        assertEquals(HelpCommand.getHelpText(), commands.execute("help 123"));
        assertEquals(HelpCommand.getHelpText(), commands.execute("  HELP lesson  "));
    }

    @Test
    void getHelpText_listsEverySectionAndCommandFormat() {
        String help = HelpCommand.getHelpText();
        for (String section : new String[] {"STUDENTS", "LESSONS", "HOMEWORK", "PAYMENTS", "STORAGE"}) {
            assertTrue(help.contains(section), "missing section " + section);
        }
        for (String format : new String[] {"add n/NAME l/LEVEL p/PHONE_NUMBER", "view STUDENT_ID",
            "schedule STUDENT_ID d/DATE t/TIME s/SUBJECT f/FEE", "assign STUDENT_ID DESCRIPTION d/DUE_DATE",
            "mark LESSON_ID STATUS", "owed", "save [FILE_PATH]", "reload"}) {
            assertTrue(help.contains(format), "missing format " + format);
        }
    }

    @Test
    void getHelpText_givesAnExampleForEachCommand() {
        String help = HelpCommand.getHelpText();
        assertTrue(help.contains("add n/Amirah Binte Rahman l/Sec 3 p/91234567"));
        assertTrue(help.contains("schedule S1 d/22-09-2026 t/1600 s/A Math f/60"));
        assertTrue(help.contains("save backups/term1.txt"));
    }
}

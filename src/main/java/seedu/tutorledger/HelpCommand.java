package seedu.tutorledger;

import java.util.List;

/**
 * Builds the text shown by the {@code help} command (feature F6.1 in the user guide).
 *
 * <p>Every command TutorLedger accepts is listed with its format and one worked example,
 * grouped by feature. The text is fixed, so it is built once and reused.
 */
public final class HelpCommand {
    /** Indentation used before each command format. */
    private static final String FORMAT_INDENT = "  ";
    /** Deeper indentation used before each example, so it reads as belonging to the format above it. */
    private static final String EXAMPLE_INDENT = "      ";

    /** All feature sections, in the order they appear in the user guide. */
    private static final List<Section> SECTIONS = List.of(
            new Section("STUDENTS", List.of(
                    new Entry("add n/NAME l/LEVEL p/PHONE_NUMBER [s/SUBJECT]... [r/RATE]",
                            "add n/Amirah Binte Rahman l/Sec 3 p/91234567 s/E Math s/A Math"),
                    new Entry("subject STUDENT_ID s/SUBJECT [s/SUBJECT]...",
                            "subject S1 s/E Math s/A Math"),
                    new Entry("view STUDENT_ID", "view S1"),
                    new Entry("list [NAME] [status/STATUS]", "list wei"),
                    new Entry("archive STUDENT_ID", "archive S5"),
                    new Entry("restore STUDENT_ID", "restore S5"),
                    new Entry("edit STUDENT_ID [n/NAME] [l/LEVEL] [p/PHONE_NUMBER] [s/SUBJECT]... [r/RATE]",
                            "edit S1 l/Sec 4"),
                    new Entry("delete STUDENT_ID [confirm]", "delete S5 confirm"))),
            new Section("LESSONS", List.of(
                    new Entry("schedule STUDENT_ID d/DATE t/TIME s/SUBJECT [dur/MINUTES] [f/FEE] [w/WEEKS]",
                            "schedule S1 d/22-09-2026 t/1600 s/A Math f/60 w/10"),
                    new Entry("log STUDENT_ID d/DATE t/TIME s/SUBJECT f/FEE a/ATTENDANCE [note/NOTES]",
                            "log S1 d/15-09-2026 t/1600 s/A Math f/60 a/present note/Discriminant"),
                    new Entry("lessons [week]", "lessons week"),
                    new Entry("filter [STUDENT_ID] [from/DATE] [to/DATE] [pay/STATUS]",
                            "filter S1 from/01-09-2026 pay/unpaid"),
                    new Entry("next [STUDENT_ID]", "next"),
                    new Entry("topics STUDENT_ID [s/SUBJECT]", "topics S1 s/A Math"),
                    new Entry("record LESSON_ID a/ATTENDANCE [note/NOTES]",
                            "record L41 a/present note/Started simultaneous equations"),
                    new Entry("reschedule LESSON_ID [d/DATE] [t/TIME]", "reschedule L42 d/25-09-2026 t/1800"),
                    new Entry("cancel LESSON_ID r/REASON", "cancel L43 r/Student sick"),
                    new Entry("edit LESSON_ID [d/DATE] [t/TIME] [s/SUBJECT] [f/FEE]"
                            + " [dur/MINUTES] [a/ATTENDANCE] [note/NOTES]",
                            "edit L42 d/25-09-2026 t/1800"),
                    new Entry("delete LESSON_ID", "delete L44"))),
            new Section("HOMEWORK", List.of(
                    new Entry("assign STUDENT_ID DESCRIPTION d/DUE_DATE", "assign S1 Worksheet 4A d/29-09-2026"),
                    new Entry("submit HOMEWORK_ID", "submit H7"),
                    new Entry("homework [STUDENT_ID]", "homework S1"))),
            new Section("PAYMENTS", List.of(
                    new Entry("mark LESSON_ID STATUS", "mark L37 paid"),
                    new Entry("pay STUDENT_ID AMOUNT LESSON_ID [LESSON_ID...]",
                            "pay S1 117.50 L37 L38"),
                    new Entry("unpaid [STUDENT_ID]", "unpaid S1"),
                    new Entry("owed", "owed"),
                    new Entry("fee_statement STUDENT_ID", "fee_statement S1"),
                    new Entry("income MM-YYYY or income YYYY", "income 09-2026"))),
            new Section("SCORES AND PROGRESS", List.of(
                    new Entry("score STUDENT_ID TEST_NAME m/MARKS/TOTAL [d/DATE]",
                            "score S1 WA1 m/28/40 d/07-09-2026"),
                    new Entry("scores STUDENT_ID", "scores S1"),
                    new Entry("progress STUDENT_ID [from/DATE] [to/DATE]",
                            "progress S1 from/01-07-2026 to/14-10-2026"))),
            new Section("FOLLOW-UP TASKS", List.of(
                    new Entry("task [STUDENT_ID] DESCRIPTION [d/DUE_DATE]",
                            "task S1 Bring Worksheet 4B d/20-10-2026"),
                    new Entry("tasks [STUDENT_ID]", "tasks"),
                    new Entry("done TASK_ID", "done T1"))),
            new Section("STORAGE", List.of(
                    new Entry("save [FILE_PATH]", "save backups/term1.txt"),
                    new Entry("reload", "reload"))),
            new Section("OTHER", List.of(
                    new Entry("help", "help"),
                    new Entry("exit", "exit"))));

    /** Width of the alias column in the shortcuts section, so the commands line up. */
    private static final int ALIAS_WIDTH = 5;

    /** The complete help text, built once when the class is loaded. */
    private static final String HELP_TEXT = buildHelpText();

    /** Prevents instantiation, because this class only holds static help text. */
    private HelpCommand() {
    }

    /**
     * Returns the help text listing every command with its format and one example.
     *
     * <p>Anything the user typed after {@code help} is ignored by the caller, so
     * {@code help 123} shows the same text as {@code help}.
     */
    public static String getHelpText() {
        return HELP_TEXT;
    }

    /** Returns the formatted help text for all sections, one line per format or example. */
    private static String buildHelpText() {
        StringBuilder text = new StringBuilder("Commands (UPPER_CASE = value you supply, [ ] = optional)");
        for (Section section : SECTIONS) {
            text.append(System.lineSeparator()).append(System.lineSeparator()).append(section.title());
            for (Entry entry : section.entries()) {
                text.append(System.lineSeparator()).append(FORMAT_INDENT).append(entry.format());
                text.append(System.lineSeparator()).append(EXAMPLE_INDENT).append(entry.example());
            }
        }
        text.append(System.lineSeparator()).append(System.lineSeparator())
                .append("LEVELS (l/LEVEL; case-insensitive, optional space)")
                .append(System.lineSeparator()).append(FORMAT_INDENT)
                .append("Pri 1 to Pri 6, Sec 1 to Sec 5, or JC 1 to JC 2")
                .append(System.lineSeparator()).append(EXAMPLE_INDENT)
                .append("Examples: l/Pri5, l/Sec 3, l/JC1");
        text.append(System.lineSeparator()).append(System.lineSeparator())
                .append("STUDENT DELETION")
                .append(System.lineSeparator()).append(FORMAT_INDENT)
                .append("With lesson records: confirm is required. Without lessons: deletion is immediate.")
                .append(System.lineSeparator()).append(FORMAT_INDENT)
                .append("Run delete S5 to review the warning, then delete S5 confirm to delete.")
                .append(System.lineSeparator()).append(FORMAT_INDENT)
                .append("To keep the records, continue with another command.");
        text.append(System.lineSeparator()).append(System.lineSeparator())
                .append("STUDENT STATUS")
                .append(System.lineSeparator()).append(FORMAT_INDENT)
                .append("list shows active students. Use status/active, status/archived or status/all.")
                .append(System.lineSeparator()).append(FORMAT_INDENT)
                .append("Archiving keeps history and existing lessons. Restore before scheduling new lessons.");
        text.append(System.lineSeparator()).append(System.lineSeparator())
                .append("SHORTCUTS (type the short form instead of the command word)");
        for (CommandAliases.Alias alias : CommandAliases.all()) {
            // An alias for a command that help does not list would be a shortcut nobody can look up.
            assert SECTIONS.stream().flatMap(section -> section.entries().stream())
                    .anyMatch(entry -> entry.format().startsWith(alias.command()))
                    : "Alias " + alias.shortForm() + " points to an unlisted command " + alias.command();
            text.append(System.lineSeparator()).append(FORMAT_INDENT)
                    .append(String.format("%-" + ALIAS_WIDTH + "s", alias.shortForm()))
                    .append(alias.command());
        }
        return text.toString();
    }

    /** One command's format (with placeholders) and a concrete example of it. */
    private record Entry(String format, String example) {
    }

    /** A titled group of related commands, such as all student commands. */
    private record Section(String title, List<Entry> entries) {
    }
}

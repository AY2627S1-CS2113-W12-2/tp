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
                    new Entry("add n/NAME l/LEVEL p/PHONE_NUMBER",
                            "add n/Amirah Binte Rahman l/Sec 3 p/91234567"),
                    new Entry("subject STUDENT_ID s/SUBJECT [s/SUBJECT]...",
                            "subject S1 s/E Math s/A Math"),
                    new Entry("view STUDENT_ID", "view S1"),
                    new Entry("list [NAME]", "list wei"),
                    new Entry("edit STUDENT_ID [n/NAME] [l/LEVEL] [p/PHONE_NUMBER] [s/SUBJECT]...",
                            "edit S1 l/Sec 4"),
                    new Entry("delete STUDENT_ID", "delete S5"))),
            new Section("LESSONS", List.of(
                    new Entry("schedule STUDENT_ID d/DATE t/TIME s/SUBJECT f/FEE",
                            "schedule S1 d/22-09-2026 t/1600 s/A Math f/60"),
                    new Entry("lessons [week]", "lessons week"),
                    new Entry("record LESSON_ID a/ATTENDANCE [note/NOTES]",
                            "record L41 a/present note/Started simultaneous equations"),
                    new Entry("edit LESSON_ID [d/DATE] [t/TIME] [s/SUBJECT] [f/FEE] [a/ATTENDANCE] [note/NOTES]",
                            "edit L42 d/25-09-2026 t/1800"),
                    new Entry("delete LESSON_ID", "delete L44"))),
            new Section("HOMEWORK", List.of(
                    new Entry("assign STUDENT_ID DESCRIPTION d/DUE_DATE", "assign S1 Worksheet 4A d/29-09-2026"),
                    new Entry("submit HOMEWORK_ID", "submit H7"),
                    new Entry("homework [STUDENT_ID]", "homework S1"))),
            new Section("PAYMENTS", List.of(
                    new Entry("mark LESSON_ID STATUS", "mark L37 paid"),
                    new Entry("unpaid [STUDENT_ID]", "unpaid S1"),
                    new Entry("owed", "owed"))),
            new Section("STORAGE", List.of(
                    new Entry("save [FILE_PATH]", "save backups/term1.txt"),
                    new Entry("reload", "reload"))),
            new Section("OTHER", List.of(
                    new Entry("help", "help"),
                    new Entry("exit", "exit"))));

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
        return text.toString();
    }

    /** One command's format (with placeholders) and a concrete example of it. */
    private record Entry(String format, String example) {
    }

    /** A titled group of related commands, such as all student commands. */
    private record Section(String title, List<Entry> entries) {
    }
}

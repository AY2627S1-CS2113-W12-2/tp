package seedu.tutorledger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Parses and runs the lesson commands, starting with {@code schedule}.
 * Like {@link StudentCommands}, each command returns its display text, and validation problems
 * are reported as an "Error: ..." message instead of crashing the program.
 */
public class LessonCommands {
    /** Command words this class understands, used by the main loop to route input here. */
    private static final Set<String> COMMAND_WORDS = Set.of("schedule");

    /** Date format shown to the user, e.g. Tue 22-09-2026. */
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("EEE dd-MM-uuuu",
            Locale.ENGLISH);

    /** Time format shown to the user, e.g. 1600. */
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("HHmm", Locale.ROOT);

    private final TutorLedgerData data;
    /** Source of today's date; injected so tests can fix the date instead of depending on the real one. */
    private final Clock clock;

    /**
     * Creates the handler for the lesson commands.
     *
     * @param data The records the commands read and change.
     * @param clock The source of today's date.
     */
    public LessonCommands(TutorLedgerData data, Clock clock) {
        this.data = data;
        this.clock = clock;
    }

    /** Returns true if the first word of the input is a lesson command. */
    public static boolean handles(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }
        String command = input.strip().split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        return COMMAND_WORDS.contains(command);
    }

    /** Executes one lesson command and returns its display text, including validation errors. */
    public String execute(String input) {
        if (!handles(input)) {
            return "Unknown command.";
        }
        String[] words = input.strip().split("\\s+", 2);
        String arguments = words.length == 2 ? words[1].trim() : "";
        try {
            return schedule(arguments);
        } catch (IllegalArgumentException exception) {
            return "Error: " + exception.getMessage();
        }
    }

    /** Handles {@code schedule STUDENT_ID d/DATE t/TIME s/SUBJECT f/FEE}. */
    private String schedule(String arguments) {
        IdAndFields input = splitIdAndFields(arguments, List.of("d", "t", "s", "f"));
        Student student = data.getStudent(input.id());
        String date = input.fields().single("d", true);
        String time = input.fields().single("t", true);
        String subject = findSubjectTakenBy(student, input.fields().single("s", true));
        String fee = input.fields().single("f", true);
        Lesson lesson = data.addLesson(student.getStudentId(), subject, date, time, fee);
        return "Scheduled " + lesson.getLessonId() + " for " + formatStudent(student) + System.lineSeparator()
                + "  " + formatSlot(lesson) + ", " + lesson.getSubject() + ", "
                + formatMoney(lesson.getFee());
    }

    /**
     * Returns the subject as it is written on the student's record, ignoring case.
     * Using the student's own spelling keeps "a math" and "A Math" from showing up as two subjects.
     *
     * @throws IllegalArgumentException If the student does not take the subject.
     */
    private static String findSubjectTakenBy(Student student, String subject) {
        for (String taken : student.getSubjects()) {
            if (taken.equalsIgnoreCase(subject)) {
                return taken;
            }
        }
        if (student.getSubjects().isEmpty()) {
            throw new IllegalArgumentException(formatStudent(student)
                    + " has no subjects yet. Add one with: subject " + student.getStudentId() + " s/SUBJECT");
        }
        throw new IllegalArgumentException(subject + " is not one of " + formatStudent(student)
                + "'s subjects (" + String.join(", ", student.getSubjects()) + ").");
    }

    /** Splits {@code ID prefix/value ...} into the ID and its prefixed values. */
    private static IdAndFields splitIdAndFields(String arguments, List<String> allowedPrefixes) {
        String[] pieces = arguments.split("\\s+", 2);
        String remainder = pieces.length == 2 ? pieces[1] : "";
        return new IdAndFields(pieces[0], ArgumentParser.parseFields(remainder, allowedPrefixes));
    }

    /** Formats when a lesson starts as "Tue 22-09-2026 1600". */
    private static String formatSlot(Lesson lesson) {
        return DISPLAY_DATE.format(lesson.getDate()) + " " + DISPLAY_TIME.format(lesson.getTime());
    }

    /** Formats a student as "S1 Amirah Binte Rahman". */
    private static String formatStudent(Student student) {
        return student.getStudentId() + " " + student.getName();
    }

    /** Formats a fee as dollars with two decimal places, e.g. $60.00. */
    private static String formatMoney(BigDecimal amount) {
        return "$" + amount.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }

    /** The ID typed straight after the command word, with the prefixed values that follow it. */
    private record IdAndFields(String id, ArgumentParser.Fields fields) {
    }
}

package seedu.tutorledger;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses and runs the homework commands: {@code assign}, {@code submit} and {@code homework}.
 * Like {@link StudentCommands}, each command returns its display text, and validation problems
 * are reported as an "Error: ..." message instead of crashing the program.
 */
public class HomeworkCommands {
    /** Command words this class understands, used by the main loop to route input here. */
    private static final Set<String> COMMAND_WORDS = Set.of("assign", "submit", "homework");

    /**
     * Matches {@code STUDENT_ID DESCRIPTION d/DUE_DATE}. The description is everything between
     * the student ID and the final {@code d/}, so it may contain spaces and commas.
     */
    private static final Pattern ASSIGN_FORMAT = Pattern.compile("(?i)(\\S+)\\s+(.+?)\\s+d/(\\S+)");

    /** Date format typed by the user, e.g. 29-09-2026. STRICT rejects dates like 31-02-2026. */
    private static final DateTimeFormatter INPUT_DATE = DateTimeFormatter.ofPattern("dd-MM-uuuu", Locale.ROOT)
            .withResolverStyle(ResolverStyle.STRICT);

    /** Date format shown to the user, e.g. Tue 29-09-2026. */
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("EEE dd-MM-uuuu",
            Locale.ENGLISH);

    private final TutorLedgerData data;
    /** Source of today's date; injected so tests can fix the date when checking OVERDUE. */
    private final Clock clock;

    public HomeworkCommands(TutorLedgerData data, Clock clock) {
        this.data = data;
        this.clock = clock;
    }

    /** Returns true if the first word of the input is a homework command. */
    public static boolean handles(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }
        String command = input.strip().split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        return COMMAND_WORDS.contains(command);
    }

    /** Executes one homework command and returns its display text, including validation errors. */
    public String execute(String input) {
        if (!handles(input)) {
            return "Unknown command.";
        }
        String[] words = input.strip().split("\\s+", 2);
        String command = words[0].toLowerCase(Locale.ROOT);
        String arguments = words.length == 2 ? words[1].trim() : "";
        try {
            return switch (command) {
            case "assign" -> assign(arguments);
            case "submit" -> submit(arguments);
            default -> listOutstanding(arguments);
            };
        } catch (IllegalArgumentException exception) {
            return "Error: " + exception.getMessage();
        }
    }

    /** Handles {@code assign STUDENT_ID DESCRIPTION d/DUE_DATE}. */
    private String assign(String arguments) {
        Matcher matcher = ASSIGN_FORMAT.matcher(arguments);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Use: assign STUDENT_ID DESCRIPTION d/DUE_DATE");
        }
        LocalDate dueDate = parseDate(matcher.group(3));
        Homework item = data.addHomework(matcher.group(1), matcher.group(2).trim(), dueDate);
        Student student = data.getStudent(item.getStudentId());
        return "Assigned " + item.getHomeworkId() + " to " + studentLabel(student) + System.lineSeparator()
                + "  " + item.getDescription() + ", due " + DISPLAY_DATE.format(item.getDueDate());
    }

    /** Handles {@code submit HOMEWORK_ID}. */
    private String submit(String arguments) {
        if (arguments.isEmpty() || arguments.contains(" ")) {
            throw new IllegalArgumentException("Use: submit HOMEWORK_ID");
        }
        Homework item = data.getHomeworkById(arguments);
        if (item.isSubmitted()) {
            throw new IllegalArgumentException(item.getHomeworkId() + " is already submitted.");
        }
        // Homework is immutable, so store a submitted copy under the same ID.
        data.putHomework(item.asSubmitted());
        Student student = data.getStudent(item.getStudentId());
        return "Marked " + item.getHomeworkId() + " as submitted: " + item.getDescription()
                + " (" + studentLabel(student) + ")";
    }

    /** Handles {@code homework [STUDENT_ID]}: lists unsubmitted homework, due soonest first. */
    private String listOutstanding(String arguments) {
        // Look the student up first so an unknown ID gives an error rather than an empty list.
        String studentId = arguments.isEmpty() ? null : data.getStudent(arguments).getStudentId();
        List<Homework> outstanding = data.getHomework().stream()
                .filter(item -> !item.isSubmitted())
                .filter(item -> studentId == null || studentId.equals(item.getStudentId()))
                .sorted(Comparator.comparing(Homework::getDueDate).thenComparing(Homework::getHomeworkId))
                .toList();

        StringBuilder output = new StringBuilder(outstanding.size()
                + (outstanding.size() == 1 ? " homework item" : " homework items") + " outstanding");
        if (outstanding.isEmpty()) {
            return output.toString();
        }

        // Work out column widths so the ID, student and description columns line up.
        int idWidth = 0;
        int studentWidth = 0;
        int descriptionWidth = 0;
        for (Homework item : outstanding) {
            idWidth = Math.max(idWidth, item.getHomeworkId().length());
            studentWidth = Math.max(studentWidth, studentLabel(data.getStudent(item.getStudentId())).length());
            descriptionWidth = Math.max(descriptionWidth, item.getDescription().length());
        }
        String rowFormat = "  %-" + idWidth + "s  %-" + studentWidth + "s  %-" + descriptionWidth + "s  due %s";

        LocalDate today = LocalDate.now(clock);
        for (Homework item : outstanding) {
            String row = String.format(rowFormat, item.getHomeworkId(),
                    studentLabel(data.getStudent(item.getStudentId())), item.getDescription(),
                    DISPLAY_DATE.format(item.getDueDate()));
            output.append(System.lineSeparator()).append(row);
            if (item.getDueDate().isBefore(today)) {
                output.append("  OVERDUE");
            }
        }
        return output.toString();
    }

    /** Converts a DD-MM-YYYY date, rejecting impossible dates with a readable message. */
    private static LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value, INPUT_DATE);
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException("Due date must be a real date in DD-MM-YYYY format.", exception);
        }
    }

    /** Formats a student as "S1 Amirah Binte Rahman". */
    private static String studentLabel(Student student) {
        return student.getStudentId() + " " + student.getName();
    }
}

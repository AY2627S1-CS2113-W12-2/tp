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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses and runs the homework commands: {@code assign}, {@code submit} and {@code homework}, plus
 * {@code edit} and {@code delete} when they are given a homework ID.
 * Like {@link StudentCommands}, each command returns its display text, and validation problems
 * are reported as an "Error: ..." message instead of crashing the program.
 */
public class HomeworkCommands {
    private static final Logger LOGGER = Logger.getLogger(HomeworkCommands.class.getName());

    /** Command words this class understands, used by the main loop to route input here. */
    private static final Set<String> COMMAND_WORDS = Set.of("assign", "submit", "homework");

    /** Command words shared with students and lessons; they are homework commands only when given a homework ID. */
    private static final Set<String> SHARED_COMMAND_WORDS = Set.of("edit", "delete");

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
        assert data != null : "HomeworkCommands needs a data store";
        assert clock != null : "HomeworkCommands needs a clock to know today's date";
        this.data = data;
        this.clock = clock;
    }

    /**
     * Returns true if the input is a homework command.
     *
     * <p>{@code edit} and {@code delete} are shared with students and lessons, so they count only when
     * the ID after them starts with H, in the same way {@link LessonCommands} claims IDs starting with L.
     */
    public static boolean handles(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }
        String[] words = input.strip().split("\\s+", 3);
        String command = words[0].toLowerCase(Locale.ROOT);
        if (COMMAND_WORDS.contains(command)) {
            return true;
        }
        // "[^/]*" keeps a prefixed value from being mistaken for a homework ID.
        return SHARED_COMMAND_WORDS.contains(command) && words.length > 1 && words[1].matches("(?i)H[^/]*");
    }

    /** Executes one homework command and returns its display text, including validation errors. */
    public String execute(String input) {
        if (!handles(input)) {
            return "Unknown command.";
        }
        String[] words = input.strip().split("\\s+", 2);
        String command = words[0].toLowerCase(Locale.ROOT);
        String arguments = words.length == 2 ? words[1].trim() : "";
        LOGGER.log(Level.FINE, "Running homework command {0} with arguments: {1}",
                new Object[]{command, arguments});
        try {
            return switch (command) {
            case "assign" -> assign(arguments);
            case "submit" -> submit(arguments);
            case "edit" -> edit(arguments);
            case "delete" -> delete(arguments);
            default -> listOutstanding(arguments);
            };
        } catch (IllegalArgumentException exception) {
            // Invalid user input is expected and already explained on screen, so it is only traced for debugging.
            LOGGER.log(Level.FINE, "Rejected homework command {0}: {1}",
                    new Object[]{input.strip(), exception.getMessage()});
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
        assert !item.isSubmitted() : "Newly assigned homework should start as outstanding";
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
        assert data.getHomeworkById(item.getHomeworkId()).isSubmitted() : "Stored homework should now be submitted";
        Student student = data.getStudent(item.getStudentId());
        return "Marked " + item.getHomeworkId() + " as submitted: " + item.getDescription()
                + " (" + studentLabel(student) + ")";
    }

    /**
     * Handles {@code edit HOMEWORK_ID [desc/DESCRIPTION] [d/DUE_DATE] [status/STATUS]}.
     * It corrects a mistake made in {@code assign}, and {@code status/outstanding} undoes a mistaken {@code submit}.
     */
    private String edit(String arguments) {
        String[] pieces = arguments.split("\\s+", 2);
        // desc/ is free text, so a description such as "Read p.3 and/or p.4" is not cut short.
        ArgumentParser.Fields fields = ArgumentParser.parseFields(pieces.length == 2 ? pieces[1] : "",
                List.of("desc", "d", "status"), Set.of("desc"));
        Homework item = data.getHomeworkById(pieces[0]);
        if (fields.isEmpty()) {
            throw new IllegalArgumentException("Give at least one value to edit: desc/, d/ or status/.");
        }
        String description = fields.singleOr("desc", null);
        LocalDate dueDate = fields.has("d") ? parseDate(fields.single("d", true)) : null;
        String status = fields.has("status") ? fields.single("status", true).toLowerCase(Locale.ROOT) : null;
        Homework edited = item.withDetails(description, dueDate, status);
        data.replaceHomework(edited);
        LOGGER.log(Level.FINE, "Edited {0} using prefixes {1}",
                new Object[]{edited.getHomeworkId(), fields.fields().keySet()});
        Student student = data.getStudent(edited.getStudentId());
        return "Edited " + edited.getHomeworkId() + " for " + studentLabel(student) + System.lineSeparator()
                + "  " + edited.getDescription() + ", due " + DISPLAY_DATE.format(edited.getDueDate()) + ", "
                + edited.getSubmitStatus();
    }

    /** Handles {@code delete HOMEWORK_ID}. The student and their other records are not affected. */
    private String delete(String arguments) {
        if (arguments.split("\\s+").length != 1) {
            throw new IllegalArgumentException("Use: delete HOMEWORK_ID");
        }
        Homework item = data.deleteHomework(arguments);
        LOGGER.log(Level.FINE, "Deleted {0}", item.getHomeworkId());
        Student student = data.getStudent(item.getStudentId());
        return "Deleted " + item.getHomeworkId() + " for " + studentLabel(student) + ": "
                + item.getDescription() + ", due " + DISPLAY_DATE.format(item.getDueDate());
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
        LOGGER.log(Level.FINE, "Found {0} outstanding homework item(s)", outstanding.size());

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
            assert !item.isSubmitted() : "Submitted homework should have been filtered out";
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

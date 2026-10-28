package seedu.tutorledger;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Runs the follow-up task commands (features F8.1 to F8.3, story US20):
 * <ul>
 *   <li>{@code task} logs a reminder for the tutor, optionally for one student and by a date,</li>
 *   <li>{@code tasks} lists the reminders that are still pending, and</li>
 *   <li>{@code done} ticks a reminder off.</li>
 * </ul>
 * Like the other command classes, each command returns its display text, and validation problems
 * are reported as an "Error: ..." message instead of crashing the program.
 */
public class TaskCommands {
    /**
     * Orders pending tasks for display: dated tasks soonest first, then undated tasks.
     * Tasks with the same date keep the order they were logged in. Shared with {@code next}.
     */
    static final Comparator<FollowUpTask> DISPLAY_ORDER = Comparator
            .comparing((FollowUpTask task) -> task.getDueDate().orElse(LocalDate.MAX))
            .thenComparingInt(task -> Integer.parseInt(task.getTaskId().substring(1)));

    /** Date format shown to the user, e.g. Tue 20-10-2026. */
    static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("EEE dd-MM-uuuu", Locale.ENGLISH);

    private static final Logger LOGGER = Logger.getLogger(TaskCommands.class.getName());

    /** Command words this class understands, used by the main loop to route input here. */
    private static final Set<String> COMMAND_WORDS = Set.of("task", "tasks", "done");

    private static final String TASK_USAGE = "Use: task [STUDENT_ID] DESCRIPTION [d/DUE_DATE]";

    /**
     * Splits {@code task} arguments into the text and an optional final {@code d/DUE_DATE}.
     * Like {@code assign}, the date must come last, so the description may contain spaces and commas.
     */
    private static final Pattern TASK_FORMAT = Pattern.compile("(?i)(.+?)(?:\\s+d/(\\S*))?");

    /** A student ID such as S1, used to tell whether a task starts with the student it is for. */
    private static final Pattern STUDENT_ID = Pattern.compile("(?i)S[1-9]\\d*");

    /** Date format typed by the user, e.g. 20-10-2026. STRICT rejects dates like 31-02-2026. */
    private static final DateTimeFormatter INPUT_DATE = DateTimeFormatter.ofPattern("dd-MM-uuuu", Locale.ROOT)
            .withResolverStyle(ResolverStyle.STRICT);

    private final TutorLedgerData data;
    /** Source of today's date; injected so tests can fix the date when checking OVERDUE. */
    private final Clock clock;

    public TaskCommands(TutorLedgerData data, Clock clock) {
        assert data != null : "TaskCommands needs a data store";
        assert clock != null : "TaskCommands needs a clock to know today's date";
        this.data = data;
        this.clock = clock;
    }

    /** Returns true if the first word of the input is a follow-up task command. */
    public static boolean handles(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }
        String command = input.strip().split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        return COMMAND_WORDS.contains(command);
    }

    /** Executes one task command and returns its display text, including validation errors. */
    public String execute(String input) {
        if (!handles(input)) {
            return "Unknown command.";
        }
        String[] words = input.strip().split("\\s+", 2);
        String command = words[0].toLowerCase(Locale.ROOT);
        String arguments = words.length == 2 ? words[1].trim() : "";
        LOGGER.log(Level.FINE, "Running task command {0} with arguments: {1}", new Object[]{command, arguments});
        try {
            return switch (command) {
            case "task" -> addTask(arguments);
            case "tasks" -> listTasks(arguments);
            default -> markDone(arguments);
            };
        } catch (IllegalArgumentException exception) {
            LOGGER.log(Level.WARNING, "Rejected task command {0}: {1}",
                    new Object[]{input.strip(), exception.getMessage()});
            return "Error: " + exception.getMessage();
        }
    }

    /** Handles {@code task [STUDENT_ID] DESCRIPTION [d/DUE_DATE]}. */
    private String addTask(String arguments) {
        Matcher matcher = TASK_FORMAT.matcher(arguments);
        if (arguments.isEmpty() || !matcher.matches()) {
            throw new IllegalArgumentException(TASK_USAGE);
        }
        String text = matcher.group(1).strip();
        LocalDate dueDate = matcher.group(2) == null ? null : parseDate(matcher.group(2));

        // A first word that looks like a student ID says who the task is for; the rest is the description.
        String studentId = null;
        String[] pieces = text.split("\\s+", 2);
        if (STUDENT_ID.matcher(pieces[0]).matches()) {
            studentId = pieces[0];
            text = pieces.length == 2 ? pieces[1] : "";
        }
        // Without this check, "task d/20-10-2026" would log a task whose description is the date itself.
        if (text.isBlank() || text.toLowerCase(Locale.ROOT).startsWith("d/")) {
            throw new IllegalArgumentException(TASK_USAGE);
        }

        FollowUpTask task = data.addTask(studentId, text, dueDate);
        assert !task.isDone() : "A newly logged task should be pending";
        LOGGER.log(Level.INFO, "Logged follow-up task {0}", task.getTaskId());

        StringBuilder output = new StringBuilder("Added " + task.getTaskId() + ": " + task.getDescription());
        List<String> details = new ArrayList<>();
        task.getStudentId().ifPresent(id -> details.add("for " + label(data.getStudent(id))));
        task.getDueDate().ifPresent(date -> details.add("by " + DISPLAY_DATE.format(date)));
        if (!details.isEmpty()) {
            output.append(System.lineSeparator()).append("  ").append(String.join(", ", details));
        }
        return output.toString();
    }

    /** Handles {@code tasks [STUDENT_ID]}: pending tasks, soonest due first, with overdue ones marked. */
    private String listTasks(String arguments) {
        if (arguments.contains(" ")) {
            throw new IllegalArgumentException("Use: tasks [STUDENT_ID]");
        }
        // Look the student up first so an unknown ID gives an error rather than an empty list.
        Student student = arguments.isEmpty() ? null : data.getStudent(arguments);
        List<FollowUpTask> pending = data.getTasks().stream()
                .filter(task -> !task.isDone())
                .filter(task -> student == null
                        || task.getStudentId().filter(student.getStudentId()::equals).isPresent())
                .sorted(DISPLAY_ORDER)
                .toList();
        LOGGER.log(Level.FINE, "Found {0} pending task(s)", pending.size());

        String heading = pending.size() + (pending.size() == 1 ? " follow-up task" : " follow-up tasks")
                + " pending" + (student == null ? "" : " for " + label(student));
        if (pending.isEmpty()) {
            return heading;
        }

        // Work out column widths so the ID, description and student columns line up.
        int idWidth = 0;
        int descriptionWidth = 0;
        int studentWidth = 0;
        for (FollowUpTask task : pending) {
            idWidth = Math.max(idWidth, task.getTaskId().length());
            descriptionWidth = Math.max(descriptionWidth, task.getDescription().length());
            studentWidth = Math.max(studentWidth, studentText(task).length());
        }
        String rowFormat = "  %-" + idWidth + "s  %-" + descriptionWidth + "s  %-" + studentWidth + "s  %s";

        LocalDate today = LocalDate.now(clock);
        StringBuilder output = new StringBuilder(heading);
        for (FollowUpTask task : pending) {
            assert !task.isDone() : "Done tasks should have been filtered out";
            String due = task.getDueDate().map(date -> "by " + DISPLAY_DATE.format(date)).orElse("no date");
            output.append(System.lineSeparator())
                    .append(String.format(rowFormat, task.getTaskId(), task.getDescription(), studentText(task), due)
                            .stripTrailing());
            if (task.isOverdue(today)) {
                output.append("  OVERDUE");
            }
        }
        return output.toString();
    }

    /** Handles {@code done TASK_ID}. */
    private String markDone(String arguments) {
        if (arguments.isEmpty() || arguments.contains(" ")) {
            throw new IllegalArgumentException("Use: done TASK_ID");
        }
        FollowUpTask task = data.getTask(arguments);
        if (task.isDone()) {
            throw new IllegalArgumentException(task.getTaskId() + " is already done.");
        }
        // Tasks are immutable, so store a done copy under the same ID.
        data.replaceTask(task.asDone());
        assert data.getTask(task.getTaskId()).isDone() : "Stored task should now be done";
        LOGGER.log(Level.INFO, "Marked task {0} as done", task.getTaskId());
        return "Marked " + task.getTaskId() + " as done: " + task.getDescription();
    }

    /** Returns "S1 Amirah Binte Rahman" for a task linked to a student, or an empty string otherwise. */
    private String studentText(FollowUpTask task) {
        return task.getStudentId().map(id -> label(data.getStudent(id))).orElse("");
    }

    private static LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value, INPUT_DATE);
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException("Due date must be a real date in DD-MM-YYYY format.", exception);
        }
    }

    /** Formats a student as "S1 Amirah Binte Rahman". */
    private static String label(Student student) {
        return student.getStudentId() + " " + student.getName();
    }
}

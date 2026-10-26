package seedu.tutorledger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Runs the {@code filter} command, which lists the lessons that match every filter given (story US37).
 *
 * <p>Format: {@code filter [STUDENT_ID] [from/DATE] [to/DATE] [pay/STATUS]}. Each filter is optional and
 * they can be combined, e.g. {@code filter S1 from/01-09-2026 pay/unpaid} lists S1's unpaid lessons since
 * 1 September. With no filters, every lesson is listed, which is the only way to see lessons outside the
 * current week.
 *
 * <p>This is kept apart from {@link LessonCommands} because it only reads lessons and never changes them.
 */
public class LessonFilterCommand {
    private static final Logger LOGGER = Logger.getLogger(LessonFilterCommand.class.getName());

    private static final String COMMAND_WORD = "filter";
    private static final String USAGE = "Use: filter [STUDENT_ID] [from/DATE] [to/DATE] [pay/STATUS]";

    /** Date format typed by the user, e.g. 21-09-2026. STRICT rejects dates like 31-02-2026. */
    private static final DateTimeFormatter INPUT_DATE = DateTimeFormatter.ofPattern("dd-MM-uuuu", Locale.ROOT)
            .withResolverStyle(ResolverStyle.STRICT);

    /** Date format shown to the user, e.g. Mon 21-09-2026. */
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("EEE dd-MM-uuuu",
            Locale.ENGLISH);

    /** Time format shown to the user, e.g. 1600. */
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("HHmm", Locale.ROOT);

    private final TutorLedgerData data;

    /**
     * Creates the handler for the filter command.
     *
     * @param data The records to search.
     */
    public LessonFilterCommand(TutorLedgerData data) {
        assert data != null : "LessonFilterCommand needs the records to search";
        this.data = data;
    }

    /** Returns true if the first word of the input is {@code filter}, in any case. */
    public static boolean handles(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }
        return input.strip().split("\\s+", 2)[0].equalsIgnoreCase(COMMAND_WORD);
    }

    /** Runs the filter command and returns its display text, or an "Error: ..." message. */
    public String execute(String input) {
        if (!handles(input)) {
            return "Unknown command.";
        }
        String[] words = input.strip().split("\\s+", 2);
        String arguments = words.length == 2 ? words[1].trim() : "";
        LOGGER.log(Level.FINE, "Running filter with arguments: {0}", arguments);
        try {
            Criteria criteria = parseCriteria(arguments);
            List<Lesson> matches = data.getLessons().stream()
                    .filter(criteria::matches)
                    .sorted(Comparator.comparing(Lesson::getDate)
                            .thenComparing(Lesson::getTime)
                            .thenComparing(Lesson::getLessonId))
                    .toList();
            LOGGER.log(Level.FINE, "Filter matched {0} of {1} lessons",
                    new Object[]{matches.size(), data.getLessons().size()});
            return format(matches, criteria);
        } catch (IllegalArgumentException exception) {
            LOGGER.log(Level.FINE, "Filter rejected: {0}", exception.getMessage());
            return "Error: " + exception.getMessage();
        }
    }

    /**
     * Reads the optional student ID and prefixed filters.
     *
     * @throws IllegalArgumentException If a value is invalid, or {@code from/} is after {@code to/}.
     */
    private Criteria parseCriteria(String arguments) {
        // A first word without a slash is the student ID; anything else is a prefixed value.
        String[] pieces = arguments.split("\\s+", 2);
        Student student = null;
        String prefixed = arguments;
        if (!pieces[0].isEmpty() && !pieces[0].contains("/")) {
            student = data.getStudent(pieces[0]);
            prefixed = pieces.length == 2 ? pieces[1] : "";
        }
        ArgumentParser.Fields fields;
        try {
            fields = ArgumentParser.parseFields(prefixed, List.of("from", "to", "pay"));
        } catch (IllegalArgumentException exception) {
            // The parser's message does not say what the command looks like, so add that.
            throw new IllegalArgumentException(exception.getMessage() + " " + USAGE);
        }
        LocalDate from = fields.has("from") ? parseDate(fields.single("from", true)) : null;
        LocalDate to = fields.has("to") ? parseDate(fields.single("to", true)) : null;
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from/ date must not be after to/ date.");
        }
        String payment = fields.has("pay") ? parsePayment(fields.single("pay", true)) : null;
        return new Criteria(student, from, to, payment);
    }

    private static LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value, INPUT_DATE);
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException("Date must be a real date in DD-MM-YYYY format.", exception);
        }
    }

    private static String parsePayment(String value) {
        String status = value.toLowerCase(Locale.ROOT);
        if (!status.equals(Lesson.PAYMENT_PAID) && !status.equals(Lesson.PAYMENT_UNPAID)
                && !status.equals(Lesson.PAYMENT_PARTIAL)) {
            throw new IllegalArgumentException("pay/ must be paid, partial or unpaid.");
        }
        return status;
    }

    /** Builds the heading, one aligned row per lesson, and the total of their fees. */
    private String format(List<Lesson> lessons, Criteria criteria) {
        StringBuilder output = new StringBuilder(count(lessons.size(), "lesson"))
                .append(" (").append(criteria.describe()).append(")");
        if (lessons.isEmpty()) {
            return output.toString();
        }
        int idWidth = 0;
        int studentWidth = 0;
        int subjectWidth = 0;
        for (Lesson lesson : lessons) {
            idWidth = Math.max(idWidth, lesson.getLessonId().length());
            studentWidth = Math.max(studentWidth, studentLabel(lesson).length());
            subjectWidth = Math.max(subjectWidth, lesson.getSubject().length());
        }
        String rowFormat = "  %-" + idWidth + "s  %s %s  %-" + studentWidth + "s  %-" + subjectWidth + "s  %s  %s";
        BigDecimal total = BigDecimal.ZERO;
        for (Lesson lesson : lessons) {
            output.append(System.lineSeparator()).append(String.format(Locale.ROOT, rowFormat,
                    lesson.getLessonId(), DISPLAY_DATE.format(lesson.getDate()),
                    DISPLAY_TIME.format(lesson.getTime()), studentLabel(lesson), lesson.getSubject(),
                    money(lesson.getFee()), lesson.getPaymentStatus()));
            total = total.add(lesson.getFee());
        }
        output.append(System.lineSeparator()).append("Total fees: ").append(money(total));
        return output.toString();
    }

    /** Formats a lesson's student as "S1 Amirah Binte Rahman". */
    private String studentLabel(Lesson lesson) {
        Student student = data.getStudent(lesson.getStudentId());
        return student.getStudentId() + " " + student.getName();
    }

    private static String money(BigDecimal amount) {
        // Lesson only accepts fees with at most two decimals, so rounding is never needed here.
        assert amount.scale() <= 2 : "Fee has more than two decimal places: " + amount;
        return "$" + amount.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }

    private static String count(int amount, String noun) {
        return amount + " " + noun + (amount == 1 ? "" : "s");
    }

    /**
     * The filters a lesson must pass. A null field means that filter was not given.
     *
     * @param student Only lessons of this student.
     * @param from Only lessons on or after this date.
     * @param to Only lessons on or before this date.
     * @param payment Only lessons with this payment status; {@code unpaid} also includes partial.
     */
    private record Criteria(Student student, LocalDate from, LocalDate to, String payment) {
        boolean matches(Lesson lesson) {
            return (student == null || student.getStudentId().equalsIgnoreCase(lesson.getStudentId()))
                    && (from == null || !lesson.getDate().isBefore(from))
                    && (to == null || !lesson.getDate().isAfter(to))
                    && (payment == null || payment.equalsIgnoreCase(lesson.getPaymentStatus())
                            || (payment.equals(Lesson.PAYMENT_UNPAID)
                                    && lesson.getPaymentStatus().equals(Lesson.PAYMENT_PARTIAL)));
        }

        /** Describes the filters in words, e.g. "S1 Amirah Binte Rahman, from Tue 01-09-2026, unpaid". */
        String describe() {
            List<String> parts = new ArrayList<>();
            if (student != null) {
                parts.add(student.getStudentId() + " " + student.getName());
            }
            if (from != null) {
                parts.add("from " + DISPLAY_DATE.format(from));
            }
            if (to != null) {
                parts.add("to " + DISPLAY_DATE.format(to));
            }
            if (payment != null) {
                parts.add(payment);
            }
            return parts.isEmpty() ? "all lessons" : String.join(", ", parts);
        }
    }
}

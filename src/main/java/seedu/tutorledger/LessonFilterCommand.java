package seedu.tutorledger;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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
public class LessonFilterCommand extends CommandHandler {
    private static final Logger LOGGER = Logger.getLogger(LessonFilterCommand.class.getName());

    private static final String COMMAND_WORD = "filter";
    private static final String USAGE = "Use: filter [STUDENT_ID] [from/DATE] [to/DATE] [pay/STATUS]";




    private final TutorLedgerData data;

    /**
     * Creates the handler for the filter command.
     *
     * @param data The records to search.
     */
    public LessonFilterCommand(TutorLedgerData data) {
        super(Set.of(COMMAND_WORD));
        assert data != null : "LessonFilterCommand needs the records to search";
        this.data = data;
    }

    /** Runs the filter command and returns the matching lessons with their total fees. */
    @Override
    protected String run(CommandLine line) {
        Criteria criteria = parseCriteria(line.arguments());
        List<Lesson> matches = data.getLessons().stream()
                .filter(criteria::matches)
                .sorted(Comparator.comparing(Lesson::getDate)
                        .thenComparing(Lesson::getTime)
                        .thenComparing(Lesson::getLessonId))
                .toList();
        LOGGER.log(Level.FINE, "Filter matched {0} of {1} lessons",
                new Object[]{matches.size(), data.getLessons().size()});
        return format(matches, criteria);
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
            return LocalDate.parse(value, Formats.INPUT_DATE);
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
        StringBuilder output = new StringBuilder(Formats.count(lessons.size(), "lesson"))
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
                    lesson.getLessonId(), Formats.DISPLAY_DATE.format(lesson.getDate()),
                    Formats.TIME.format(lesson.getTime()), studentLabel(lesson), lesson.getSubject(),
                    Formats.money(lesson.getFee()), lesson.getPaymentStatus()));
            if (lesson.isCancelled()) {
                output.append(" [cancelled: ").append(lesson.getCancellationReason()).append("; not chargeable]");
            } else {
                total = total.add(lesson.getFee());
            }
        }
        String totalLabel = lessons.stream().anyMatch(Lesson::isCancelled)
                ? "Total fees (excluding cancelled lessons): " : "Total fees: ";
        output.append(System.lineSeparator()).append(totalLabel).append(Formats.money(total));
        return output.toString();
    }

    /** Formats a lesson's student as "S1 Amirah Binte Rahman". */
    private String studentLabel(Lesson lesson) {
        Student student = data.getStudent(lesson.getStudentId());
        return student.getStudentId() + " " + student.getName();
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
                    && (payment == null || !lesson.isCancelled())
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
                parts.add("from " + Formats.DISPLAY_DATE.format(from));
            }
            if (to != null) {
                parts.add("to " + Formats.DISPLAY_DATE.format(to));
            }
            if (payment != null) {
                parts.add(payment);
            }
            return parts.isEmpty() ? "all lessons" : String.join(", ", parts);
        }
    }
}

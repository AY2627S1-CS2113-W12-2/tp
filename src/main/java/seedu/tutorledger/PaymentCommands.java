package seedu.tutorledger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses and runs commands that update lesson payment status.
 */
public class PaymentCommands {
    private static final Set<String> COMMAND_WORDS = Set.of("mark", "unpaid", "owed");
    private static final Pattern MARK_FORMAT = Pattern.compile("(?i)(\\S+)\\s+(\\S+)");
    private static final DateTimeFormatter DISPLAY_DATE =
            DateTimeFormatter.ofPattern("EEE dd-MM-uuuu", Locale.ENGLISH);
    private static final String SEPARATOR = "_".repeat(60);

    private final TutorLedgerData data;
    private final Clock clock;

    public PaymentCommands(TutorLedgerData data) {
        this(data, Clock.systemDefaultZone());
    }

    public PaymentCommands(TutorLedgerData data, Clock clock) {
        this.data = data;
        this.clock = clock;
    }

    /** Returns true if the input begins with a payment command. */
    public static boolean handles(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }
        String command = input.strip().split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        return COMMAND_WORDS.contains(command);
    }

    /** Executes one payment command and returns its display text or a validation error. */
    public String execute(String input) {
        if (!handles(input)) {
            return "Unknown payment command.";
        }
        String[] words = input.strip().split("\\s+", 2);
        String command = words[0].toLowerCase(Locale.ROOT);
        String arguments = words.length == 2 ? words[1].trim() : "";
        if (command.equals("unpaid")) {
            return listUnpaid(arguments);
        }
        if (command.equals("owed")) {
            return listOwed(arguments);
        }
        return mark(arguments);
    }

    private String mark(String arguments) {
        Matcher matcher = MARK_FORMAT.matcher(arguments);
        if (!matcher.matches()) {
            return "Error: Use: mark LESSON_ID STATUS (STATUS must be paid or unpaid).";
        }

        String status = matcher.group(2).toLowerCase(Locale.ROOT);
        if (!status.equals(Lesson.PAYMENT_PAID) && !status.equals(Lesson.PAYMENT_UNPAID)) {
            return "Error: STATUS must be paid or unpaid.";
        }
        try {
            Lesson lesson = data.updateLessonPaymentStatus(matcher.group(1), status);
            return "Marked " + lesson.getLessonId() + " as " + lesson.getPaymentStatus() + ".";
        } catch (IllegalArgumentException exception) {
            return "Error: " + exception.getMessage();
        }
    }

    /** Lists unpaid lessons that have taken place, optionally for one student. */
    private String listUnpaid(String studentId) {
        String selectedStudentId = null;
        if (!studentId.isEmpty()) {
            try {
                selectedStudentId = data.getStudent(studentId).getStudentId();
            } catch (IllegalArgumentException exception) {
                return "Error: " + exception.getMessage();
            }
        }

        List<Lesson> unpaidLessons = unpaidLessons(LocalDate.now(clock), selectedStudentId);

        int studentWidth = unpaidLessons.stream()
                .map(lesson -> studentLabel(data.getStudent(lesson.getStudentId())).length())
                .max(Integer::compareTo)
                .orElse(0);
        int subjectWidth = unpaidLessons.stream()
                .map(lesson -> lesson.getSubject().length())
                .max(Integer::compareTo)
                .orElse(0);

        StringBuilder output = new StringBuilder("        ").append(SEPARATOR)
                .append(System.lineSeparator())
                .append(unpaidLessons.size()).append(" unpaid ")
                .append(unpaidLessons.size() == 1 ? "lesson" : "lessons");
        for (Lesson lesson : unpaidLessons) {
            Student student = data.getStudent(lesson.getStudentId());
            output.append(System.lineSeparator())
                    .append(String.format(Locale.ROOT, "%s  %s  %-"
                                    + studentWidth + "s  %-"
                                    + subjectWidth + "s  $%s",
                            lesson.getLessonId(), DISPLAY_DATE.format(lesson.getDate()),
                            studentLabel(student), lesson.getSubject(),
                            lesson.getFee().setScale(2, RoundingMode.UNNECESSARY).toPlainString()));
        }
        output.append(System.lineSeparator()).append("       ").append(SEPARATOR);
        return output.toString();
    }

    /** Totals fees for students with unpaid lessons that have taken place. */
    private String listOwed(String arguments) {
        if (!arguments.isEmpty()) {
            return "Error: Use: owed.";
        }

        Map<String, OwedTotal> totals = new HashMap<>();
        for (Lesson lesson : unpaidLessons(LocalDate.now(clock), null)) {
            totals.merge(lesson.getStudentId().toUpperCase(Locale.ROOT),
                    new OwedTotal(lesson.getFee(), 1), OwedTotal::plus);
        }

        List<Student> owingStudents = data.findStudents("").stream()
                .filter(student -> totals.containsKey(student.getStudentId()))
                .toList();
        StringBuilder output = new StringBuilder("        ").append(SEPARATOR)
                .append(System.lineSeparator());
        if (owingStudents.isEmpty()) {
            output.append("No students owe anything.");
        } else {
            output.append("Outstanding balances (").append(owingStudents.size())
                    .append(owingStudents.size() == 1 ? " student)" : " students)");
            for (Student student : owingStudents) {
                OwedTotal total = totals.get(student.getStudentId());
                output.append(System.lineSeparator())
                        .append(studentLabel(student)).append("  $")
                        .append(total.amount().setScale(2, RoundingMode.UNNECESSARY).toPlainString())
                        .append("  (").append(total.lessonCount())
                        .append(total.lessonCount() == 1 ? " lesson)" : " lessons)");
            }
        }
        output.append(System.lineSeparator()).append("       ").append(SEPARATOR);
        return output.toString();
    }

    private List<Lesson> unpaidLessons(LocalDate today, String studentId) {
        return data.getLessons().stream()
                .filter(lesson -> Lesson.PAYMENT_UNPAID.equalsIgnoreCase(lesson.getPaymentStatus()))
                .filter(lesson -> !lesson.getDate().isAfter(today))
                .filter(lesson -> studentId == null || studentId.equalsIgnoreCase(lesson.getStudentId()))
                .sorted(Comparator.comparing(Lesson::getDate)
                        .thenComparing(Lesson::getTime)
                        .thenComparing(Lesson::getLessonId))
                .toList();
    }

    private static String studentLabel(Student student) {
        return student.getStudentId() + " " + student.getName();
    }

    private record OwedTotal(BigDecimal amount, int lessonCount) {
        private OwedTotal plus(OwedTotal other) {
            return new OwedTotal(amount.add(other.amount), lessonCount + other.lessonCount);
        }
    }
}

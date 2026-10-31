package seedu.tutorledger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses and runs commands that update lesson payment status.
 */
public class PaymentCommands {
    private static final Logger LOGGER = Logger.getLogger(PaymentCommands.class.getName());
    private static final Set<String> COMMAND_WORDS = Set.of("mark", "pay", "unpaid", "owed", "fee_statement",
            "income");
    private static final Pattern MARK_FORMAT = Pattern.compile("(?i)(\\S+)\\s+(\\S+)");
    private static final Pattern INCOME_FORMAT = Pattern.compile("([0-9]{2})-([0-9]{4})|([0-9]{4})");
    private static final DateTimeFormatter DISPLAY_DATE =
            DateTimeFormatter.ofPattern("EEE dd-MM-uuuu", Locale.ENGLISH);
    private static final DateTimeFormatter DISPLAY_MONTH =
            DateTimeFormatter.ofPattern("MMMM uuuu", Locale.ENGLISH);
    private static final String SEPARATOR = "_".repeat(60);

    private final TutorLedgerData data;
    private final Clock clock;

    public PaymentCommands(TutorLedgerData data) {
        this(data, Clock.systemDefaultZone());
    }

    public PaymentCommands(TutorLedgerData data, Clock clock) {
        assert data != null : "PaymentCommands needs shared records";
        assert clock != null : "PaymentCommands needs a clock";
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
        LOGGER.log(Level.FINE, "Running payment command {0} with arguments: {1}",
                new Object[]{command, arguments});
        try {
            return switch (command) {
            case "unpaid" -> listUnpaid(arguments);
            case "owed" -> listOwed(arguments);
            case "fee_statement" -> feeStatement(arguments);
            case "income" -> income(arguments);
            case "pay" -> pay(arguments);
            case "mark" -> mark(arguments);
            // handles() lets only payment command words through, so reaching here means a word was missed above.
            default -> throw new AssertionError("Unexpected payment command: " + command);
            };
        } catch (IllegalArgumentException exception) {
            LOGGER.log(Level.FINE, "Payment command {0} rejected: {1}",
                    new Object[]{command, exception.getMessage()});
            return "Error: " + exception.getMessage();
        }
    }

    /** Allocates a payment across the listed lessons in order, allowing the final lesson to be partial. */
    private String pay(String arguments) {
        String[] words = arguments.split("\\s+");
        if (arguments.isBlank() || words.length < 3) {
            return "Error: Use: pay STUDENT_ID AMOUNT LESSON_ID [LESSON_ID...].";
        }

        Student student = data.getStudent(words[0]);
        BigDecimal amount = parsePaymentAmount(words[1]);
        List<Lesson> lessons = validateLessonsForPayment(student, words);
        BigDecimal totalOwed = lessons.stream().map(Lesson::getAmountOwed).reduce(BigDecimal.ZERO, BigDecimal::add);

        if (amount.compareTo(totalOwed) > 0) {
            throw new IllegalArgumentException("Payment exceeds the selected lessons' outstanding balance ($"
                    + formatMoney(totalOwed) + ").");
        }

        List<String> paidLessonIds = applyPayment(lessons, amount);
        return paymentConfirmation(amount, paidLessonIds);
    }

    /** Parses a positive dollar amount with at most two decimal places. */
    private static BigDecimal parsePaymentAmount(String amountText) {
        if (!amountText.matches("\\d+(?:\\.\\d{1,2})?")) {
            throw new IllegalArgumentException("AMOUNT must be a non-negative dollar amount with at most 2 decimals.");
        }
        BigDecimal amount = new BigDecimal(amountText);
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("AMOUNT must be greater than $0.00.");
        }
        return amount;
    }

    /** Resolves and validates every lesson before any payment records are updated. */
    private List<Lesson> validateLessonsForPayment(Student student, String[] words) {
        Set<String> lessonIds = new HashSet<>();
        List<Lesson> lessons = new ArrayList<>();
        for (int index = 2; index < words.length; index++) {
            Lesson lesson = data.getLesson(words[index]);
            String lessonId = lesson.getLessonId().toUpperCase(Locale.ROOT);
            if (!lessonIds.add(lessonId)) {
                throw new IllegalArgumentException("Lesson " + lesson.getLessonId() + " was listed more than once.");
            }
            if (!student.getStudentId().equalsIgnoreCase(lesson.getStudentId())) {
                throw new IllegalArgumentException("Lesson " + lesson.getLessonId() + " does not belong to "
                        + student.getStudentId() + ".");
            }
            if (lesson.isCancelled()) {
                throw new IllegalArgumentException("Cancelled lesson " + lesson.getLessonId() + " cannot be paid.");
            }
            if (Lesson.PAYMENT_PAID.equalsIgnoreCase(lesson.getPaymentStatus())) {
                throw new IllegalArgumentException("Lesson " + lesson.getLessonId() + " is already paid.");
            }
            lessons.add(lesson);
        }
        return lessons;
    }

    /** Allocates the payment in command order and returns the IDs that received money. */
    private List<String> applyPayment(List<Lesson> lessons, BigDecimal amount) {
        BigDecimal remaining = amount;
        List<String> paidLessonIds = new ArrayList<>();
        for (Lesson lesson : lessons) {
            BigDecimal allocation = remaining.min(lesson.getAmountOwed());
            if (allocation.signum() > 0) {
                data.updateLessonAmountPaid(lesson.getLessonId(), lesson.getAmountPaid().add(allocation));
                paidLessonIds.add(lesson.getLessonId());
                remaining = remaining.subtract(allocation);
            }
            if (remaining.signum() == 0) {
                break;
            }
        }
        return paidLessonIds;
    }

    /** Formats a confirmation for the lessons that received the payment. */
    private static String paymentConfirmation(BigDecimal amount, List<String> paidLessonIds) {
        return "Recorded payment of $" + formatMoney(amount) + " for "
                + paidLessonIds.size() + (paidLessonIds.size() == 1 ? " lesson: " : " lessons: ")
                + String.join(", ", paidLessonIds) + ".";
    }

    private static String formatMoney(BigDecimal amount) {
        assert amount.scale() <= 2 : "Payment amount has more than two decimal places: " + amount;
        return amount.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
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
        Lesson lesson = data.updateLessonPaymentStatus(matcher.group(1), status);
        return "Marked " + lesson.getLessonId() + " as " + lesson.getPaymentStatus() + ".";
    }

    /** Lists unpaid lessons that have taken place, optionally for one student. */
    private String listUnpaid(String studentId) {
        String selectedStudentId = null;
        if (!studentId.isEmpty()) {
            selectedStudentId = data.getStudent(studentId).getStudentId();
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
        String rowFormat = "%s  %s  %-"
                + studentWidth + "s  %-"
                + subjectWidth + "s  $%s";

        StringBuilder output = new StringBuilder("        ").append(SEPARATOR)
                .append(System.lineSeparator())
                .append(unpaidLessons.size()).append(" unpaid ")
                .append(unpaidLessons.size() == 1 ? "lesson" : "lessons");
        for (Lesson lesson : unpaidLessons) {
            Student student = data.getStudent(lesson.getStudentId());
            output.append(System.lineSeparator())
                    .append(String.format(Locale.ROOT, rowFormat,
                            lesson.getLessonId(), DISPLAY_DATE.format(lesson.getDate()),
                            studentLabel(student), lesson.getSubject(),
                            lesson.getAmountOwed().setScale(2, RoundingMode.UNNECESSARY).toPlainString()));
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
                    new OwedTotal(lesson.getAmountOwed(), 1), OwedTotal::plus);
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

    /** Lists all lesson fees and their payment statuses for one student. */
    private String feeStatement(String studentId) {
        if (studentId.isEmpty() || studentId.matches(".*\\s+.*")) {
            return "Error: Use: fee_statement STUDENT_ID.";
        }

        Student student = data.getStudent(studentId);

        List<Lesson> lessons = data.getLessons().stream()
                .filter(lesson -> student.getStudentId().equalsIgnoreCase(lesson.getStudentId()))
                .sorted(Comparator.comparing(Lesson::getDate)
                        .thenComparing(Lesson::getTime)
                        .thenComparing(Lesson::getLessonId))
                .toList();

        StringBuilder output = new StringBuilder("        ").append(SEPARATOR)
                .append(System.lineSeparator())
                .append("Payment history for ").append(studentLabel(student))
                .append(" (").append(lessons.size()).append(lessons.size() == 1 ? " lesson)" : " lessons)");
        if (lessons.isEmpty()) {
            output.append(System.lineSeparator()).append("No lesson payments found.");
        }
        for (Lesson lesson : lessons) {
            output.append(System.lineSeparator())
                    .append(String.format(Locale.ROOT, "%s  %s  %-20s  $%s  %s",
                            lesson.getLessonId(), DISPLAY_DATE.format(lesson.getDate()), lesson.getSubject(),
                            lesson.getFee().setScale(2, RoundingMode.UNNECESSARY).toPlainString(),
                            lesson.getPaymentStatus() + (lesson.getAmountPaid().signum() > 0
                                    && !Lesson.PAYMENT_PAID.equals(lesson.getPaymentStatus())
                                    ? " ($" + formatMoney(lesson.getAmountPaid()) + " paid)" : "")));
        }
        output.append(System.lineSeparator()).append("       ").append(SEPARATOR);
        return output.toString();
    }

    /** Lists income from paid lessons for one month or for every month in a year. */
    private String income(String period) {
        Matcher matcher = INCOME_FORMAT.matcher(period);
        if (!matcher.matches()) {
            return "Error: Use: income MM-YYYY or income YYYY.";
        }

        if (matcher.group(3) != null) {
            int year = Integer.parseInt(matcher.group(3));
            if (year == 0) {
                return "Error: YEAR must be between 0001 and 9999.";
            }
            return yearlyIncome(year);
        }

        int month = Integer.parseInt(matcher.group(1));
        int year = Integer.parseInt(matcher.group(2));
        if (month < 1 || month > 12 || year == 0) {
            return "Error: MONTH must be 01-12 and YEAR must be between 0001 and 9999.";
        }
        return monthlyIncome(YearMonth.of(year, month));
    }

    private String monthlyIncome(YearMonth month) {
        List<Lesson> payments = paidLessons(month);
        BigDecimal total = payments.stream().map(Lesson::getAmountPaid).reduce(BigDecimal.ZERO, BigDecimal::add);
        StringBuilder output = new StringBuilder("        ").append(SEPARATOR)
                .append(System.lineSeparator())
                .append("Income for ").append(DISPLAY_MONTH.format(month)).append(": $")
                .append(total.setScale(2, RoundingMode.UNNECESSARY).toPlainString());
        appendPayments(output, payments);
        output.append(System.lineSeparator()).append("       ").append(SEPARATOR);
        return output.toString();
    }

    private String yearlyIncome(int year) {
        List<Lesson> allPayments = data.getLessons().stream()
                .filter(lesson -> lesson.getAmountPaid().signum() > 0)
                .filter(lesson -> !lesson.isCancelled())
                .filter(lesson -> lesson.getDate().getYear() == year)
                .sorted(Comparator.comparing(Lesson::getDate)
                        .thenComparing(Lesson::getTime)
                        .thenComparing(Lesson::getLessonId))
                .toList();

        StringBuilder output = new StringBuilder("        ").append(SEPARATOR)
                .append(System.lineSeparator())
                .append("Income for ").append(year);
        for (Month month : Month.values()) {
            YearMonth yearMonth = YearMonth.of(year, month);
            List<Lesson> payments = allPayments.stream()
                    .filter(lesson -> YearMonth.from(lesson.getDate()).equals(yearMonth))
                    .toList();
            BigDecimal total = payments.stream().map(Lesson::getAmountPaid).reduce(BigDecimal.ZERO, BigDecimal::add);
            output.append(System.lineSeparator())
                    .append(month.getDisplayName(TextStyle.FULL, Locale.ENGLISH))
                    .append(": $").append(total.setScale(2, RoundingMode.UNNECESSARY).toPlainString());
            appendPayments(output, payments);
        }
        output.append(System.lineSeparator()).append("       ").append(SEPARATOR);
        return output.toString();
    }

    private List<Lesson> paidLessons(YearMonth month) {
        return data.getLessons().stream()
                .filter(lesson -> lesson.getAmountPaid().signum() > 0)
                .filter(lesson -> !lesson.isCancelled())
                .filter(lesson -> YearMonth.from(lesson.getDate()).equals(month))
                .sorted(Comparator.comparing(Lesson::getDate)
                        .thenComparing(Lesson::getTime)
                        .thenComparing(Lesson::getLessonId))
                .toList();
    }

    private void appendPayments(StringBuilder output, List<Lesson> payments) {
        for (Lesson lesson : payments) {
            Student student = data.getStudent(lesson.getStudentId());
            output.append(System.lineSeparator())
                    .append(String.format(Locale.ROOT, "  %s  %s  %s  %s  $%s",
                            lesson.getLessonId(), DISPLAY_DATE.format(lesson.getDate()),
                            studentLabel(student), lesson.getSubject(),
                            lesson.getAmountPaid().setScale(2, RoundingMode.UNNECESSARY).toPlainString()));
        }
    }

    private List<Lesson> unpaidLessons(LocalDate today, String studentId) {
        return data.getLessons().stream()
                .filter(lesson -> !Lesson.PAYMENT_PAID.equalsIgnoreCase(lesson.getPaymentStatus()))
                // A cancelled lesson did not take place, so there is nothing to charge for it.
                .filter(lesson -> !lesson.isCancelled())
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

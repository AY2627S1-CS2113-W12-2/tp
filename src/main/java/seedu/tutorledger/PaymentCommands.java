package seedu.tutorledger;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses and runs commands that update lesson payment status.
 */
public class PaymentCommands {
    private static final Set<String> COMMAND_WORDS = Set.of("mark");
    private static final Pattern MARK_FORMAT = Pattern.compile("(?i)(\\S+)\\s+(\\S+)");

    private final TutorLedgerData data;

    public PaymentCommands(TutorLedgerData data) {
        this.data = data;
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
        String arguments = words.length == 2 ? words[1].trim() : "";
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
}

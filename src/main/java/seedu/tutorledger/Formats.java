package seedu.tutorledger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Locale;

/**
 * Holds the date, time, money and count formats that every command shows, so the same value always looks the
 * same wherever it appears.
 *
 * <p>Before this class, each command class kept its own copy of these formats. Keeping one copy means a change,
 * such as showing dates differently, is made once and cannot leave two commands disagreeing.
 */
final class Formats {
    /**
     * A date as the user types it and as the data file stores it, e.g. 20-10-2026. STRICT rejects dates that do
     * not exist, such as 31-02-2026, instead of quietly moving them to the end of the month.
     */
    static final DateTimeFormatter INPUT_DATE = DateTimeFormatter.ofPattern("dd-MM-uuuu", Locale.ROOT)
            .withResolverStyle(ResolverStyle.STRICT);

    /** A date as shown to the user, with its weekday, e.g. Tue 20-10-2026. */
    static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("EEE dd-MM-uuuu", Locale.ENGLISH);

    /** A 24-hour time, as typed, stored and shown, e.g. 1600. */
    static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HHmm", Locale.ROOT);

    /** Prevents instantiation, because this class only holds shared formats. */
    private Formats() {
    }

    /**
     * Returns an amount with two decimal places and no dollar sign, e.g. 62.50.
     *
     * @param amount A fee, payment or total; fees and payments have at most two decimal places.
     */
    static String amount(BigDecimal amount) {
        // Fees and payments are validated to two decimals and sums of them keep that, so nothing is ever rounded.
        assert amount.scale() <= 2 : "Amount has more than two decimal places: " + amount;
        return amount.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }

    /** Returns an amount with a dollar sign and two decimal places, e.g. $62.50. */
    static String money(BigDecimal amount) {
        return "$" + amount(amount);
    }

    /** Returns a count with its noun, in the plural unless the count is 1, e.g. "1 lesson" or "3 lessons". */
    static String count(long amount, String noun) {
        return amount + " " + noun + (amount == 1 ? "" : "s");
    }

    /** Returns a student as their ID and name, e.g. "S1 Amirah Binte Rahman". */
    static String studentLabel(Student student) {
        return student.getStudentId() + " " + student.getName();
    }
}

package seedu.tutorledger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a student's result for one test or assessment, such as 28 out of 40 for "WA1" (feature F7.1).
 *
 * <p>Marks are kept as {@link BigDecimal} so half marks such as 17.5 are stored exactly. Like the other
 * records, a score is immutable once created.
 */
public class Score {
    private static final Logger LOGGER = Logger.getLogger(Score.class.getName());

    /** Date format used when a score is created from text, e.g. 21-09-2026. */
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd-MM-uuuu", Locale.ROOT)
            .withResolverStyle(ResolverStyle.STRICT);

    /** Marks may have up to 2 decimal places, like a fee. */
    private static final String NUMBER_FORMAT = "\\d+(?:\\.\\d{1,2})?";

    private final String scoreId;
    private final String studentId;
    private final String testName;
    private final BigDecimal marks;
    private final BigDecimal totalMarks;
    private final LocalDate date;

    /**
     * Creates a score after validating every value.
     *
     * @param scoreId The score's ID, such as A1.
     * @param studentId The ID of the student who took the test.
     * @param testName What the test was, such as "WA1" or "Mid-year exam".
     * @param marks The marks scored, as typed (e.g. "28" or "17.5").
     * @param totalMarks The maximum possible marks, as typed (e.g. "40").
     * @param date The date the test was taken, in DD-MM-YYYY format.
     * @throws IllegalArgumentException If any value is missing or invalid.
     */
    public Score(String scoreId, String studentId, String testName, String marks, String totalMarks, String date) {
        this(scoreId, studentId, testName, parseNumber(marks, "Marks"), parseNumber(totalMarks, "Total marks"),
                parseDate(date));
    }

    /** Creates a score from values that have already been converted. */
    public Score(String scoreId, String studentId, String testName, BigDecimal marks, BigDecimal totalMarks,
            LocalDate date) {
        this.scoreId = requireText(scoreId, "Score ID");
        this.studentId = requireText(studentId, "Student ID");
        this.testName = requireText(testName, "Test name").strip();
        if (marks == null || totalMarks == null || date == null) {
            throw new IllegalArgumentException("Marks, total marks and date are all needed.");
        }
        if (totalMarks.signum() <= 0) {
            throw new IllegalArgumentException("Total marks must be more than 0.");
        }
        if (marks.signum() < 0 || marks.compareTo(totalMarks) > 0) {
            throw new IllegalArgumentException("Marks must be between 0 and the total marks.");
        }
        this.marks = marks;
        this.totalMarks = totalMarks;
        this.date = date;
        LOGGER.log(Level.FINE, "Created score {0} for {1}: {2}/{3}",
                new Object[]{scoreId, studentId, marks, totalMarks});
    }

    public String getScoreId() {
        return scoreId;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getTestName() {
        return testName;
    }

    public BigDecimal getMarks() {
        return marks;
    }

    public BigDecimal getTotalMarks() {
        return totalMarks;
    }

    public LocalDate getDate() {
        return date;
    }

    /**
     * Returns the score as a percentage rounded to one decimal place, e.g. 70.0 for 28 out of 40.
     * Percentages let tests with different totals be compared fairly.
     */
    public BigDecimal getPercentage() {
        BigDecimal percentage = marks.multiply(BigDecimal.valueOf(100)).divide(totalMarks, 1, RoundingMode.HALF_UP);
        assert percentage.signum() >= 0 && percentage.compareTo(BigDecimal.valueOf(100)) <= 0
                : "A valid score must be between 0% and 100%";
        return percentage;
    }

    /** Returns the marks as the user would write them, e.g. "28/40" or "17.5/20". */
    public String getMarksText() {
        return plain(marks) + "/" + plain(totalMarks);
    }

    /** Writes a number without needless trailing zeros, so 28.00 is shown as 28. */
    private static String plain(BigDecimal number) {
        return number.stripTrailingZeros().toPlainString();
    }

    private static BigDecimal parseNumber(String value, String fieldName) {
        if (value == null || !value.strip().matches(NUMBER_FORMAT)) {
            throw new IllegalArgumentException(fieldName + " must be a number of 0 or more, with at most 2 decimals.");
        }
        return new BigDecimal(value.strip());
    }

    private static LocalDate parseDate(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Date cannot be null.");
        }
        try {
            return LocalDate.parse(value.strip(), DATE);
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException("Date must be a real date in DD-MM-YYYY format.", exception);
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank.");
        }
        return value;
    }
}

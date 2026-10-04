package seedu.duke;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.util.Locale;

/**
 * Represents a lesson, including its schedule, attendance, fee, and payment status.
 */
public class Lesson {
    private static final DateTimeFormatter DATE_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("dd-MM-uuuu")
            .toFormatter(Locale.ROOT)
            .withResolverStyle(ResolverStyle.STRICT);

    private final String lessonId;
    private final String subject;
    private final LocalDate date;
    private final LocalTime time;
    private final Attendance attendance;
    private final BigDecimal fee;
    private final String notes;
    private final String paymentStatus;

    /**
     * Creates a lesson after validating and converting its details.
     *
     * @param lessonId the lesson identifier
     * @param subject the lesson subject
     * @param date the lesson date in DD-MM-YYYY format
     * @param time the lesson time in HHMM format
     * @param attendance the attendance status
     * @param fee the non-negative fee, with at most two decimal places
     * @param notes lesson notes
     * @param paymentStatus the payment status
     */
    public Lesson(String lessonId, String subject, String date, String time, String attendance,
            String fee, String notes, String paymentStatus) {
        this.lessonId = requireText(lessonId, "Lesson ID");
        this.subject = requireText(subject, "Subject");
        this.date = parseDate(date);
        this.time = parseTime(time);
        this.attendance = Attendance.parse(attendance);
        this.fee = parseFee(fee);
        this.notes = requireNonNull(notes, "Notes");
        this.paymentStatus = requireText(paymentStatus, "Payment status");
    }

    public String getLessonId() {
        return lessonId;
    }

    public String getSubject() {
        return subject;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getTime() {
        return time;
    }

    public Attendance getAttendance() {
        return attendance;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public String getNotes() {
        return notes;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    private static String requireText(String value, String fieldName) {
        requireNonNull(value, fieldName);
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank.");
        }
        return value;
    }

    private static String requireNonNull(String value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null.");
        }
        return value;
    }

    private static LocalDate parseDate(String value) {
        requireNonNull(value, "Date");
        try {
            return LocalDate.parse(value, DATE_FORMATTER);
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException("Date must be a real date in DD-MM-YYYY format.", exception);
        }
    }

    private static LocalTime parseTime(String value) {
        requireNonNull(value, "Time");
        if (!value.matches("\\d{4}")) {
            throw new IllegalArgumentException("Time must be in 24-hour HHMM format.");
        }
        try {
            return LocalTime.of(Integer.parseInt(value.substring(0, 2)),
                    Integer.parseInt(value.substring(2, 4)));
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException("Time must be a valid 24-hour time in HHMM format.", exception);
        }
    }

    private static BigDecimal parseFee(String value) {
        requireNonNull(value, "Fee");
        if (!value.matches("\\d+(?:\\.\\d{1,2})?")) {
            throw new IllegalArgumentException("Fee must be a non-negative dollar amount with at most 2 decimals.");
        }
        return new BigDecimal(value);
    }

    /**
     * The attendance statuses accepted for a lesson.
     */
    public enum Attendance {
        PRESENT,
        ABSENT,
        LATE;

        private static Attendance parse(String value) {
            requireNonNull(value, "Attendance");
            try {
                return Attendance.valueOf(value.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Attendance must be present, absent, or late.", exception);
            }
        }
    }
}

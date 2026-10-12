package seedu.tutorledger;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a lesson, including its schedule, attendance, fee, and payment status.
 */
public class Lesson {
    private static final Logger LOGGER = Logger.getLogger(Lesson.class.getName());

    public static final String PAYMENT_PAID = "paid";
    public static final String PAYMENT_UNPAID = "unpaid";

    private static final DateTimeFormatter DATE_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("dd-MM-uuuu")
            .toFormatter(Locale.ROOT)
            .withResolverStyle(ResolverStyle.STRICT);

    private final String lessonId;
    private final String studentId;
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
     * @param date The lesson date in DD-MM-YYYY format.
     * @param time the lesson time in HHMM format
     * @param attendance the attendance status
     * @param fee the non-negative fee, with at most two decimal places
     * @param notes lesson notes
     */
    public Lesson(String lessonId, String subject, String date, String time, String attendance,
            String fee, String notes) {
        LOGGER.log(Level.INFO, "Creating lesson {0} for subject {1}", new Object[]{lessonId, subject});
        this.lessonId = requireText(lessonId, "Lesson ID");
        this.studentId = null;
        this.subject = requireText(subject, "Subject");
        this.date = parseDate(date);
        this.time = parseTime(time);
        this.attendance = Attendance.parse(attendance);
        this.fee = parseFee(fee);
        this.notes = requireNonNull(notes, "Notes");
        this.paymentStatus = validatePaymentStatus(PAYMENT_UNPAID);
        LOGGER.log(Level.INFO, "Lesson created: {0}", this.lessonId);
    }

    /** Creates a lesson linked to a student for student views and deletion. */
    public Lesson(String lessonId, String studentId, String subject, String date, String time, String attendance,
            String fee, String notes) {
        LOGGER.log(Level.INFO, "Creating lesson {0} for student {1}", new Object[]{lessonId, studentId});
        this.lessonId = requireText(lessonId, "Lesson ID");
        this.studentId = studentId;
        this.subject = requireText(subject, "Subject");
        this.date = parseDate(date);
        this.time = parseTime(time);
        this.attendance = Attendance.parse(attendance);
        this.fee = parseFee(fee);
        this.notes = requireNonNull(notes, "Notes");
        this.paymentStatus = validatePaymentStatus(PAYMENT_UNPAID);
        LOGGER.log(Level.INFO, "Lesson created: {0}", this.lessonId);
    }

    private Lesson(String lessonId, String studentId, String subject, String date, String time, String attendance,
            String fee, String notes, String paymentStatus) {
        LOGGER.log(Level.INFO, "Creating lesson {0} with payment status {1}", new Object[]{lessonId, paymentStatus});
        this.lessonId = requireText(lessonId, "Lesson ID");
        this.studentId = studentId;
        this.subject = requireText(subject, "Subject");
        this.date = parseDate(date);
        this.time = parseTime(time);
        this.attendance = Attendance.parse(attendance);
        this.fee = parseFee(fee);
        this.notes = requireNonNull(notes, "Notes");
        this.paymentStatus = validatePaymentStatus(paymentStatus);
        LOGGER.log(Level.INFO, "Lesson creation complete: {0}", this.lessonId);
    }

    /** Creates a lesson from values that have already been validated and converted. */
    private Lesson(String lessonId, String studentId, String subject, LocalDate date, LocalTime time,
            Attendance attendance, BigDecimal fee, String notes, String paymentStatus) {
        LOGGER.log(Level.INFO, "Creating validated lesson {0}", lessonId);
        this.lessonId = lessonId;
        this.studentId = studentId;
        this.subject = subject;
        this.date = date;
        this.time = time;
        this.attendance = attendance;
        this.fee = fee;
        this.notes = notes;
        this.paymentStatus = paymentStatus;
        LOGGER.log(Level.INFO, "Validated lesson created: {0}", lessonId);
    }

    private Lesson(Lesson lesson, String paymentStatus) {
        LOGGER.log(Level.INFO, "Updating payment status for lesson {0} to {1}", new Object[]{lesson.lessonId, paymentStatus});
        this.lessonId = lesson.lessonId;
        this.studentId = lesson.studentId;
        this.subject = lesson.subject;
        this.date = lesson.date;
        this.time = lesson.time;
        this.attendance = lesson.attendance;
        this.fee = lesson.fee;
        this.notes = lesson.notes;
        this.paymentStatus = validatePaymentStatus(paymentStatus);
        LOGGER.log(Level.INFO, "Lesson payment status updated: {0}", this.lessonId);
    }

    public String getLessonId() {
        LOGGER.log(Level.FINE, "Retrieving lesson ID for lesson {0}", lessonId);
        return lessonId;
    }

    public String getStudentId() {
        LOGGER.log(Level.FINE, "Retrieving student ID for lesson {0}", lessonId);
        return studentId;
    }

    public String getSubject() {
        LOGGER.log(Level.FINE, "Retrieving subject for lesson {0}", lessonId);
        return subject;
    }

    public LocalDate getDate() {
        LOGGER.log(Level.FINE, "Retrieving date for lesson {0}", lessonId);
        return date;
    }

    public LocalTime getTime() {
        LOGGER.log(Level.FINE, "Retrieving time for lesson {0}", lessonId);
        return time;
    }

    public Attendance getAttendance() {
        LOGGER.log(Level.FINE, "Retrieving attendance for lesson {0}", lessonId);
        return attendance;
    }

    public BigDecimal getFee() {
        LOGGER.log(Level.FINE, "Retrieving fee for lesson {0}", lessonId);
        return fee;
    }

    public String getNotes() {
        LOGGER.log(Level.FINE, "Retrieving notes for lesson {0}", lessonId);
        return notes;
    }

    public String getPaymentStatus() {
        LOGGER.log(Level.FINE, "Retrieving payment status for lesson {0}", lessonId);
        return paymentStatus;
    }

    /** Returns true once attendance has been recorded for this lesson. */
    public boolean isRecorded() {
        boolean recorded = attendance != Attendance.NOT_RECORDED;
        LOGGER.log(Level.FINE, "Lesson {0} recorded status: {1}", new Object[]{lessonId, recorded});
        return recorded;
    }

    /**
     * Returns a copy of this lesson with some details changed, leaving this lesson untouched.
     *
     * <p>Pass {@code null} for any detail that should stay as it is. The lesson ID, student and
     * payment status are always kept, because recording or editing a lesson never changes them.
     *
     * @param newSubject The new subject, or null to keep the current one.
     * @param newDate The new date in DD-MM-YYYY format, or null to keep the current one.
     * @param newTime The new time in HHMM format, or null to keep the current one.
     * @param newAttendance The new attendance status, or null to keep the current one.
     * @param newFee The new non-negative fee, or null to keep the current one.
     * @param newNotes The new notes, or null to keep the current ones.
     * @return The changed copy.
     * @throws IllegalArgumentException If any new value is invalid.
     */
    public Lesson withDetails(String newSubject, String newDate, String newTime, String newAttendance,
            String newFee, String newNotes) {
        LOGGER.log(Level.INFO, "Updating lesson {0} details: subject={1}, date={2}, time={3}",
                new Object[]{lessonId, newSubject, newDate, newTime});
        Lesson updated = new Lesson(lessonId, studentId,
                newSubject == null ? subject : requireText(newSubject, "Subject"),
                newDate == null ? date : parseDate(newDate),
                newTime == null ? time : parseTime(newTime),
                newAttendance == null ? attendance : Attendance.parse(newAttendance),
                newFee == null ? fee : parseFee(newFee),
                newNotes == null ? notes : newNotes,
                paymentStatus);
        LOGGER.log(Level.INFO, "Lesson {0} details updated", lessonId);
        return updated;
    }

    /** Returns a copy with the supplied paid/unpaid status. */
    public Lesson withPaymentStatus(String status) {
        LOGGER.log(Level.INFO, "Changing payment status for lesson {0} to {1}", new Object[]{lessonId, status});
        Lesson updated = new Lesson(this, status);
        LOGGER.log(Level.INFO, "Lesson {0} payment status changed to {1}", new Object[]{lessonId, updated.paymentStatus});
        return updated;
    }

    private static String validatePaymentStatus(String value) {
        LOGGER.log(Level.FINE, "Validating payment status: {0}", value);
        requireNonNull(value, "Payment status");
        if (!value.equalsIgnoreCase(PAYMENT_PAID) && !value.equalsIgnoreCase(PAYMENT_UNPAID)) {
            throw new IllegalArgumentException("Payment status must be paid or unpaid.");
        }
        return value.toLowerCase(Locale.ROOT);
    }

    private static String requireText(String value, String fieldName) {
        LOGGER.log(Level.FINE, "Validating text field {0}", fieldName);
        requireNonNull(value, fieldName);
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank.");
        }
        return value;
    }

    private static String requireNonNull(String value, String fieldName) {
        LOGGER.log(Level.FINE, "Validating non-null field {0}", fieldName);
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null.");
        }
        return value;
    }

    private static LocalDate parseDate(String value) {
        LOGGER.log(Level.FINE, "Parsing date value: {0}", value);
        requireNonNull(value, "Date");
        try {
            return LocalDate.parse(value, DATE_FORMATTER);
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException("Date must be a real date in DD-MM-YYYY format.", exception);
        }
    }

    private static LocalTime parseTime(String value) {
        LOGGER.log(Level.FINE, "Parsing time value: {0}", value);
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
        LOGGER.log(Level.FINE, "Parsing fee value: {0}", value);
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
        NOT_RECORDED,
        PRESENT,
        ABSENT,
        LATE;

        private static Attendance parse(String value) {
            LOGGER.log(Level.FINE, "Parsing attendance value: {0}", value);
            requireNonNull(value, "Attendance");
            try {
                return Attendance.valueOf(value.trim().replace(' ', '_').toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Attendance must be present, absent, or late.", exception);
            }
        }
    }
}

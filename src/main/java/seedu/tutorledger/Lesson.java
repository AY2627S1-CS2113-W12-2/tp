package seedu.tutorledger;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a lesson, including its schedule, attendance, fee, and payment status.
 *
 * <p>A lesson can also be cancelled with a reason. Cancelling is kept apart from attendance, so a
 * lesson the tutor called off is never mistaken for one the student missed.
 */
public class Lesson {
    public static final String PAYMENT_PAID = "paid";
    public static final String PAYMENT_UNPAID = "unpaid";
    /** Legacy lessons and commands without dur/ represent one hour. */
    public static final int DEFAULT_DURATION_MINUTES = 60;

    private static final Logger LOGGER = Logger.getLogger(Lesson.class.getName());

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
    /** Billing duration; changing it does not implicitly recalculate the saved fee. */
    private final int durationMinutes;
    /** Why the lesson was called off, or an empty string if it was not cancelled. */
    private final String cancellationReason;

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
        this.durationMinutes = DEFAULT_DURATION_MINUTES;
        this.cancellationReason = "";
        LOGGER.log(Level.INFO, "Lesson created: {0}", this.lessonId);
    }

    /** Creates a lesson linked to a student for student views and deletion. */
    public Lesson(String lessonId, String studentId, String subject, String date, String time, String attendance,
            String fee, String notes) {
        this(lessonId, studentId, subject, date, time, attendance, fee, notes, DEFAULT_DURATION_MINUTES);
    }

    /** Creates a linked lesson with a validated billing duration and an already chosen total fee. */
    public Lesson(String lessonId, String studentId, String subject, String date, String time, String attendance,
            String fee, String notes, int durationMinutes) {
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
        this.durationMinutes = validateDuration(durationMinutes);
        this.cancellationReason = "";
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
        this.durationMinutes = DEFAULT_DURATION_MINUTES;
        this.cancellationReason = "";
        LOGGER.log(Level.INFO, "Lesson creation complete: {0}", this.lessonId);
    }

    /** Creates a lesson from values that have already been validated and converted. */
    private Lesson(String lessonId, String studentId, String subject, LocalDate date, LocalTime time,
            Attendance attendance, BigDecimal fee, String notes, String paymentStatus,
            String cancellationReason, int durationMinutes) {
        LOGGER.log(Level.INFO, "Creating validated lesson {0}", lessonId);
        // This constructor skips validation, so check that its caller really did validate everything.
        assert lessonId != null && !lessonId.isBlank() : "Lesson ID should already be validated";
        assert subject != null && !subject.isBlank() : "Subject should already be validated";
        assert date != null && time != null : "Date and time should already be parsed";
        assert attendance != null : "Attendance should already be parsed";
        assert fee != null && fee.signum() >= 0 && fee.scale() <= 2 : "Fee should already be validated";
        assert notes != null : "Notes should be empty rather than null";
        assert durationMinutes >= 1 && durationMinutes <= 1440 : "Duration should already be validated";
        assert PAYMENT_PAID.equals(paymentStatus) || PAYMENT_UNPAID.equals(paymentStatus)
                : "Payment status should already be validated";
        assert cancellationReason != null : "A lesson that is not cancelled has an empty reason, not null";
        this.lessonId = lessonId;
        this.studentId = studentId;
        this.subject = subject;
        this.date = date;
        this.time = time;
        this.attendance = attendance;
        this.fee = fee;
        this.notes = notes;
        this.paymentStatus = paymentStatus;
        this.durationMinutes = durationMinutes;
        this.cancellationReason = cancellationReason;
        LOGGER.log(Level.INFO, "Validated lesson created: {0}", lessonId);
    }

    private Lesson(Lesson lesson, String paymentStatus) {
        LOGGER.log(Level.INFO, "Updating payment status for lesson {0} to {1}",
                new Object[]{lesson.lessonId, paymentStatus});
        this.lessonId = lesson.lessonId;
        this.studentId = lesson.studentId;
        this.subject = lesson.subject;
        this.date = lesson.date;
        this.time = lesson.time;
        this.attendance = lesson.attendance;
        this.fee = lesson.fee;
        this.notes = lesson.notes;
        this.paymentStatus = validatePaymentStatus(paymentStatus);
        this.durationMinutes = lesson.durationMinutes;
        this.cancellationReason = lesson.cancellationReason;
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

    public int getDurationMinutes() {
        return durationMinutes;
    }

    /** Rejects invalid duration values at the model boundary, including direct API calls. */
    static int validateDuration(int minutes) {
        if (minutes < 1 || minutes > 1440) {
            throw new IllegalArgumentException("Duration must be a whole number of minutes from 1 to 1440.");
        }
        return minutes;
    }

    /** Parses a command or hand-edited duration without leaking integer-overflow exceptions. */
    static int parseDuration(String value) {
        if (value == null || !value.matches("[0-9]{1,4}")) {
            throw new IllegalArgumentException("Duration must be a whole number of minutes from 1 to 1440.");
        }
        return validateDuration(Integer.parseInt(value));
    }

    /** Returns a copy with a new duration, retaining its saved fee and all historical details. */
    public Lesson withDurationMinutes(int minutes) {
        return new Lesson(lessonId, studentId, subject, date, time, attendance, fee, notes, paymentStatus,
                cancellationReason, validateDuration(minutes));
    }

    public String getNotes() {
        LOGGER.log(Level.FINE, "Retrieving notes for lesson {0}", lessonId);
        return notes;
    }

    public String getPaymentStatus() {
        LOGGER.log(Level.FINE, "Retrieving payment status for lesson {0}", lessonId);
        return paymentStatus;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    /** Returns true if the lesson was called off, so it will not take place and is not charged. */
    public boolean isCancelled() {
        return !cancellationReason.isEmpty();
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
     * <p>Pass {@code null} for any detail that should stay as it is. The lesson ID, student,
     * payment status and cancellation are always kept, because recording or editing a lesson never
     * changes them.
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
                paymentStatus, cancellationReason, durationMinutes);
        // As documented above, recording or editing must never change who or what the lesson is.
        assert updated.lessonId.equals(lessonId) : "withDetails changed the lesson ID";
        assert Objects.equals(updated.studentId, studentId) : "withDetails changed the student";
        assert updated.paymentStatus.equals(paymentStatus) : "withDetails changed the payment status";
        assert updated.cancellationReason.equals(cancellationReason) : "withDetails changed the cancellation";
        LOGGER.log(Level.INFO, "Lesson {0} details updated", lessonId);
        return updated;
    }

    /** Returns a copy with the supplied paid/unpaid status. */
    public Lesson withPaymentStatus(String status) {
        LOGGER.log(Level.INFO, "Changing payment status for lesson {0} to {1}", new Object[]{lessonId, status});
        Lesson updated = new Lesson(this, status);
        LOGGER.log(Level.INFO, "Lesson {0} payment status changed to {1}",
                new Object[]{lessonId, updated.paymentStatus});
        return updated;
    }

    /**
     * Returns a cancelled copy of this lesson that keeps every other detail.
     *
     * @param reason Why the lesson was called off, such as "Student sick".
     * @return The cancelled copy.
     * @throws IllegalArgumentException If the reason is blank.
     */
    public Lesson withCancellation(String reason) {
        LOGGER.log(Level.INFO, "Cancelling lesson {0}: {1}", new Object[]{lessonId, reason});
        Lesson cancelled = new Lesson(lessonId, studentId, subject, date, time, attendance, fee, notes,
                paymentStatus, requireText(reason, "Reason").strip(), durationMinutes);
        assert cancelled.isCancelled() : "A lesson given a non-blank reason should count as cancelled";
        return cancelled;
    }

    /**
     * Formats a date the way it is typed, e.g. 22-09-2026.
     *
     * @param date The date to format.
     * @return The date in DD-MM-YYYY format.
     */
    static String formatDate(LocalDate date) {
        return DATE_FORMATTER.format(date);
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

    /**
     * Returns the date typed in DD-MM-YYYY format.
     * It is shared with the lesson commands so that they reject a bad date with the same message.
     *
     * @param value The date as typed.
     * @return The parsed date.
     * @throws IllegalArgumentException If the value is not a real date in DD-MM-YYYY format.
     */
    static LocalDate parseDate(String value) {
        LOGGER.log(Level.FINE, "Parsing date value: {0}", value);
        requireNonNull(value, "Date");
        try {
            return LocalDate.parse(value, DATE_FORMATTER);
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException("Date must be a real date in DD-MM-YYYY format.", exception);
        }
    }

    /**
     * Returns the time typed in 24-hour HHMM format.
     *
     * @param value The time as typed.
     * @return The parsed time.
     * @throws IllegalArgumentException If the value is not a valid 24-hour HHMM time.
     */
    static LocalTime parseTime(String value) {
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

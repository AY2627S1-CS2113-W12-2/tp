package seedu.duke;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.util.Locale;

/**
 * Represents a student and the details of a lesson or assignment associated with them.
 */
public class Student {
    private static final DateTimeFormatter DATE_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("dd-MM-uuuu")
            .toFormatter(Locale.ROOT)
            .withResolverStyle(ResolverStyle.STRICT);

    private final String studentId;
    private final String name;
    private final String level;
    private final String phoneNumber;
    private final String subject;
    private final LocalDate date;
    private final LocalDate dueDate;
    private final LocalTime time;
    private final BigDecimal fee;
    private final Attendance attendance;
    private final String notes;
    private final String description;

    /**
     * Creates a student record after validating and converting the supplied values.
     *
     * @param studentId the student's identifier
     * @param name the student's name
     * @param level the student's secondary-school level
     * @param phoneNumber the student's eight-digit phone number
     * @param subject the lesson subject
     * @param date the lesson date in DD-MM-YYYY format
     * @param dueDate the due date in DD-MM-YYYY format
     * @param time the lesson time in HHMM format
     * @param fee the non-negative fee in dollars, with at most two decimal places
     * @param attendance the attendance status
     * @param notes free-form notes
     * @param description free-form description
     */
    public Student(String studentId, String name, String level, String phoneNumber, String subject,
            String date, String dueDate, String time, String fee, String attendance,
            String notes, String description) {
        this.studentId = requireNonNull(studentId, "Student ID");
        this.name = validateName(name);
        this.level = validateLevel(level);
        this.phoneNumber = validatePhoneNumber(phoneNumber);
        this.subject = requireNonNull(subject, "Subject");
        this.date = parseDate(date, "Date");
        this.dueDate = parseDate(dueDate, "Due date");
        this.time = parseTime(time);
        this.fee = parseFee(fee);
        this.attendance = Attendance.parse(attendance);
        this.notes = requireNonNull(notes, "Notes");
        this.description = requireNonNull(description, "Description");
    }

    public String getStudentId() {
        return studentId;
    }

    public String getName() {
        return name;
    }

    public String getLevel() {
        return level;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getSubject() {
        return subject;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public LocalTime getTime() {
        return time;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public Attendance getAttendance() {
        return attendance;
    }

    public String getNotes() {
        return notes;
    }

    public String getDescription() {
        return description;
    }

    private static String requireNonNull(String value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null.");
        }
        return value;
    }

    private static String validateName(String name) {
        requireNonNull(name, "Name");
        if (!name.matches("\\p{L}+(?:[ '\\-]\\p{L}+)*")) {
            throw new IllegalArgumentException(
                    "Name must contain only letters, spaces, hyphens, or apostrophes.");
        }
        return name;
    }

    private static String validateLevel(String level) {
        requireNonNull(level, "Level");
        if (!level.matches("(?i)Sec ?[1-5]")) {
            throw new IllegalArgumentException("Level must be Sec 1 to Sec 5.");
        }
        return "Sec " + level.substring(level.length() - 1);
    }

    private static String validatePhoneNumber(String phoneNumber) {
        requireNonNull(phoneNumber, "Phone number");
        if (!phoneNumber.matches("\\d{8}")) {
            throw new IllegalArgumentException("Phone number must contain exactly 8 digits.");
        }
        return phoneNumber;
    }

    private static LocalDate parseDate(String value, String fieldName) {
        requireNonNull(value, fieldName);
        try {
            return LocalDate.parse(value, DATE_FORMATTER);
        } catch (java.time.DateTimeException exception) {
            throw new IllegalArgumentException(fieldName + " must be a real date in DD-MM-YYYY format.",
                    exception);
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
        } catch (java.time.DateTimeException exception) {
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
     * The supported attendance statuses.
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

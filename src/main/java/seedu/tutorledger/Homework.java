package seedu.tutorledger;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Locale;

/**
 * Represents a homework item and its submission status.
 */
public class Homework {
    /** Status of homework that has been assigned but not handed in. */
    public static final String STATUS_OUTSTANDING = "outstanding";
    /** Status of homework that has been handed in. */
    public static final String STATUS_SUBMITTED = "submitted";

    private final String homeworkId;
    private final String submitStatus;
    private final String studentId;
    private final String description;
    private final LocalDate dueDate;

    /**
     * Creates a homework record.
     *
     * @param homeworkId the homework identifier
     * @param submitStatus the homework submission status
     */
    public Homework(String homeworkId, String submitStatus) {
        this(homeworkId, null, null, (LocalDate) null, submitStatus);
    }

    /** Creates homework linked to a student with the details needed by view. */
    public Homework(String homeworkId, String studentId, String description, LocalDate dueDate, String submitStatus) {
        this.homeworkId = requireText(homeworkId, "Homework ID");
        this.submitStatus = requireText(submitStatus, "Submission status");
        this.studentId = studentId;
        this.description = description;
        this.dueDate = dueDate;
        if (studentId != null) {
            requireText(studentId, "Student ID");
            requireText(description, "Description");
            if (dueDate == null) {
                throw new IllegalArgumentException("Due date cannot be null.");
            }
        }
    }

    /** Creates linked homework from the documented date format. */
    public Homework(String homeworkId, String studentId, String description, String dueDate, String submitStatus) {
        this(homeworkId, studentId, description, LocalDate.parse(dueDate,
                DateTimeFormatter.ofPattern("dd-MM-uuuu", Locale.ROOT).withResolverStyle(ResolverStyle.STRICT)),
                submitStatus);
    }

    public String getHomeworkId() {
        return homeworkId;
    }

    public String getSubmitStatus() {
        return submitStatus;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    /** Returns true if this homework has been handed in. */
    public boolean isSubmitted() {
        return STATUS_SUBMITTED.equalsIgnoreCase(submitStatus);
    }

    /**
     * Returns a copy of this homework marked as submitted.
     * A copy is returned because Homework is immutable.
     */
    public Homework asSubmitted() {
        return new Homework(homeworkId, studentId, description, dueDate, STATUS_SUBMITTED);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null.");
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank.");
        }
        return value;
    }
}

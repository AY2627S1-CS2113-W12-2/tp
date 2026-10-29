package seedu.tutorledger;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a homework item and its submission status.
 */
public class Homework {
    /** Status of homework that has been assigned but not handed in. */
    public static final String STATUS_OUTSTANDING = "outstanding";
    /** Status of homework that has been handed in. */
    public static final String STATUS_SUBMITTED = "submitted";

    private static final Logger LOGGER = Logger.getLogger(Homework.class.getName());

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
        LOGGER.log(Level.INFO, "Creating homework shell {0} status={1}", new Object[]{homeworkId, submitStatus});
        this.homeworkId = requireText(homeworkId, "Homework ID");
        this.submitStatus = requireText(submitStatus, "Submission status");
        this.studentId = null;
        this.description = null;
        this.dueDate = null;
        LOGGER.log(Level.INFO, "Homework shell created: {0}", this.homeworkId);
    }

    /** Creates homework linked to a student with the details needed by view. */
    public Homework(String homeworkId, String studentId, String description, LocalDate dueDate, String submitStatus) {
        LOGGER.log(Level.INFO, "Creating homework {0} for student {1}", new Object[]{homeworkId, studentId});
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
        LOGGER.log(Level.INFO, "Homework created: {0}", this.homeworkId);
    }

    /** Creates linked homework from the documented date format. */
    public Homework(String homeworkId, String studentId, String description, String dueDate, String submitStatus) {
        LOGGER.log(Level.INFO, "Creating homework {0} from string date {1}", new Object[]{homeworkId, dueDate});
        this.homeworkId = requireText(homeworkId, "Homework ID");
        this.submitStatus = requireText(submitStatus, "Submission status");
        this.studentId = studentId;
        this.description = description;
        this.dueDate = LocalDate.parse(dueDate,
                DateTimeFormatter.ofPattern("dd-MM-uuuu", Locale.ROOT).withResolverStyle(ResolverStyle.STRICT));
        if (studentId != null) {
            requireText(studentId, "Student ID");
            requireText(description, "Description");
        }
        LOGGER.log(Level.INFO, "Homework created from string date: {0}", this.homeworkId);
    }

    public String getHomeworkId() {
        LOGGER.log(Level.FINE, "Retrieving homeworkId for {0}", homeworkId);
        return homeworkId;
    }

    public String getSubmitStatus() {
        LOGGER.log(Level.FINE, "Retrieving submitStatus for {0}", homeworkId);
        return submitStatus;
    }

    public String getStudentId() {
        LOGGER.log(Level.FINE, "Retrieving studentId for {0}", homeworkId);
        return studentId;
    }

    public String getDescription() {
        LOGGER.log(Level.FINE, "Retrieving description for {0}", homeworkId);
        return description;
    }

    public LocalDate getDueDate() {
        LOGGER.log(Level.FINE, "Retrieving dueDate for {0}", homeworkId);
        return dueDate;
    }

    /** Returns true if this homework has been handed in. */
    public boolean isSubmitted() {
        boolean submitted = STATUS_SUBMITTED.equalsIgnoreCase(submitStatus);
        LOGGER.log(Level.FINE, "Checking if homework {0} is submitted: {1}", new Object[]{homeworkId, submitted});
        return submitted;
    }

    /**
     * Returns a copy of this homework marked as submitted.
     * A copy is returned because Homework is immutable.
     */
    public Homework asSubmitted() {
        LOGGER.log(Level.INFO, "Marking homework {0} as submitted", homeworkId);
        Homework updated = new Homework(homeworkId, studentId, description, dueDate, STATUS_SUBMITTED);
        LOGGER.log(Level.INFO, "Homework marked as submitted: {0}", updated.homeworkId);
        return updated;
    }

    /**
     * Returns a copy of this homework with some details changed, leaving this homework untouched.
     * Pass {@code null} for any detail that should stay as it is.
     *
     * @param newDescription The new description, or null to keep the current one.
     * @param newDueDate The new due date, or null to keep the current one.
     * @param newStatus {@value #STATUS_OUTSTANDING} or {@value #STATUS_SUBMITTED}, or null to keep the current one.
     * @return The changed copy, with the same ID and student.
     * @throws IllegalArgumentException If the description is blank or the status is not recognised.
     */
    public Homework withDetails(String newDescription, LocalDate newDueDate, String newStatus) {
        LOGGER.log(Level.FINE, "Updating homework {0}: description={1}, dueDate={2}, status={3}",
                new Object[]{homeworkId, newDescription, newDueDate, newStatus});
        assert studentId != null : "Only homework linked to a student can be edited";
        if (newStatus != null && !newStatus.equals(STATUS_OUTSTANDING) && !newStatus.equals(STATUS_SUBMITTED)) {
            throw new IllegalArgumentException("Status must be " + STATUS_OUTSTANDING + " or "
                    + STATUS_SUBMITTED + ".");
        }
        Homework updated = new Homework(homeworkId, studentId,
                newDescription == null ? description : newDescription,
                newDueDate == null ? dueDate : newDueDate,
                newStatus == null ? submitStatus : newStatus);
        assert updated.homeworkId.equals(homeworkId) : "withDetails changed the homework ID";
        assert updated.studentId.equals(studentId) : "withDetails changed the student";
        return updated;
    }

    private static String requireText(String value, String fieldName) {
        LOGGER.log(Level.FINE, "Validating required text field {0}", fieldName);
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null.");
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank.");
        }
        return value;
    }
}

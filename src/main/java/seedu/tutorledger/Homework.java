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
    private static final Logger LOGGER = Logger.getLogger(Homework.class.getName());

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

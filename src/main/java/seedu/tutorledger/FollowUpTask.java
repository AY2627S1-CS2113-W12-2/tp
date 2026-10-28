package seedu.tutorledger;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a reminder the tutor logs for themselves, such as "Bring Worksheet 4B" (feature F8.1, story US20).
 *
 * <p>A task may be linked to a student and may have a due date; both are optional, because some reminders
 * (such as "Print the term calendar") are not about any one student or day. Like the other records, a task
 * is immutable: marking it done returns a new copy.
 */
public class FollowUpTask {
    /** Status of a task that still needs doing. */
    public static final String STATUS_PENDING = "pending";
    /** Status of a task the tutor has finished. */
    public static final String STATUS_DONE = "done";

    private static final Logger LOGGER = Logger.getLogger(FollowUpTask.class.getName());

    private final String taskId;
    private final String description;
    /** The student the task is for, or null if it is not about one student. */
    private final String studentId;
    /** When the task should be done by, or null if it has no date. */
    private final LocalDate dueDate;
    private final String status;

    /**
     * Creates a task after validating its values.
     *
     * @param taskId The task's ID, such as T1.
     * @param description What needs to be done.
     * @param studentId The linked student's ID, or null for none.
     * @param dueDate The date it should be done by, or null for none.
     * @param status {@link #STATUS_PENDING} or {@link #STATUS_DONE}, in any case.
     * @throws IllegalArgumentException If the ID or description is blank, or the status is unknown.
     */
    public FollowUpTask(String taskId, String description, String studentId, LocalDate dueDate, String status) {
        this.taskId = requireText(taskId, "Task ID");
        this.description = requireText(description, "Description").strip();
        if (studentId != null && studentId.isBlank()) {
            throw new IllegalArgumentException("Student ID cannot be blank; leave it out instead.");
        }
        this.studentId = studentId;
        this.dueDate = dueDate;
        String normalisedStatus = requireText(status, "Status").toLowerCase(Locale.ROOT);
        if (!normalisedStatus.equals(STATUS_PENDING) && !normalisedStatus.equals(STATUS_DONE)) {
            throw new IllegalArgumentException("Task status must be pending or done.");
        }
        this.status = normalisedStatus;
        LOGGER.log(Level.FINE, "Created task {0} ({1})", new Object[]{taskId, normalisedStatus});
    }

    public String getTaskId() {
        return taskId;
    }

    public String getDescription() {
        return description;
    }

    /** Returns the linked student's ID, if the task is about one student. */
    public Optional<String> getStudentId() {
        return Optional.ofNullable(studentId);
    }

    /** Returns the due date, if the task has one. */
    public Optional<LocalDate> getDueDate() {
        return Optional.ofNullable(dueDate);
    }

    public String getStatus() {
        return status;
    }

    /** Returns true once the tutor has marked the task as done. */
    public boolean isDone() {
        return status.equals(STATUS_DONE);
    }

    /**
     * Returns true if the task is still pending and its due date is before {@code today}.
     * A task without a due date is never overdue.
     */
    public boolean isOverdue(LocalDate today) {
        return !isDone() && dueDate != null && dueDate.isBefore(today);
    }

    /** Returns a copy of this task marked as done. A copy is returned because tasks are immutable. */
    public FollowUpTask asDone() {
        FollowUpTask done = new FollowUpTask(taskId, description, studentId, dueDate, STATUS_DONE);
        assert done.isDone() && done.taskId.equals(taskId) : "asDone should only change the status";
        return done;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank.");
        }
        return value;
    }
}

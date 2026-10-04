package seedu.duke;

/**
 * Represents a homework item and its submission status.
 */
public class Homework {
    private final String homeworkId;
    private final String submitStatus;

    /**
     * Creates a homework record.
     *
     * @param homeworkId the homework identifier
     * @param submitStatus the homework submission status
     */
    public Homework(String homeworkId, String submitStatus) {
        this.homeworkId = requireText(homeworkId, "Homework ID");
        this.submitStatus = requireText(submitStatus, "Submission status");
    }

    public String getHomeworkId() {
        return homeworkId;
    }

    public String getSubmitStatus() {
        return submitStatus;
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

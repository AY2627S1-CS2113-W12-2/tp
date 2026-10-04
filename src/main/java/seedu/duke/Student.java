package seedu.duke;

/**
 * Represents a student profile.
 */
public class Student {
    private final String studentId;
    private final String name;
    private final String level;
    private final String phoneNumber;

    /**
     * Creates a student profile after validating the supplied values.
     *
     * @param studentId the student's identifier
     * @param name the student's name
     * @param level the student's secondary-school level
     * @param phoneNumber the student's eight-digit phone number
     */
    public Student(String studentId, String name, String level, String phoneNumber) {
        this.studentId = requireNonNull(studentId, "Student ID");
        this.name = validateName(name);
        this.level = validateLevel(level);
        this.phoneNumber = validatePhoneNumber(phoneNumber);
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

}

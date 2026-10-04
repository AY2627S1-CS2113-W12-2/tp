package seedu.tutorledger;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Represents a student profile.
 */
public class Student {
    private final String studentId;
    private final String name;
    private final String level;
    private final String phoneNumber;
    private final List<String> subjects;

    /**
     * Creates a student profile after validating the supplied values.
     *
     * @param studentId the student's identifier
     * @param name the student's name
     * @param level the student's secondary-school level
     * @param phoneNumber the student's eight-digit phone number
     */
    public Student(String studentId, String name, String level, String phoneNumber) {
        this(studentId, name, level, phoneNumber, List.of());
    }

    /** Creates a student profile with its current subjects. */
    public Student(String studentId, String name, String level, String phoneNumber, List<String> subjects) {
        this.studentId = validateId(studentId);
        this.name = validateName(name);
        this.level = validateLevel(level);
        this.phoneNumber = validatePhoneNumber(phoneNumber);
        this.subjects = validateSubjects(subjects);
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

    public List<String> getSubjects() {
        return subjects;
    }

    /** Returns a replacement profile after validating all changed fields. */
    public Student withDetails(String newName, String newLevel, String newPhoneNumber, List<String> newSubjects) {
        return new Student(studentId, newName, newLevel, newPhoneNumber, newSubjects);
    }

    private static String validateId(String id) {
        requireNonNull(id, "Student ID");
        if (!id.matches("(?i)S[1-9]\\d*")) {
            throw new IllegalArgumentException("Student ID must look like S1.");
        }
        return id.toUpperCase(Locale.ROOT);
    }

    private static List<String> validateSubjects(List<String> values) {
        if (values == null) {
            throw new IllegalArgumentException("Subjects cannot be null.");
        }
        List<String> result = new ArrayList<>();
        for (String value : values) {
            requireNonNull(value, "Subject");
            String subject = value.trim();
            if (subject.isEmpty()) {
                throw new IllegalArgumentException("Subject cannot be blank.");
            }
            if (result.stream().noneMatch(existing -> existing.equalsIgnoreCase(subject))) {
                result.add(subject);
            }
        }
        return List.copyOf(result);
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

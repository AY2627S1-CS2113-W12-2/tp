package seedu.tutorledger;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a student profile.
 */
public class Student {
    private static final Logger LOGGER = Logger.getLogger(Student.class.getName());

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
        LOGGER.log(Level.INFO, "Constructing student with ID={0}, name={1}", new Object[]{studentId, name});
        this.studentId = validateId(studentId);
        this.name = validateName(name);
        this.level = validateLevel(level);
        this.phoneNumber = validatePhoneNumber(phoneNumber);
        this.subjects = validateSubjects(List.of());
        LOGGER.log(Level.INFO, "Student constructed: {0}", this.studentId);
    }

    /** Creates a student profile with its current subjects. */
    public Student(String studentId, String name, String level, String phoneNumber, List<String> subjects) {
        LOGGER.log(Level.INFO, "Constructing student with ID={0}, name={1}, subjects={2}",
                new Object[]{studentId, name, subjects});
        this.studentId = validateId(studentId);
        this.name = validateName(name);
        this.level = validateLevel(level);
        this.phoneNumber = validatePhoneNumber(phoneNumber);
        this.subjects = validateSubjects(subjects);
        LOGGER.log(Level.INFO, "Student constructed: {0}", this.studentId);
    }

    public String getStudentId() {
        LOGGER.log(Level.FINE, "Retrieving studentId for {0}", studentId);
        return studentId;
    }

    public String getName() {
        LOGGER.log(Level.FINE, "Retrieving name for {0}", studentId);
        return name;
    }

    public String getLevel() {
        LOGGER.log(Level.FINE, "Retrieving level for {0}", studentId);
        return level;
    }

    public String getPhoneNumber() {
        LOGGER.log(Level.FINE, "Retrieving phone number for {0}", studentId);
        return phoneNumber;
    }

    public List<String> getSubjects() {
        LOGGER.log(Level.FINE, "Retrieving subjects for {0}: {1}", new Object[]{studentId, subjects});
        return subjects;
    }

    /** Returns a replacement profile after validating all changed fields. */
    public Student withDetails(String newName, String newLevel, String newPhoneNumber, List<String> newSubjects) {
        LOGGER.log(Level.INFO, "Updating student {0} details: name={1}, level={2}, phone={3}, subjects={4}",
                new Object[]{studentId, newName, newLevel, newPhoneNumber, newSubjects});
        Student updated = new Student(studentId, newName, newLevel, newPhoneNumber, newSubjects);
        LOGGER.log(Level.INFO, "Updated student record: {0}", updated.studentId);
        return updated;
    }

    private static String validateId(String id) {
        LOGGER.log(Level.FINE, "Validating student ID: {0}", id);
        requireNonNull(id, "Student ID");
        if (!id.matches("(?i)S[1-9]\\d*")) {
            throw new IllegalArgumentException("Student ID must look like S1.");
        }
        return id.toUpperCase(Locale.ROOT);
    }

    private static List<String> validateSubjects(List<String> values) {
        LOGGER.log(Level.FINE, "Validating subjects: {0}", values);
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
        LOGGER.log(Level.FINE, "Validated subject list: {0}", result);
        return List.copyOf(result);
    }

    private static String requireNonNull(String value, String fieldName) {
        LOGGER.log(Level.FINE, "Checking field {0} is non-null", fieldName);
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null.");
        }
        return value;
    }

    private static String validateName(String name) {
        LOGGER.log(Level.FINE, "Validating name: {0}", name);
        requireNonNull(name, "Name");
        if (!name.matches("\\p{L}+(?:[ '\\-]\\p{L}+)*")) {
            throw new IllegalArgumentException(
                    "Name must contain only letters, spaces, hyphens, or apostrophes.");
        }
        return name;
    }

    private static String validateLevel(String level) {
        LOGGER.log(Level.FINE, "Validating level: {0}", level);
        requireNonNull(level, "Level");
        if (!level.matches("(?i)Sec ?[1-5]")) {
            throw new IllegalArgumentException("Level must be Sec 1 to Sec 5.");
        }
        return "Sec " + level.substring(level.length() - 1);
    }

    private static String validatePhoneNumber(String phoneNumber) {
        LOGGER.log(Level.FINE, "Validating phone number: {0}", phoneNumber);
        requireNonNull(phoneNumber, "Phone number");
        if (!phoneNumber.matches("\\d{8}")) {
            throw new IllegalArgumentException("Phone number must contain exactly 8 digits.");
        }
        return phoneNumber;
    }

}

package seedu.tutorledger;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Represents a student profile.
 */
public class Student {
    private static final Logger LOGGER = Logger.getLogger(Student.class.getName());

    /**
     * The words "s/o" (son of) and "d/o" (daughter of), in any case, which may join two words of a name,
     * as in "Ravi s/o Kumar". They are common in Singapore names but look like the prefixes s/ and d/.
     */
    private static final Pattern NAME_CONNECTOR = Pattern.compile("(?i)[sd]/o");

    private final String studentId;
    private final String name;
    private final String level;
    private final String phoneNumber;
    private final List<String> subjects;
    /** Null means no default is set; zero is a valid rate for free lessons. */
    private final BigDecimal defaultHourlyRate;
    /** Archived profiles stay available by ID but are hidden from the default student list. */
    private final boolean archived;
    /**
     * Why the tutor flagged this student as needing extra attention (US14). Null means not flagged;
     * an empty string means flagged without a reason.
     */
    private final String attentionReason;

    /**
     * Creates a student profile after validating the supplied values.
     *
     * @param studentId the student's identifier
     * @param name the student's name
     * @param level the student's primary-school, secondary-school or junior-college level
     * @param phoneNumber the student's eight-digit phone number
     */
    public Student(String studentId, String name, String level, String phoneNumber) {
        this(studentId, name, level, phoneNumber, List.of());
    }

    /** Creates a student profile with its current subjects. */
    public Student(String studentId, String name, String level, String phoneNumber, List<String> subjects) {
        this(studentId, name, level, phoneNumber, subjects, null);
    }

    /** Creates a complete profile; a null rate means that no default has been set. */
    public Student(String studentId, String name, String level, String phoneNumber, List<String> subjects,
            String hourlyRate) {
        this(studentId, name, level, phoneNumber, subjects, hourlyRate, false, null);
    }

    /** Creates a validated immutable copy, retaining archive and attention state alongside profile details. */
    private Student(String studentId, String name, String level, String phoneNumber, List<String> subjects,
            String hourlyRate, boolean archived, String attentionReason) {
        LOGGER.log(Level.FINE, "Constructing student with ID={0}, name={1}, subjects={2}",
                new Object[]{studentId, name, subjects});
        this.studentId = validateId(studentId);
        this.name = validateName(name);
        this.level = validateLevel(level);
        this.phoneNumber = validatePhoneNumber(phoneNumber);
        this.subjects = validateSubjects(subjects);
        this.defaultHourlyRate = hourlyRate == null ? null : parseHourlyRate(hourlyRate);
        this.archived = archived;
        this.attentionReason = attentionReason == null ? null : validateReason(attentionReason);
        LOGGER.log(Level.FINE, "Student constructed: {0}", this.studentId);
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

    /** Returns the rate if configured, distinguishing a missing rate from a zero-dollar rate. */
    public Optional<BigDecimal> getDefaultHourlyRate() {
        return Optional.ofNullable(defaultHourlyRate);
    }

    /** Returns whether this profile is hidden from the active student list. */
    public boolean isArchived() {
        return archived;
    }

    /** Returns whether the tutor has flagged this student as needing extra attention. */
    public boolean isFlagged() {
        return attentionReason != null;
    }

    /** Returns the flag's reason, which may be empty, or nothing when the student is not flagged. */
    public Optional<String> getAttentionReason() {
        return Optional.ofNullable(attentionReason);
    }

    /**
     * Returns a profile flagged with the given reason (empty for none), or unflagged when reason is null.
     * All other details are kept.
     */
    public Student withAttentionReason(String reason) {
        Student updated = new Student(studentId, name, level, phoneNumber, subjects, rateText(), archived, reason);
        assert updated.studentId.equals(studentId) : "Flagging must preserve the student ID";
        assert updated.isFlagged() == (reason != null) : "Flag state was not copied";
        LOGGER.log(Level.FINE, "Student {0} flagged: {1}", new Object[]{studentId, updated.isFlagged()});
        return updated;
    }

    /** Returns a profile with the requested archive state while keeping all other details. */
    public Student withArchived(boolean archived) {
        Student updated = new Student(studentId, name, level, phoneNumber, subjects, rateText(), archived,
                attentionReason);
        assert updated.studentId.equals(studentId) : "Archiving must preserve the student ID";
        assert updated.archived == archived : "Archive state was not copied";
        LOGGER.log(Level.FINE, "Student {0} archive state set to {1}", new Object[]{studentId, archived});
        return updated;
    }

    /** Returns a replacement profile with a validated rate, or clears it when rate is null. */
    public Student withDefaultHourlyRate(String rate) {
        Student updated = new Student(studentId, name, level, phoneNumber, subjects, rate, archived, attentionReason);
        assert updated.studentId.equals(studentId) : "Changing a rate must preserve the student ID";
        LOGGER.log(Level.FINE, "Default hourly rate updated for {0}", studentId);
        return updated;
    }

    /** Returns a replacement profile after validating all changed fields. */
    public Student withDetails(String newName, String newLevel, String newPhoneNumber, List<String> newSubjects) {
        LOGGER.log(Level.FINE, "Updating student {0} details: name={1}, level={2}, phone={3}, subjects={4}",
                new Object[]{studentId, newName, newLevel, newPhoneNumber, newSubjects});
        Student updated = new Student(studentId, newName, newLevel, newPhoneNumber, newSubjects, rateText(),
                archived, attentionReason);
        LOGGER.log(Level.FINE, "Updated student record: {0}", updated.studentId);
        return updated;
    }

    /** Returns the rate in the text form the constructor accepts, or null when no rate is set. */
    private String rateText() {
        return defaultHourlyRate == null ? null : defaultHourlyRate.toPlainString();
    }

    /** Trims the reason; a line break would split the student's record in the data file. */
    private static String validateReason(String reason) {
        String trimmed = reason.strip();
        if (trimmed.contains("\n") || trimmed.contains("\r")) {
            throw new IllegalArgumentException("A flag reason must fit on one line.");
        }
        return trimmed;
    }

    /** Validates user and storage input even when Java assertions are disabled. */
    private static BigDecimal parseHourlyRate(String value) {
        if (!value.matches("[0-9]+(?:\\.[0-9]{1,2})?")) {
            throw new IllegalArgumentException(
                    "Hourly rate must be a non-negative dollar amount with at most 2 decimals.");
        }
        return new BigDecimal(value);
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

    /**
     * Returns whether a name connector ("s/o" or "d/o") starts at the given index, followed by whitespace
     * or the end of the text. Command parsing uses this to keep the word inside a name.
     *
     * @param text The text being parsed.
     * @param index Where the word would start.
     */
    static boolean isNameConnectorAt(String text, int index) {
        assert index >= 0 && index <= text.length() : "Index outside the text: " + index;
        int end = index + 3;
        return NAME_CONNECTOR.matcher(text).region(index, Math.min(end, text.length())).matches()
                && (end == text.length() || Character.isWhitespace(text.charAt(end)));
    }

    private static String validateName(String name) {
        LOGGER.log(Level.FINE, "Validating name: {0}", name);
        requireNonNull(name, "Name");
        // Check the grammar iteratively. A backtracking regex here can overflow the stack for a very long name.
        if (!isValidName(name)) {
            throw new IllegalArgumentException("Name must contain only letters, spaces, hyphens, or apostrophes."
                    + " It may also contain s/o or d/o between words, as in Ravi s/o Kumar.");
        }
        return name;
    }

    /** Returns whether a name contains letter words joined by the supported separators. */
    private static boolean isValidName(String name) {
        int index = 0;
        while (index < name.length()) {
            int codePoint = name.codePointAt(index);
            if (!Character.isLetter(codePoint)) {
                return false;
            }
            index += Character.charCount(codePoint);

            while (index < name.length()) {
                codePoint = name.codePointAt(index);
                if (!Character.isLetter(codePoint)) {
                    break;
                }
                index += Character.charCount(codePoint);
            }
            if (index == name.length()) {
                return true;
            }

            // " s/o " or " d/o " joins two words; the connector itself is defined once, in NAME_CONNECTOR.
            if (name.charAt(index) == ' ' && isNameConnectorAt(name, index + 1)
                    && index + 4 < name.length() && name.charAt(index + 4) == ' ') {
                index += 5;
            } else if (name.charAt(index) == ' ' || name.charAt(index) == '-'
                    || name.charAt(index) == '\'') {
                index++;
            } else {
                return false;
            }

            if (index == name.length() || !Character.isLetter(name.codePointAt(index))) {
                return false;
            }
        }
        return false;
    }

    /** Validates the supported year ranges and normalises case and the optional single space. */
    private static String validateLevel(String level) {
        LOGGER.log(Level.FINE, "Validating level: {0}", level);
        String upper = level == null ? "" : level.toUpperCase(Locale.ROOT);
        String stage;
        if (upper.matches("PRI ?[1-6]")) {
            stage = "Pri";
        } else if (upper.matches("SEC ?[1-5]")) {
            stage = "Sec";
        } else if (upper.matches("JC ?[1-2]")) {
            stage = "JC";
        } else {
            throw new IllegalArgumentException("Level must be Pri 1 to Pri 6, Sec 1 to Sec 5, or JC 1 to JC 2.");
        }
        // Every accepted year is one digit; validation above also guarantees that level is non-null.
        assert level != null : "A validated level cannot be null";
        String normalised = stage + " " + level.charAt(level.length() - 1);
        LOGGER.log(Level.FINE, "Normalised student level: {0}", normalised);
        return normalised;
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

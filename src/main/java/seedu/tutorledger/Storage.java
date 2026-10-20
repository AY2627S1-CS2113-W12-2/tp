package seedu.tutorledger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Saves TutorLedger's records to a plain-text file and loads them back (features F5.1 and F5.2).
 *
 * <p>The file has one record per line, with fields separated by {@code |}, so a tutor can read and
 * edit it in any text editor. For example:
 * <pre>
 * NEXT | S3 | L2 | H2 | A2
 * S | S1 | Amirah Binte Rahman | Sec 3 | 91234567 | E Math | A Math
 * L | L1 | S1 | 22-09-2026 | 1600 | A Math | 60 | present | unpaid | Quadratic equations
 * H | H1 | S1 | 29-09-2026 | outstanding | Worksheet 4A
 * A | A1 | S1 | 21-09-2026 | 28 | 40 | WA1
 * </pre>
 * A {@code |} or {@code \} inside a value is written with a backslash in front ({@code \|} or
 * {@code \\}), so text such as notes can contain them without breaking the line into extra fields.
 *
 * <p>Loading never stops at a bad line: the line is skipped and the reason is reported, so one typo
 * in a hand-edited file does not lose everything else.
 */
public class Storage {
    /** Where the data is kept, relative to the folder TutorLedger is run from. */
    public static final String DEFAULT_FILE_PATH = "data/tutorledger.txt";

    /**
     * Traces loading and saving at FINE level, so it only appears when debug logging is enabled
     * and never clutters the tutor's screen.
     */
    private static final Logger LOGGER = Logger.getLogger(Storage.class.getName());

    /** Explains the format to anyone who opens the file. Lines starting with # are ignored when loading. */
    private static final List<String> HEADER = List.of(
            "# TutorLedger data file. You may edit it in a text editor; run \"reload\" in TutorLedger afterwards.",
            "# Lines starting with # are ignored. Fields are separated by |. Write \\| for a | inside a value.",
            "#   NEXT | next student ID | next lesson ID | next homework ID | next score ID",
            "#   S | STUDENT_ID | NAME | LEVEL | PHONE_NUMBER | SUBJECT | SUBJECT ...",
            "#   R | STUDENT_ID | DEFAULT_HOURLY_RATE (omit the line if no rate is set)",
            "#   L | LESSON_ID | STUDENT_ID | DATE | TIME | SUBJECT | FEE | ATTENDANCE | PAYMENT | NOTES | MINUTES",
            "#   Older L lines without MINUTES default to 60. Saved fees are never recalculated on load.",
            "#   H | HOMEWORK_ID | STUDENT_ID | DUE_DATE | STATUS | DESCRIPTION",
            "#   A | SCORE_ID | STUDENT_ID | DATE | MARKS | TOTAL_MARKS | TEST_NAME");

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd-MM-uuuu", Locale.ROOT);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HHmm", Locale.ROOT);
    private static final String SEPARATOR = " | ";
    private static final int LESSON_FIELD_COUNT = 10;
    private static final int HOMEWORK_FIELD_COUNT = 6;
    private static final int SCORE_FIELD_COUNT = 7;
    /** A student line has at least these fields; any further fields are subjects. */
    private static final int STUDENT_MIN_FIELD_COUNT = 5;

    /** The path as the user would write it, used in messages. */
    private final String displayPath;
    private final Path filePath;

    /**
     * Creates storage that reads and writes the given file.
     *
     * @param filePath The data file, relative to the folder TutorLedger is run from, or absolute.
     */
    public Storage(String filePath) {
        assert filePath != null && !filePath.isBlank() : "Storage needs a data file path";
        this.displayPath = filePath;
        this.filePath = Path.of(filePath);
    }

    /** Returns the data file's path as it should be shown to the user. */
    public String getDisplayPath() {
        return displayPath;
    }

    /**
     * Reads the data file.
     *
     * @return The records that could be read, and a description of every line that was skipped.
     * @throws java.nio.file.NoSuchFileException If the data file does not exist.
     * @throws IOException If the file exists but cannot be read.
     */
    public LoadResult load() throws IOException {
        LOGGER.log(Level.FINE, "Loading data from {0}", filePath.toAbsolutePath());
        // Decoding the bytes ourselves replaces invalid characters instead of failing, so a file saved
        // in the wrong encoding still loads, with only the affected lines skipped.
        String text = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
        if (text.startsWith("﻿")) {
            // Some editors add an invisible byte-order mark at the start of the file.
            text = text.substring(1);
        }
        return decode(text.lines().toList());
    }

    /** Writes all records to the data file, creating its folder if needed. */
    public void save(TutorLedgerData data) throws IOException {
        saveTo(filePath, data);
    }

    /**
     * Writes all records to {@code path}, replacing any file already there.
     * Any folders in the path that do not exist yet are created.
     */
    public static void saveTo(Path path, TutorLedgerData data) throws IOException {
        LOGGER.log(Level.FINE, "Saving data to {0}", path.toAbsolutePath());
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(path, encode(data), StandardCharsets.UTF_8);
    }

    /** Returns the complete file contents for the given records. */
    public static String encode(TutorLedgerData data) {
        List<String> lines = new ArrayList<>(HEADER);
        lines.add(join("NEXT", "S" + data.getNextStudentNumber(), "L" + data.getNextLessonNumber(),
                "H" + data.getNextHomeworkNumber(), "A" + data.getNextScoreNumber()));
        for (Student student : data.getStudents()) {
            List<String> fields = new ArrayList<>(List.of("S", student.getStudentId(), student.getName(),
                    student.getLevel(), student.getPhoneNumber()));
            fields.addAll(student.getSubjects());
            lines.add(join(fields.toArray(String[]::new)));
            student.getDefaultHourlyRate().ifPresent(rate ->
                    lines.add(join("R", student.getStudentId(), rate.toPlainString())));
        }
        for (Lesson lesson : data.getLessons()) {
            lines.add(join("L", lesson.getLessonId(), lesson.getStudentId(), DATE.format(lesson.getDate()),
                    TIME.format(lesson.getTime()), lesson.getSubject(), lesson.getFee().toPlainString(),
                    lesson.getAttendance().name().toLowerCase(Locale.ROOT).replace('_', ' '),
                    lesson.getPaymentStatus(), lesson.getNotes(), Integer.toString(lesson.getDurationMinutes())));
        }
        for (Homework item : data.getHomework()) {
            lines.add(join("H", item.getHomeworkId(), item.getStudentId(), DATE.format(item.getDueDate()),
                    item.getSubmitStatus(), item.getDescription()));
        }
        for (Score score : data.getScores()) {
            lines.add(join("A", score.getScoreId(), score.getStudentId(), DATE.format(score.getDate()),
                    score.getMarks().toPlainString(), score.getTotalMarks().toPlainString(), score.getTestName()));
        }
        long rateCount = data.getStudents().stream()
                .filter(student -> student.getDefaultHourlyRate().isPresent()).count();
        // Student rates have their own lines so legacy subjects remain unambiguous.
        assert lines.size() == HEADER.size() + 1 + data.getStudents().size() + data.getLessons().size()
                + data.getHomework().size() + data.getScores().size() + rateCount : "Some records were not encoded";
        return String.join(System.lineSeparator(), lines) + System.lineSeparator();
    }

    /**
     * Rebuilds the records from the lines of a data file.
     *
     * <p>Students are read first, then rates, lessons, homework and scores, which must belong to a student
     * that already exists. This means the lines may appear in any order in the file.
     */
    public static LoadResult decode(List<String> lines) {
        TutorLedgerData data = new TutorLedgerData();
        // Sorted by line number, so problems are reported in file order even though there are two passes.
        Map<Integer, String> problems = new TreeMap<>();
        int[] savedNextIds = null;

        for (int i = 0; i < lines.size(); i++) {
            List<String> fields = splitFields(lines.get(i));
            String type = fields.getFirst().toUpperCase(Locale.ROOT);
            try {
                switch (type) {
                case "", "L", "H", "A", "R" -> {
                    // Linked records and rates wait for the second pass; blank and comment lines are ignored.
                }
                case "NEXT" -> {
                    if (savedNextIds != null) {
                        throw new IllegalArgumentException("Only one NEXT line is allowed.");
                    }
                    savedNextIds = parseNextIds(fields);
                }
                case "S" -> data.importStudent(parseStudent(fields));
                default -> throw new IllegalArgumentException("Unknown record type \"" + fields.getFirst()
                        + "\". A line must start with S, R, L, H, A or NEXT.");
                }
            } catch (IllegalArgumentException exception) {
                problems.put(i + 1, exception.getMessage());
            }
        }

        Set<String> ratedStudents = new HashSet<>();
        for (int i = 0; i < lines.size(); i++) {
            List<String> fields = splitFields(lines.get(i));
            String type = fields.getFirst().toUpperCase(Locale.ROOT);
            try {
                if (type.equals("R")) {
                    importRate(fields, data, ratedStudents);
                } else if (type.equals("L")) {
                    data.importLesson(parseLesson(fields, data));
                } else if (type.equals("H")) {
                    data.importHomework(parseHomework(fields, data));
                } else if (type.equals("A")) {
                    data.importScore(parseScore(fields, data));
                }
            } catch (IllegalArgumentException exception) {
                problems.put(i + 1, exception.getMessage());
            }
        }

        // Importing records already moved each counter past the highest ID in the file. The NEXT line
        // can only move it further, to skip IDs of deleted records; it is never allowed to reuse an ID.
        if (savedNextIds != null) {
            data.setNextStudentNumber(Math.max(savedNextIds[0], data.getNextStudentNumber()));
            data.setNextLessonNumber(Math.max(savedNextIds[1], data.getNextLessonNumber()));
            data.setNextHomeworkNumber(Math.max(savedNextIds[2], data.getNextHomeworkNumber()));
            // Files saved before scores existed have no score counter; 0 leaves the counter as it is.
            data.setNextScoreNumber(Math.max(savedNextIds[3], data.getNextScoreNumber()));
        }

        List<String> problemLines = new ArrayList<>();
        problems.forEach((lineNumber, reason) -> problemLines.add("Line " + lineNumber + ": " + reason));
        // Each line is either read or reported, never both, so there cannot be more problems than lines.
        assert problemLines.size() <= lines.size() : "More problems were reported than there are lines";
        LOGGER.log(Level.FINE, "Decoded {0} lines: {1} students, {2} lessons, {3} homework, {4} scores, "
                + "{5} skipped", new Object[]{lines.size(), data.getStudents().size(), data.getLessons().size(),
                    data.getHomework().size(), data.getScores().size(), problemLines.size()});
        return new LoadResult(data, problemLines);
    }

    /**
     * Reads {@code NEXT | S6 | L45 | H9 | A3} into the numbers 6, 45, 9 and 3.
     * The score ID is optional so that files saved before scores were added still load; it is then 0.
     */
    private static int[] parseNextIds(List<String> fields) {
        String usage = "A NEXT line must look like: NEXT | S1 | L1 | H1 | A1";
        if (fields.size() != 4 && fields.size() != 5) {
            throw new IllegalArgumentException(usage);
        }
        String[] prefixes = {"S", "L", "H", "A"};
        int[] numbers = new int[prefixes.length];
        for (int i = 0; i < fields.size() - 1; i++) {
            String id = fields.get(i + 1);
            if (!id.matches("(?i)" + prefixes[i] + "[1-9]\\d{0,8}")) {
                throw new IllegalArgumentException(usage);
            }
            numbers[i] = Integer.parseInt(id.substring(1));
        }
        return numbers;
    }

    private static Student parseStudent(List<String> fields) {
        if (fields.size() < STUDENT_MIN_FIELD_COUNT) {
            throw new IllegalArgumentException(wrongFieldCount("A student", STUDENT_MIN_FIELD_COUNT, fields));
        }
        // Blank subject fields are skipped, so a stray | at the end of the line does no harm.
        List<String> subjects = fields.subList(STUDENT_MIN_FIELD_COUNT, fields.size()).stream()
                .filter(subject -> !subject.isBlank())
                .toList();
        return new Student(fields.get(1), fields.get(2), fields.get(3), fields.get(4), subjects);
    }

    private static Lesson parseLesson(List<String> fields, TutorLedgerData data) {
        if (fields.size() != LESSON_FIELD_COUNT && fields.size() != LESSON_FIELD_COUNT + 1) {
            throw new IllegalArgumentException(wrongFieldCount("A lesson", LESSON_FIELD_COUNT, fields));
        }
        // Looking the student up checks they exist and gives their ID in its usual upper-case form.
        String studentId = data.getStudent(fields.get(2)).getStudentId();
        Lesson lesson = new Lesson(fields.get(1).toUpperCase(Locale.ROOT), studentId, fields.get(5),
                fields.get(3), fields.get(4), fields.get(7), fields.get(6), fields.get(9));
        if (fields.size() == LESSON_FIELD_COUNT + 1) {
            lesson = lesson.withDurationMinutes(Lesson.parseDuration(fields.get(10)));
        }
        return lesson.withPaymentStatus(fields.get(8));
    }

    /** Applies the first valid rate for a student without disturbing their profile or history. */
    private static void importRate(List<String> fields, TutorLedgerData data, Set<String> ratedStudents) {
        if (fields.size() != 3) {
            throw new IllegalArgumentException(wrongFieldCount("A rate", 3, fields));
        }
        Student student = data.getStudent(fields.get(1));
        String id = student.getStudentId();
        if (ratedStudents.contains(id)) {
            throw new IllegalArgumentException("Duplicate hourly rate for student " + id + ".");
        }
        data.replaceStudent(student.withDefaultHourlyRate(fields.get(2)));
        ratedStudents.add(id);
    }

    private static Homework parseHomework(List<String> fields, TutorLedgerData data) {
        if (fields.size() != HOMEWORK_FIELD_COUNT) {
            throw new IllegalArgumentException(wrongFieldCount("A homework", HOMEWORK_FIELD_COUNT, fields));
        }
        String studentId = data.getStudent(fields.get(2)).getStudentId();
        String status = fields.get(4).toLowerCase(Locale.ROOT);
        if (!status.equals(Homework.STATUS_OUTSTANDING) && !status.equals(Homework.STATUS_SUBMITTED)) {
            throw new IllegalArgumentException("Homework status must be outstanding or submitted.");
        }
        try {
            return new Homework(fields.get(1).toUpperCase(Locale.ROOT), studentId, fields.get(5),
                    fields.get(3), status);
        } catch (DateTimeException exception) {
            // Homework parses its own date and throws a date exception rather than IllegalArgumentException.
            throw new IllegalArgumentException("Due date must be a real date in DD-MM-YYYY format.", exception);
        }
    }

    private static Score parseScore(List<String> fields, TutorLedgerData data) {
        if (fields.size() != SCORE_FIELD_COUNT) {
            throw new IllegalArgumentException(wrongFieldCount("A score", SCORE_FIELD_COUNT, fields));
        }
        String studentId = data.getStudent(fields.get(2)).getStudentId();
        return new Score(fields.get(1).toUpperCase(Locale.ROOT), studentId, fields.get(6), fields.get(4),
                fields.get(5), fields.get(3));
    }

    private static String wrongFieldCount(String record, int expected, List<String> fields) {
        String atLeast = record.equals("A student") ? "at least " : "";
        return record + " line needs " + atLeast + expected + " fields but has " + fields.size() + ".";
    }

    /** Joins fields into one line, escaping any | or \ inside them. */
    private static String join(String... fields) {
        List<String> escaped = new ArrayList<>();
        for (String field : fields) {
            escaped.add(field.replace("\\", "\\\\").replace("|", "\\|"));
        }
        return String.join(SEPARATOR, escaped);
    }

    /**
     * Splits a line at each unescaped {@code |} and trims the spaces around each field.
     * A comment line ({@code #...}) or blank line gives a single empty field.
     */
    static List<String> splitFields(String line) {
        if (line.isBlank() || line.strip().startsWith("#")) {
            return List.of("");
        }
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            boolean isEscape = c == '\\' && i + 1 < line.length()
                    && (line.charAt(i + 1) == '|' || line.charAt(i + 1) == '\\');
            if (isEscape) {
                current.append(line.charAt(i + 1));
                i++; // The escaped character has been used, so skip over it.
            } else if (c == '|') {
                fields.add(current.toString().strip());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString().strip());
        // Callers read the record type from the first field, so there must always be one.
        assert !fields.isEmpty() : "Splitting a line should give at least one field";
        return fields;
    }

    /**
     * The outcome of reading a data file.
     *
     * @param data The records that were read successfully.
     * @param problems One message per skipped line, such as {@code Line 18: ...}, in file order.
     */
    public record LoadResult(TutorLedgerData data, List<String> problems) {
    }
}

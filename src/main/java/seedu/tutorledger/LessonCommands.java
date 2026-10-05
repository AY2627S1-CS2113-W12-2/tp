package seedu.tutorledger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Parses and runs the lesson commands: {@code schedule}, {@code lessons} and {@code record}.
 * Like {@link StudentCommands}, each command returns its display text, and validation problems
 * are reported as an "Error: ..." message instead of crashing the program.
 */
public class LessonCommands {
    /** Command words this class understands, used by the main loop to route input here. */
    private static final Set<String> COMMAND_WORDS = Set.of("schedule", "lessons", "record");

    /** Attendance values a tutor can type; "not recorded" is only the state a new lesson starts in. */
    private static final Set<String> ATTENDANCE_WORDS = Set.of("present", "absent", "late");

    /** Date format shown to the user, e.g. Tue 22-09-2026. */
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("EEE dd-MM-uuuu",
            Locale.ENGLISH);

    /** Time format shown to the user, e.g. 1600. */
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("HHmm", Locale.ROOT);

    private final TutorLedgerData data;
    /** Source of today's date; injected so tests can fix the date instead of depending on the real one. */
    private final Clock clock;

    /**
     * Creates the handler for the lesson commands.
     *
     * @param data The records the commands read and change.
     * @param clock The source of today's date.
     */
    public LessonCommands(TutorLedgerData data, Clock clock) {
        this.data = data;
        this.clock = clock;
    }

    /** Returns true if the first word of the input is a lesson command. */
    public static boolean handles(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }
        String command = input.strip().split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        return COMMAND_WORDS.contains(command);
    }

    /** Executes one lesson command and returns its display text, including validation errors. */
    public String execute(String input) {
        if (!handles(input)) {
            return "Unknown command.";
        }
        String[] words = input.strip().split("\\s+", 2);
        String command = words[0].toLowerCase(Locale.ROOT);
        String arguments = words.length == 2 ? words[1].trim() : "";
        try {
            return switch (command) {
            case "schedule" -> schedule(arguments);
            case "lessons" -> listLessons(arguments);
            default -> record(arguments);
            };
        } catch (IllegalArgumentException exception) {
            return "Error: " + exception.getMessage();
        }
    }

    /** Handles {@code schedule STUDENT_ID d/DATE t/TIME s/SUBJECT f/FEE}. */
    private String schedule(String arguments) {
        IdAndFields input = splitIdAndFields(arguments, List.of("d", "t", "s", "f"));
        Student student = data.getStudent(input.id());
        String date = input.fields().single("d", true);
        String time = input.fields().single("t", true);
        String subject = findSubjectTakenBy(student, input.fields().single("s", true));
        String fee = input.fields().single("f", true);
        Lesson lesson = data.addLesson(student.getStudentId(), subject, date, time, fee);
        return "Scheduled " + lesson.getLessonId() + " for " + formatStudent(student) + System.lineSeparator()
                + "  " + formatSlot(lesson) + ", " + lesson.getSubject() + ", "
                + formatMoney(lesson.getFee());
    }

    /** Handles {@code lessons [week]}: lists today's lessons, or this week's, earliest first. */
    private String listLessons(String arguments) {
        boolean isWeekView = arguments.equalsIgnoreCase("week");
        if (!isWeekView && !arguments.isEmpty()) {
            throw new IllegalArgumentException("Use: lessons [week]");
        }
        LocalDate today = LocalDate.now(clock);
        // A week runs Monday to Sunday, so step back to the most recent Monday (or stay on it).
        LocalDate firstDay = isWeekView
                ? today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                : today;
        LocalDate lastDay = isWeekView ? firstDay.plusDays(6) : today;
        String heading = isWeekView
                ? "This week, " + DISPLAY_DATE.format(firstDay) + " to " + DISPLAY_DATE.format(lastDay)
                : "Today, " + DISPLAY_DATE.format(today);

        List<Lesson> lessons = data.getLessons().stream()
                .filter(lesson -> !lesson.getDate().isBefore(firstDay) && !lesson.getDate().isAfter(lastDay))
                .sorted(Comparator.comparing(Lesson::getDate)
                        .thenComparing(Lesson::getTime)
                        .thenComparing(Lesson::getLessonId))
                .toList();
        StringBuilder output = new StringBuilder(heading + ": " + formatCount(lessons.size(), "lesson"));
        if (lessons.isEmpty()) {
            return output.toString();
        }

        // Work out column widths so the ID and student columns line up.
        int idWidth = 0;
        int studentWidth = 0;
        for (Lesson lesson : lessons) {
            String studentLabel = formatStudent(data.getStudent(lesson.getStudentId()));
            idWidth = Math.max(idWidth, lesson.getLessonId().length());
            studentWidth = Math.max(studentWidth, studentLabel.length());
        }
        String rowFormat = "  %-" + idWidth + "s  %s  %-" + studentWidth + "s  %s";
        for (Lesson lesson : lessons) {
            String studentLabel = formatStudent(data.getStudent(lesson.getStudentId()));
            output.append(System.lineSeparator())
                    .append(String.format(rowFormat, lesson.getLessonId(), formatSlot(lesson), studentLabel,
                            lesson.getSubject()));
        }
        return output.toString();
    }

    /** Handles {@code record LESSON_ID a/ATTENDANCE [note/NOTES]}. */
    private String record(String arguments) {
        IdAndFields input = splitIdAndFields(arguments, List.of("a", "note"));
        Lesson lesson = data.getLesson(input.id());
        String attendance = parseAttendance(input.fields().single("a", true));
        // A lesson that has not happened yet has no outcome to record.
        if (lesson.getDate().isAfter(LocalDate.now(clock))) {
            throw new IllegalArgumentException("Only a lesson dated today or earlier can be recorded.");
        }
        // Recording again replaces the attendance, and the notes only if note/ was typed.
        Lesson recorded = lesson.withDetails(null, null, null, attendance, null, parseNotes(input.fields()));
        data.replaceLesson(recorded);
        Student student = data.getStudent(recorded.getStudentId());
        return "Recorded " + recorded.getLessonId() + " for " + formatStudent(student)
                + " (" + formatSlot(recorded) + ")" + System.lineSeparator()
                + "  Attendance: " + formatAttendance(recorded) + formatNotesLine(recorded);
    }

    /**
     * Returns the attendance in lower case after checking it is one a tutor may type.
     *
     * @throws IllegalArgumentException If the value is not present, absent or late.
     */
    private static String parseAttendance(String value) {
        String word = value.toLowerCase(Locale.ROOT);
        if (!ATTENDANCE_WORDS.contains(word)) {
            throw new IllegalArgumentException("Attendance must be present, absent, or late.");
        }
        return word;
    }

    /**
     * Returns the notes typed after {@code note/}, or null if {@code note/} was left out.
     * A bare {@code note/} gives an empty string, which clears the lesson's notes.
     */
    private static String parseNotes(ArgumentParser.Fields fields) {
        if (!fields.has("note")) {
            return null;
        }
        String notes = fields.single("note", false);
        return notes == null ? "" : notes;
    }

    /**
     * Returns the subject as it is written on the student's record, ignoring case.
     * Using the student's own spelling keeps "a math" and "A Math" from showing up as two subjects.
     *
     * @throws IllegalArgumentException If the student does not take the subject.
     */
    private static String findSubjectTakenBy(Student student, String subject) {
        for (String taken : student.getSubjects()) {
            if (taken.equalsIgnoreCase(subject)) {
                return taken;
            }
        }
        if (student.getSubjects().isEmpty()) {
            throw new IllegalArgumentException(formatStudent(student)
                    + " has no subjects yet. Add one with: subject " + student.getStudentId() + " s/SUBJECT");
        }
        throw new IllegalArgumentException(subject + " is not one of " + formatStudent(student)
                + "'s subjects (" + String.join(", ", student.getSubjects()) + ").");
    }

    /** Splits {@code ID prefix/value ...} into the ID and its prefixed values. */
    private static IdAndFields splitIdAndFields(String arguments, List<String> allowedPrefixes) {
        String[] pieces = arguments.split("\\s+", 2);
        String remainder = pieces.length == 2 ? pieces[1] : "";
        return new IdAndFields(pieces[0], ArgumentParser.parseFields(remainder, allowedPrefixes));
    }

    /** Formats when a lesson starts as "Tue 22-09-2026 1600". */
    private static String formatSlot(Lesson lesson) {
        return DISPLAY_DATE.format(lesson.getDate()) + " " + DISPLAY_TIME.format(lesson.getTime());
    }

    /** Formats attendance the way it is typed and shown, e.g. "present" or "not recorded". */
    private static String formatAttendance(Lesson lesson) {
        return lesson.getAttendance().name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    /** Returns an indented "Notes: ..." line for a lesson that has notes, or nothing if it has none. */
    private static String formatNotesLine(Lesson lesson) {
        return lesson.getNotes().isBlank() ? "" : System.lineSeparator() + "  Notes: " + lesson.getNotes();
    }

    /** Formats a student as "S1 Amirah Binte Rahman". */
    private static String formatStudent(Student student) {
        return student.getStudentId() + " " + student.getName();
    }

    /** Formats a fee as dollars with two decimal places, e.g. $60.00. */
    private static String formatMoney(BigDecimal amount) {
        return "$" + amount.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }

    /** Formats an amount with its noun, adding an "s" unless there is exactly one, e.g. "4 lessons". */
    private static String formatCount(int amount, String noun) {
        return amount + " " + noun + (amount == 1 ? "" : "s");
    }

    /** The ID typed straight after the command word, with the prefixed values that follow it. */
    private record IdAndFields(String id, ArgumentParser.Fields fields) {
    }
}

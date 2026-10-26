package seedu.tutorledger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Parses and runs the lesson commands: {@code schedule}, {@code log}, {@code lessons}, {@code next},
 * {@code record}, {@code reschedule} and {@code cancel}, plus {@code edit} and {@code delete} when they
 * are given a lesson ID.
 * Like {@link StudentCommands}, each command returns its display text, and validation problems
 * are reported as an "Error: ..." message instead of crashing the program.
 */
public class LessonCommands {
    /**
     * Records each lesson command and its outcome at FINE level, so the trace only appears when
     * debug logging is enabled and never clutters the tutor's screen.
     */
    private static final Logger LOGGER = Logger.getLogger(LessonCommands.class.getName());

    /** Command words that always belong to lessons, used by the main loop to route input here. */
    private static final Set<String> COMMAND_WORDS = Set.of("schedule", "log", "lessons", "next", "record",
            "reschedule", "cancel");

    /** Command words shared with students; they are lesson commands only when given a lesson ID. */
    private static final Set<String> SHARED_COMMAND_WORDS = Set.of("edit", "delete");

    /** Attendance values a tutor can type; "not recorded" is only the state a new lesson starts in. */
    private static final Set<String> ATTENDANCE_WORDS = Set.of("present", "absent", "late");

    /** Date format shown to the user, e.g. Tue 22-09-2026. */
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("EEE dd-MM-uuuu",
            Locale.ENGLISH);

    /** Time format shown to the user, e.g. 1600. */
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("HHmm", Locale.ROOT);

    /** Day-of-week format shown for a weekly slot, e.g. Tue. */
    private static final DateTimeFormatter DISPLAY_WEEKDAY = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH);

    /** Most weekly lessons one {@code schedule} can add: a year, which is longer than any school term. */
    private static final int MAX_WEEKS = 52;

    /** Orders lessons by when they start, using the ID to break a tie between cancelled lessons. */
    private static final Comparator<Lesson> BY_START = Comparator.comparing(Lesson::getDate)
            .thenComparing(Lesson::getTime)
            .thenComparing(Lesson::getLessonId);

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
        assert data != null : "LessonCommands needs the records to work on";
        assert clock != null : "LessonCommands needs a clock to know today's date";
        this.data = data;
        this.clock = clock;
    }

    /**
     * Returns true if the input is a lesson command.
     *
     * <p>{@code edit} and {@code delete} are shared with students, so they count only when the ID
     * after them starts with L. This is how the user guide tells the two apart: {@code edit L42 ...}
     * changes a lesson, while {@code edit S1 ...} is left for {@link StudentCommands}.
     */
    public static boolean handles(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }
        String[] words = input.strip().split("\\s+", 3);
        String command = words[0].toLowerCase(Locale.ROOT);
        if (COMMAND_WORDS.contains(command)) {
            return true;
        }
        // "[^/]*" keeps a prefixed value such as l/Sec 4 from being mistaken for a lesson ID.
        return SHARED_COMMAND_WORDS.contains(command) && words.length > 1 && words[1].matches("(?i)L[^/]*");
    }

    /** Executes one lesson command and returns its display text, including validation errors. */
    public String execute(String input) {
        if (!handles(input)) {
            return "Unknown command.";
        }
        String[] words = input.strip().split("\\s+", 2);
        String command = words[0].toLowerCase(Locale.ROOT);
        String arguments = words.length == 2 ? words[1].trim() : "";
        LOGGER.log(Level.FINE, "Running lesson command {0} with arguments: {1}", new Object[]{command, arguments});
        try {
            return switch (command) {
            case "schedule" -> schedule(arguments);
            case "log" -> log(arguments);
            case "lessons" -> listLessons(arguments);
            case "next" -> showNext(arguments);
            case "record" -> record(arguments);
            case "reschedule" -> reschedule(arguments);
            case "cancel" -> cancel(arguments);
            case "edit" -> edit(arguments);
            default -> {
                // handles() let only lesson command words through, so delete is the only one left.
                assert command.equals("delete") : "Unexpected lesson command: " + command;
                yield delete(arguments);
            }
            };
        } catch (IllegalArgumentException exception) {
            // A rejected command is the user's mistake, not a bug, so it is only traced for debugging.
            LOGGER.log(Level.FINE, "Lesson command {0} rejected: {1}",
                    new Object[]{command, exception.getMessage()});
            return "Error: " + exception.getMessage();
        }
    }

    /** Schedules single or weekly lessons with an optional fee override and billing duration. */
    private String schedule(String arguments) {
        IdAndFields input = splitIdAndFields(arguments, List.of("d", "t", "s", "f", "dur", "w"));
        Student student = data.getStudent(input.id());
        String date = input.fields().single("d", true);
        String time = input.fields().single("t", true);
        String subject = findSubjectTakenBy(student, input.fields().single("s", true));
        String fee = input.fields().singleOr("f", null);
        int duration = input.fields().has("dur") ? Lesson.parseDuration(input.fields().single("dur", true))
                : Lesson.DEFAULT_DURATION_MINUTES;
        if (input.fields().has("w")) {
            int weekCount = parseWeekCount(input.fields().single("w", true));
            return scheduleWeekly(student, subject, date, time, fee, weekCount, duration);
        }
        Lesson lesson = data.addLesson(student.getStudentId(), subject, date, time, fee, duration);
        assert lesson.getStudentId().equals(student.getStudentId()) : "Lesson was scheduled for the wrong student";
        assert !lesson.isRecorded() : "A newly scheduled lesson should not have attendance yet";
        LOGGER.log(Level.FINE, "Scheduled {0} for {1}", new Object[]{lesson.getLessonId(), student.getStudentId()});
        return "Scheduled " + lesson.getLessonId() + " for " + formatStudent(student) + System.lineSeparator()
                + "  " + formatSlot(lesson) + ", " + lesson.getSubject() + ", "
                + formatMoney(lesson.getFee());
    }

    /** Schedules a weekly slot and describes the lessons it added. */
    private String scheduleWeekly(Student student, String subject, String date, String time, String fee,
            int weekCount, int duration) {
        List<Lesson> lessons = data.addWeeklyLessons(student.getStudentId(), subject, date, time, fee,
                weekCount, duration);
        assert lessons.size() == weekCount : "Every week of the slot should have its own lesson";
        Lesson first = lessons.getFirst();
        Lesson last = lessons.getLast();
        assert last.getDate().equals(first.getDate().plusWeeks(weekCount - 1)) : "Lessons should be a week apart";
        LOGGER.log(Level.FINE, "Scheduled {0} weekly lessons for {1}",
                new Object[]{weekCount, student.getStudentId()});
        return "Scheduled " + weekCount + " weekly lessons for " + formatStudent(student) + ": "
                + first.getLessonId() + " to " + last.getLessonId() + System.lineSeparator()
                + "  Every " + DISPLAY_WEEKDAY.format(first.getDate()) + " " + DISPLAY_TIME.format(first.getTime())
                + (duration == Lesson.DEFAULT_DURATION_MINUTES ? "" : " (" + duration + " min)")
                + ", " + subject + ", " + formatMoney(first.getFee()) + " each" + System.lineSeparator()
                + "  From " + DISPLAY_DATE.format(first.getDate()) + " to " + DISPLAY_DATE.format(last.getDate());
    }

    /**
     * Handles {@code log STUDENT_ID d/DATE t/TIME s/SUBJECT f/FEE a/ATTENDANCE [note/NOTES]}.
     * It adds a lesson that has already happened, with its outcome, in one step.
     */
    private String log(String arguments) {
        IdAndFields input = splitIdAndFields(arguments, List.of("d", "t", "s", "f", "a", "note"));
        ArgumentParser.Fields fields = input.fields();
        Student student = data.getStudent(input.id());
        String date = fields.single("d", true);
        String time = fields.single("t", true);
        String subject = findSubjectTakenBy(student, fields.single("s", true));
        String fee = fields.single("f", true);
        String attendance = parseAttendance(fields.single("a", true));
        String notes = parseNotes(fields);
        // The same rule as record: a lesson that has not happened yet has no outcome to log.
        if (Lesson.parseDate(date).isAfter(LocalDate.now(clock))) {
            throw new IllegalArgumentException("Only a lesson dated today or earlier can be logged. "
                    + "Use schedule for a lesson that has not happened yet.");
        }
        Lesson lesson = data.addLesson(student.getStudentId(), subject, date, time, fee, attendance,
                notes == null ? "" : notes);
        assert lesson.isRecorded() : "A logged lesson should already have its attendance";
        LOGGER.log(Level.FINE, "Logged {0} for {1}", new Object[]{lesson.getLessonId(), student.getStudentId()});
        return "Logged " + lesson.getLessonId() + " for " + formatStudent(student) + System.lineSeparator()
                + "  " + formatSlot(lesson) + ", " + lesson.getSubject() + ", " + formatMoney(lesson.getFee())
                + ", " + formatAttendance(lesson) + formatNotesLine(lesson);
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
        assert !firstDay.isAfter(lastDay) : "The listed range must not end before it starts";
        assert !today.isBefore(firstDay) && !today.isAfter(lastDay) : "The listed range must include today";
        String heading = isWeekView
                ? "This week, " + DISPLAY_DATE.format(firstDay) + " to " + DISPLAY_DATE.format(lastDay)
                : "Today, " + DISPLAY_DATE.format(today);

        List<Lesson> lessons = data.getLessons().stream()
                .filter(lesson -> !lesson.getDate().isBefore(firstDay) && !lesson.getDate().isAfter(lastDay))
                .sorted(BY_START)
                .toList();
        LOGGER.log(Level.FINE, "Listing {0} lessons from {1} to {2}",
                new Object[]{lessons.size(), firstDay, lastDay});
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
            if (lesson.isCancelled()) {
                output.append("  (cancelled: ").append(lesson.getCancellationReason()).append(")");
            }
        }
        return output.toString();
    }

    /**
     * Handles {@code next [STUDENT_ID]}: summarises the next lesson, or that student's next lesson,
     * with everything needed to prepare for it.
     */
    private String showNext(String arguments) {
        if (arguments.split("\\s+").length > 1) {
            throw new IllegalArgumentException("Use: next [STUDENT_ID]");
        }
        Student chosen = arguments.isEmpty() ? null : data.getStudent(arguments);
        LocalDateTime now = LocalDateTime.now(clock);
        // A lesson that started earlier, or already has attendance, is no longer the one to prepare for.
        Optional<Lesson> upcoming = data.getLessons().stream()
                .filter(lesson -> !lesson.isCancelled() && !lesson.isRecorded())
                .filter(lesson -> chosen == null || chosen.getStudentId().equals(lesson.getStudentId()))
                .filter(lesson -> !startOf(lesson).isBefore(now))
                .min(BY_START);
        if (upcoming.isEmpty()) {
            return chosen == null ? "No upcoming lessons." : "No upcoming lessons for " + formatStudent(chosen) + ".";
        }
        Lesson lesson = upcoming.get();
        Student student = data.getStudent(lesson.getStudentId());
        LOGGER.log(Level.FINE, "Next lesson is {0}", lesson.getLessonId());
        return "Next lesson: " + lesson.getLessonId() + ", " + formatSlot(lesson) + System.lineSeparator()
                + "  Student: " + formatStudent(student) + " (" + student.getLevel() + ")" + System.lineSeparator()
                + "  Subject: " + lesson.getSubject() + System.lineSeparator()
                + "  Last topic: " + describeLastTopic(lesson) + System.lineSeparator()
                + describeOutstandingHomework(student) + System.lineSeparator()
                + "  Unpaid: " + describeUnpaid(student);
    }

    /** Describes the notes of the student's latest lesson before {@code upcoming}, where they left off. */
    private String describeLastTopic(Lesson upcoming) {
        LocalDateTime start = startOf(upcoming);
        return data.getLessons().stream()
                .filter(lesson -> lesson.getStudentId().equals(upcoming.getStudentId()))
                .filter(lesson -> !lesson.isCancelled() && !lesson.getNotes().isBlank())
                .filter(lesson -> startOf(lesson).isBefore(start))
                .max(BY_START)
                .map(lesson -> lesson.getNotes() + " (" + lesson.getLessonId() + ", "
                        + DISPLAY_DATE.format(lesson.getDate()) + ")")
                .orElse("none recorded yet");
    }

    /** Lists the student's homework that is not submitted, soonest due first, flagging overdue work. */
    private String describeOutstandingHomework(Student student) {
        LocalDate today = LocalDate.now(clock);
        List<Homework> outstanding = data.getHomework().stream()
                .filter(item -> student.getStudentId().equalsIgnoreCase(item.getStudentId()))
                .filter(item -> !item.isSubmitted())
                .sorted(Comparator.comparing(Homework::getDueDate).thenComparing(Homework::getHomeworkId))
                .toList();
        if (outstanding.isEmpty()) {
            return "  Outstanding homework: none";
        }
        StringBuilder text = new StringBuilder("  Outstanding homework: " + outstanding.size());
        for (Homework item : outstanding) {
            text.append(System.lineSeparator()).append("    ").append(item.getHomeworkId()).append("  ")
                    .append(item.getDescription()).append(", due ").append(DISPLAY_DATE.format(item.getDueDate()));
            if (item.getDueDate().isBefore(today)) {
                text.append(" (overdue)");
            }
        }
        return text.toString();
    }

    /** Describes the student's unpaid lessons up to today, counted the same way as the owed command. */
    private String describeUnpaid(Student student) {
        LocalDate today = LocalDate.now(clock);
        List<Lesson> unpaid = data.getLessons().stream()
                .filter(lesson -> lesson.getStudentId().equals(student.getStudentId()))
                .filter(lesson -> lesson.getAmountOwed().signum() > 0)
                .filter(lesson -> !lesson.isCancelled() && !lesson.getDate().isAfter(today))
                .toList();
        if (unpaid.isEmpty()) {
            return "none";
        }
        BigDecimal total = unpaid.stream().map(Lesson::getAmountOwed).reduce(BigDecimal.ZERO, BigDecimal::add);
        return formatCount(unpaid.size(), "lesson") + ", " + formatMoney(total);
    }

    /** Handles {@code record LESSON_ID a/ATTENDANCE [note/NOTES]}. */
    private String record(String arguments) {
        IdAndFields input = splitIdAndFields(arguments, List.of("a", "note"));
        Lesson lesson = data.getLesson(input.id());
        String attendance = parseAttendance(input.fields().single("a", true));
        if (lesson.isCancelled()) {
            throw new IllegalArgumentException(lesson.getLessonId() + " was cancelled, so it has no attendance.");
        }
        // A lesson that has not happened yet has no outcome to record.
        if (lesson.getDate().isAfter(LocalDate.now(clock))) {
            throw new IllegalArgumentException("Only a lesson dated today or earlier can be recorded.");
        }
        // Recording again replaces the attendance, and the notes only if note/ was typed.
        Lesson recorded = lesson.withDetails(null, null, null, attendance, null, parseNotes(input.fields()));
        // parseAttendance only allows present, absent or late, so the copy must count as recorded.
        assert recorded.isRecorded() : "Recording a lesson should leave it with attendance";
        data.replaceLesson(recorded);
        LOGGER.log(Level.FINE, "Recorded {0} as {1}", new Object[]{recorded.getLessonId(), attendance});
        Student student = data.getStudent(recorded.getStudentId());
        return "Recorded " + recorded.getLessonId() + " for " + formatStudent(student)
                + " (" + formatSlot(recorded) + ")" + System.lineSeparator()
                + "  Attendance: " + formatAttendance(recorded) + formatNotesLine(recorded);
    }

    /** Edits lesson details; changing duration alone preserves the saved total fee. */
    private String edit(String arguments) {
        IdAndFields input = splitIdAndFields(arguments, List.of("d", "t", "s", "f", "a", "note", "dur"));
        ArgumentParser.Fields fields = input.fields();
        if (fields.isEmpty()) {
            throw new IllegalArgumentException("Give at least one value to edit.");
        }
        Lesson lesson = data.getLesson(input.id());
        Student student = data.getStudent(lesson.getStudentId());
        // A value that was not typed stays null, which withDetails reads as "keep the current value".
        String subject = fields.has("s") ? findSubjectTakenBy(student, fields.single("s", true)) : null;
        String attendance = fields.has("a") ? parseAttendance(fields.single("a", true)) : null;
        Lesson edited = lesson.withDetails(subject, fields.singleOr("d", null), fields.singleOr("t", null),
                attendance, fields.singleOr("f", null), parseNotes(fields));
        if (fields.has("dur")) {
            edited = edited.withDurationMinutes(Lesson.parseDuration(fields.single("dur", true)));
        }
        if (edited.isCancelled() && edited.isRecorded()) {
            throw new IllegalArgumentException(edited.getLessonId() + " was cancelled, so it has no attendance.");
        }
        // The same rule as record: attendance and notes describe a lesson that has already happened.
        if (hasOutcome(edited) && edited.getDate().isAfter(LocalDate.now(clock))) {
            throw new IllegalArgumentException("A lesson dated after today cannot have attendance or notes.");
        }
        data.replaceLesson(edited);
        LOGGER.log(Level.FINE, "Edited {0} using prefixes {1}",
                new Object[]{edited.getLessonId(), fields.fields().keySet()});
        return "Edited " + edited.getLessonId() + " for " + formatStudent(student) + System.lineSeparator()
                + "  " + formatSlot(edited) + ", " + edited.getSubject() + ", " + formatMoney(edited.getFee())
                + ", " + formatAttendance(edited) + ", " + edited.getPaymentStatus()
                + formatNotesLine(edited);
    }

    /**
     * Handles {@code reschedule LESSON_ID [d/DATE] [t/TIME]}: moves a lesson that has not happened yet
     * to a new slot. Unlike {@code edit}, it checks that the move makes sense as a real-life change.
     */
    private String reschedule(String arguments) {
        IdAndFields input = splitIdAndFields(arguments, List.of("d", "t"));
        ArgumentParser.Fields fields = input.fields();
        if (fields.isEmpty()) {
            throw new IllegalArgumentException("Give a new date d/, a new time t/, or both.");
        }
        Lesson lesson = data.getLesson(input.id());
        if (lesson.isCancelled()) {
            throw new IllegalArgumentException(lesson.getLessonId()
                    + " was cancelled. Schedule a make-up lesson instead.");
        }
        if (hasOutcome(lesson)) {
            throw new IllegalArgumentException(lesson.getLessonId()
                    + " already has attendance or notes, so it has taken place. Use edit to correct its slot.");
        }
        Lesson moved = lesson.withDetails(null, fields.singleOr("d", null), fields.singleOr("t", null),
                null, null, null);
        if (startOf(moved).equals(startOf(lesson))) {
            throw new IllegalArgumentException(lesson.getLessonId() + " is already at " + formatSlot(lesson) + ".");
        }
        if (startOf(moved).isBefore(LocalDateTime.now(clock))) {
            throw new IllegalArgumentException("A lesson can only be rescheduled to a time that has not passed.");
        }
        data.replaceLesson(moved);
        LOGGER.log(Level.FINE, "Rescheduled {0} from {1} to {2}",
                new Object[]{lesson.getLessonId(), startOf(lesson), startOf(moved)});
        Student student = data.getStudent(moved.getStudentId());
        return "Rescheduled " + moved.getLessonId() + " for " + formatStudent(student) + System.lineSeparator()
                + "  From: " + formatSlot(lesson) + System.lineSeparator()
                + "  To:   " + formatSlot(moved);
    }

    /**
     * Handles {@code cancel LESSON_ID r/REASON}. The lesson is kept but marked cancelled, so it can be
     * told apart from a lesson the student missed. It frees its slot and is no longer charged.
     */
    private String cancel(String arguments) {
        IdAndFields input = splitIdAndFields(arguments, List.of("r"));
        Lesson lesson = data.getLesson(input.id());
        String reason = input.fields().single("r", true);
        if (lesson.isCancelled()) {
            throw new IllegalArgumentException(lesson.getLessonId() + " is already cancelled ("
                    + lesson.getCancellationReason() + ").");
        }
        if (lesson.isRecorded()) {
            throw new IllegalArgumentException(lesson.getLessonId()
                    + " already has attendance, so it took place and cannot be cancelled.");
        }
        Lesson cancelled = lesson.withCancellation(reason);
        data.replaceLesson(cancelled);
        LOGGER.log(Level.FINE, "Cancelled {0}: {1}", new Object[]{cancelled.getLessonId(), reason});
        Student student = data.getStudent(cancelled.getStudentId());
        String paidReminder = cancelled.getAmountPaid().signum() > 0
                ? System.lineSeparator() + "  This lesson has " + formatMoney(cancelled.getAmountPaid())
                        + " paid, so refund or carry over that amount."
                : "";
        return "Cancelled " + cancelled.getLessonId() + " for " + formatStudent(student)
                + " (" + formatSlot(cancelled) + ")" + System.lineSeparator()
                + "  Reason: " + cancelled.getCancellationReason() + paidReminder;
    }

    /** Handles {@code delete LESSON_ID}. The student and their homework are not affected. */
    private String delete(String arguments) {
        if (arguments.split("\\s+").length != 1) {
            throw new IllegalArgumentException("Use: delete LESSON_ID");
        }
        Lesson lesson = data.deleteLesson(arguments);
        LOGGER.log(Level.FINE, "Deleted {0}", lesson.getLessonId());
        Student student = data.getStudent(lesson.getStudentId());
        return "Deleted " + lesson.getLessonId() + " for " + formatStudent(student) + ": "
                + formatSlot(lesson) + ", " + lesson.getSubject();
    }

    /**
     * Returns the number of weeks typed for {@code w/}.
     *
     * @throws IllegalArgumentException If it is not a whole number from 2 to {@value #MAX_WEEKS}.
     */
    private static int parseWeekCount(String value) {
        // At most two digits, so parseInt below can never overflow.
        boolean isInRange = value.matches("\\d{1,2}")
                && Integer.parseInt(value) >= 2 && Integer.parseInt(value) <= MAX_WEEKS;
        if (!isInRange) {
            throw new IllegalArgumentException("Weeks must be a whole number from 2 to " + MAX_WEEKS + ".");
        }
        return Integer.parseInt(value);
    }

    /** Returns true if the lesson has attendance or notes, which only a lesson that took place can have. */
    private static boolean hasOutcome(Lesson lesson) {
        return lesson.isRecorded() || !lesson.getNotes().isBlank();
    }

    /** Returns the date and time the lesson starts. */
    private static LocalDateTime startOf(Lesson lesson) {
        return LocalDateTime.of(lesson.getDate(), lesson.getTime());
    }

    /**
     * Returns the attendance in lower case after checking it is one a tutor may type.
     *
     * @throws IllegalArgumentException If the value is not present, absent or late.
     */
    private static String parseAttendance(String value) {
        // Callers pass a required a/ value, which the parser has already checked is present.
        assert value != null : "Attendance should have been checked as required before parsing";
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
        assert student != null : "The student should have been looked up before checking subjects";
        assert subject != null : "Subject should have been checked as required before matching";
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
        String duration = lesson.getDurationMinutes() == Lesson.DEFAULT_DURATION_MINUTES ? ""
                : " (" + lesson.getDurationMinutes() + " min)";
        return DISPLAY_DATE.format(lesson.getDate()) + " " + DISPLAY_TIME.format(lesson.getTime()) + duration;
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
        // Lesson only accepts fees with at most two decimals, so rounding is never needed here.
        assert amount.scale() <= 2 : "Fee has more than two decimal places: " + amount;
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

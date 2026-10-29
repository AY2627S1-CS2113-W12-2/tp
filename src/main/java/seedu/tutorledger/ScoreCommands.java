package seedu.tutorledger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Runs the score and progress commands (features F7.1 to F7.3):
 * <ul>
 *   <li>{@code score} records a test result for a student,</li>
 *   <li>{@code scores} lists a student's results and whether they are improving, and</li>
 *   <li>{@code progress} summarises a student's attendance and scores over a period such as a term.</li>
 * </ul>
 * Like the other command classes, each command returns its display text, and validation problems
 * are reported as an "Error: ..." message instead of crashing the program.
 */
public class ScoreCommands {
    private static final Logger LOGGER = Logger.getLogger(ScoreCommands.class.getName());

    /** Command words this class understands, used by the main loop to route input here. */
    private static final Set<String> COMMAND_WORDS = Set.of("score", "scores", "progress");

    /** Finds the first m/ or d/ prefix, which ends the test name in a score command. */
    private static final Pattern FIRST_SCORE_PREFIX = Pattern.compile("(?i)(?<!\\S)[md]/");

    /** Marks typed as MARKS/TOTAL, e.g. 28/40 or 17.5/20. */
    private static final Pattern MARKS_FORMAT = Pattern.compile("([^/\\s]+)/([^/\\s]+)");

    /** Date format typed by the user, e.g. 21-09-2026. STRICT rejects dates like 31-02-2026. */
    private static final DateTimeFormatter INPUT_DATE = DateTimeFormatter.ofPattern("dd-MM-uuuu", Locale.ROOT)
            .withResolverStyle(ResolverStyle.STRICT);

    /** Date format shown to the user, e.g. Mon 21-09-2026. */
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("EEE dd-MM-uuuu",
            Locale.ENGLISH);

    /** Orders scores oldest first; scores on the same day keep the order they were recorded in. */
    private static final Comparator<Score> BY_DATE = Comparator.comparing(Score::getDate)
            .thenComparingInt(score -> Integer.parseInt(score.getScoreId().substring(1)));

    private final TutorLedgerData data;
    /** Source of today's date; injected so tests can fix the date. */
    private final Clock clock;

    public ScoreCommands(TutorLedgerData data, Clock clock) {
        assert data != null : "ScoreCommands needs a data store";
        assert clock != null : "ScoreCommands needs a clock to know today's date";
        this.data = data;
        this.clock = clock;
    }

    /** Returns true if the first word of the input is a score or progress command. */
    public static boolean handles(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }
        String command = input.strip().split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        return COMMAND_WORDS.contains(command);
    }

    /** Executes one command and returns its display text, including validation errors. */
    public String execute(String input) {
        if (!handles(input)) {
            return "Unknown command.";
        }
        String[] words = input.strip().split("\\s+", 2);
        String command = words[0].toLowerCase(Locale.ROOT);
        String arguments = words.length == 2 ? words[1].trim() : "";
        LOGGER.log(Level.FINE, "Running score command {0} with arguments: {1}", new Object[]{command, arguments});
        try {
            return switch (command) {
            case "score" -> score(arguments);
            case "scores" -> listScores(arguments);
            default -> progress(arguments);
            };
        } catch (IllegalArgumentException exception) {
            // A rejected command is the user's mistake and is already explained on screen, so it is only traced.
            LOGGER.log(Level.FINE, "Rejected score command {0}: {1}",
                    new Object[]{input.strip(), exception.getMessage()});
            return "Error: " + exception.getMessage();
        }
    }

    /** Handles {@code score STUDENT_ID TEST_NAME m/MARKS/TOTAL [d/DATE]}. */
    private String score(String arguments) {
        String usage = "Use: score STUDENT_ID TEST_NAME m/MARKS/TOTAL [d/DATE]";
        String[] pieces = arguments.split("\\s+", 2);
        if (pieces.length < 2) {
            throw new IllegalArgumentException(usage);
        }
        String studentId = pieces[0];
        String rest = pieces[1];

        // The test name is everything before the first prefix, so it may contain spaces.
        Matcher prefix = FIRST_SCORE_PREFIX.matcher(rest);
        if (!prefix.find() || rest.substring(0, prefix.start()).isBlank()) {
            throw new IllegalArgumentException(usage);
        }
        String testName = rest.substring(0, prefix.start()).strip();
        ArgumentParser.Fields fields = ArgumentParser.parseFields(rest.substring(prefix.start()), List.of("m", "d"));

        Matcher marks = MARKS_FORMAT.matcher(fields.single("m", true));
        if (!marks.matches()) {
            throw new IllegalArgumentException("Write the marks as MARKS/TOTAL, for example m/28/40.");
        }
        // The date is optional because scores are usually recorded on the day the test is returned.
        String date = fields.singleOr("d", INPUT_DATE.format(LocalDate.now(clock)));

        Score score = data.addScore(studentId, testName, marks.group(1), marks.group(2), date);
        Student student = data.getStudent(score.getStudentId());
        LOGGER.log(Level.INFO, "Recorded score {0} for {1}", new Object[]{score.getScoreId(), student.getStudentId()});

        StringBuilder output = new StringBuilder("Recorded " + score.getScoreId() + " for " + label(student));
        output.append(System.lineSeparator()).append("  ").append(score.getTestName()).append(": ")
                .append(score.getMarksText()).append(" (").append(percent(score.getPercentage())).append(") on ")
                .append(DISPLAY_DATE.format(score.getDate()));

        // Comparing with the test just before this one answers "is the student improving?" straight away.
        List<Score> history = scoresOf(student.getStudentId());
        int position = history.indexOf(score);
        assert position >= 0 : "The score just recorded should be in the student's history";
        if (position > 0) {
            Score previous = history.get(position - 1);
            output.append(System.lineSeparator()).append("  ")
                    .append(describeChange(score.getPercentage().subtract(previous.getPercentage())))
                    .append(" from ").append(previous.getTestName()).append(" (")
                    .append(percent(previous.getPercentage())).append(")");
        }
        return output.toString();
    }

    /** Handles {@code scores STUDENT_ID}: every score for one student, oldest first, with the trend. */
    private String listScores(String arguments) {
        if (arguments.isEmpty() || arguments.contains(" ")) {
            throw new IllegalArgumentException("Use: scores STUDENT_ID");
        }
        Student student = data.getStudent(arguments);
        List<Score> history = scoresOf(student.getStudentId());
        String heading = count(history.size(), "score") + " for " + label(student);
        if (history.isEmpty()) {
            return heading;
        }
        return heading + System.lineSeparator() + scoreTable(history) + System.lineSeparator() + trend(history);
    }

    /**
     * Handles {@code progress STUDENT_ID [from/DATE] [to/DATE]}: attendance and scores over a period.
     * Without from/, the period starts at the student's first record; without to/, it ends today.
     */
    private String progress(String arguments) {
        String[] pieces = arguments.split("\\s+", 2);
        if (pieces[0].isEmpty()) {
            throw new IllegalArgumentException("Use: progress STUDENT_ID [from/DATE] [to/DATE]");
        }
        Student student = data.getStudent(pieces[0]);
        ArgumentParser.Fields fields = ArgumentParser.parseFields(pieces.length == 2 ? pieces[1] : "",
                List.of("from", "to"));
        LocalDate today = LocalDate.now(clock);
        LocalDate from = fields.has("from") ? parseDate(fields.single("from", true)) : LocalDate.MIN;
        LocalDate to = fields.has("to") ? parseDate(fields.single("to", true)) : today;
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from/ must not be after to/.");
        }
        String id = student.getStudentId();

        // Only lessons that have already happened can have attendance, so later lessons are left out.
        List<Lesson> lessons = data.getLessons().stream()
                .filter(lesson -> id.equals(lesson.getStudentId()))
                // A cancelled lesson was never going to be attended, so it does not count either way.
                .filter(lesson -> !lesson.isCancelled())
                .filter(lesson -> isWithin(lesson.getDate(), from, to) && !lesson.getDate().isAfter(today))
                .toList();
        List<Score> periodScores = scoresOf(id).stream()
                .filter(score -> isWithin(score.getDate(), from, to))
                .toList();
        LOGGER.log(Level.FINE, "Progress for {0}: {1} lessons, {2} scores",
                new Object[]{id, lessons.size(), periodScores.size()});

        String period = from.equals(LocalDate.MIN) ? "up to " + DISPLAY_DATE.format(to)
                : DISPLAY_DATE.format(from) + " to " + DISPLAY_DATE.format(to);
        StringBuilder output = new StringBuilder("Progress for " + label(student) + ", " + period);
        output.append(System.lineSeparator()).append(attendanceSummary(lessons));
        if (periodScores.isEmpty()) {
            output.append(System.lineSeparator()).append("Scores: none in this period");
        } else {
            output.append(System.lineSeparator()).append("Scores: ").append(count(periodScores.size(), "test"))
                    .append(", average ").append(percent(average(periodScores)))
                    .append(System.lineSeparator()).append(scoreTable(periodScores))
                    .append(System.lineSeparator()).append(trend(periodScores));
        }
        return output.toString();
    }

    /** Describes how often the student came, counting a late arrival as attending. */
    private static String attendanceSummary(List<Lesson> lessons) {
        int present = countAttendance(lessons, Lesson.Attendance.PRESENT);
        int late = countAttendance(lessons, Lesson.Attendance.LATE);
        int absent = countAttendance(lessons, Lesson.Attendance.ABSENT);
        int notRecorded = countAttendance(lessons, Lesson.Attendance.NOT_RECORDED);
        int recorded = present + late + absent;
        assert recorded + notRecorded == lessons.size() : "Every lesson has exactly one attendance value";

        if (recorded == 0) {
            String result = "Attendance: no recorded lessons in this period";
            return notRecorded == 0 ? result
                    : result + System.lineSeparator() + "  " + count(notRecorded, "lesson") + " not recorded yet";
        }
        BigDecimal rate = BigDecimal.valueOf(100L * (present + late))
                .divide(BigDecimal.valueOf(recorded), 1, RoundingMode.HALF_UP);
        String result = "Attendance: attended " + (present + late) + " of " + count(recorded, "recorded lesson")
                + " (" + percent(rate) + ")" + System.lineSeparator()
                + "  " + present + " present, " + late + " late, " + absent + " absent";
        if (notRecorded > 0) {
            result += System.lineSeparator() + "  " + count(notRecorded, "lesson") + " not recorded yet";
        }
        return result;
    }

    private static int countAttendance(List<Lesson> lessons, Lesson.Attendance attendance) {
        return (int) lessons.stream().filter(lesson -> lesson.getAttendance() == attendance).count();
    }

    /** Lays out scores in aligned columns, showing the change from the previous score on each row. */
    private static String scoreTable(List<Score> scores) {
        int nameWidth = scores.stream().mapToInt(score -> score.getTestName().length()).max().orElse(0);
        int marksWidth = scores.stream().mapToInt(score -> score.getMarksText().length()).max().orElse(0);
        int idWidth = scores.stream().mapToInt(score -> score.getScoreId().length()).max().orElse(0);
        String rowFormat = "  %-" + idWidth + "s  %s  %-" + nameWidth + "s  %-" + marksWidth + "s  %6s";

        StringBuilder table = new StringBuilder();
        for (int i = 0; i < scores.size(); i++) {
            Score score = scores.get(i);
            if (i > 0) {
                table.append(System.lineSeparator());
            }
            table.append(String.format(rowFormat, score.getScoreId(), DISPLAY_DATE.format(score.getDate()),
                    score.getTestName(), score.getMarksText(), percent(score.getPercentage())));
            if (i > 0) {
                BigDecimal change = score.getPercentage().subtract(scores.get(i - 1).getPercentage());
                table.append("  ").append(change.signum() > 0 ? "+" : "").append(change.toPlainString());
            }
        }
        return table.toString();
    }

    /** Compares the first and latest score, so the tutor can see the overall direction at a glance. */
    private static String trend(List<Score> scores) {
        if (scores.size() < 2) {
            return "Trend: record at least 2 scores to see a trend";
        }
        BigDecimal change = scores.getLast().getPercentage().subtract(scores.getFirst().getPercentage());
        String direction = change.signum() > 0 ? "improving" : change.signum() < 0 ? "declining" : "steady";
        return "Trend: " + direction + ", " + describeChange(change).toLowerCase(Locale.ROOT)
                + " from the first test to the latest";
    }

    /** Turns a change in percentage points into words, e.g. "Up 12.5 points". */
    private static String describeChange(BigDecimal change) {
        if (change.signum() == 0) {
            return "No change";
        }
        return (change.signum() > 0 ? "Up " : "Down ") + change.abs().toPlainString() + " points";
    }

    private static BigDecimal average(List<Score> scores) {
        BigDecimal total = scores.stream().map(Score::getPercentage).reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(scores.size()), 1, RoundingMode.HALF_UP);
    }

    /** Returns one student's scores, oldest first. */
    private List<Score> scoresOf(String studentId) {
        return data.getScores().stream()
                .filter(score -> studentId.equals(score.getStudentId()))
                .sorted(BY_DATE)
                .toList();
    }

    private static boolean isWithin(LocalDate date, LocalDate from, LocalDate to) {
        return !date.isBefore(from) && !date.isAfter(to);
    }

    private static LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value, INPUT_DATE);
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException("Date must be a real date in DD-MM-YYYY format.", exception);
        }
    }

    private static String percent(BigDecimal value) {
        return value.toPlainString() + "%";
    }

    private static String count(int amount, String noun) {
        return amount + " " + noun + (amount == 1 ? "" : "s");
    }

    /** Formats a student as "S1 Amirah Binte Rahman". */
    private static String label(Student student) {
        return student.getStudentId() + " " + student.getName();
    }
}

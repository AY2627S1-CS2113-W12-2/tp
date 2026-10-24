package seedu.tutorledger;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Lists lesson notes as covered topics (US12), without storing a second copy of lesson history. */
public class TopicCommands {
    private static final Logger LOGGER = Logger.getLogger(TopicCommands.class.getName());
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEE dd-MM-uuuu", Locale.ENGLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HHmm", Locale.ROOT);
    /** Most recent lessons appear first, with a stable ID order when dates and times are equal. */
    private static final Comparator<Lesson> NEWEST_FIRST = Comparator.comparing(Lesson::getDate)
            .thenComparing(Lesson::getTime).thenComparing(Lesson::getLessonId).reversed();

    private final TutorLedgerData data;
    private final Clock clock;

    /** Shares the existing records and clock so eligibility uses the same date as other commands. */
    public TopicCommands(TutorLedgerData data, Clock clock) {
        assert data != null : "TopicCommands needs shared records";
        assert clock != null : "TopicCommands needs a clock";
        this.data = data;
        this.clock = clock;
    }

    /** Returns whether the first command word is topics; null and blank input belong to other handlers. */
    public static boolean handles(String input) {
        return input != null && !input.isBlank()
                && input.strip().split("\\s+", 2)[0].equalsIgnoreCase("topics");
    }

    /** Executes {@code topics STUDENT_ID [s/SUBJECT]}, reporting validation errors without mutation. */
    public String execute(String input) {
        if (!handles(input)) {
            return "Unknown topics command.";
        }
        try {
            String[] command = input.strip().split("\\s+", 2);
            if (command.length < 2) {
                throw new IllegalArgumentException("Use: topics STUDENT_ID [s/SUBJECT]");
            }
            String[] arguments = command[1].split("\\s+", 2);
            Student student = data.getStudent(arguments[0]);
            ArgumentParser.Fields fields = ArgumentParser.parseFields(arguments.length == 2 ? arguments[1] : "",
                    List.of("s"));
            String subject = fields.singleOr("s", null);
            return listTopics(student, subject);
        } catch (IllegalArgumentException exception) {
            LOGGER.log(Level.FINE, "Topics command rejected: {0}", exception.getMessage());
            return "Error: " + exception.getMessage();
        }
    }

    /** Selects attended lessons with notes; current subject enrolment and payment do not limit history. */
    private String listTopics(Student student, String subject) {
        LocalDate today = LocalDate.now(clock);
        List<Lesson> lessons = data.getLessons().stream()
                .filter(lesson -> student.getStudentId().equalsIgnoreCase(lesson.getStudentId()))
                .filter(lesson -> !lesson.isCancelled() && !lesson.getDate().isAfter(today))
                .filter(lesson -> lesson.getAttendance() == Lesson.Attendance.PRESENT
                        || lesson.getAttendance() == Lesson.Attendance.LATE)
                .filter(lesson -> !lesson.getNotes().isBlank())
                .filter(lesson -> subject == null || lesson.getSubject().equalsIgnoreCase(subject))
                .sorted(NEWEST_FIRST)
                .toList();
        LOGGER.log(Level.FINE, "Found {0} covered-topic lessons for {1}",
                new Object[]{lessons.size(), student.getStudentId()});
        String studentLabel = student.getStudentId() + " " + student.getName()
                + (subject == null ? "" : " (subject: " + subject + ")");
        if (lessons.isEmpty()) {
            return "No topics recorded for " + studentLabel + ".";
        }
        StringBuilder output = new StringBuilder("Topics covered with " + studentLabel + ": "
                + lessons.size() + (lessons.size() == 1 ? " lesson" : " lessons"));
        for (Lesson lesson : lessons) {
            assert !lesson.getNotes().isBlank() : "A covered-topic entry must contain notes";
            output.append(System.lineSeparator()).append("  ").append(lesson.getLessonId()).append("  ")
                    .append(DATE.format(lesson.getDate())).append(" ").append(TIME.format(lesson.getTime()))
                    .append("  ").append(lesson.getSubject()).append(System.lineSeparator())
                    .append("    ").append(lesson.getNotes());
        }
        return output.toString();
    }
}

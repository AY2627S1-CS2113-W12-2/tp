package seedu.tutorledger;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Lists lesson notes as covered topics, without storing a second copy of lesson history.
 *
 * <p>A lesson's notes are its topic tag: {@code topics S1} lists what was covered with one student (US12), and
 * {@code topics k/vectors} finds every lesson that covered a topic, with any student (US22).
 */
public class TopicCommands extends CommandHandler {
    private static final Logger LOGGER = Logger.getLogger(TopicCommands.class.getName());
    /** Most recent lessons appear first, with a stable ID order when dates and times are equal. */
    private static final Comparator<Lesson> NEWEST_FIRST = Comparator.comparing(Lesson::getDate)
            .thenComparing(Lesson::getTime).thenComparing(Lesson::getLessonId).reversed();
    private static final String USAGE = "Use: topics STUDENT_ID [s/SUBJECT] [k/KEYWORD], or topics k/KEYWORD"
            + " [s/SUBJECT] to search every student.";

    private final TutorLedgerData data;
    private final Clock clock;

    /** Shares the existing records and clock so eligibility uses the same date as other commands. */
    public TopicCommands(TutorLedgerData data, Clock clock) {
        super(Set.of("topics"));
        assert data != null : "TopicCommands needs shared records";
        assert clock != null : "TopicCommands needs a clock";
        this.data = data;
        this.clock = clock;
    }

    /**
     * Runs {@code topics STUDENT_ID [s/SUBJECT] [k/KEYWORD]} or {@code topics k/KEYWORD [s/SUBJECT]}.
     *
     * <p>The second form finds every lesson that covered a topic, whichever student it was with (US22).
     */
    @Override
    protected String run(CommandLine line) {
        return listTopics(parseQuery(line.arguments()));
    }

    /**
     * Reads the optional student ID and the prefixed filters.
     *
     * @throws IllegalArgumentException If a value is invalid, or neither a student nor a keyword is given.
     */
    private Query parseQuery(String arguments) {
        // As in filter, a first word without a slash is the student ID and the rest are prefixed values.
        String[] pieces = arguments.split("\\s+", 2);
        Student student = null;
        String prefixed = arguments;
        if (!pieces[0].isEmpty() && !pieces[0].contains("/")) {
            student = data.getStudent(pieces[0]);
            prefixed = pieces.length == 2 ? pieces[1] : "";
        }
        ArgumentParser.Fields fields;
        try {
            // A keyword is free text, so a topic such as "and/or logic" is searched for as typed.
            fields = ArgumentParser.parseFields(prefixed, List.of("s", "k"), Set.of("k"));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(exception.getMessage() + " " + USAGE);
        }
        String keyword = fields.singleOr("k", null);
        if (student == null && keyword == null) {
            // Without either, the command would print every note ever written, which is not a topic search.
            throw new IllegalArgumentException(USAGE);
        }
        return new Query(student, fields.singleOr("s", null), keyword);
    }

    /** Selects attended lessons with notes; current subject enrolment and payment do not limit history. */
    private String listTopics(Query query) {
        LocalDate today = LocalDate.now(clock);
        List<Lesson> lessons = data.getLessons().stream()
                .filter(lesson -> lesson.hasTakenPlaceBy(today))
                .filter(lesson -> lesson.getAttendance() == Lesson.Attendance.PRESENT
                        || lesson.getAttendance() == Lesson.Attendance.LATE)
                .filter(lesson -> !lesson.getNotes().isBlank())
                .filter(query::matches)
                .sorted(NEWEST_FIRST)
                .toList();
        LOGGER.log(Level.FINE, "Found {0} covered-topic lessons for {1}",
                new Object[]{lessons.size(), query.describe()});
        if (lessons.isEmpty()) {
            return "No topics recorded for " + query.describe() + ".";
        }
        StringBuilder output = new StringBuilder("Topics covered with " + query.describe() + ": "
                + lessons.size() + (lessons.size() == 1 ? " lesson" : " lessons"));
        for (Lesson lesson : lessons) {
            assert !lesson.getNotes().isBlank() : "A covered-topic entry must contain notes";
            assert query.matches(lesson) : "Every listed lesson must match the query";
            output.append(System.lineSeparator()).append("  ").append(lesson.getLessonId()).append("  ")
                    .append(Formats.DISPLAY_DATE.format(lesson.getDate())).append(" ")
                    .append(Formats.TIME.format(lesson.getTime()))
                    .append("  ").append(query.student() == null ? studentLabel(lesson) + "  " : "")
                    .append(lesson.getSubject()).append(System.lineSeparator())
                    .append("    ").append(lesson.getNotes());
        }
        return output.toString();
    }

    /** Formats a lesson's student as "S1 Alice Tan", so a search across students shows whose lesson it was. */
    private String studentLabel(Lesson lesson) {
        Student student = data.getStudent(lesson.getStudentId());
        return student.getStudentId() + " " + student.getName();
    }

    /**
     * The filters a covered-topic lesson must pass. A null field means that filter was not given.
     *
     * @param student Only lessons with this student.
     * @param subject Only lessons of this subject, ignoring case.
     * @param keyword Only lessons whose notes contain this text, ignoring case.
     */
    private record Query(Student student, String subject, String keyword) {
        Query {
            assert student != null || keyword != null : "A topics query needs a student or a keyword";
        }

        boolean matches(Lesson lesson) {
            return (student == null || student.getStudentId().equalsIgnoreCase(lesson.getStudentId()))
                    && (subject == null || lesson.getSubject().equalsIgnoreCase(subject))
                    && (keyword == null || lesson.getNotes().toLowerCase(Locale.ROOT)
                            .contains(keyword.toLowerCase(Locale.ROOT)));
        }

        /** Describes the query, e.g. "S1 Alice Tan (subject: Math)" or "any student (topic: vectors)". */
        String describe() {
            List<String> filters = new ArrayList<>();
            if (subject != null) {
                filters.add("subject: " + subject);
            }
            if (keyword != null) {
                filters.add("topic: " + keyword);
            }
            String who = student == null ? "any student" : student.getStudentId() + " " + student.getName();
            return who + (filters.isEmpty() ? "" : " (" + String.join(", ", filters) + ")");
        }
    }
}

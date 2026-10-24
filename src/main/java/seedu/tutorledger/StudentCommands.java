package seedu.tutorledger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses student commands, including the optional default hourly rate (US5). */
public class StudentCommands {
    /** Records rejected commands when debug logging is enabled. */
    private static final Logger LOGGER = Logger.getLogger(StudentCommands.class.getName());
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEE dd-MM-uuuu", Locale.ENGLISH);
    /** Locates the first list prefix, leaving the preceding text as the existing free-text name query. */
    private static final Pattern LIST_PREFIX = Pattern.compile("(?i)(?<!\\S)[a-z]+/");
    private final TutorLedgerData data;
    private final Clock clock;

    public StudentCommands(TutorLedgerData data, Clock clock) {
        assert data != null : "StudentCommands needs shared records";
        assert clock != null : "StudentCommands needs a clock";
        this.data = data;
        this.clock = clock;
    }

    /** Executes one command and returns its display text, including validation errors. */
    public String execute(String input) {
        if (input == null || input.isBlank()) {
            return "Enter a command.";
        }
        String[] words = input.strip().split("\\s+", 2);
        String command = words[0].toLowerCase(Locale.ROOT);
        String arguments = words.length == 2 ? words[1].trim() : "";
        try {
            return switch (command) {
            case "add" -> add(arguments);
            case "subject" -> subject(arguments);
            case "view" -> view(arguments);
            case "list" -> list(arguments);
            case "edit" -> edit(arguments);
            case "delete" -> delete(arguments);
            case "archive" -> setArchived(arguments, true);
            case "restore" -> setArchived(arguments, false);
            case "help" -> HelpCommand.getHelpText();
            default -> "Unknown command: " + words[0] + ".";
            };
        } catch (IllegalArgumentException exception) {
            LOGGER.log(Level.FINE, "Student command {0} rejected: {1}",
                    new Object[]{command, exception.getMessage()});
            return "Error: " + exception.getMessage();
        }
    }

    /**
     * Handles {@code add n/NAME l/LEVEL p/PHONE_NUMBER [s/SUBJECT]... [r/RATE]}.
     * Subjects are optional, so a whole student record can be entered in one line (story US38)
     * instead of following {@code add} with a separate {@code subject} command.
     */
    private String add(String arguments) {
        ArgumentParser.Fields fields = ArgumentParser.parseFields(arguments, List.of("n", "l", "p", "s", "r"));
        String name = fields.single("n", true);
        String level = fields.single("l", true);
        String phone = fields.single("p", true);
        List<String> subjects = fields.values("s");
        // Checked before adding, so a bad subject does not leave a half-entered student behind.
        if (subjects.stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException("Give a non-blank s/SUBJECT, or leave s/ out.");
        }
        Student student = data.addStudent(name, level, phone, subjects, fields.singleOr("r", null));
        String subjectLine = subjects.isEmpty() ? "" : System.lineSeparator() + "Subjects: " + subjectText(student);
        String rateLine = fields.has("r") ? System.lineSeparator() + rateText(student) : "";
        return "Added " + details(student) + subjectLine + rateLine + System.lineSeparator()
                + studentCountText();
    }

    /** Explains the difference between stored profiles and the default active-only list when needed. */
    private String studentCountText() {
        int total = data.getStudents().size();
        long archived = data.getStudents().stream().filter(Student::isArchived).count();
        String breakdown = archived == 0 ? "" : " in total (" + (total - archived)
                + " active, " + archived + " archived)";
        return "You now have " + count(total, "student") + breakdown + ".";
    }

    private String subject(String arguments) {
        IdAndFields input = parseIdAndFields(arguments, List.of("s"));
        Student student = data.getStudent(input.id());
        List<String> additions = input.fields().values("s");
        if (additions.isEmpty() || additions.stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException("Give at least one non-blank s/SUBJECT.");
        }
        List<String> subjects = new ArrayList<>(student.getSubjects());
        subjects.addAll(additions);
        Student updated = student.withDetails(student.getName(), student.getLevel(),
                student.getPhoneNumber(), subjects);
        int added = updated.getSubjects().size() - student.getSubjects().size();
        data.replaceStudent(updated);
        return "Added " + count(added, "subject") + " to " + student.getStudentId() + " " + student.getName()
                + ". Subjects: " + subjectText(updated);
    }

    /** Lists active profiles by default without changing shared lookups used by payments and storage. */
    private String list(String arguments) {
        String query = arguments;
        String status = "active";
        Matcher prefix = LIST_PREFIX.matcher(arguments);
        if (prefix.find()) {
            query = arguments.substring(0, prefix.start()).trim();
            ArgumentParser.Fields fields = ArgumentParser.parseFields(arguments.substring(prefix.start()),
                    List.of("status"));
            status = fields.single("status", true).toLowerCase(Locale.ROOT);
        }
        if (!List.of("active", "archived", "all").contains(status)) {
            throw new IllegalArgumentException("Use status/active, status/archived or status/all.");
        }
        String selectedStatus = status;
        List<Student> matches = data.findStudents(query).stream()
                .filter(student -> selectedStatus.equals("all")
                        || student.isArchived() == selectedStatus.equals("archived"))
                .toList();
        String scope = status.equals("active") ? "" : " (" + status + ")";
        String heading = count(matches.size(), "student") + scope
                + (query.isEmpty() ? ":" : " match \"" + query + "\":");
        StringBuilder result = new StringBuilder(heading);
        for (Student student : matches) {
            result.append(System.lineSeparator()).append("  ").append(student.getStudentId()).append("  ")
                    .append(student.getName()).append(" (").append(student.getLevel()).append(")  ")
                    .append(subjectText(student)).append(student.isArchived() ? "  [archived]" : "");
        }
        return result.toString();
    }

    /** Archives or restores a profile without changing its linked records or permanent ID. */
    private String setArchived(String arguments, boolean archived) {
        Student student = data.getStudent(singleId(arguments));
        if (student.isArchived() == archived) {
            return student.getStudentId() + " " + student.getName() + " is already "
                    + (archived ? "archived." : "active.");
        }
        data.replaceStudent(student.withArchived(archived));
        LOGGER.log(Level.FINE, "Student {0} archive state changed to {1}",
                new Object[]{student.getStudentId(), archived});
        return (archived ? "Archived " : "Restored ") + student.getStudentId() + ": " + student.getName()
                + " (" + student.getLevel() + ")." + System.lineSeparator()
                + (archived ? "History is kept. Existing lessons are unchanged; restore before scheduling new ones."
                        : "The student is active again. History and existing lessons are unchanged.");
    }

    private String edit(String arguments) {
        IdAndFields input = parseIdAndFields(arguments, List.of("n", "l", "p", "s", "r"));
        if (input.fields().isEmpty()) {
            throw new IllegalArgumentException("Give at least one value to edit.");
        }
        Student student = data.getStudent(input.id());
        String name = input.fields().singleOr("n", student.getName());
        String level = input.fields().singleOr("l", student.getLevel());
        String phone = input.fields().singleOr("p", student.getPhoneNumber());
        List<String> subjects = input.fields().has("s") ? input.fields().values("s") : student.getSubjects();
        if (subjects.size() > 1 && subjects.stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException("Use s/ alone to clear all subjects.");
        }
        if (subjects.size() == 1 && subjects.getFirst().isBlank()) {
            subjects = List.of();
        }
        Student updated = student.withDetails(name, level, phone, subjects);
        if (input.fields().has("r")) {
            // Only edit accepts a blank r/, which deliberately clears the rate.
            updated = updated.withDefaultHourlyRate(input.fields().single("r", false));
        }
        data.replaceStudent(updated);
        return "Edited " + details(updated) + System.lineSeparator() + "Subjects: " + subjectText(updated)
                + (input.fields().has("r") ? System.lineSeparator() + rateText(updated) : "");
    }

    /**
     * Handles {@code delete STUDENT_ID [confirm]} (US34). Without explicit confirmation,
     * students with any lesson records receive a warning and all records stay unchanged.
     */
    private String delete(String arguments) {
        String[] words = arguments.split("\\s+");
        if (arguments.isBlank() || words.length > 2
                || (words.length == 2 && !words[1].equalsIgnoreCase("confirm"))) {
            throw new IllegalArgumentException("Use: delete STUDENT_ID [confirm]");
        }
        Student student = data.getStudent(singleId(words[0]));
        boolean confirmed = words.length == 2;
        if (!confirmed) {
            long lessonCount = data.getLessons().stream()
                    .filter(lesson -> student.getStudentId().equalsIgnoreCase(lesson.getStudentId()))
                    .count();
            if (lessonCount > 0) {
                return deletionWarning(student, lessonCount);
            }
        }
        LOGGER.log(Level.FINE, "Deleting student {0}; explicitly confirmed: {1}",
                new Object[]{student.getStudentId(), confirmed});
        TutorLedgerData.Deletion removed = data.deleteStudent(student.getStudentId());
        String result = "Deleted " + student.getStudentId() + ": " + student.getName() + " (" + student.getLevel()
                + ")" + System.lineSeparator() + "Also deleted " + count(removed.lessonCount(), "lesson") + " and "
                + count(removed.homeworkCount(), "homework item") + ".";
        // Scores are only mentioned when there were some, so the usual message stays short.
        if (removed.scoreCount() > 0) {
            result += System.lineSeparator() + "Also deleted " + count(removed.scoreCount(), "score") + ".";
        }
        return result;
    }

    /** Describes the current cascade without deleting anything or remembering a pending action. */
    private String deletionWarning(Student student, long lessonCount) {
        assert student != null : "A deletion warning needs an existing student";
        assert lessonCount > 0 : "Only students with lessons require a deletion warning";
        String id = student.getStudentId();
        long homeworkCount = data.getHomework().stream()
                .filter(item -> id.equalsIgnoreCase(item.getStudentId())).count();
        long scoreCount = data.getScores().stream()
                .filter(score -> id.equalsIgnoreCase(score.getStudentId())).count();
        LOGGER.log(Level.FINE, "Deletion requires confirmation for {0}: {1} linked lessons",
                new Object[]{id, lessonCount});
        return "Warning: deleting " + id + ": " + student.getName() + " (" + student.getLevel()
                + ") will permanently remove:" + System.lineSeparator()
                + "  " + count(lessonCount, "lesson") + ", " + count(homeworkCount, "homework item")
                + " and " + count(scoreCount, "score") + "." + System.lineSeparator()
                + "This cannot be undone. No records have been deleted." + System.lineSeparator()
                + "To confirm, type: delete " + id + " confirm" + System.lineSeparator()
                + "Otherwise, continue with another command.";
    }

    private String view(String arguments) {
        Student student = data.getStudent(singleId(arguments));
        String id = student.getStudentId();
        StringBuilder output = new StringBuilder(id + "  " + student.getName() + "  (" + student.getLevel()
                + ")  " + student.getPhoneNumber());
        if (student.isArchived()) {
            output.append(System.lineSeparator()).append("Status: archived");
        }
        output.append(System.lineSeparator()).append("Subjects: ").append(subjectText(student));
        output.append(System.lineSeparator()).append(rateText(student));
        output.append(System.lineSeparator()).append("Lessons");
        List<Lesson> lessons = data.getLessons().stream()
                .filter(lesson -> id.equalsIgnoreCase(lesson.getStudentId()))
                .sorted(Comparator.comparing(Lesson::getDate).thenComparing(Lesson::getTime)
                        .thenComparing(Lesson::getLessonId).reversed())
                .toList();
        LocalDate today = LocalDate.now(clock);
        BigDecimal owed = BigDecimal.ZERO;
        int unpaidCount = 0;
        for (Lesson lesson : lessons) {
            output.append(System.lineSeparator()).append("  ").append(lesson.getLessonId()).append("  ")
                    .append(DATE.format(lesson.getDate())).append(" ")
                    .append(String.format(Locale.ROOT, "%02d%02d",
                            lesson.getTime().getHour(), lesson.getTime().getMinute()))
                    .append("  ").append(lesson.getSubject()).append("  ").append(money(lesson.getFee()))
                    .append("  ").append(lesson.isCancelled() ? "cancelled"
                            : lesson.getAttendance().name().toLowerCase(Locale.ROOT).replace('_', ' '))
                    .append("  ").append(lesson.getPaymentStatus());
            if (lesson.getDurationMinutes() != Lesson.DEFAULT_DURATION_MINUTES) {
                output.append("  ").append(lesson.getDurationMinutes()).append(" min");
            }
            if (lesson.isCancelled()) {
                output.append(System.lineSeparator()).append("       Reason: ").append(lesson.getCancellationReason());
            }
            if (!lesson.getNotes().isBlank()) {
                output.append(System.lineSeparator()).append("       ").append(lesson.getNotes());
            }
            // A cancelled lesson did not take place, so it is not owed.
            boolean isOwed = !lesson.isCancelled() && !lesson.getDate().isAfter(today)
                    && lesson.getPaymentStatus().equalsIgnoreCase("unpaid");
            if (isOwed) {
                owed = owed.add(lesson.getFee());
                unpaidCount++;
            }
        }
        output.append(System.lineSeparator()).append("Outstanding homework");
        data.getHomework().stream()
                .filter(item -> id.equalsIgnoreCase(item.getStudentId()))
                .filter(item -> !item.getSubmitStatus().equalsIgnoreCase("submitted"))
                .sorted(Comparator.comparing(Homework::getDueDate).thenComparing(Homework::getHomeworkId))
                .forEach(item -> output.append(System.lineSeparator()).append("  ").append(item.getHomeworkId())
                        .append("  ").append(item.getDescription()).append("  due ")
                        .append(DATE.format(item.getDueDate())));
        output.append(System.lineSeparator()).append("Amount owed: ").append(money(owed))
                .append(" (").append(unpaidCount).append(unpaidCount == 1 ? " lesson)" : " lessons)");
        return output.toString();
    }

    private static String details(Student student) {
        return student.getStudentId() + ": " + student.getName() + " (" + student.getLevel() + "), "
                + student.getPhoneNumber();
    }

    private static String subjectText(Student student) {
        return student.getSubjects().isEmpty() ? "none" : String.join(", ", student.getSubjects());
    }

    /** Formats the optional rate consistently for add, edit and view. */
    private static String rateText(Student student) {
        return "Default hourly rate: " + student.getDefaultHourlyRate()
                .map(rate -> money(rate) + "/hour").orElse("not set");
    }

    private static String money(BigDecimal amount) {
        return "$" + amount.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }

    private static String count(long amount, String noun) {
        return amount + " " + noun + (amount == 1 ? "" : "s");
    }

    private static String singleId(String argument) {
        if (!argument.matches("(?i)S[1-9]\\d*")) {
            throw new IllegalArgumentException("Give a student ID such as S1.");
        }
        return argument;
    }

    private static IdAndFields parseIdAndFields(String arguments, List<String> allowed) {
        String[] pieces = arguments.split("\\s+", 2);
        String id = singleId(pieces[0]);
        String remainder = pieces.length == 2 ? pieces[1] : "";
        return new IdAndFields(id, ArgumentParser.parseFields(remainder, allowed));
    }

    private record IdAndFields(String id, ArgumentParser.Fields fields) {
    }
}

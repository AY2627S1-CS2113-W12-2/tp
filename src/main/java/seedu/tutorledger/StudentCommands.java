package seedu.tutorledger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses and runs the six student commands from the v1 user guide. */
public class StudentCommands {
    private static final Pattern PREFIX = Pattern.compile("(?i)(?<!\\S)([a-z]+)/");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEE dd-MM-uuuu", Locale.ENGLISH);
    private final TutorLedgerData data;
    private final Clock clock;

    public StudentCommands(TutorLedgerData data, Clock clock) {
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
            default -> "Unknown command: " + words[0] + ".";
            };
        } catch (IllegalArgumentException exception) {
            return "Error: " + exception.getMessage();
        }
    }

    private String add(String arguments) {
        Fields fields = parseFields(arguments, List.of("n", "l", "p"));
        String name = fields.single("n", true);
        String level = fields.single("l", true);
        String phone = fields.single("p", true);
        Student student = data.addStudent(name, level, phone);
        return "Added " + details(student) + System.lineSeparator()
                + "You now have " + count(data.getStudents().size(), "student") + ".";
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

    private String list(String query) {
        List<Student> matches = data.findStudents(query);
        String heading = query.isEmpty() ? count(matches.size(), "student") + ":"
                : count(matches.size(), "student") + " match \"" + query + "\":";
        StringBuilder result = new StringBuilder(heading);
        for (Student student : matches) {
            result.append(System.lineSeparator()).append("  ").append(student.getStudentId()).append("  ")
                    .append(student.getName()).append(" (").append(student.getLevel()).append(")  ")
                    .append(subjectText(student));
        }
        return result.toString();
    }

    private String edit(String arguments) {
        IdAndFields input = parseIdAndFields(arguments, List.of("n", "l", "p", "s"));
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
        data.replaceStudent(updated);
        return "Edited " + details(updated) + System.lineSeparator() + "Subjects: " + subjectText(updated);
    }

    private String delete(String arguments) {
        Student student = data.getStudent(singleId(arguments));
        TutorLedgerData.Deletion removed = data.deleteStudent(student.getStudentId());
        return "Deleted " + student.getStudentId() + ": " + student.getName() + " (" + student.getLevel() + ")"
                + System.lineSeparator() + "Also deleted " + count(removed.lessonCount(), "lesson") + " and "
                + count(removed.homeworkCount(), "homework item") + ".";
    }

    private String view(String arguments) {
        Student student = data.getStudent(singleId(arguments));
        String id = student.getStudentId();
        StringBuilder output = new StringBuilder(id + "  " + student.getName() + "  (" + student.getLevel()
                + ")  " + student.getPhoneNumber());
        output.append(System.lineSeparator()).append("Subjects: ").append(subjectText(student));
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
                    .append("  ").append(lesson.getAttendance().name().toLowerCase(Locale.ROOT).replace('_', ' '))
                    .append("  ").append(lesson.getPaymentStatus());
            if (!lesson.getNotes().isBlank()) {
                output.append(System.lineSeparator()).append("       ").append(lesson.getNotes());
            }
            if (!lesson.getDate().isAfter(today) && lesson.getPaymentStatus().equalsIgnoreCase("unpaid")) {
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

    private static String money(BigDecimal amount) {
        return "$" + amount.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }

    private static String count(int amount, String noun) {
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
        return new IdAndFields(id, parseFields(remainder, allowed));
    }

    /** Separates prefixed values without splitting names or subjects containing spaces. */
    private static Fields parseFields(String text, List<String> allowed) {
        Map<String, List<String>> values = new HashMap<>();
        Matcher matcher = PREFIX.matcher(text);
        int previousEnd = 0;
        String previousKey = null;
        while (matcher.find()) {
            if (previousKey == null && !text.substring(0, matcher.start()).isBlank()) {
                throw new IllegalArgumentException("Expected a prefixed value.");
            }
            if (previousKey != null) {
                values.get(previousKey).add(text.substring(previousEnd, matcher.start()).trim());
            }
            String key = matcher.group(1).toLowerCase(Locale.ROOT);
            if (!allowed.contains(key)) {
                throw new IllegalArgumentException("Unexpected prefix " + key + "/.");
            }
            values.computeIfAbsent(key, ignored -> new ArrayList<>());
            previousKey = key;
            previousEnd = matcher.end();
        }
        if (previousKey == null) {
            if (!text.isBlank()) {
                throw new IllegalArgumentException("Expected a prefixed value.");
            }
        } else {
            values.get(previousKey).add(text.substring(previousEnd).trim());
        }
        return new Fields(values);
    }

    private record IdAndFields(String id, Fields fields) {
    }

    private record Fields(Map<String, List<String>> fields) {
        boolean isEmpty() {
            return fields.isEmpty();
        }

        boolean has(String key) {
            return fields.containsKey(key);
        }

        List<String> values(String key) {
            return fields.getOrDefault(key, List.of());
        }

        String single(String key, boolean required) {
            List<String> entries = values(key);
            if (entries.size() > 1) {
                throw new IllegalArgumentException("Give " + key + "/ only once.");
            }
            if (entries.isEmpty() || entries.getFirst().isBlank()) {
                if (required) {
                    throw new IllegalArgumentException("Give a non-blank " + key + "/ value.");
                }
                return null;
            }
            return entries.getFirst();
        }

        String singleOr(String key, String fallback) {
            return has(key) ? single(key, true) : fallback;
        }
    }
}

package seedu.tutorledger;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Shared in-memory records used by student commands and future feature commands. */
public class TutorLedgerData {
    private final Map<String, Student> students = new LinkedHashMap<>();
    private final Map<String, Lesson> lessons = new LinkedHashMap<>();
    private final Map<String, Homework> homework = new LinkedHashMap<>();
    private int nextStudentNumber = 1;
    /** Number used for the next homework ID (H1, H2, ...); never decreases so IDs are not reused. */
    private int nextHomeworkNumber = 1;

    /** Adds a student and assigns the next permanent ID. */
    public Student addStudent(String name, String level, String phoneNumber) {
        Student candidate = new Student("S" + nextStudentNumber, name, level, phoneNumber);
        ensureUnique(candidate, null);
        students.put(candidate.getStudentId(), candidate);
        nextStudentNumber++;
        return candidate;
    }

    /** Replaces an existing profile after checking for duplicate identity. */
    public void replaceStudent(Student student) {
        getStudent(student.getStudentId());
        ensureUnique(student, student.getStudentId());
        students.put(student.getStudentId(), student);
    }

    /** Returns a student, accepting IDs in either case. */
    public Student getStudent(String id) {
        String key = normaliseId(id);
        Student student = students.get(key);
        if (student == null) {
            throw new IllegalArgumentException("No student found with ID " + key + ".");
        }
        return student;
    }

    /** Returns students in numeric ID order, optionally filtered by name. */
    public List<Student> findStudents(String nameFragment) {
        String needle = nameFragment.toLowerCase(Locale.ROOT);
        return students.values().stream()
                .filter(student -> student.getName().toLowerCase(Locale.ROOT).contains(needle))
                .sorted(Comparator.comparingInt(student -> Integer.parseInt(student.getStudentId().substring(1))))
                .toList();
    }

    /** Imports a student for storage reload while advancing the ID counter. */
    public void importStudent(Student student) {
        ensureUnique(student, null);
        if (students.putIfAbsent(student.getStudentId(), student) != null) {
            throw new IllegalArgumentException("Duplicate student ID " + student.getStudentId() + ".");
        }
        nextStudentNumber = Math.max(nextStudentNumber, Integer.parseInt(student.getStudentId().substring(1)) + 1);
    }

    public int getNextStudentNumber() {
        return nextStudentNumber;
    }

    /** Restores the saved counter, including IDs belonging to deleted students. */
    public void setNextStudentNumber(int value) {
        if (value < nextStudentNumber) {
            throw new IllegalArgumentException("Next student number cannot reuse an ID.");
        }
        nextStudentNumber = value;
    }

    public Collection<Student> getStudents() {
        return List.copyOf(students.values());
    }

    public Collection<Lesson> getLessons() {
        return List.copyOf(lessons.values());
    }

    public Collection<Homework> getHomework() {
        return List.copyOf(homework.values());
    }

    /** Registers a lesson after checking its student link. */
    public void putLesson(Lesson lesson) {
        getStudent(lesson.getStudentId());
        lessons.put(lesson.getLessonId().toUpperCase(Locale.ROOT), lesson);
    }

    /** Registers homework after checking its student link. */
    public void putHomework(Homework item) {
        getStudent(item.getStudentId());
        String key = item.getHomeworkId().toUpperCase(Locale.ROOT);
        homework.put(key, item);
        // Keep the counter ahead of any H<number> ID added directly, so addHomework never reuses it.
        if (key.matches("H[1-9]\\d*")) {
            nextHomeworkNumber = Math.max(nextHomeworkNumber, Integer.parseInt(key.substring(1)) + 1);
        }
    }

    /**
     * Creates outstanding homework for an existing student and assigns it the next homework ID.
     *
     * @param studentId the ID of the student receiving the homework, in either case
     * @param description what the homework is
     * @param dueDate the date the homework is due
     * @return the newly stored homework
     */
    public Homework addHomework(String studentId, String description, LocalDate dueDate) {
        Student student = getStudent(studentId);
        Homework item = new Homework("H" + nextHomeworkNumber, student.getStudentId(), description, dueDate,
                Homework.STATUS_OUTSTANDING);
        putHomework(item);
        return item;
    }

    /** Returns a homework item, accepting IDs in either case. */
    public Homework getHomeworkById(String id) {
        if (id == null || !id.matches("(?i)H[1-9]\\d*")) {
            throw new IllegalArgumentException("Homework ID must look like H1.");
        }
        String key = id.toUpperCase(Locale.ROOT);
        Homework item = homework.get(key);
        if (item == null) {
            throw new IllegalArgumentException("No homework found with ID " + key + ".");
        }
        return item;
    }

    public int getNextHomeworkNumber() {
        return nextHomeworkNumber;
    }

    /** Removes a student and all linked records, returning their deletion counts. */
    public Deletion deleteStudent(String id) {
        Student student = getStudent(id);
        String key = student.getStudentId();
        students.remove(key);
        int lessonCount = removeLinked(lessons, key);
        int homeworkCount = removeLinked(homework, key);
        return new Deletion(student, lessonCount, homeworkCount);
    }

    private static <T> int removeLinked(Map<String, T> records, String studentId) {
        List<String> keys = new ArrayList<>();
        for (Map.Entry<String, T> entry : records.entrySet()) {
            T record = entry.getValue();
            String owner = record instanceof Lesson lesson ? lesson.getStudentId() : ((Homework) record).getStudentId();
            if (studentId.equalsIgnoreCase(owner)) {
                keys.add(entry.getKey());
            }
        }
        keys.forEach(records::remove);
        return keys.size();
    }

    private void ensureUnique(Student candidate, String excludedId) {
        for (Student existing : students.values()) {
            if (!existing.getStudentId().equals(excludedId)
                    && existing.getName().equalsIgnoreCase(candidate.getName())
                    && existing.getPhoneNumber().equals(candidate.getPhoneNumber())) {
                throw new IllegalArgumentException("A student with that name and phone number already exists.");
            }
        }
    }

    private static String normaliseId(String id) {
        if (id == null || !id.matches("(?i)S[1-9]\\d*")) {
            throw new IllegalArgumentException("Student ID must look like S1.");
        }
        return id.toUpperCase(Locale.ROOT);
    }

    /** Student deletion result for the user-facing command. */
    public record Deletion(Student student, int lessonCount, int homeworkCount) {
    }
}

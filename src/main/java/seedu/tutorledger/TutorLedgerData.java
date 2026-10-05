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
    /** Number used for the next lesson ID (L1, L2, ...); never decreases so IDs are not reused. */
    private int nextLessonNumber = 1;
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
        String key = lesson.getLessonId().toUpperCase(Locale.ROOT);
        lessons.put(key, lesson);
        // Keep the counter ahead of any L<number> ID added directly, so addLesson never reuses it.
        if (key.matches("L[1-9]\\d*")) {
            nextLessonNumber = Math.max(nextLessonNumber, Integer.parseInt(key.substring(1)) + 1);
        }
    }

    /**
     * Schedules a lesson for an existing student and assigns it the next lesson ID.
     * A new lesson starts as not recorded and unpaid, with no notes.
     *
     * @param studentId The ID of the student taking the lesson, in either case.
     * @param subject The subject being taught.
     * @param date The lesson date in DD-MM-YYYY format.
     * @param time The lesson start time in HHMM format.
     * @param fee The fee charged for this lesson.
     * @return The newly stored lesson.
     * @throws IllegalArgumentException If a value is invalid or another lesson starts at the same time.
     */
    public Lesson addLesson(String studentId, String subject, String date, String time, String fee) {
        Student student = getStudent(studentId);
        Lesson lesson = new Lesson("L" + nextLessonNumber, student.getStudentId(), subject, date, time,
                Lesson.Attendance.NOT_RECORDED.name(), fee, "");
        ensureSlotFree(lesson);
        putLesson(lesson);
        return lesson;
    }

    /**
     * Replaces an existing lesson with a changed copy that has the same ID.
     *
     * @throws IllegalArgumentException If the lesson was moved to a slot where another lesson starts.
     */
    public void replaceLesson(Lesson lesson) {
        Lesson existing = getLesson(lesson.getLessonId());
        // Only a lesson that actually moved needs the check, so other edits are never blocked by it.
        boolean isMoved = !existing.getDate().equals(lesson.getDate())
                || !existing.getTime().equals(lesson.getTime());
        if (isMoved) {
            ensureSlotFree(lesson);
        }
        lessons.put(existing.getLessonId().toUpperCase(Locale.ROOT), lesson);
    }

    /** Removes a lesson and returns it, so the caller can report what was deleted. */
    public Lesson deleteLesson(String id) {
        Lesson lesson = getLesson(id);
        lessons.remove(lesson.getLessonId().toUpperCase(Locale.ROOT));
        return lesson;
    }

    public int getNextLessonNumber() {
        return nextLessonNumber;
    }

    /** Returns a lesson by ID, accepting IDs in either case. */
    public Lesson getLesson(String id) {
        if (id == null || !id.matches("(?i)L[1-9]\\d*")) {
            throw new IllegalArgumentException("Lesson ID must look like L1.");
        }
        String key = id.toUpperCase(Locale.ROOT);
        Lesson lesson = lessons.get(key);
        if (lesson == null) {
            throw new IllegalArgumentException("No lesson found with ID " + key + ".");
        }
        return lesson;
    }

    /** Changes an existing lesson's payment status without modifying its other details. */
    public Lesson updateLessonPaymentStatus(String id, String status) {
        Lesson updated = getLesson(id).withPaymentStatus(status);
        lessons.put(updated.getLessonId().toUpperCase(Locale.ROOT), updated);
        return updated;
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

    /** Rejects a lesson that would start at the same date and time as a different lesson. */
    private void ensureSlotFree(Lesson candidate) {
        for (Lesson existing : lessons.values()) {
            if (!existing.getLessonId().equalsIgnoreCase(candidate.getLessonId())
                    && existing.getDate().equals(candidate.getDate())
                    && existing.getTime().equals(candidate.getTime())) {
                throw new IllegalArgumentException(existing.getLessonId()
                        + " already starts at that date and time.");
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

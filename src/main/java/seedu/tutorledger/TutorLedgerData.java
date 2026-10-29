package seedu.tutorledger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Shared in-memory records used by student commands and future feature commands. */
public class TutorLedgerData {
    private static final Logger LOGGER = Logger.getLogger(TutorLedgerData.class.getName());

    /** Start time format used when describing a clashing lesson, e.g. 1600. */
    private static final DateTimeFormatter START_TIME = DateTimeFormatter.ofPattern("HHmm", Locale.ROOT);

    private final Map<String, Student> students = new LinkedHashMap<>();
    private final Map<String, Lesson> lessons = new LinkedHashMap<>();
    private final Map<String, Homework> homework = new LinkedHashMap<>();
    private final Map<String, Score> scores = new LinkedHashMap<>();
    private final Map<String, FollowUpTask> tasks = new LinkedHashMap<>();
    private int nextStudentNumber = 1;
    /** Number used for the next lesson ID (L1, L2, ...); never decreases so IDs are not reused. */
    private int nextLessonNumber = 1;
    /** Number used for the next homework ID (H1, H2, ...); never decreases so IDs are not reused. */
    private int nextHomeworkNumber = 1;
    /** Number used for the next score ID (A1, A2, ...); never decreases so IDs are not reused. */
    private int nextScoreNumber = 1;
    /** Number used for the next follow-up task ID (T1, T2, ...); never decreases so IDs are not reused. */
    private int nextTaskNumber = 1;

    /** Adds a student and assigns the next permanent ID. */
    public Student addStudent(String name, String level, String phoneNumber) {
        return addStudent(name, level, phoneNumber, List.of(), null);
    }

    /** Validates the complete profile before storing it or consuming its permanent ID. */
    public Student addStudent(String name, String level, String phoneNumber, List<String> subjects, String hourlyRate) {
        LOGGER.log(Level.INFO, "Adding student: name={0}, level={1}, phone={2}",
                new Object[]{name, level, phoneNumber});
        Student candidate = new Student("S" + nextStudentNumber, name, level, phoneNumber, subjects, hourlyRate);
        ensureUnique(candidate, null);
        students.put(candidate.getStudentId(), candidate);
        nextStudentNumber++;
        LOGGER.log(Level.INFO, "Student added: {0}", candidate.getStudentId());
        return candidate;
    }

    /** Replaces an existing profile after checking for duplicate identity. */
    public void replaceStudent(Student student) {
        LOGGER.log(Level.INFO, "Replacing student {0}", student.getStudentId());
        getStudent(student.getStudentId());
        ensureUnique(student, student.getStudentId());
        students.put(student.getStudentId(), student);
    }

    /** Returns a student, accepting IDs in either case. */
    public Student getStudent(String id) {
        LOGGER.log(Level.FINE, "Looking up student by ID: {0}", id);
        String key = normaliseId(id);
        Student student = students.get(key);
        if (student == null) {
            throw new IllegalArgumentException("No student found with ID " + key + ".");
        }
        return student;
    }

    /** Returns students in numeric ID order, optionally filtered by name. */
    public List<Student> findStudents(String nameFragment) {
        LOGGER.log(Level.FINE, "Finding students matching: {0}", nameFragment);
        String needle = nameFragment.toLowerCase(Locale.ROOT);
        return students.values().stream()
                .filter(student -> student.getName().toLowerCase(Locale.ROOT).contains(needle))
                .sorted(Comparator.comparingInt(student -> Integer.parseInt(student.getStudentId().substring(1))))
                .toList();
    }

    /** Imports a student for storage reload while advancing the ID counter. */
    public void importStudent(Student student) {
        LOGGER.log(Level.INFO, "Importing student: {0}", student.getStudentId());
        ensureUnique(student, null);
        if (students.putIfAbsent(student.getStudentId(), student) != null) {
            throw new IllegalArgumentException("Duplicate student ID " + student.getStudentId() + ".");
        }
        nextStudentNumber = Math.max(nextStudentNumber, Integer.parseInt(student.getStudentId().substring(1)) + 1);
    }

    public int getNextStudentNumber() {
        LOGGER.log(Level.FINE, "Current next student number: {0}", nextStudentNumber);
        return nextStudentNumber;
    }

    /** Restores the saved counter, including IDs belonging to deleted students. */
    public void setNextStudentNumber(int value) {
        LOGGER.log(Level.INFO, "Setting next student number to {0}", value);
        if (value < nextStudentNumber) {
            throw new IllegalArgumentException("Next student number cannot reuse an ID.");
        }
        nextStudentNumber = value;
    }

    public Collection<Student> getStudents() {
        LOGGER.log(Level.FINE, "Retrieving all students ({0})", students.size());
        return List.copyOf(students.values());
    }

    public Collection<Lesson> getLessons() {
        LOGGER.log(Level.FINE, "Retrieving all lessons ({0})", lessons.size());
        return List.copyOf(lessons.values());
    }

    public Collection<Homework> getHomework() {
        LOGGER.log(Level.FINE, "Retrieving all homework ({0})", homework.size());
        return List.copyOf(homework.values());
    }

    /** Registers a lesson after checking its student link. */
    public void putLesson(Lesson lesson) {
        LOGGER.log(Level.INFO, "Putting lesson {0} into store", lesson.getLessonId());
        getStudent(lesson.getStudentId());
        String key = lesson.getLessonId().toUpperCase(Locale.ROOT);
        lessons.put(key, lesson);
        // Keep the counter ahead of any L<number> ID added directly, so addLesson never reuses it.
        if (key.matches("L[1-9]\\d*")) {
            nextLessonNumber = Math.max(nextLessonNumber, Integer.parseInt(key.substring(1)) + 1);
        }
        LOGGER.log(Level.INFO, "Lesson stored: {0}", key);
    }

    /**
     * Schedules a lesson for an existing student and assigns it the next lesson ID.
     * A new lesson starts as not recorded, with no notes. Zero-fee lessons are already paid;
     * lessons with a positive fee start unpaid.
     *
     * @param studentId The ID of the student taking the lesson, in either case.
     * @param subject The subject being taught.
     * @param date The lesson date in DD-MM-YYYY format.
     * @param time The lesson start time in HHMM format.
     * @param fee The total fee charged for this lesson, or null to use the student's hourly rate.
     * @return The newly stored lesson.
     * @throws IllegalArgumentException If a value is invalid or another lesson starts at the same time.
     */
    public Lesson addLesson(String studentId, String subject, String date, String time, String fee) {
        return addLesson(studentId, subject, date, time, fee, Lesson.DEFAULT_DURATION_MINUTES);
    }

    /**
     * Adds a lesson whose attendance and notes are already known, and assigns it the next lesson ID.
     * This is how a lesson that has already happened is logged after the fact. It starts unpaid.
     *
     * @param studentId The ID of the student taking the lesson, in either case.
     * @param subject The subject being taught.
     * @param date The lesson date in DD-MM-YYYY format.
     * @param time The lesson start time in HHMM format.
     * @param fee The fee charged for this lesson.
     * @param attendance The attendance status, such as present.
     * @param notes What was covered, or an empty string for none.
     * @return The newly stored lesson.
     * @throws IllegalArgumentException If a value is invalid or another lesson starts at the same time.
     */
    public Lesson addLesson(String studentId, String subject, String date, String time, String fee,
            String attendance, String notes) {
        return addLesson(studentId, subject, date, time, fee, attendance, notes, Lesson.DEFAULT_DURATION_MINUTES);
    }

    /** Schedules an unrecorded lesson with an explicit duration and optional fee override. */
    public Lesson addLesson(String studentId, String subject, String date, String time, String fee,
            int durationMinutes) {
        requireActiveStudent(studentId);
        return addLesson(studentId, subject, date, time, fee, Lesson.Attendance.NOT_RECORDED.name(), "",
                durationMinutes);
    }

    /** Validates a complete lesson before insertion, shared by scheduling and after-the-fact logging. */
    private Lesson addLesson(String studentId, String subject, String date, String time, String fee,
            String attendance, String notes, int durationMinutes) {
        LOGGER.log(Level.INFO, "Adding lesson for student {0} subject={1} date={2} time={3} fee={4}",
                new Object[]{studentId, subject, date, time, fee});
        Student student = getStudent(studentId);
        fee = resolveLessonFee(student, fee, durationMinutes);
        int lessonNumber = nextLessonNumber;
        Lesson lesson = new Lesson("L" + lessonNumber, student.getStudentId(), subject, date, time,
                attendance, fee, notes, durationMinutes);
        ensureSlotFree(lesson);
        putLesson(lesson);
        // putLesson must move the counter past the ID just used, or the next lesson would overwrite this one.
        assert nextLessonNumber == lessonNumber + 1 : "Lesson counter did not advance after scheduling";
        assert lessons.get(lesson.getLessonId()) == lesson : "Scheduled lesson was not stored";
        LOGGER.log(Level.INFO, "Lesson scheduled: {0}", lesson.getLessonId());
        return lesson;
    }

    /**
     * Schedules the same weekly slot for several weeks in a row, giving each lesson the next lesson ID.
     *
     * <p>Either every lesson is added or none is: all the slots are checked before any lesson is
     * stored, so a clash in a later week never leaves the earlier weeks half-scheduled.
     *
     * @param studentId The ID of the student taking the lessons, in either case.
     * @param subject The subject being taught.
     * @param firstDate The date of the first lesson in DD-MM-YYYY format.
     * @param time The start time of every lesson in HHMM format.
     * @param fee The fee charged for each lesson.
     * @param weekCount How many weekly lessons to add, counting the first one.
     * @return The new lessons, earliest first.
     * @throws IllegalArgumentException If a value is invalid or any week's slot is already taken.
     */
    public List<Lesson> addWeeklyLessons(String studentId, String subject, String firstDate, String time,
            String fee, int weekCount) {
        return addWeeklyLessons(studentId, subject, firstDate, time, fee, weekCount, Lesson.DEFAULT_DURATION_MINUTES);
    }

    /** Schedules every week with the same fixed fee and duration, or stores nothing if a slot conflicts. */
    public List<Lesson> addWeeklyLessons(String studentId, String subject, String firstDate, String time,
            String fee, int weekCount, int durationMinutes) {
        assert weekCount >= 1 : "A weekly slot needs at least one lesson";
        LOGGER.log(Level.INFO, "Scheduling {0} weekly lessons for student {1} from {2}",
                new Object[]{weekCount, studentId, firstDate});
        Student student = requireActiveStudent(studentId);
        fee = resolveLessonFee(student, fee, durationMinutes);
        LocalDate start = Lesson.parseDate(firstDate);
        int firstNumber = nextLessonNumber;
        List<Lesson> planned = new ArrayList<>();
        for (int week = 0; week < weekCount; week++) {
            Lesson lesson = new Lesson("L" + (firstNumber + week), student.getStudentId(), subject,
                    Lesson.formatDate(start.plusWeeks(week)), time, Lesson.Attendance.NOT_RECORDED.name(),
                    fee, "", durationMinutes);
            ensureSlotFree(lesson);
            planned.add(lesson);
        }
        planned.forEach(this::putLesson);
        assert nextLessonNumber == firstNumber + weekCount
                : "Lesson counter did not advance past every new lesson";
        LOGGER.log(Level.INFO, "Weekly lessons scheduled: L{0} to L{1}",
                new Object[]{firstNumber, firstNumber + weekCount - 1});
        return planned;
    }

    /** Rejects new scheduling for archived students; imports, corrections and historical logging stay available. */
    private Student requireActiveStudent(String id) {
        Student student = getStudent(id);
        if (student.isArchived()) {
            LOGGER.log(Level.FINE, "Scheduling rejected for archived student {0}", student.getStudentId());
            throw new IllegalArgumentException(student.getStudentId()
                    + " is archived. Restore with restore " + student.getStudentId() + " before scheduling lessons.");
        }
        assert !student.isArchived() : "A schedulable student must be active";
        return student;
    }

    /** Resolves a fixed total once; explicit fees and prior lessons are never repriced. */
    private static String resolveLessonFee(Student student, String fee, int durationMinutes) {
        Lesson.validateDuration(durationMinutes);
        if (fee == null) {
            BigDecimal rate = student.getDefaultHourlyRate().orElseThrow(() -> new IllegalArgumentException(
                    "Set a default hourly rate with edit " + student.getStudentId() + " r/RATE, or give f/FEE."));
            fee = rate.multiply(BigDecimal.valueOf(durationMinutes))
                    .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP).toPlainString();
            LOGGER.log(Level.FINE, "Calculated fee {0} for {1} minutes for student {2}",
                    new Object[]{fee, durationMinutes, student.getStudentId()});
        }
        return fee;
    }

    /**
     * Replaces an existing lesson with a changed copy that has the same ID.
     *
     * @throws IllegalArgumentException If the lesson was moved to a slot where another lesson starts.
     */
    public void replaceLesson(Lesson lesson) {
        assert lesson != null : "Replacement lesson should not be null";
        LOGGER.log(Level.INFO, "Replacing lesson {0}", lesson.getLessonId());
        Lesson existing = getLesson(lesson.getLessonId());
        // The replacement is a copy of an existing lesson, so it must still belong to the same student.
        assert existing.getStudentId().equals(lesson.getStudentId())
                : "Replacement moved the lesson to another student";
        // Only a lesson that now takes up different time needs the check, so other edits are never blocked
        // by it. That covers a move, a longer or shorter lesson, and a cancellation being undone.
        boolean takesNewTime = !existing.getStart().equals(lesson.getStart())
                || existing.getDurationMinutes() != lesson.getDurationMinutes()
                || (existing.isCancelled() && !lesson.isCancelled());
        if (takesNewTime) {
            ensureSlotFree(lesson);
        }
        lessons.put(existing.getLessonId().toUpperCase(Locale.ROOT), lesson);
        LOGGER.log(Level.FINE, "Lesson replaced: {0} (takes new time={1})",
                new Object[]{lesson.getLessonId(), takesNewTime});
    }

    /** Removes a lesson and returns it, so the caller can report what was deleted. */
    public Lesson deleteLesson(String id) {
        LOGGER.log(Level.INFO, "Deleting lesson {0}", id);
        Lesson lesson = getLesson(id);
        String key = lesson.getLessonId().toUpperCase(Locale.ROOT);
        lessons.remove(key);
        assert !lessons.containsKey(key) : "Deleted lesson is still stored";
        LOGGER.log(Level.FINE, "Lesson deleted: {0}", key);
        return lesson;
    }

    /**
     * Imports a saved lesson, keeping its ID, for storage loading.
     *
     * <p>Unlike {@link #putLesson}, this rejects a lesson whose ID is already taken or that starts at
     * the same date and time as another lesson, so a hand-edited data file cannot break those rules.
     * A lesson that only overlaps another one is still loaded, so that older data files keep every lesson.
     *
     * @throws IllegalArgumentException If the student is missing, the ID is taken or the slot is not free.
     */
    public void importLesson(Lesson lesson) {
        LOGGER.log(Level.INFO, "Importing lesson {0}", lesson.getLessonId());
        if (!lesson.getLessonId().matches("(?i)L[1-9]\\d*")) {
            throw new IllegalArgumentException("Lesson ID must look like L1.");
        }
        if (lessons.containsKey(lesson.getLessonId().toUpperCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Duplicate lesson ID " + lesson.getLessonId() + ".");
        }
        // Files saved before overlaps were checked may hold overlapping lessons. Rejecting them here would
        // drop those lessons from the tutor's records, so loading only rejects two lessons with one start.
        ensureNoClash(lesson, false);
        putLesson(lesson);
    }

    public int getNextLessonNumber() {
        LOGGER.log(Level.FINE, "Current next lesson number: {0}", nextLessonNumber);
        return nextLessonNumber;
    }

    /** Restores the saved lesson counter, including IDs belonging to deleted lessons. */
    public void setNextLessonNumber(int value) {
        LOGGER.log(Level.INFO, "Setting next lesson number to {0}", value);
        if (value < nextLessonNumber) {
            throw new IllegalArgumentException("Next lesson number cannot reuse an ID.");
        }
        nextLessonNumber = value;
    }

    /** Returns a lesson by ID, accepting IDs in either case. */
    public Lesson getLesson(String id) {
        LOGGER.log(Level.FINE, "Looking up lesson by ID: {0}", id);
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
        LOGGER.log(Level.INFO, "Updating payment status for lesson {0} to {1}", new Object[]{id, status});
        Lesson updated = getLesson(id).withPaymentStatus(status);
        lessons.put(updated.getLessonId().toUpperCase(Locale.ROOT), updated);
        return updated;
    }

    /** Replaces a lesson's allocated payment amount without changing its other details. */
    public Lesson updateLessonAmountPaid(String id, BigDecimal amountPaid) {
        LOGGER.log(Level.INFO, "Updating amount paid for lesson {0} to {1}", new Object[]{id, amountPaid});
        Lesson updated = getLesson(id).withAmountPaid(amountPaid);
        lessons.put(updated.getLessonId().toUpperCase(Locale.ROOT), updated);
        return updated;
    }

    /** Registers homework after checking its student link. */
    public void putHomework(Homework item) {
        LOGGER.log(Level.INFO, "Putting homework {0} into store", item.getHomeworkId());
        getStudent(item.getStudentId());
        String key = item.getHomeworkId().toUpperCase(Locale.ROOT);
        homework.put(key, item);
        // Keep the counter ahead of any H<number> ID added directly, so addHomework never reuses it.
        if (key.matches("H[1-9]\\d*")) {
            nextHomeworkNumber = Math.max(nextHomeworkNumber, Integer.parseInt(key.substring(1)) + 1);
        }
        LOGGER.log(Level.INFO, "Homework stored: {0}", key);
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
        LOGGER.log(Level.INFO, "Assigning homework to student {0}: description={1}, dueDate={2}",
                new Object[]{studentId, description, dueDate});
        Student student = getStudent(studentId);
        Homework item = new Homework("H" + nextHomeworkNumber, student.getStudentId(), description, dueDate,
                Homework.STATUS_OUTSTANDING);
        int numberBefore = nextHomeworkNumber;
        putHomework(item);
        assert nextHomeworkNumber == numberBefore + 1 : "Homework counter should advance so IDs are never reused";
        LOGGER.log(Level.INFO, "Homework assigned: {0}", item.getHomeworkId());
        return item;
    }

    /** Returns a homework item, accepting IDs in either case. */
    public Homework getHomeworkById(String id) {
        LOGGER.log(Level.FINE, "Looking up homework by ID: {0}", id);
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

    /**
     * Imports saved homework, keeping its ID, for storage loading.
     *
     * @throws IllegalArgumentException If the student is missing or the ID is already taken.
     */
    public void importHomework(Homework item) {
        LOGGER.log(Level.INFO, "Importing homework {0}", item.getHomeworkId());
        if (!item.getHomeworkId().matches("(?i)H[1-9]\\d*")) {
            throw new IllegalArgumentException("Homework ID must look like H1.");
        }
        if (homework.containsKey(item.getHomeworkId().toUpperCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Duplicate homework ID " + item.getHomeworkId() + ".");
        }
        putHomework(item);
    }

    public int getNextHomeworkNumber() {
        LOGGER.log(Level.FINE, "Current next homework number: {0}", nextHomeworkNumber);
        return nextHomeworkNumber;
    }

    /** Restores the saved homework counter, including IDs belonging to deleted homework. */
    public void setNextHomeworkNumber(int value) {
        LOGGER.log(Level.INFO, "Setting next homework number to {0}", value);
        if (value < nextHomeworkNumber) {
            throw new IllegalArgumentException("Next homework number cannot reuse an ID.");
        }
        nextHomeworkNumber = value;
    }

    public Collection<Score> getScores() {
        LOGGER.log(Level.FINE, "Retrieving all scores ({0})", scores.size());
        return List.copyOf(scores.values());
    }

    /**
     * Records a score for an existing student and gives it the next score ID.
     *
     * @param studentId The student who took the test, in either case.
     * @param testName What the test was.
     * @param marks The marks scored, as typed.
     * @param totalMarks The maximum possible marks, as typed.
     * @param date The test date in DD-MM-YYYY format.
     * @return The stored score.
     * @throws IllegalArgumentException If the student does not exist or a value is invalid.
     */
    public Score addScore(String studentId, String testName, String marks, String totalMarks, String date) {
        LOGGER.log(Level.INFO, "Recording score for student {0}: {1}", new Object[]{studentId, testName});
        Student student = getStudent(studentId);
        Score score = new Score("A" + nextScoreNumber, student.getStudentId(), testName, marks, totalMarks, date);
        int numberBefore = nextScoreNumber;
        putScore(score);
        assert nextScoreNumber == numberBefore + 1 : "Score counter should advance so IDs are never reused";
        return score;
    }

    /**
     * Imports a saved score, keeping its ID, for storage loading.
     *
     * @throws IllegalArgumentException If the student is missing or the ID is invalid or already taken.
     */
    public void importScore(Score score) {
        LOGGER.log(Level.INFO, "Importing score {0}", score.getScoreId());
        if (!score.getScoreId().matches("(?i)A[1-9]\\d*")) {
            throw new IllegalArgumentException("Score ID must look like A1.");
        }
        if (scores.containsKey(score.getScoreId().toUpperCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Duplicate score ID " + score.getScoreId() + ".");
        }
        putScore(score);
    }

    /** Stores a score after checking its student, keeping the ID counter ahead of its ID. */
    private void putScore(Score score) {
        getStudent(score.getStudentId());
        String key = score.getScoreId().toUpperCase(Locale.ROOT);
        scores.put(key, score);
        if (key.matches("A[1-9]\\d*")) {
            nextScoreNumber = Math.max(nextScoreNumber, Integer.parseInt(key.substring(1)) + 1);
        }
    }

    public int getNextScoreNumber() {
        return nextScoreNumber;
    }

    /** Restores the saved score counter, including IDs belonging to deleted scores. */
    public void setNextScoreNumber(int value) {
        LOGGER.log(Level.INFO, "Setting next score number to {0}", value);
        if (value < nextScoreNumber) {
            throw new IllegalArgumentException("Next score number cannot reuse an ID.");
        }
        nextScoreNumber = value;
    }

    public Collection<FollowUpTask> getTasks() {
        LOGGER.log(Level.FINE, "Retrieving all follow-up tasks ({0})", tasks.size());
        return List.copyOf(tasks.values());
    }

    /**
     * Logs a pending follow-up task and gives it the next task ID.
     *
     * @param studentId The student the task is for, in either case, or null if it is not about one student.
     * @param description What needs to be done.
     * @param dueDate When it should be done by, or null for no date.
     * @return The stored task.
     * @throws IllegalArgumentException If the student does not exist or the description is blank.
     */
    public FollowUpTask addTask(String studentId, String description, LocalDate dueDate) {
        LOGGER.log(Level.INFO, "Adding follow-up task for student {0}: {1}", new Object[]{studentId, description});
        // Looking the student up both checks they exist and gives their ID in its usual upper-case form.
        String linkedId = studentId == null ? null : getStudent(studentId).getStudentId();
        FollowUpTask task = new FollowUpTask("T" + nextTaskNumber, description, linkedId, dueDate,
                FollowUpTask.STATUS_PENDING);
        int numberBefore = nextTaskNumber;
        putTask(task);
        assert nextTaskNumber == numberBefore + 1 : "Task counter should advance so IDs are never reused";
        return task;
    }

    /** Returns a follow-up task, accepting IDs in either case. */
    public FollowUpTask getTask(String id) {
        if (id == null || !id.matches("(?i)T[1-9]\\d*")) {
            throw new IllegalArgumentException("Task ID must look like T1.");
        }
        String key = id.toUpperCase(Locale.ROOT);
        FollowUpTask task = tasks.get(key);
        if (task == null) {
            throw new IllegalArgumentException("No follow-up task found with ID " + key + ".");
        }
        return task;
    }

    /** Replaces an existing task with a changed copy that has the same ID, such as one marked done. */
    public void replaceTask(FollowUpTask task) {
        getTask(task.getTaskId());
        putTask(task);
    }

    /**
     * Imports a saved task, keeping its ID, for storage loading.
     *
     * @throws IllegalArgumentException If the linked student is missing or the ID is invalid or already taken.
     */
    public void importTask(FollowUpTask task) {
        LOGGER.log(Level.INFO, "Importing follow-up task {0}", task.getTaskId());
        if (!task.getTaskId().matches("(?i)T[1-9]\\d*")) {
            throw new IllegalArgumentException("Task ID must look like T1.");
        }
        if (tasks.containsKey(task.getTaskId().toUpperCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Duplicate task ID " + task.getTaskId() + ".");
        }
        putTask(task);
    }

    /** Stores a task after checking any linked student, keeping the ID counter ahead of its ID. */
    private void putTask(FollowUpTask task) {
        task.getStudentId().ifPresent(this::getStudent);
        String key = task.getTaskId().toUpperCase(Locale.ROOT);
        tasks.put(key, task);
        if (key.matches("T[1-9]\\d*")) {
            nextTaskNumber = Math.max(nextTaskNumber, Integer.parseInt(key.substring(1)) + 1);
        }
    }

    public int getNextTaskNumber() {
        return nextTaskNumber;
    }

    /** Restores the saved task counter, including IDs belonging to deleted tasks. */
    public void setNextTaskNumber(int value) {
        LOGGER.log(Level.INFO, "Setting next task number to {0}", value);
        if (value < nextTaskNumber) {
            throw new IllegalArgumentException("Next task number cannot reuse an ID.");
        }
        nextTaskNumber = value;
    }

    /**
     * Replaces every record and ID counter with those of {@code source}.
     *
     * <p>Used by {@code reload}. The command classes all share this one object, so the data is
     * copied into it rather than swapping in a new object they would not see.
     */
    public void replaceAllWith(TutorLedgerData source) {
        LOGGER.log(Level.INFO, "Replacing all records from source data");
        students.clear();
        students.putAll(source.students);
        lessons.clear();
        lessons.putAll(source.lessons);
        homework.clear();
        homework.putAll(source.homework);
        scores.clear();
        scores.putAll(source.scores);
        tasks.clear();
        tasks.putAll(source.tasks);
        nextStudentNumber = source.nextStudentNumber;
        nextLessonNumber = source.nextLessonNumber;
        nextHomeworkNumber = source.nextHomeworkNumber;
        nextScoreNumber = source.nextScoreNumber;
        nextTaskNumber = source.nextTaskNumber;
    }

    /** Removes a student and all linked records, returning their deletion counts. */
    public Deletion deleteStudent(String id) {
        LOGGER.log(Level.INFO, "Deleting student {0}", id);
        Student student = getStudent(id);
        String key = student.getStudentId();
        students.remove(key);
        int lessonCount = removeLinked(lessons, key);
        int homeworkCount = removeLinked(homework, key);
        int scoreCount = removeLinked(scores, key);
        // A reminder about a student who has been deleted is no longer useful, so it goes too.
        int taskCount = removeLinked(tasks, key);
        return new Deletion(student, lessonCount, homeworkCount, scoreCount, taskCount);
    }

    private static <T> int removeLinked(Map<String, T> records, String studentId) {
        LOGGER.log(Level.FINE, "Removing linked records for student {0}", studentId);
        List<String> keys = new ArrayList<>();
        for (Map.Entry<String, T> entry : records.entrySet()) {
            String owner = switch (entry.getValue()) {
            case Lesson lesson -> lesson.getStudentId();
            case Homework item -> item.getStudentId();
            case Score score -> score.getStudentId();
            case FollowUpTask task -> task.getStudentId().orElse(null);
            default -> throw new AssertionError("Unexpected linked record type: " + entry.getValue());
            };
            if (studentId.equalsIgnoreCase(owner)) {
                keys.add(entry.getKey());
            }
        }
        keys.forEach(records::remove);
        return keys.size();
    }

    private void ensureUnique(Student candidate, String excludedId) {
        LOGGER.log(Level.FINE, "Ensuring uniqueness for student {0}", candidate.getStudentId());
        for (Student existing : students.values()) {
            if (!existing.getStudentId().equals(excludedId)
                    && existing.getName().equalsIgnoreCase(candidate.getName())
                    && existing.getPhoneNumber().equals(candidate.getPhoneNumber())) {
                throw new IllegalArgumentException("A student with that name and phone number already exists.");
            }
        }
    }

    /**
     * Rejects a lesson that would be taught at the same time as a different lesson, even for a minute.
     * A tutor can only teach one lesson at a time, so this applies across all students.
     */
    private void ensureSlotFree(Lesson candidate) {
        ensureNoClash(candidate, true);
    }

    /**
     * Rejects a lesson that clashes with a different lesson.
     * A cancelled lesson will not take place, so it neither blocks a slot nor is blocked by one.
     *
     * @param candidate The lesson about to be stored.
     * @param isOverlapChecked Whether a lesson that only overlaps another one, without sharing its start
     *     time, also counts as a clash.
     * @throws IllegalArgumentException If another lesson starts at the same time, or overlaps when checked.
     */
    private void ensureNoClash(Lesson candidate, boolean isOverlapChecked) {
        assert candidate != null : "Cannot check the slot of a null lesson";
        if (candidate.isCancelled()) {
            return;
        }
        LOGGER.log(Level.FINE, "Checking slot availability for lesson {0} from {1} to {2}",
                new Object[]{candidate.getLessonId(), candidate.getStart(), candidate.getEnd()});
        for (Lesson existing : lessons.values()) {
            if (existing.getLessonId().equalsIgnoreCase(candidate.getLessonId()) || existing.isCancelled()) {
                continue;
            }
            // The same start is reported on its own, since it is the clearest description of the clash.
            if (existing.getStart().equals(candidate.getStart())) {
                throw new IllegalArgumentException(existing.getLessonId()
                        + " already starts at that date and time.");
            }
            if (isOverlapChecked && existing.overlaps(candidate)) {
                throw new IllegalArgumentException("That time overlaps " + existing.getLessonId() + " ("
                        + Lesson.formatDate(existing.getDate()) + " " + existing.getTime().format(START_TIME)
                        + ", " + existing.getDurationMinutes() + " min).");
            }
        }
    }

    private static String normaliseId(String id) {
        LOGGER.log(Level.FINE, "Normalising student ID: {0}", id);
        if (id == null || !id.matches("(?i)S[1-9]\\d*")) {
            throw new IllegalArgumentException("Student ID must look like S1.");
        }
        return id.toUpperCase(Locale.ROOT);
    }

    /** Student deletion result for the user-facing command. */
    public record Deletion(Student student, int lessonCount, int homeworkCount, int scoreCount, int taskCount) {
    }
}

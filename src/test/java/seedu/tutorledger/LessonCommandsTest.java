package seedu.tutorledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Checks the lesson commands against the examples and rules in the v1 user guide. */
class LessonCommandsTest {
    private static final String NEW_LINE = System.lineSeparator();

    private TutorLedgerData data;
    private LessonCommands commands;

    @BeforeEach
    void setUp() {
        data = new TutorLedgerData();
        // S1 takes E Math and A Math; S2 takes Pure Chemistry.
        data.replaceStudent(data.addStudent("Amirah Binte Rahman", "Sec 3", "91234567")
                .withDetails("Amirah Binte Rahman", "Sec 3", "91234567", List.of("E Math", "A Math")));
        data.replaceStudent(data.addStudent("Tan Wei Ming", "Sec 4", "98765432")
                .withDetails("Tan Wei Ming", "Sec 4", "98765432", List.of("Pure Chemistry")));
        // "Today" is Monday 21-09-2026, the same day the user guide examples assume.
        commands = new LessonCommands(data,
                Clock.fixed(Instant.parse("2026-09-21T10:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void handles_lessonCommandWords_trueInAnyCase() {
        assertTrue(LessonCommands.handles("schedule S1 d/22-09-2026 t/1600 s/A Math f/60"));
        assertTrue(LessonCommands.handles("  SCHEDULE"));
        assertTrue(LessonCommands.handles("lessons"));
        assertTrue(LessonCommands.handles("Lessons week"));
        assertTrue(LessonCommands.handles("record L1 a/present"));
    }

    @Test
    void handles_otherInput_false() {
        assertFalse(LessonCommands.handles("add n/Tan Wei Ming l/Sec 3 p/91234567"));
        assertFalse(LessonCommands.handles("mark L1 paid"));
        assertFalse(LessonCommands.handles(""));
        assertFalse(LessonCommands.handles(null));
        assertEquals("Unknown command.", commands.execute("view S1"));
    }

    @Test
    void schedule_validLesson_storesItAndShowsTheSlot() {
        String output = commands.execute("schedule S1 d/22-09-2026 t/1600 s/A Math f/60");

        assertEquals("Scheduled L1 for S1 Amirah Binte Rahman" + NEW_LINE
                + "  Tue 22-09-2026 1600, A Math, $60.00", output);
        Lesson lesson = data.getLesson("L1");
        assertEquals("S1", lesson.getStudentId());
        assertEquals(LocalDate.of(2026, 9, 22), lesson.getDate());
        assertEquals(LocalTime.of(16, 0), lesson.getTime());
        assertEquals(new BigDecimal("60"), lesson.getFee());
        assertFalse(lesson.isRecorded());
        assertEquals(Lesson.PAYMENT_UNPAID, lesson.getPaymentStatus());
    }

    @Test
    void schedule_prefixesInAnyOrderAndCase_usesTheStudentsSpellingOfTheSubject() {
        String output = commands.execute("SCHEDULE s2 S/pure chemistry F/62.50 D/26-09-2026 T/0900");

        assertEquals("Scheduled L1 for S2 Tan Wei Ming" + NEW_LINE
                + "  Sat 26-09-2026 0900, Pure Chemistry, $62.50", output);
        assertEquals("Pure Chemistry", data.getLesson("L1").getSubject());
    }

    @Test
    void schedule_subjectTheStudentDoesNotTake_isRejected() {
        assertEquals(
                "Error: Pure Chemistry is not one of S1 Amirah Binte Rahman's subjects (E Math, A Math).",
                commands.execute("schedule S1 d/22-09-2026 t/1600 s/Pure Chemistry f/60"));

        data.addStudent("Daniel Lim", "Sec 2", "81234567");
        assertEquals("Error: S3 Daniel Lim has no subjects yet. Add one with: subject S3 s/SUBJECT",
                commands.execute("schedule S3 d/22-09-2026 t/1600 s/E Math f/60"));
        assertTrue(data.getLessons().isEmpty());
    }

    @Test
    void schedule_slotAlreadyTaken_isRejectedEvenForAnotherStudent() {
        commands.execute("schedule S1 d/22-09-2026 t/1600 s/A Math f/60");

        assertEquals("Error: L1 already starts at that date and time.",
                commands.execute("schedule S2 d/22-09-2026 t/1600 s/Pure Chemistry f/62.50"));
        assertEquals(1, data.getLessons().size());
        // A different time on the same day is fine.
        assertTrue(commands.execute("schedule S2 d/22-09-2026 t/1730 s/Pure Chemistry f/62.50")
                .startsWith("Scheduled L2"));
    }

    @Test
    void schedule_missingOrInvalidValues_storesNothing() {
        assertEquals("Error: Give a non-blank f/ value.",
                commands.execute("schedule S1 d/22-09-2026 t/1600 s/A Math"));
        assertEquals("Error: Give a non-blank d/ value.",
                commands.execute("schedule S1 d/ t/1600 s/A Math f/60"));
        assertEquals("Error: Give t/ only once.",
                commands.execute("schedule S1 d/22-09-2026 t/1600 t/1700 s/A Math f/60"));
        assertEquals("Error: Date must be a real date in DD-MM-YYYY format.",
                commands.execute("schedule S1 d/31-02-2026 t/1600 s/A Math f/60"));
        assertEquals("Error: Time must be a valid 24-hour time in HHMM format.",
                commands.execute("schedule S1 d/22-09-2026 t/2460 s/A Math f/60"));
        assertEquals("Error: Fee must be a non-negative dollar amount with at most 2 decimals.",
                commands.execute("schedule S1 d/22-09-2026 t/1600 s/A Math f/-60"));
        assertEquals("Error: Unexpected prefix a/.",
                commands.execute("schedule S1 d/22-09-2026 t/1600 s/A Math f/60 a/present"));
        assertTrue(data.getLessons().isEmpty());
    }

    @Test
    void schedule_unknownOrMissingStudent_isRejected() {
        assertEquals("Error: No student found with ID S9.",
                commands.execute("schedule S9 d/22-09-2026 t/1600 s/A Math f/60"));
        assertEquals("Error: Student ID must look like S1.", commands.execute("schedule"));
        assertEquals("Error: Student ID must look like S1.",
                commands.execute("schedule d/22-09-2026 t/1600 s/A Math f/60"));
    }

    /** Schedules the four lessons of the user guide's "lessons week" example, plus two outside it. */
    private void scheduleExampleWeek() {
        data.replaceStudent(data.addStudent("Tan Wei Ling", "Sec 2", "98765432")
                .withDetails("Tan Wei Ling", "Sec 2", "98765432", List.of("Science")));
        data.replaceStudent(data.addStudent("Daniel Lim", "Sec 2", "81234567")
                .withDetails("Daniel Lim", "Sec 2", "81234567", List.of("E Math")));
        // Entered out of order on purpose, to show that the listing sorts by date and time.
        commands.execute("schedule S3 d/26-09-2026 t/1030 s/Science f/50");
        commands.execute("schedule S1 d/22-09-2026 t/1600 s/A Math f/60");
        commands.execute("schedule S2 d/26-09-2026 t/0900 s/Pure Chemistry f/62.50");
        commands.execute("schedule S4 d/24-09-2026 t/1900 s/E Math f/55");
        commands.execute("schedule S1 d/20-09-2026 t/1600 s/E Math f/60");
        commands.execute("schedule S1 d/28-09-2026 t/1600 s/E Math f/60");
    }

    @Test
    void lessons_week_listsMondayToSundayInTheOrderTheyHappen() {
        scheduleExampleWeek();

        assertEquals("This week, Mon 21-09-2026 to Sun 27-09-2026: 4 lessons" + NEW_LINE
                + "  L2  Tue 22-09-2026 1600  S1 Amirah Binte Rahman  A Math" + NEW_LINE
                + "  L4  Thu 24-09-2026 1900  S4 Daniel Lim           E Math" + NEW_LINE
                + "  L3  Sat 26-09-2026 0900  S2 Tan Wei Ming         Pure Chemistry" + NEW_LINE
                + "  L1  Sat 26-09-2026 1030  S3 Tan Wei Ling         Science",
                commands.execute("lessons WEEK"));
    }

    @Test
    void lessons_week_countsFromMondayEvenWhenTodayIsSunday() {
        scheduleExampleWeek();
        LessonCommands sundayCommands = new LessonCommands(data,
                Clock.fixed(Instant.parse("2026-09-27T10:00:00Z"), ZoneOffset.UTC));

        String output = sundayCommands.execute("lessons week");

        assertTrue(output.startsWith("This week, Mon 21-09-2026 to Sun 27-09-2026: 4 lessons"));
        assertFalse(output.contains("28-09-2026"));
    }

    @Test
    void lessons_noArgument_listsOnlyTodaysLessons() {
        scheduleExampleWeek();
        commands.execute("schedule S2 d/21-09-2026 t/1800 s/Pure Chemistry f/62.50");
        commands.execute("schedule S1 d/21-09-2026 t/0930 s/E Math f/60");

        assertEquals("Today, Mon 21-09-2026: 2 lessons" + NEW_LINE
                + "  L8  Mon 21-09-2026 0930  S1 Amirah Binte Rahman  E Math" + NEW_LINE
                + "  L7  Mon 21-09-2026 1800  S2 Tan Wei Ming         Pure Chemistry",
                commands.execute("lessons"));
    }

    @Test
    void lessons_nothingScheduled_reportsZeroOrOneWithTheRightPlural() {
        assertEquals("Today, Mon 21-09-2026: 0 lessons", commands.execute("lessons"));
        assertEquals("This week, Mon 21-09-2026 to Sun 27-09-2026: 0 lessons",
                commands.execute("lessons week"));

        commands.execute("schedule S1 d/21-09-2026 t/1600 s/A Math f/60");
        assertTrue(commands.execute("lessons").startsWith("Today, Mon 21-09-2026: 1 lesson" + NEW_LINE));
    }

    @Test
    void lessons_unknownArgument_isRejected() {
        assertEquals("Error: Use: lessons [week]", commands.execute("lessons month"));
        assertEquals("Error: Use: lessons [week]", commands.execute("lessons week extra"));
    }

    @Test
    void record_attendanceAndNotes_savesBothAgainstTheLesson() {
        commands.execute("schedule S1 d/21-09-2026 t/1600 s/A Math f/60");

        String output = commands.execute(
                "record L1 a/present note/Discriminant recap. Started simultaneous equations.");

        assertEquals("Recorded L1 for S1 Amirah Binte Rahman (Mon 21-09-2026 1600)" + NEW_LINE
                + "  Attendance: present" + NEW_LINE
                + "  Notes: Discriminant recap. Started simultaneous equations.", output);
        Lesson lesson = data.getLesson("L1");
        assertEquals(Lesson.Attendance.PRESENT, lesson.getAttendance());
        assertEquals("Discriminant recap. Started simultaneous equations.", lesson.getNotes());
        // Recording must not disturb the rest of the lesson.
        assertEquals("A Math", lesson.getSubject());
        assertEquals(new BigDecimal("60"), lesson.getFee());
        assertEquals(Lesson.PAYMENT_UNPAID, lesson.getPaymentStatus());
    }

    @Test
    void record_withoutNotes_showsOnlyAttendance() {
        commands.execute("schedule S1 d/20-09-2026 t/1600 s/A Math f/60");

        assertEquals("Recorded L1 for S1 Amirah Binte Rahman (Sun 20-09-2026 1600)" + NEW_LINE
                + "  Attendance: absent", commands.execute("RECORD l1 A/Absent"));
        assertEquals(Lesson.Attendance.ABSENT, data.getLesson("L1").getAttendance());
    }

    @Test
    void record_again_replacesAttendanceAndKeepsNotesUnlessNoteIsGiven() {
        commands.execute("schedule S1 d/21-09-2026 t/1600 s/A Math f/60");
        commands.execute("record L1 a/present note/Started vectors");

        commands.execute("record L1 a/late");
        assertEquals(Lesson.Attendance.LATE, data.getLesson("L1").getAttendance());
        assertEquals("Started vectors", data.getLesson("L1").getNotes());

        commands.execute("record L1 a/late note/Finished vectors");
        assertEquals("Finished vectors", data.getLesson("L1").getNotes());

        // A bare note/ clears the notes.
        commands.execute("record L1 a/late note/");
        assertEquals("", data.getLesson("L1").getNotes());
    }

    @Test
    void record_keepsThePaymentStatus() {
        commands.execute("schedule S1 d/21-09-2026 t/1600 s/A Math f/60");
        data.updateLessonPaymentStatus("L1", Lesson.PAYMENT_PAID);

        commands.execute("record L1 a/present");

        assertEquals(Lesson.PAYMENT_PAID, data.getLesson("L1").getPaymentStatus());
    }

    @Test
    void record_lessonAfterToday_isRejected() {
        commands.execute("schedule S1 d/22-09-2026 t/1600 s/A Math f/60");

        assertEquals("Error: Only a lesson dated today or earlier can be recorded.",
                commands.execute("record L1 a/present"));
        assertFalse(data.getLesson("L1").isRecorded());
    }

    @Test
    void record_missingOrInvalidValues_changesNothing() {
        commands.execute("schedule S1 d/21-09-2026 t/1600 s/A Math f/60");

        assertEquals("Error: Give a non-blank a/ value.", commands.execute("record L1"));
        assertEquals("Error: Give a non-blank a/ value.",
                commands.execute("record L1 note/Started vectors"));
        assertEquals("Error: Attendance must be present, absent, or late.",
                commands.execute("record L1 a/excused"));
        assertEquals("Error: Attendance must be present, absent, or late.",
                commands.execute("record L1 a/not recorded"));
        assertEquals("Error: Unexpected prefix f/.", commands.execute("record L1 a/present f/70"));
        assertEquals("Error: No lesson found with ID L9.", commands.execute("record L9 a/present"));
        assertEquals("Error: Lesson ID must look like L1.", commands.execute("record S1 a/present"));
        assertEquals("Error: Lesson ID must look like L1.", commands.execute("record"));
        assertFalse(data.getLesson("L1").isRecorded());
        assertEquals("", data.getLesson("L1").getNotes());
    }
}

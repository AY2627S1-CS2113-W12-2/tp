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
    void handles_sharedCommandWord_trueOnlyWithALessonId() {
        assertTrue(LessonCommands.handles("edit L42 d/25-09-2026"));
        assertTrue(LessonCommands.handles("EDIT l42 f/55"));
        // Still a lesson command when the ID is malformed, so the error can describe a lesson ID.
        assertTrue(LessonCommands.handles("edit Lx f/55"));
        assertTrue(LessonCommands.handles("delete L44"));
        assertTrue(LessonCommands.handles("Delete l44"));

        assertFalse(LessonCommands.handles("edit S1 l/Sec 4"));
        assertFalse(LessonCommands.handles("edit"));
        assertFalse(LessonCommands.handles("delete S5"));
        assertFalse(LessonCommands.handles("delete"));
        // l/ is the student level prefix, not a lesson ID.
        assertFalse(LessonCommands.handles("edit l/Sec 4"));
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
        assertEquals("Error: Set a default hourly rate with edit S1 r/RATE, or give f/FEE.",
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

    @Test
    void edit_dateAndTime_movesTheLessonAndKeepsEverythingElse() {
        commands.execute("schedule S1 d/24-09-2026 t/1900 s/E Math f/55");

        assertEquals("Edited L1 for S1 Amirah Binte Rahman" + NEW_LINE
                + "  Fri 25-09-2026 1800, E Math, $55.00, not recorded, unpaid",
                commands.execute("edit L1 d/25-09-2026 t/1800"));
        Lesson lesson = data.getLesson("L1");
        assertEquals(LocalDate.of(2026, 9, 25), lesson.getDate());
        assertEquals(LocalTime.of(18, 0), lesson.getTime());
        assertEquals("E Math", lesson.getSubject());
        assertEquals(new BigDecimal("55"), lesson.getFee());
    }

    @Test
    void edit_subjectAndFee_usesTheStudentsSpellingAndKeepsThePaymentStatus() {
        commands.execute("schedule S1 d/24-09-2026 t/1900 s/E Math f/55");
        data.updateLessonPaymentStatus("L1", Lesson.PAYMENT_PAID);

        assertEquals("Edited L1 for S1 Amirah Binte Rahman" + NEW_LINE
                + "  Thu 24-09-2026 1900, A Math, $62.50, not recorded, paid",
                commands.execute("EDIT l1 S/a math F/62.50"));
        assertEquals("A Math", data.getLesson("L1").getSubject());
        assertEquals(Lesson.PAYMENT_PAID, data.getLesson("L1").getPaymentStatus());
    }

    @Test
    void edit_attendanceAndNotes_changesARecordedLesson() {
        commands.execute("schedule S1 d/21-09-2026 t/1600 s/A Math f/60");
        commands.execute("record L1 a/present note/Started vectors");

        assertEquals("Edited L1 for S1 Amirah Binte Rahman" + NEW_LINE
                + "  Mon 21-09-2026 1600, A Math, $60.00, late, unpaid" + NEW_LINE
                + "  Notes: Finished vectors", commands.execute("edit L1 a/late note/Finished vectors"));

        // A bare note/ clears the notes and leaves the attendance alone.
        commands.execute("edit L1 note/");
        assertEquals("", data.getLesson("L1").getNotes());
        assertEquals(Lesson.Attendance.LATE, data.getLesson("L1").getAttendance());
    }

    @Test
    void edit_ontoAnotherLessonsSlot_isRejected() {
        commands.execute("schedule S1 d/22-09-2026 t/1600 s/A Math f/60");
        commands.execute("schedule S2 d/26-09-2026 t/0900 s/Pure Chemistry f/62.50");

        assertEquals("Error: L1 already starts at that date and time.",
                commands.execute("edit L2 d/22-09-2026 t/1600"));
        assertEquals(LocalDate.of(2026, 9, 26), data.getLesson("L2").getDate());
        // Re-typing a lesson's own slot is not a clash with itself.
        assertTrue(commands.execute("edit L1 d/22-09-2026 t/1600 f/65").startsWith("Edited L1"));
    }

    @Test
    void edit_outcomeOnALessonAfterToday_isRejected() {
        String expected = "Error: A lesson dated after today cannot have attendance or notes.";
        commands.execute("schedule S1 d/22-09-2026 t/1600 s/A Math f/60");
        assertEquals(expected, commands.execute("edit L1 a/present"));
        assertEquals(expected, commands.execute("edit L1 note/Bring worksheet"));
        assertFalse(data.getLesson("L1").isRecorded());

        // A lesson that was already recorded cannot be moved into the future either.
        commands.execute("schedule S1 d/21-09-2026 t/1600 s/A Math f/60");
        commands.execute("record L2 a/present");
        assertEquals(expected, commands.execute("edit L2 d/28-09-2026"));
        assertEquals(LocalDate.of(2026, 9, 21), data.getLesson("L2").getDate());

        // Moving a future lesson back to today and recording it in one command is allowed.
        assertTrue(commands.execute("edit L1 d/21-09-2026 t/0900 a/absent").contains("absent"));
    }

    @Test
    void edit_missingOrInvalidValues_changesNothing() {
        commands.execute("schedule S1 d/22-09-2026 t/1600 s/A Math f/60");

        assertEquals("Error: Give at least one value to edit.", commands.execute("edit L1"));
        assertEquals("Error: Give a non-blank d/ value.", commands.execute("edit L1 d/"));
        assertEquals("Error: Date must be a real date in DD-MM-YYYY format.",
                commands.execute("edit L1 d/2026-09-25"));
        assertEquals("Error: Time must be in 24-hour HHMM format.", commands.execute("edit L1 t/6pm"));
        assertEquals("Error: Fee must be a non-negative dollar amount with at most 2 decimals.",
                commands.execute("edit L1 f/55.555"));
        assertEquals(
                "Error: Pure Chemistry is not one of S1 Amirah Binte Rahman's subjects (E Math, A Math).",
                commands.execute("edit L1 s/Pure Chemistry"));
        assertEquals("Error: Attendance must be present, absent, or late.",
                commands.execute("edit L1 a/excused"));
        // Payment status is changed with mark, and student prefixes do not apply to a lesson.
        assertEquals("Error: Unexpected prefix n/.", commands.execute("edit L1 n/Tan Wei Ming"));
        // One bad value rejects the whole edit, so the valid fee below must not be applied.
        assertEquals("Error: Time must be in 24-hour HHMM format.", commands.execute("edit L1 f/70 t/6pm"));

        Lesson lesson = data.getLesson("L1");
        assertEquals(LocalDate.of(2026, 9, 22), lesson.getDate());
        assertEquals(LocalTime.of(16, 0), lesson.getTime());
        assertEquals(new BigDecimal("60"), lesson.getFee());
        assertEquals("A Math", lesson.getSubject());
    }

    @Test
    void edit_unknownOrMalformedLessonId_isRejected() {
        assertEquals("Error: No lesson found with ID L9.", commands.execute("edit L9 f/55"));
        assertEquals("Error: Lesson ID must look like L1.", commands.execute("edit Lx f/55"));
    }

    @Test
    void delete_existingLesson_removesOnlyThatLesson() {
        commands.execute("schedule S1 d/22-09-2026 t/1600 s/A Math f/60");
        commands.execute("schedule S2 d/26-09-2026 t/0900 s/Pure Chemistry f/62.50");
        data.addHomework("S2", "Worksheet 4A", LocalDate.of(2026, 9, 29));

        assertEquals("Deleted L2 for S2 Tan Wei Ming: Sat 26-09-2026 0900, Pure Chemistry",
                commands.execute("DELETE l2"));

        assertEquals(1, data.getLessons().size());
        assertEquals("L1", data.getLesson("L1").getLessonId());
        // The student and their homework are not affected.
        assertEquals("Tan Wei Ming", data.getStudent("S2").getName());
        assertEquals(1, data.getHomework().size());
    }

    @Test
    void delete_thenSchedule_doesNotReuseTheDeletedId() {
        commands.execute("schedule S1 d/22-09-2026 t/1600 s/A Math f/60");
        commands.execute("delete L1");

        // The freed slot can be used again, but the lesson gets a new ID.
        assertTrue(commands.execute("schedule S1 d/22-09-2026 t/1600 s/A Math f/60")
                .startsWith("Scheduled L2"));
        assertEquals("Error: No lesson found with ID L1.", commands.execute("delete L1"));
    }

    @Test
    void delete_unknownMalformedOrExtraArguments_deletesNothing() {
        commands.execute("schedule S1 d/22-09-2026 t/1600 s/A Math f/60");

        assertEquals("Error: No lesson found with ID L9.", commands.execute("delete L9"));
        assertEquals("Error: Lesson ID must look like L1.", commands.execute("delete Lx"));
        assertEquals("Error: Use: delete LESSON_ID", commands.execute("delete L1 L2"));
        assertEquals(1, data.getLessons().size());
    }

    @Test
    void handles_v2CommandWords_trueInAnyCase() {
        assertTrue(LessonCommands.handles("log S1 d/15-09-2026 t/1600 s/A Math f/60 a/present"));
        assertTrue(LessonCommands.handles("next"));
        assertTrue(LessonCommands.handles("NEXT S1"));
        assertTrue(LessonCommands.handles("reschedule L1 d/25-09-2026"));
        assertTrue(LessonCommands.handles("Cancel L1 r/Student sick"));
    }

    @Test
    void schedule_weekly_addsOneLessonPerWeekWithConsecutiveIds() {
        assertEquals("Scheduled 4 weekly lessons for S1 Amirah Binte Rahman: L1 to L4" + NEW_LINE
                + "  Every Tue 1600, A Math, $60.00 each" + NEW_LINE
                + "  From Tue 22-09-2026 to Tue 13-10-2026",
                commands.execute("schedule S1 d/22-09-2026 t/1600 s/a math f/60 W/4"));

        assertEquals(4, data.getLessons().size());
        assertEquals(LocalDate.of(2026, 9, 29), data.getLesson("L2").getDate());
        assertEquals(LocalDate.of(2026, 10, 6), data.getLesson("L3").getDate());
        assertEquals(LocalTime.of(16, 0), data.getLesson("L4").getTime());
        assertEquals("A Math", data.getLesson("L4").getSubject());
        assertFalse(data.getLesson("L4").isRecorded());
    }

    @Test
    void schedule_weeklyWithAClashInALaterWeek_addsNothing() {
        commands.execute("schedule S2 d/06-10-2026 t/1600 s/Pure Chemistry f/62.50");

        assertEquals("Error: L1 already starts at that date and time.",
                commands.execute("schedule S1 d/22-09-2026 t/1600 s/A Math f/60 w/4"));
        assertEquals(1, data.getLessons().size());
        // The rejected slot used up no IDs.
        assertTrue(commands.execute("schedule S1 d/22-09-2026 t/1600 s/A Math f/60").startsWith("Scheduled L2"));
    }

    @Test
    void schedule_invalidWeekCount_addsNothing() {
        String expected = "Error: Weeks must be a whole number from 2 to 52.";
        assertEquals(expected, commands.execute("schedule S1 d/22-09-2026 t/1600 s/A Math f/60 w/1"));
        assertEquals(expected, commands.execute("schedule S1 d/22-09-2026 t/1600 s/A Math f/60 w/53"));
        assertEquals(expected, commands.execute("schedule S1 d/22-09-2026 t/1600 s/A Math f/60 w/-3"));
        assertEquals(expected, commands.execute("schedule S1 d/22-09-2026 t/1600 s/A Math f/60 w/ten"));
        assertEquals(expected, commands.execute("schedule S1 d/22-09-2026 t/1600 s/A Math f/60 w/100"));
        assertEquals("Error: Give a non-blank w/ value.",
                commands.execute("schedule S1 d/22-09-2026 t/1600 s/A Math f/60 w/"));
        assertTrue(data.getLessons().isEmpty());
    }

    @Test
    void log_pastLesson_addsItWithItsAttendanceAndNotes() {
        assertEquals("Logged L1 for S1 Amirah Binte Rahman" + NEW_LINE
                + "  Tue 15-09-2026 1600, A Math, $60.00, present" + NEW_LINE
                + "  Notes: Discriminant recap",
                commands.execute("LOG s1 d/15-09-2026 t/1600 s/a math f/60 a/Present note/Discriminant recap"));

        Lesson lesson = data.getLesson("L1");
        assertEquals(LocalDate.of(2026, 9, 15), lesson.getDate());
        assertEquals("A Math", lesson.getSubject());
        assertEquals(Lesson.Attendance.PRESENT, lesson.getAttendance());
        assertEquals("Discriminant recap", lesson.getNotes());
        assertEquals(Lesson.PAYMENT_UNPAID, lesson.getPaymentStatus());
    }

    @Test
    void log_todayWithoutNotes_showsOnlyTheAttendance() {
        assertEquals("Logged L1 for S2 Tan Wei Ming" + NEW_LINE
                + "  Mon 21-09-2026 0900, Pure Chemistry, $62.50, absent",
                commands.execute("log S2 d/21-09-2026 t/0900 s/Pure Chemistry f/62.50 a/absent"));
        assertEquals("", data.getLesson("L1").getNotes());
    }

    @Test
    void log_futureMissingOrInvalidValues_addsNothing() {
        assertEquals("Error: Only a lesson dated today or earlier can be logged. "
                + "Use schedule for a lesson that has not happened yet.",
                commands.execute("log S1 d/22-09-2026 t/1600 s/A Math f/60 a/present"));
        assertEquals("Error: Give a non-blank a/ value.",
                commands.execute("log S1 d/15-09-2026 t/1600 s/A Math f/60"));
        assertEquals("Error: Attendance must be present, absent, or late.",
                commands.execute("log S1 d/15-09-2026 t/1600 s/A Math f/60 a/excused"));
        assertEquals("Error: Date must be a real date in DD-MM-YYYY format.",
                commands.execute("log S1 d/31-02-2026 t/1600 s/A Math f/60 a/present"));
        assertEquals("Error: Unexpected prefix w/.",
                commands.execute("log S1 d/15-09-2026 t/1600 s/A Math f/60 a/present w/4"));
        assertTrue(data.getLessons().isEmpty());

        commands.execute("log S1 d/15-09-2026 t/1600 s/A Math f/60 a/present");
        assertEquals("Error: L1 already starts at that date and time.",
                commands.execute("log S2 d/15-09-2026 t/1600 s/Pure Chemistry f/62.50 a/late"));
        assertEquals(1, data.getLessons().size());
    }

    @Test
    void reschedule_upcomingLesson_movesItAndKeepsEverythingElse() {
        commands.execute("schedule S1 d/22-09-2026 t/1600 s/A Math f/60");
        data.updateLessonPaymentStatus("L1", Lesson.PAYMENT_PAID);

        assertEquals("Rescheduled L1 for S1 Amirah Binte Rahman" + NEW_LINE
                + "  From: Tue 22-09-2026 1600" + NEW_LINE
                + "  To:   Fri 25-09-2026 1800",
                commands.execute("RESCHEDULE l1 D/25-09-2026 T/1800"));

        Lesson lesson = data.getLesson("L1");
        assertEquals(LocalDate.of(2026, 9, 25), lesson.getDate());
        assertEquals(LocalTime.of(18, 0), lesson.getTime());
        assertEquals("A Math", lesson.getSubject());
        assertEquals(new BigDecimal("60"), lesson.getFee());
        assertEquals(Lesson.PAYMENT_PAID, lesson.getPaymentStatus());
    }

    @Test
    void reschedule_onlyDateOrOnlyTime_keepsTheOtherPart() {
        commands.execute("schedule S1 d/22-09-2026 t/1600 s/A Math f/60");

        commands.execute("reschedule L1 t/1730");
        assertEquals(LocalDate.of(2026, 9, 22), data.getLesson("L1").getDate());
        assertEquals(LocalTime.of(17, 30), data.getLesson("L1").getTime());

        commands.execute("reschedule L1 d/23-09-2026");
        assertEquals(LocalDate.of(2026, 9, 23), data.getLesson("L1").getDate());
        assertEquals(LocalTime.of(17, 30), data.getLesson("L1").getTime());
    }

    @Test
    void reschedule_lessonThatTookPlaceOrWasCancelled_isRejected() {
        commands.execute("schedule S1 d/21-09-2026 t/0900 s/A Math f/60");
        commands.execute("record L1 a/present");
        assertEquals("Error: L1 already has attendance or notes, so it has taken place. "
                + "Use edit to correct its slot.", commands.execute("reschedule L1 d/25-09-2026"));

        commands.execute("schedule S1 d/22-09-2026 t/1600 s/A Math f/60");
        commands.execute("cancel L2 r/Exam week");
        assertEquals("Error: L2 was cancelled. Schedule a make-up lesson instead.",
                commands.execute("reschedule L2 d/25-09-2026"));
        assertEquals(LocalDate.of(2026, 9, 22), data.getLesson("L2").getDate());
    }

    @Test
    void reschedule_toAPassedSameOrTakenSlot_changesNothing() {
        commands.execute("schedule S1 d/22-09-2026 t/1600 s/A Math f/60");
        commands.execute("schedule S2 d/26-09-2026 t/0900 s/Pure Chemistry f/62.50");

        // "Now" is 1000 on Mon 21-09-2026, so 0930 that morning has already passed.
        assertEquals("Error: A lesson can only be rescheduled to a time that has not passed.",
                commands.execute("reschedule L1 d/21-09-2026 t/0930"));
        assertEquals("Error: L1 is already at Tue 22-09-2026 1600.", commands.execute("reschedule L1 t/1600"));
        assertEquals("Error: L2 already starts at that date and time.",
                commands.execute("reschedule L1 d/26-09-2026 t/0900"));
        assertEquals("Error: Give a new date d/, a new time t/, or both.", commands.execute("reschedule L1"));
        assertEquals("Error: Unexpected prefix f/.", commands.execute("reschedule L1 f/70"));
        assertEquals("Error: No lesson found with ID L9.", commands.execute("reschedule L9 d/25-09-2026"));

        assertEquals(LocalDate.of(2026, 9, 22), data.getLesson("L1").getDate());
        assertEquals(LocalTime.of(16, 0), data.getLesson("L1").getTime());
    }

    @Test
    void reschedule_toLaterToday_isAllowed() {
        commands.execute("schedule S1 d/22-09-2026 t/1600 s/A Math f/60");

        assertTrue(commands.execute("reschedule L1 d/21-09-2026 t/1000").startsWith("Rescheduled L1"));
    }

    @Test
    void cancel_upcomingLesson_keepsItMarkedCancelledWithTheReason() {
        commands.execute("schedule S1 d/22-09-2026 t/1600 s/A Math f/60");

        assertEquals("Cancelled L1 for S1 Amirah Binte Rahman (Tue 22-09-2026 1600)" + NEW_LINE
                + "  Reason: Student sick", commands.execute("CANCEL l1 R/Student sick"));

        Lesson lesson = data.getLesson("L1");
        assertTrue(lesson.isCancelled());
        assertEquals("Student sick", lesson.getCancellationReason());
        assertFalse(lesson.isRecorded());
        assertEquals(LocalDate.of(2026, 9, 22), lesson.getDate());
    }

    @Test
    void cancel_paidLesson_remindsAboutTheFee() {
        commands.execute("schedule S1 d/22-09-2026 t/1600 s/A Math f/60");
        data.updateLessonPaymentStatus("L1", Lesson.PAYMENT_PAID);

        assertEquals("Cancelled L1 for S1 Amirah Binte Rahman (Tue 22-09-2026 1600)" + NEW_LINE
                + "  Reason: Tutor unwell" + NEW_LINE
                + "  This lesson was already paid for, so refund or carry over the fee.",
                commands.execute("cancel L1 r/Tutor unwell"));
    }

    @Test
    void cancel_freesTheSlotAndIsMarkedInTheLessonList() {
        commands.execute("schedule S1 d/21-09-2026 t/1600 s/A Math f/60");
        commands.execute("cancel L1 r/Public holiday");

        assertTrue(commands.execute("schedule S2 d/21-09-2026 t/1600 s/Pure Chemistry f/62.50")
                .startsWith("Scheduled L2"));
        assertEquals("Today, Mon 21-09-2026: 2 lessons" + NEW_LINE
                + "  L1  Mon 21-09-2026 1600  S1 Amirah Binte Rahman  A Math  (cancelled: Public holiday)" + NEW_LINE
                + "  L2  Mon 21-09-2026 1600  S2 Tan Wei Ming         Pure Chemistry",
                commands.execute("lessons"));
    }

    @Test
    void cancel_recordedCancelledOrMissingReason_isRejected() {
        commands.execute("schedule S1 d/21-09-2026 t/0900 s/A Math f/60");
        commands.execute("record L1 a/present");
        assertEquals("Error: L1 already has attendance, so it took place and cannot be cancelled.",
                commands.execute("cancel L1 r/Student sick"));
        assertFalse(data.getLesson("L1").isCancelled());

        commands.execute("schedule S1 d/22-09-2026 t/1600 s/A Math f/60");
        assertEquals("Error: Give a non-blank r/ value.", commands.execute("cancel L2"));
        assertEquals("Error: Give a non-blank r/ value.", commands.execute("cancel L2 r/  "));
        commands.execute("cancel L2 r/Student sick");
        assertEquals("Error: L2 is already cancelled (Student sick).", commands.execute("cancel L2 r/Exam week"));
        assertEquals("Student sick", data.getLesson("L2").getCancellationReason());
        assertEquals("Error: No lesson found with ID L9.", commands.execute("cancel L9 r/Student sick"));
    }

    @Test
    void recordAndEdit_cancelledLesson_refuseAttendanceButAllowOtherCorrections() {
        commands.execute("schedule S1 d/21-09-2026 t/0900 s/A Math f/60");
        commands.execute("cancel L1 r/Student sick");

        assertEquals("Error: L1 was cancelled, so it has no attendance.", commands.execute("record L1 a/present"));
        assertEquals("Error: L1 was cancelled, so it has no attendance.", commands.execute("edit L1 a/present"));
        assertFalse(data.getLesson("L1").isRecorded());

        assertTrue(commands.execute("edit L1 f/50").startsWith("Edited L1"));
        assertTrue(data.getLesson("L1").isCancelled());
        assertEquals(new BigDecimal("50"), data.getLesson("L1").getFee());
    }

    @Test
    void next_upcomingLessons_summarisesTheEarliestWithWhatToPrepare() {
        commands.execute("log S1 d/15-09-2026 t/1600 s/A Math f/60 a/present note/Discriminant recap");
        commands.execute("log S1 d/18-09-2026 t/1600 s/E Math f/60 a/absent");
        data.updateLessonPaymentStatus("L2", Lesson.PAYMENT_PAID);
        // L3 started at 0900 today, before "now", so it is no longer the next lesson.
        commands.execute("schedule S1 d/21-09-2026 t/0900 s/A Math f/60");
        commands.execute("schedule S2 d/24-09-2026 t/1600 s/Pure Chemistry f/62.50");
        commands.execute("schedule S1 d/22-09-2026 t/1600 s/A Math f/60");
        data.addHomework("S1", "Worksheet 4A", LocalDate.of(2026, 9, 25));
        data.addHomework("S1", "Past paper Q3", LocalDate.of(2026, 9, 18));
        data.addHomework("S2", "Titration practice", LocalDate.of(2026, 9, 23));
        data.addHomework("S1", "Vectors exercise", LocalDate.of(2026, 9, 20));
        data.putHomework(data.getHomeworkById("H4").asSubmitted());

        assertEquals("Next lesson: L5, Tue 22-09-2026 1600" + NEW_LINE
                + "  Student: S1 Amirah Binte Rahman (Sec 3)" + NEW_LINE
                + "  Subject: A Math" + NEW_LINE
                + "  Last topic: Discriminant recap (L1, Tue 15-09-2026)" + NEW_LINE
                + "  Outstanding homework: 2" + NEW_LINE
                + "    H2  Past paper Q3, due Fri 18-09-2026 (overdue)" + NEW_LINE
                + "    H1  Worksheet 4A, due Fri 25-09-2026" + NEW_LINE
                + "  Unpaid: 2 lessons, $120.00", commands.execute("next"));
    }

    @Test
    void next_forOneStudent_skipsOtherStudentsAndCancelledLessons() {
        commands.execute("schedule S1 d/22-09-2026 t/1600 s/A Math f/60");
        commands.execute("schedule S2 d/23-09-2026 t/1600 s/Pure Chemistry f/62.50");
        commands.execute("schedule S2 d/24-09-2026 t/1600 s/Pure Chemistry f/62.50");
        commands.execute("cancel L2 r/School camp");

        assertEquals("Next lesson: L3, Thu 24-09-2026 1600" + NEW_LINE
                + "  Student: S2 Tan Wei Ming (Sec 4)" + NEW_LINE
                + "  Subject: Pure Chemistry" + NEW_LINE
                + "  Last topic: none recorded yet" + NEW_LINE
                + "  Outstanding homework: none" + NEW_LINE
                + "  Unpaid: none", commands.execute("next s2"));
    }

    @Test
    void next_lessonStartingNow_countsAsUpcoming() {
        commands.execute("schedule S1 d/22-09-2026 t/1600 s/A Math f/60");
        commands.execute("schedule S2 d/21-09-2026 t/1000 s/Pure Chemistry f/62.50");

        assertTrue(commands.execute("next").startsWith("Next lesson: L2, Mon 21-09-2026 1000"));
    }

    @Test
    void next_nothingUpcomingOrBadArguments_saysSo() {
        assertEquals("No upcoming lessons.", commands.execute("next"));

        commands.execute("log S1 d/15-09-2026 t/1600 s/A Math f/60 a/present");
        commands.execute("schedule S2 d/22-09-2026 t/1600 s/Pure Chemistry f/62.50");
        assertEquals("No upcoming lessons for S1 Amirah Binte Rahman.", commands.execute("next S1"));
        assertEquals("Error: No student found with ID S9.", commands.execute("next S9"));
        assertEquals("Error: Use: next [STUDENT_ID]", commands.execute("next S1 S2"));
    }
}

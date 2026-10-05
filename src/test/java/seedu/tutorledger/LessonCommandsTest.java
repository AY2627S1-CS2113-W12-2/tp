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
}

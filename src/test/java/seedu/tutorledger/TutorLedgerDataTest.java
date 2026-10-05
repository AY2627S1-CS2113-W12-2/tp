package seedu.tutorledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Checks how lessons are stored: ID assignment, the one-lesson-per-slot rule, and removal. */
class TutorLedgerDataTest {
    private TutorLedgerData data;

    @BeforeEach
    void setUp() {
        data = new TutorLedgerData();
        data.addStudent("Amirah Binte Rahman", "Sec 3", "91234567");
        data.addStudent("Tan Wei Ming", "Sec 4", "98765432");
    }

    @Test
    void addLesson_validDetails_storesAnUnrecordedUnpaidLessonWithTheNextId() {
        Lesson first = data.addLesson("s1", "A Math", "22-09-2026", "1600", "60");
        Lesson second = data.addLesson("S2", "Pure Chemistry", "26-09-2026", "0900", "62.50");

        assertEquals("L1", first.getLessonId());
        assertEquals("L2", second.getLessonId());
        assertEquals("S1", first.getStudentId());
        assertEquals(LocalDate.of(2026, 9, 22), first.getDate());
        assertEquals(LocalTime.of(16, 0), first.getTime());
        assertFalse(first.isRecorded());
        assertEquals("", first.getNotes());
        assertEquals(Lesson.PAYMENT_UNPAID, first.getPaymentStatus());
        assertEquals(first, data.getLesson("l1"));
    }

    @Test
    void addLesson_unknownStudentOrInvalidValue_storesNothing() {
        assertThrows(IllegalArgumentException.class,
                () -> data.addLesson("S9", "A Math", "22-09-2026", "1600", "60"));
        assertThrows(IllegalArgumentException.class,
                () -> data.addLesson("S1", "A Math", "31-02-2026", "1600", "60"));

        assertTrue(data.getLessons().isEmpty());
        assertEquals(1, data.getNextLessonNumber());
    }

    @Test
    void addLesson_slotAlreadyTaken_throwsEvenForAnotherStudent() {
        data.addLesson("S1", "A Math", "22-09-2026", "1600", "60");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> data.addLesson("S2", "Pure Chemistry", "22-09-2026", "1600", "62.50"));

        assertEquals("L1 already starts at that date and time.", exception.getMessage());
        assertEquals(1, data.getLessons().size());
    }

    @Test
    void addLesson_afterLessonWithHigherIdWasPut_continuesFromThatId() {
        data.putLesson(new Lesson("L41", "S1", "A Math", "22-09-2026", "1600", "not recorded", "60", ""));

        assertEquals("L42", data.addLesson("S1", "A Math", "29-09-2026", "1600", "60").getLessonId());
    }

    @Test
    void replaceLesson_movedToFreeSlot_storesTheChangedCopy() {
        Lesson lesson = data.addLesson("S1", "A Math", "22-09-2026", "1600", "60");

        data.replaceLesson(lesson.withDetails(null, "25-09-2026", "1800", null, null, null));

        assertEquals(LocalDate.of(2026, 9, 25), data.getLesson("L1").getDate());
        assertEquals(1, data.getLessons().size());
    }

    @Test
    void replaceLesson_movedOntoAnotherLesson_throwsAndKeepsTheOriginal() {
        data.addLesson("S1", "A Math", "22-09-2026", "1600", "60");
        Lesson second = data.addLesson("S2", "Pure Chemistry", "26-09-2026", "0900", "62.50");

        assertThrows(IllegalArgumentException.class,
                () -> data.replaceLesson(second.withDetails(null, "22-09-2026", "1600", null, null, null)));

        assertEquals(LocalDate.of(2026, 9, 26), data.getLesson("L2").getDate());
    }

    @Test
    void replaceLesson_sameSlot_isNotTreatedAsAClashWithItself() {
        Lesson lesson = data.addLesson("S1", "A Math", "22-09-2026", "1600", "60");

        data.replaceLesson(lesson.withDetails(null, null, null, null, "55", null));

        assertEquals("55", data.getLesson("L1").getFee().toPlainString());
    }

    @Test
    void replaceLesson_unknownLesson_throws() {
        Lesson stranger = new Lesson("L9", "S1", "A Math", "22-09-2026", "1600", "not recorded", "60", "");

        assertThrows(IllegalArgumentException.class, () -> data.replaceLesson(stranger));
    }

    @Test
    void deleteLesson_existingLesson_removesItAndNeverReusesItsId() {
        data.addLesson("S1", "A Math", "22-09-2026", "1600", "60");

        Lesson removed = data.deleteLesson("l1");

        assertEquals("L1", removed.getLessonId());
        assertTrue(data.getLessons().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> data.getLesson("L1"));
        assertEquals("L2", data.addLesson("S1", "A Math", "22-09-2026", "1600", "60").getLessonId());
    }

    @Test
    void deleteLesson_unknownOrMalformedId_throws() {
        assertThrows(IllegalArgumentException.class, () -> data.deleteLesson("L9"));
        assertThrows(IllegalArgumentException.class, () -> data.deleteLesson("S1"));
    }
}

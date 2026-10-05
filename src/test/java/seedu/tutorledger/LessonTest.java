package seedu.tutorledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

class LessonTest {
    @Test
    void acceptsValidLessonDetails() {
        Lesson lesson = new Lesson("L123", "Pure Chemistry", "22-09-2026", "1600",
                "late", "62.50", "Started vectors");

        assertEquals("L123", lesson.getLessonId());
        assertEquals("Pure Chemistry", lesson.getSubject());
        assertEquals(LocalDate.of(2026, 9, 22), lesson.getDate());
        assertEquals(LocalTime.of(16, 0), lesson.getTime());
        assertEquals(Lesson.Attendance.LATE, lesson.getAttendance());
        assertEquals(new BigDecimal("62.50"), lesson.getFee());
        assertEquals("Started vectors", lesson.getNotes());
        assertEquals(Lesson.PAYMENT_UNPAID, lesson.getPaymentStatus());
    }

    @Test
    void changesPaymentStatusOnACopy() {
        Lesson lesson = new Lesson("L123", "Pure Chemistry", "22-09-2026", "1600",
                "late", "62.50", "Started vectors");

        Lesson paid = lesson.withPaymentStatus("PAID");

        assertEquals(Lesson.PAYMENT_PAID, paid.getPaymentStatus());
        assertEquals(Lesson.PAYMENT_UNPAID, lesson.getPaymentStatus());
        assertEquals(lesson.getFee(), paid.getFee());
    }

    @Test
    void withDetails_someValuesNull_changesOnlyTheSuppliedDetails() {
        Lesson lesson = new Lesson("L123", "S1", "Pure Chemistry", "22-09-2026", "1600",
                "not recorded", "62.50", "").withPaymentStatus("paid");

        Lesson moved = lesson.withDetails(null, "25-09-2026", "1800", null, null, null);

        assertEquals(LocalDate.of(2026, 9, 25), moved.getDate());
        assertEquals(LocalTime.of(18, 0), moved.getTime());
        assertEquals("L123", moved.getLessonId());
        assertEquals("S1", moved.getStudentId());
        assertEquals("Pure Chemistry", moved.getSubject());
        assertEquals(new BigDecimal("62.50"), moved.getFee());
        assertEquals(Lesson.PAYMENT_PAID, moved.getPaymentStatus());
        // The original is immutable, so it still has its old slot.
        assertEquals(LocalDate.of(2026, 9, 22), lesson.getDate());
    }

    @Test
    void withDetails_allValuesGiven_replacesEveryDetail() {
        Lesson lesson = new Lesson("L123", "S1", "Pure Chemistry", "22-09-2026", "1600",
                "not recorded", "62.50", "");

        Lesson changed = lesson.withDetails("A Math", "21-09-2026", "0900", "late", "55", "Started vectors");

        assertEquals("A Math", changed.getSubject());
        assertEquals(LocalDate.of(2026, 9, 21), changed.getDate());
        assertEquals(LocalTime.of(9, 0), changed.getTime());
        assertEquals(Lesson.Attendance.LATE, changed.getAttendance());
        assertEquals(new BigDecimal("55"), changed.getFee());
        assertEquals("Started vectors", changed.getNotes());
    }

    @Test
    void withDetails_invalidValue_throws() {
        Lesson lesson = new Lesson("L123", "S1", "Pure Chemistry", "22-09-2026", "1600",
                "not recorded", "62.50", "");

        assertThrows(IllegalArgumentException.class,
                () -> lesson.withDetails(null, "31-02-2026", null, null, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> lesson.withDetails(null, null, "2460", null, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> lesson.withDetails(" ", null, null, null, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> lesson.withDetails(null, null, null, null, "-5", null));
    }

    @Test
    void isRecorded_onlyAfterAttendanceIsSet() {
        Lesson lesson = new Lesson("L123", "S1", "Pure Chemistry", "22-09-2026", "1600",
                "not recorded", "62.50", "");

        assertFalse(lesson.isRecorded());
        assertTrue(lesson.withDetails(null, null, null, "absent", null, null).isRecorded());
    }

    @Test
    void rejectsInvalidDate() {
        assertThrows(IllegalArgumentException.class,
                () -> new Lesson("L123", "Chemistry", "31-02-2026", "1600",
                        "present", "62.50", ""));
    }

    @Test
    void rejectsInvalidTime() {
        assertThrows(IllegalArgumentException.class,
                () -> new Lesson("L123", "Chemistry", "22-09-2026", "2460",
                        "present", "62.50", ""));
    }

    @Test
    void rejectsInvalidFee() {
        assertThrows(IllegalArgumentException.class,
                () -> new Lesson("L123", "Chemistry", "22-09-2026", "1600",
                        "present", "1.234", ""));
    }

    @Test
    void rejectsInvalidAttendance() {
        assertThrows(IllegalArgumentException.class,
                () -> new Lesson("L123", "Chemistry", "22-09-2026", "1600",
                        "excused", "62.50", ""));
    }
}

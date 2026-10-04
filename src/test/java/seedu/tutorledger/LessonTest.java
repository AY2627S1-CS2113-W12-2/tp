package seedu.tutorledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

class LessonTest {
    @Test
    void acceptsValidLessonDetails() {
        Lesson lesson = new Lesson("L123", "Pure Chemistry", "22-09-2026", "1600",
                "late", "62.50", "Started vectors", "paid");

        assertEquals("L123", lesson.getLessonId());
        assertEquals("Pure Chemistry", lesson.getSubject());
        assertEquals(LocalDate.of(2026, 9, 22), lesson.getDate());
        assertEquals(LocalTime.of(16, 0), lesson.getTime());
        assertEquals(Lesson.Attendance.LATE, lesson.getAttendance());
        assertEquals(new BigDecimal("62.50"), lesson.getFee());
        assertEquals("Started vectors", lesson.getNotes());
        assertEquals("paid", lesson.getPaymentStatus());
    }

    @Test
    void rejectsInvalidDate() {
        assertThrows(IllegalArgumentException.class,
                () -> new Lesson("L123", "Chemistry", "31-02-2026", "1600",
                        "present", "62.50", "", "unpaid"));
    }

    @Test
    void rejectsInvalidTime() {
        assertThrows(IllegalArgumentException.class,
                () -> new Lesson("L123", "Chemistry", "22-09-2026", "2460",
                        "present", "62.50", "", "unpaid"));
    }

    @Test
    void rejectsInvalidFee() {
        assertThrows(IllegalArgumentException.class,
                () -> new Lesson("L123", "Chemistry", "22-09-2026", "1600",
                        "present", "1.234", "", "unpaid"));
    }

    @Test
    void rejectsInvalidAttendance() {
        assertThrows(IllegalArgumentException.class,
                () -> new Lesson("L123", "Chemistry", "22-09-2026", "1600",
                        "excused", "62.50", "", "unpaid"));
    }
}

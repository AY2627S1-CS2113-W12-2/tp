package seedu.tutorledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/** Verifies that the first-launch data is small, valid and useful for trying the commands. */
class SampleDataTest {
    @Test
    void create_containsLinkedRecordsForMainFeatures() {
        TutorLedgerData data = SampleData.create(LocalDate.of(2026, 11, 2));

        assertEquals(2, data.getStudents().size());
        assertEquals(2, data.getLessons().size());
        assertEquals(1, data.getHomework().size());
        assertEquals(1, data.getScores().size());
        assertEquals(1, data.getTasks().size());
        assertEquals("S1", data.getHomeworkById("H1").getStudentId());
        assertEquals("S1", data.getScores().iterator().next().getStudentId());
        assertEquals("S1", data.getTasks().iterator().next().getStudentId().orElseThrow());
        assertTrue(data.getLesson("L1").isRecorded());
        assertEquals(Lesson.PAYMENT_PAID, data.getLesson("L1").getPaymentStatus());
    }

    @Test
    void create_returnsIndependentDataSets() {
        LocalDate today = LocalDate.of(2026, 11, 2);
        TutorLedgerData first = SampleData.create(today);
        TutorLedgerData second = SampleData.create(today);

        first.deleteStudent("S2");

        assertEquals(2, second.getStudents().size());
        assertEquals(2, second.getLessons().size());
    }

    @Test
    void create_placesRecordsRelativeToToday() {
        LocalDate today = LocalDate.of(2030, 3, 10);
        TutorLedgerData data = SampleData.create(today);

        assertEquals(today.minusDays(7), data.getLesson("L1").getDate());
        assertEquals(today.plusDays(2), data.getLesson("L2").getDate());
        assertEquals(today.plusDays(7), data.getHomeworkById("H1").getDueDate());
    }
}

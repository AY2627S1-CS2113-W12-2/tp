package seedu.tutorledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Verifies that the first-launch data is small, valid and useful for trying the commands. */
class SampleDataTest {
    @Test
    void create_containsLinkedRecordsForMainFeatures() {
        TutorLedgerData data = SampleData.create();

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
        TutorLedgerData first = SampleData.create();
        TutorLedgerData second = SampleData.create();

        first.deleteStudent("S2");

        assertEquals(2, second.getStudents().size());
        assertEquals(2, second.getLessons().size());
    }
}

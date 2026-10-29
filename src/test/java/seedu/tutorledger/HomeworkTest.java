package seedu.tutorledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class HomeworkTest {
    @Test
    void acceptsHomeworkDetails() {
        Homework homework = new Homework("HW123", "submitted");

        assertEquals("HW123", homework.getHomeworkId());
        assertEquals("submitted", homework.getSubmitStatus());
    }

    @Test
    void rejectsBlankHomeworkId() {
        assertThrows(IllegalArgumentException.class, () -> new Homework(" ", "submitted"));
    }

    @Test
    void rejectsBlankSubmissionStatus() {
        assertThrows(IllegalArgumentException.class, () -> new Homework("HW123", " "));
    }

    @Test
    void withDetails_someValues_changesOnlyThoseAndKeepsIdAndStudent() {
        Homework item = new Homework("H1", "S1", "Worksheet 4A", LocalDate.of(2026, 9, 29),
                Homework.STATUS_SUBMITTED);

        Homework edited = item.withDetails(null, LocalDate.of(2026, 9, 30), Homework.STATUS_OUTSTANDING);

        assertEquals("H1", edited.getHomeworkId());
        assertEquals("S1", edited.getStudentId());
        assertEquals("Worksheet 4A", edited.getDescription());
        assertEquals(LocalDate.of(2026, 9, 30), edited.getDueDate());
        assertFalse(edited.isSubmitted());
        assertTrue(item.isSubmitted());
    }

    @Test
    void withDetails_blankDescriptionOrUnknownStatus_throws() {
        Homework item = new Homework("H1", "S1", "Worksheet 4A", LocalDate.of(2026, 9, 29),
                Homework.STATUS_OUTSTANDING);

        assertThrows(IllegalArgumentException.class, () -> item.withDetails(" ", null, null));
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> item.withDetails(null, null, "done"));
        assertEquals("Status must be outstanding or submitted.", exception.getMessage());
    }
}

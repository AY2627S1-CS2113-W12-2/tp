package seedu.tutorledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}

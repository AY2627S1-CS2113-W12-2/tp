package seedu.tutorledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class StudentTest {
    @Test
    void acceptsValidStudentDetails() {
        Student student = new Student("S123", "Tan Wei Ming", "Sec 3", "91234567");

        assertEquals("S123", student.getStudentId());
        assertEquals("Tan Wei Ming", student.getName());
        assertEquals("Sec 3", student.getLevel());
        assertEquals("91234567", student.getPhoneNumber());
    }

    @Test
    void acceptsOptionalLevelSpace() {
        Student student = new Student("S124", "O'Neil-Smith", "Sec4", "81234567");

        assertEquals("Sec 4", student.getLevel());
    }

    @Test
    void rejectsInvalidName() {
        assertThrows(IllegalArgumentException.class,
                () -> new Student("S123", "Tan Wei 3", "Sec 3", "91234567"));
    }

    @Test
    void rejectsInvalidPhoneNumber() {
        assertThrows(IllegalArgumentException.class,
                () -> new Student("S123", "Tan Wei Ming", "Sec 3", "1234567"));
    }
}

package seedu.tutorledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Locale;

import org.junit.jupiter.api.Test;

/** Checks student field validation and canonical school levels at the model boundary. */
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
    void acceptsEverySupportedSchoolYearAndNormalisesItsCaseAndSpacing() {
        String[] stages = {"Pri", "Sec", "JC"};
        int[] highestYears = {6, 5, 2};
        for (int stage = 0; stage < stages.length; stage++) {
            for (int year = 1; year <= highestYears[stage]; year++) {
                String canonical = stages[stage] + " " + year;
                String compact = stages[stage].toLowerCase(Locale.ROOT) + year;
                Student spaced = new Student("S1", "Alice Tan", canonical, "91234567");
                Student unspaced = new Student("S1", "Alice Tan", compact, "91234567");
                assertEquals(canonical, spaced.getLevel());
                assertEquals(canonical, unspaced.getLevel());
            }
        }
        assertEquals("Pri 5", new Student("S1", "Alice Tan", "pRi 5", "91234567").getLevel());
        assertEquals("JC 2", new Student("S1", "Alice Tan", "jC 2", "91234567").getLevel());
    }

    @Test
    void rejectsOutOfRangeMalformedAndUnsupportedLevels() {
        String[] invalid = {null, "", " ", "Pri 0", "Pri 7", "Sec 0", "Sec 6", "JC 0", "JC 3",
            "Pri -1", "Pri 1.5", "Pri 01", "Sec  3", "JC\t1", "JC 1 extra", " Pri 1", "Pri 1 ",
            "P5", "Primary 5", "Junior College 1", "K2", "Poly 1", "University 1"};
        for (String value : invalid) {
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> new Student("S1", "Alice Tan", value, "91234567"), "Level: " + value);
            assertEquals("Level must be Pri 1 to Pri 6, Sec 1 to Sec 5, or JC 1 to JC 2.", error.getMessage());
        }
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

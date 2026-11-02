package seedu.tutorledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
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
    void acceptsVeryLongNameWithoutOverflowingTheStack() {
        String longName = String.join(" ", Collections.nCopies(2_000, "A"));

        assertDoesNotThrow(() -> new Student("S123", longName, "Sec 3", "91234567"));
    }

    @Test
    void rejectsInvalidPhoneNumber() {
        assertThrows(IllegalArgumentException.class,
                () -> new Student("S123", "Tan Wei Ming", "Sec 3", "1234567"));
    }

    @Test
    void newStudent_isNotFlagged() {
        Student student = new Student("S1", "Alice Tan", "Pri 5", "91234567");

        assertFalse(student.isFlagged());
        assertTrue(student.getAttentionReason().isEmpty());
    }

    @Test
    void withAttentionReason_trimsTheReasonAndAllowsAnEmptyOne() {
        Student student = new Student("S1", "Alice Tan", "Pri 5", "91234567");

        assertEquals("Weak at fractions", student.withAttentionReason("  Weak at fractions  ")
                .getAttentionReason().orElseThrow());
        // Empty and blank both mean "flagged, but no reason given", which is different from not flagged.
        Student blank = student.withAttentionReason("   ");
        assertTrue(blank.isFlagged());
        assertEquals("", blank.getAttentionReason().orElseThrow());
    }

    @Test
    void withAttentionReason_nullUnflags() {
        Student flagged = new Student("S1", "Alice Tan", "Pri 5", "91234567").withAttentionReason("Exam");

        Student unflagged = flagged.withAttentionReason(null);

        assertFalse(unflagged.isFlagged());
        assertTrue(unflagged.getAttentionReason().isEmpty());
    }

    @Test
    void withAttentionReason_lineBreak_isRejectedBecauseItWouldSplitTheSavedLine() {
        Student student = new Student("S1", "Alice Tan", "Pri 5", "91234567");

        for (String reason : new String[] {"Weak\nat fractions", "Weak\rat fractions", "Weak\r\nat fractions"}) {
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> student.withAttentionReason(reason));
            assertEquals("A flag reason must fit on one line.", error.getMessage());
        }
        // A line break at either end is just trimmed away, so it is accepted.
        assertEquals("Exam", student.withAttentionReason("Exam\n").getAttentionReason().orElseThrow());
    }

    @Test
    void withAttentionReason_keepsEveryOtherDetailAndLeavesTheOriginalUnchanged() {
        Student original = new Student("S7", "Alice Tan", "Pri 5", "91234567", List.of("Math", "Science"), "42.50")
                .withArchived(true);

        Student flagged = original.withAttentionReason("Exam");

        assertEquals("S7", flagged.getStudentId());
        assertEquals("Alice Tan", flagged.getName());
        assertEquals("Pri 5", flagged.getLevel());
        assertEquals("91234567", flagged.getPhoneNumber());
        assertEquals(List.of("Math", "Science"), flagged.getSubjects());
        assertEquals(new BigDecimal("42.50"), flagged.getDefaultHourlyRate().orElseThrow());
        assertTrue(flagged.isArchived());
        // Students are immutable: flagging returns a copy and never changes the original.
        assertFalse(original.isFlagged());
    }

    @Test
    void otherCopyMethods_keepTheFlag() {
        Student flagged = new Student("S1", "Alice Tan", "Pri 5", "91234567").withAttentionReason("Exam");

        assertEquals("Exam", flagged.withArchived(true).getAttentionReason().orElseThrow());
        assertEquals("Exam", flagged.withDefaultHourlyRate("60").getAttentionReason().orElseThrow());
        assertEquals("Exam", flagged.withDefaultHourlyRate(null).getAttentionReason().orElseThrow());
        assertEquals("Exam", flagged.withDetails("Alicia Tan", "Pri 6", "81234567", List.of("Math"))
                .getAttentionReason().orElseThrow());
    }
}

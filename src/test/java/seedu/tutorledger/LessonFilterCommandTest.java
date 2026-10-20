package seedu.tutorledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Checks that {@code filter} combines its filters and rejects invalid values without changing anything. */
class LessonFilterCommandTest {
    private static final String NEW_LINE = System.lineSeparator();

    private TutorLedgerData data;
    private LessonFilterCommand command;

    @BeforeEach
    void setUp() {
        data = new TutorLedgerData();
        data.addStudent("Amirah Binte Rahman", "Sec 3", "91234567");
        data.addStudent("Tan Wei Ming", "Sec 4", "98765432");
        // Added out of date order, to check the results come back sorted.
        data.putLesson(new Lesson("L3", "S1", "A Math", "22-09-2026", "1600", "not recorded", "60", ""));
        data.putLesson(new Lesson("L1", "S1", "E Math", "01-09-2026", "1600", "present", "55.50", ""));
        data.putLesson(new Lesson("L2", "S2", "Chemistry", "08-09-2026", "0900", "present", "70", "")
                .withPaymentStatus("paid"));
        command = new LessonFilterCommand(data);
    }

    @Test
    void handles_filterWordInAnyCase_trueOnlyForFilter() {
        assertTrue(LessonFilterCommand.handles("filter"));
        assertTrue(LessonFilterCommand.handles("  FILTER S1 pay/paid"));
        assertFalse(LessonFilterCommand.handles("filters"));
        assertFalse(LessonFilterCommand.handles("lessons week"));
        assertFalse(LessonFilterCommand.handles("   "));
        assertFalse(LessonFilterCommand.handles(null));
    }

    @Test
    void execute_noFilters_listsEveryLessonEarliestFirstWithTotal() {
        String expected = "3 lessons (all lessons)" + NEW_LINE
                + "  L1  Tue 01-09-2026 1600  S1 Amirah Binte Rahman  E Math     $55.50  unpaid" + NEW_LINE
                + "  L2  Tue 08-09-2026 0900  S2 Tan Wei Ming         Chemistry  $70.00  paid" + NEW_LINE
                + "  L3  Tue 22-09-2026 1600  S1 Amirah Binte Rahman  A Math     $60.00  unpaid" + NEW_LINE
                + "Total fees: $185.50";
        assertEquals(expected, command.execute("filter"));
    }

    @Test
    void execute_studentFilter_onlyThatStudentsLessons() {
        String output = command.execute("filter s1");
        assertTrue(output.startsWith("2 lessons (S1 Amirah Binte Rahman)"), output);
        assertFalse(output.contains("L2"));
        assertTrue(output.endsWith("Total fees: $115.50"));
    }

    @Test
    void execute_dateRangeIsInclusive() {
        String output = command.execute("filter from/01-09-2026 to/08-09-2026");
        assertTrue(output.startsWith("2 lessons (from Tue 01-09-2026, to Tue 08-09-2026)"), output);
        assertTrue(output.contains("L1") && output.contains("L2"));
        assertFalse(output.contains("L3"));
    }

    @Test
    void execute_allFiltersCombined_mustMatchEveryFilter() {
        String output = command.execute("FILTER S1 PAY/UNPAID from/02-09-2026");
        assertEquals("1 lesson (S1 Amirah Binte Rahman, from Wed 02-09-2026, unpaid)" + NEW_LINE
                + "  L3  Tue 22-09-2026 1600  S1 Amirah Binte Rahman  A Math  $60.00  unpaid" + NEW_LINE
                + "Total fees: $60.00", output);
    }

    @Test
    void execute_noMatches_showsCountOnly() {
        assertEquals("0 lessons (S2 Tan Wei Ming, unpaid)", command.execute("filter S2 pay/unpaid"));
    }

    @Test
    void execute_invalidValues_reportErrors() {
        assertEquals("Error: No student found with ID S9.", command.execute("filter S9"));
        assertEquals("Error: pay/ must be paid or unpaid.", command.execute("filter pay/owing"));
        assertEquals("Error: Date must be a real date in DD-MM-YYYY format.",
                command.execute("filter from/31-02-2026"));
        assertEquals("Error: from/ date must not be after to/ date.",
                command.execute("filter from/10-09-2026 to/01-09-2026"));
        assertTrue(command.execute("filter d/01-09-2026").startsWith("Error: Unexpected prefix d/."));
        assertTrue(command.execute("filter S1 S2").startsWith("Error: Expected a prefixed value."));
        assertTrue(command.execute("filter pay/paid pay/unpaid").startsWith("Error: Give pay/ only once."));
    }

    @Test
    void execute_neverChangesLessons() {
        List<Lesson> before = List.copyOf(data.getLessons());
        command.execute("filter S1 pay/unpaid");
        assertEquals(before, List.copyOf(data.getLessons()));
    }
}

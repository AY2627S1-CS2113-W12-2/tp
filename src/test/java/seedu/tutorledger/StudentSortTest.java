package seedu.tutorledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Checks {@code list sort/owed} and {@code list sort/last} (story US41, issue #47).
 *
 * <p>Today is Mon 21-09-2026. The records are chosen so that each rule changes the order if it is broken:
 * <ul>
 *   <li>S1 Alice Tan owes $60 (one unpaid lesson; a paid one is ignored). Last lesson 14-09.</li>
 *   <li>S2 Bob Lim owes $120 (a $160 lesson with $40 paid). Last lesson 01-09; a future lesson is ignored.</li>
 *   <li>S3 Cara Tan owes nothing (a paid lesson, plus an unpaid lesson that was cancelled). Last lesson
 *       10-09; the later cancelled lesson does not count.</li>
 *   <li>S4 Dan Ng has no lessons.</li>
 * </ul>
 */
class StudentSortTest {
    private static final String NEWLINE = System.lineSeparator();
    private TutorLedgerData data;
    private StudentCommands commands;

    @BeforeEach
    void setUp() {
        data = new TutorLedgerData();
        data.addStudent("Alice Tan", "Sec 3", "91234567");
        data.addStudent("Bob Lim", "Sec 4", "92345678");
        data.addStudent("Cara Tan", "Sec 2", "93456789");
        data.addStudent("Dan Ng", "Sec 1", "94567890");

        data.putLesson(new Lesson("L1", "S1", "Math", "14-09-2026", "1600", "present", "60", ""));
        data.putLesson(new Lesson("L2", "S1", "Math", "07-09-2026", "1600", "present", "50", "")
                .withPaymentStatus(Lesson.PAYMENT_PAID));
        data.putLesson(new Lesson("L3", "S2", "Physics", "01-09-2026", "1600", "present", "160", "")
                .withAmountPaid(new BigDecimal("40")));
        data.putLesson(new Lesson("L4", "S2", "Physics", "28-09-2026", "1600", "present", "80", ""));
        data.putLesson(new Lesson("L5", "S3", "Science", "10-09-2026", "1600", "present", "45", "")
                .withPaymentStatus(Lesson.PAYMENT_PAID));
        data.putLesson(new Lesson("L6", "S3", "Science", "20-09-2026", "1600", "present", "60", "")
                .withCancellation("Student sick"));

        Clock clock = Clock.fixed(Instant.parse("2026-09-21T10:00:00Z"), ZoneOffset.UTC);
        commands = new StudentCommands(data, clock);
    }

    @Test
    void sortOwedPutsLargestBalanceFirstAndKeepsIdOrderForTies() {
        String expected = String.join(NEWLINE,
                "4 students, most owed first:",
                "  S2  Bob Lim (Sec 4)  none  owes $120.00",
                "  S1  Alice Tan (Sec 3)  none  owes $60.00",
                "  S3  Cara Tan (Sec 2)  none  owes $0.00",
                "  S4  Dan Ng (Sec 1)  none  owes $0.00");
        assertEquals(expected, commands.execute("list sort/owed"));
    }

    @Test
    void sortLastPutsStudentsWithoutLessonsFirstThenOldestLastLesson() {
        String expected = String.join(NEWLINE,
                "4 students, longest since last lesson first:",
                "  S4  Dan Ng (Sec 1)  none  no past lessons",
                "  S2  Bob Lim (Sec 4)  none  last lesson Tue 01-09-2026",
                "  S3  Cara Tan (Sec 2)  none  last lesson Thu 10-09-2026",
                "  S1  Alice Tan (Sec 3)  none  last lesson Mon 14-09-2026");
        assertEquals(expected, commands.execute("list sort/last"));
    }

    @Test
    void sortWorksWithNameQueryStatusAndAnyCase() {
        String result = commands.execute("list tan sort/OWED");
        assertTrue(result.startsWith("2 students match \"tan\", most owed first:"));
        assertTrue(result.indexOf("S1  Alice Tan") < result.indexOf("S3  Cara Tan"));

        commands.execute("archive S4");
        assertFalse(commands.execute("list sort/last").contains("Dan Ng"));
        String all = commands.execute("list status/all sort/last");
        assertTrue(all.startsWith("4 students (all), longest since last lesson first:"));
        assertTrue(all.contains("S4  Dan Ng (Sec 1)  none  [archived]  no past lessons"));

        // The prefixes may come in either order.
        assertEquals(all, commands.execute("list sort/last status/all"));
    }

    @Test
    void listWithoutSortIsUnchanged() {
        String result = commands.execute("list");
        assertTrue(result.startsWith("4 students:"));
        assertFalse(result.contains("owes"));
        assertFalse(result.contains("last lesson"));
        List<String> ids = List.of("S1", "S2", "S3", "S4");
        for (int i = 1; i < ids.size(); i++) {
            assertTrue(result.indexOf(ids.get(i - 1) + "  ") < result.indexOf(ids.get(i) + "  "));
        }
    }

    @Test
    void invalidSortValuesAreRejected() {
        assertEquals("Error: Use sort/owed or sort/last.", commands.execute("list sort/name"));
        assertEquals("Error: Give a non-blank sort/ value.", commands.execute("list sort/"));
        assertEquals("Error: Give sort/ only once.", commands.execute("list sort/owed sort/last"));
    }
}

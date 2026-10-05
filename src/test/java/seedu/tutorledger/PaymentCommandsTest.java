package seedu.tutorledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaymentCommandsTest {
    private TutorLedgerData data;
    private PaymentCommands commands;

    @BeforeEach
    void setUp() {
        data = new TutorLedgerData();
        data.addStudent("Tan Wei Ming", "Sec 3", "91234567");
        data.putLesson(new Lesson("L37", "S1", "Pure Chemistry", "22-09-2026", "1600",
                "present", "62.50", "Started vectors"));
        commands = new PaymentCommands(data,
                Clock.fixed(Instant.parse("2026-09-21T10:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void markPaidAndUnpaidUpdatesTheStoredLesson() {
        assertEquals("unpaid", data.getLesson("L37").getPaymentStatus());

        assertEquals("Marked L37 as paid.", commands.execute("mark L37 paid"));
        assertEquals("paid", data.getLesson("L37").getPaymentStatus());

        assertEquals("Marked L37 as unpaid.", commands.execute("mark L37 unpaid"));
        assertEquals("unpaid", data.getLesson("L37").getPaymentStatus());
    }

    @Test
    void markAcceptsCaseInsensitiveCommandIdAndStatus() {
        assertEquals("Marked L37 as paid.", commands.execute("MARK l37 PAID"));
    }

    @Test
    void rejectsInvalidStatusAndMalformedArgumentsWithoutChangingLesson() {
        assertTrue(commands.execute("mark L37 pending").startsWith("Error:"));
        assertTrue(commands.execute("mark L37").startsWith("Error:"));
        assertTrue(commands.execute("mark L37 paid extra").startsWith("Error:"));
        assertEquals("unpaid", data.getLesson("L37").getPaymentStatus());
    }

    @Test
    void rejectsUnknownAndMalformedLessonIds() {
        assertTrue(commands.execute("mark L38 paid").contains("No lesson found"));
        assertTrue(commands.execute("mark invalid paid").contains("Lesson ID must look like L1"));
    }

    @Test
    void unpaidListsPastAndTodayLessonsOldestFirstAndExcludesFutureOrPaidLessons() {
        data.putLesson(new Lesson("L33", "S1", "E Math", "08-09-2026", "1600",
                "present", "60", ""));
        data.putLesson(new Lesson("L38", "S1", "A Math", "19-09-2026", "1800",
                "late", "55", ""));
        data.putLesson(new Lesson("L39", "S1", "Physics", "21-09-2026", "1600",
                "present", "45", ""));
        data.putLesson(new Lesson("L40", "S1", "Biology", "22-09-2026", "1600",
                "present", "50", ""));
        commands.execute("mark L38 paid");

        String output = commands.execute("unpaid");

        assertTrue(output.startsWith("        " + "_".repeat(60) + System.lineSeparator()
                + "2 unpaid lessons"));
        assertTrue(output.indexOf("L33") < output.indexOf("L39"));
        assertTrue(output.contains("Tue 08-09-2026  S1 Tan Wei Ming  E Math"));
        assertTrue(output.contains("Mon 21-09-2026  S1 Tan Wei Ming  Physics"));
        assertTrue(output.contains("$60.00"));
        assertTrue(output.contains("$45.00"));
        assertTrue(!output.contains("L38"));
        assertTrue(!output.contains("L40"));
    }

    @Test
    void unpaidCanFilterByStudentAndReportsUnknownStudent() {
        data.addStudent("Amirah Binte Rahman", "Sec 3", "81234567");
        data.putLesson(new Lesson("L38", "S2", "E Math", "15-09-2026", "1600",
                "present", "60", ""));

        String output = commands.execute("unpaid s2");

        assertTrue(output.contains("1 unpaid lesson"));
        assertTrue(output.contains("L38  Tue 15-09-2026  S2 Amirah Binte Rahman"));
        assertTrue(!output.contains("L37"));
        assertTrue(commands.execute("unpaid S99").startsWith("Error: No student found"));
    }

    @Test
    void unpaidWithNoMatchesReturnsZeroCount() {
        String output = commands.execute("unpaid");

        assertTrue(output.contains("0 unpaid lessons"));
        assertTrue(output.endsWith("       " + "_".repeat(60)));
    }

    @Test
    void handlesUnpaidButRejectsExtraArguments() {
        assertTrue(PaymentCommands.handles("unpaid"));
        assertTrue(PaymentCommands.handles("UNPAID s1"));
        assertTrue(commands.execute("unpaid S1 extra").startsWith("Error:"));
    }
}

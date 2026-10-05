package seedu.tutorledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        commands = new PaymentCommands(data);
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
}

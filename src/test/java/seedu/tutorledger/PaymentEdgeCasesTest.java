package seedu.tutorledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Reproduces payment interactions with free lessons, fee edits, cancellation and persistence. */
class PaymentEdgeCasesTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-26T08:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path tempDir;

    private TutorLedgerData data;
    private LessonCommands lessons;
    private PaymentCommands payments;

    @BeforeEach
    void setUp() {
        data = new TutorLedgerData();
        data.addStudent("Test Student", "Sec 3", "91234567", List.of("Math"), null);
        lessons = new LessonCommands(data, CLOCK);
        payments = new PaymentCommands(data, CLOCK);
    }

    @Test
    void freeLesson_recordAndEdit_keepItSettledWithoutIncome() {
        lessons.execute("schedule S1 d/01-09-2026 t/1600 s/Math f/0");
        assertEquals(Lesson.PAYMENT_PAID, data.getLesson("L1").getPaymentStatus());

        assertTrue(lessons.execute("record L1 a/present").startsWith("Recorded L1"));
        assertTrue(lessons.execute("edit L1 note/Free makeup dur/90").startsWith("Edited L1"));
        assertEquals(Lesson.Attendance.PRESENT, data.getLesson("L1").getAttendance());
        assertEquals(Lesson.PAYMENT_PAID, data.getLesson("L1").getPaymentStatus());
        assertEquals(0, data.getLesson("L1").getAmountPaid().signum());
        assertTrue(payments.execute("income 09-2026").contains("Income for September 2026: $0.00"));
        assertTrue(payments.execute("unpaid S1").contains("0 unpaid lessons"));
        assertTrue(payments.execute("owed").contains("No students owe anything."));
        assertFalse(new LessonFilterCommand(data).execute("filter pay/unpaid").contains("L1"));
        assertEquals("Marked L1 as paid.", payments.execute("mark L1 unpaid"));
    }

    @Test
    void freeLesson_unlinkedConstructorAndCopies_keepItSettled() {
        Lesson free = new Lesson("L1", "Math", "01-09-2026", "1600", "not recorded", "0", "");

        assertEquals(Lesson.PAYMENT_PAID, free.getPaymentStatus());
        assertEquals(Lesson.PAYMENT_PAID, free.withDurationMinutes(90).getPaymentStatus());
        assertEquals(Lesson.PAYMENT_PAID, free.withCancellation("Unavailable").getPaymentStatus());
        assertEquals(Lesson.PAYMENT_UNPAID,
                free.withDetails(null, null, null, null, "60", null).getPaymentStatus());
    }

    @Test
    void feeEdits_preservePaymentsAndIncomeWhenTheBalanceChanges() {
        lessons.execute("log S1 d/01-09-2026 t/1600 s/Math f/60 a/present");
        payments.execute("pay S1 40 L1");
        String incomeBefore = payments.execute("income 09-2026");

        assertTrue(lessons.execute("edit L1 f/40").startsWith("Edited L1"));
        assertEquals(Lesson.PAYMENT_PAID, data.getLesson("L1").getPaymentStatus());
        assertTrue(lessons.execute("edit L1 f/80").startsWith("Edited L1"));
        assertEquals(new BigDecimal("40"), data.getLesson("L1").getAmountPaid());
        assertEquals(new BigDecimal("40"), data.getLesson("L1").getAmountOwed());
        assertEquals(Lesson.PAYMENT_PARTIAL, data.getLesson("L1").getPaymentStatus());
        assertEquals(incomeBefore, payments.execute("income 09-2026"));
        assertTrue(payments.execute("owed").contains("$40.00"));
    }

    @Test
    void feeReduction_belowPartialOrFullPayment_rejectsTheWholeEdit() {
        lessons.execute("log S1 d/01-09-2026 t/1600 s/Math f/60 a/present");
        for (String amount : List.of("40", "60")) {
            data.updateLessonAmountPaid("L1", new BigDecimal(amount));
            String before = Storage.encode(data);

            assertEquals("Error: Fee cannot be less than the amount already paid ($" + amount + ".00).",
                    lessons.execute("edit L1 f/30 note/Should not be saved"));
            assertEquals(before, Storage.encode(data));
            assertThrows(IllegalArgumentException.class,
                    () -> data.getLesson("L1").withDetails(null, null, null, null, "30", null));
        }
    }

    @Test
    void cancelledPartialPayment_remindsAboutTheActualAmountAndRetainsIt() {
        lessons.execute("schedule S1 d/28-10-2026 t/1600 s/Math f/60");
        payments.execute("pay S1 20 L1");

        assertTrue(lessons.execute("cancel L1 r/Unavailable")
                .contains("This lesson has $20.00 paid, so refund or carry over that amount."));
        assertEquals(new BigDecimal("20"), data.getLesson("L1").getAmountPaid());
        assertTrue(data.getLesson("L1").isCancelled());
    }

    @Test
    void cancellation_withoutAnyMoneyReceived_doesNotSuggestARefund() {
        lessons.execute("schedule S1 d/28-10-2026 t/1600 s/Math f/0");
        lessons.execute("schedule S1 d/29-10-2026 t/1600 s/Math f/60");

        assertFalse(lessons.execute("cancel L1 r/Unavailable").contains("refund"));
        assertFalse(lessons.execute("cancel L2 r/Unavailable").contains("refund"));
    }

    @Test
    void saveAndReload_preserveFreeLessonsEditedFeesAndCancelledPayments() throws Exception {
        lessons.execute("schedule S1 d/01-09-2026 t/1600 s/Math f/0");
        lessons.execute("record L1 a/present");
        lessons.execute("log S1 d/02-09-2026 t/1600 s/Math f/60 a/present");
        payments.execute("pay S1 40 L2");
        lessons.execute("edit L2 f/40");
        lessons.execute("edit L2 f/80");
        lessons.execute("schedule S1 d/28-10-2026 t/1600 s/Math f/60");
        payments.execute("pay S1 20 L3");
        lessons.execute("cancel L3 r/Unavailable");
        String incomeBefore = payments.execute("income 09-2026");
        Storage storage = new Storage(tempDir.resolve("payments.txt").toString());
        storage.save(data);

        Storage.LoadResult loaded = storage.load();

        assertTrue(loaded.problems().isEmpty());
        assertEquals(Storage.encode(data), Storage.encode(loaded.data()));
        assertEquals(Lesson.PAYMENT_PAID, loaded.data().getLesson("L1").getPaymentStatus());
        assertEquals(new BigDecimal("40"), loaded.data().getLesson("L2").getAmountPaid());
        assertEquals(new BigDecimal("40"), loaded.data().getLesson("L2").getAmountOwed());
        assertEquals(new BigDecimal("20"), loaded.data().getLesson("L3").getAmountPaid());
        assertEquals(incomeBefore, new PaymentCommands(loaded.data(), CLOCK).execute("income 09-2026"));
    }

    @Test
    void legacyFreeLesson_reloadNormalisesItsStatusWithoutAddingIncome() {
        Storage.LoadResult loaded = Storage.decode(List.of(
                "S | S1 | Test Student | Sec 3 | 91234567 | Math",
                "L | L1 | S1 | 01-09-2026 | 1600 | Math | 0 | present | unpaid | Free lesson"));

        assertTrue(loaded.problems().isEmpty());
        assertEquals(Lesson.PAYMENT_PAID, loaded.data().getLesson("L1").getPaymentStatus());
        assertEquals(0, loaded.data().getLesson("L1").getAmountPaid().signum());
    }
}

package seedu.tutorledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Checks that historical payments stay intact while cancelled charges are clearly distinguished. */
class CancelledPaymentViewsTest {
    @TempDir
    Path tempDir;

    /** Builds cancelled unpaid, partial, paid and free lessons beside one active charge. */
    private TutorLedgerData sample() {
        TutorLedgerData data = new TutorLedgerData();
        data.addStudent("Alice Tan", "Pri5", "91234567", List.of("Math"), null);
        for (int index = 1; index <= 4; index++) {
            String fee = index == 4 ? "0" : "60";
            Lesson lesson = data.addLesson("S1", "Math", "0" + index + "-01-2020", "1600", fee);
            if (index == 2 || index == 3) {
                lesson = lesson.withAmountPaid(new BigDecimal(index == 2 ? "25" : "60"));
            }
            data.replaceLesson(lesson.withCancellation("Sick"));
        }
        data.addLesson("S1", "Math", "05-01-2020", "1600", "40");
        return data;
    }

    @Test
    void paymentFilters_excludeEveryCancelledStatusAndRestoreEligibilityOnUncancel() {
        TutorLedgerData data = sample();
        LessonFilterCommand filter = new LessonFilterCommand(data);
        for (String status : List.of("paid", "partial", "unpaid")) {
            String output = filter.execute("filter S1 pay/" + status);
            for (String id : List.of("L1", "L2", "L3", "L4")) {
                assertFalse(output.contains(id), output);
            }
        }
        data.replaceLesson(data.getLesson("L2").withoutCancellation());
        assertTrue(filter.execute("filter S1 pay/partial").contains("L2"));
        assertEquals(new BigDecimal("25"), data.getLesson("L2").getAmountPaid());
    }

    @Test
    void historicalViews_labelCancellationAndExcludeItsFeesWithoutChangingPayments() {
        TutorLedgerData data = sample();
        String before = Storage.encode(data);
        String filter = new LessonFilterCommand(data).execute("filter S1");
        String statement = new PaymentCommands(data,
                Clock.fixed(Instant.parse("2026-11-02T10:00:00Z"), ZoneOffset.UTC))
                .execute("fee_statement S1");
        for (String output : List.of(filter, statement)) {
            assertEquals(4, output.lines().filter(line -> line.contains("[cancelled: Sick; not chargeable]")).count());
        }
        assertTrue(filter.endsWith("Total fees (excluding cancelled lessons): $40.00"), filter);
        assertTrue(statement.contains("partial ($25.00 paid) [cancelled: Sick; not chargeable]"), statement);
        assertEquals(before, Storage.encode(data));
    }

    @Test
    void cancelledOnlyHistory_hasZeroChargeableFees() {
        TutorLedgerData data = sample();
        data.deleteLesson("L5");
        assertTrue(new LessonFilterCommand(data).execute("filter")
                .endsWith("Total fees (excluding cancelled lessons): $0.00"));
    }

    @Test
    void originalReproduction_afterRestartKeepsPaymentsAndReadOnlyFile() throws IOException {
        Path file = tempDir.resolve("data.txt");
        String setup = TutorLedgerCommandRoutingTest.runCommands(file,
                "add n/Alice Tan l/Pri5 p/91234567 s/Math",
                "schedule S1 d/01-01-2020 t/1600 s/Math f/60", "pay S1 25 L1", "cancel L1 r/Sick", "exit");
        assertFalse(setup.contains("Error:"), setup);
        Files.writeString(file, Files.readString(file) + "# Preserve this hand edit\n");
        String before = Files.readString(file);
        String output = TutorLedgerCommandRoutingTest.runCommands(file,
                "filter S1 pay/unpaid", "filter S1", "fee_statement S1", "owed", "exit");
        assertTrue(output.contains("0 lessons (S1 Alice Tan, unpaid)"), output);
        assertTrue(output.contains("partial ($25.00 paid) [cancelled: Sick; not chargeable]"), output);
        assertTrue(output.contains("No students owe anything."), output);
        assertEquals(before, Files.readString(file));
        assertEquals(new BigDecimal("25"), new Storage(file.toString()).load().data()
                .getLesson("L1").getAmountPaid());
    }
}

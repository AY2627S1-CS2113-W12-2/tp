package seedu.tutorledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises US5 across commands, immutable records, payment totals and storage compatibility. */
class DefaultHourlyRateTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-20T10:00:00Z"), ZoneOffset.UTC);
    private static final String SLOT = " S1 d/20-10-2026 t/1600 s/E Math";

    @TempDir
    Path tempDir;

    private TutorLedgerData data;
    private StudentCommands students;
    private LessonCommands lessons;

    @BeforeEach
    void setUp() {
        data = new TutorLedgerData();
        students = new StudentCommands(data, CLOCK);
        lessons = new LessonCommands(data, CLOCK);
        students.execute("add n/Tan Wei Ming l/Sec 3 p/91234567 s/E Math");
    }

    @Test
    void studentRate_setReplaceClear_distinguishesZeroFromMissing() {
        assertTrue(data.getStudent("S1").getDefaultHourlyRate().isEmpty());
        assertTrue(students.execute("view S1").contains("Default hourly rate: not set"));
        assertTrue(students.execute("EDIT s1 R/60.50").contains("$60.50/hour"));
        assertEquals(new BigDecimal("60.50"), data.getStudent("S1").getDefaultHourlyRate().orElseThrow());
        students.execute("edit S1 r/0");
        assertEquals(0, data.getStudent("S1").getDefaultHourlyRate().orElseThrow().signum());
        assertTrue(students.execute("edit S1 r/").contains("Default hourly rate: not set"));
        assertTrue(data.getStudent("S1").getDefaultHourlyRate().isEmpty());
    }

    @Test
    void studentRate_otherEditsAndSubjects_keepTheRate() {
        students.execute("edit S1 r/60");
        students.execute("edit S1 l/Sec 4 s/A Math");
        students.execute("subject S1 s/E Math");
        assertEquals(new BigDecimal("60"), data.getStudent("S1").getDefaultHourlyRate().orElseThrow());
        assertEquals(List.of("A Math", "E Math"), data.getStudent("S1").getSubjects());
    }

    @Test
    void add_completeProfile_createsSubjectsAndRateTogether() {
        String result = students.execute("ADD R/62.50 P/81234567 N/Amirah L/Sec 2 S/A Math");
        assertTrue(result.contains("Added S2"));
        assertTrue(result.contains("Default hourly rate: $62.50/hour"));
        assertEquals(List.of("A Math"), data.getStudent("S2").getSubjects());
        assertEquals(new BigDecimal("62.50"), data.getStudent("S2").getDefaultHourlyRate().orElseThrow());
    }

    @Test
    void add_invalidRateOrDuplicateIdentity_consumesNoId() {
        String before = Storage.encode(data);
        for (String rate : List.of("", "-1", "2.001", "1e2", "NaN", "1 r/2")) {
            assertTrue(students.execute("add n/Amirah l/Sec 2 p/81234567 s/A Math r/" + rate)
                    .startsWith("Error:"), rate);
            assertEquals(before, Storage.encode(data));
        }
        assertTrue(students.execute("add n/Tan Wei Ming l/Sec 3 p/91234567 r/60").startsWith("Error:"));
        assertEquals(2, data.getNextStudentNumber());
    }

    @Test
    void edit_invalidRateOrRepeatedPrefix_changesNoFields() {
        students.execute("edit S1 r/60");
        String before = Storage.encode(data);
        for (String rate : List.of("-1", "1.001", "abc", "1e2", "1 r/2", " r/")) {
            assertTrue(students.execute("edit S1 l/Sec 5 r/" + rate).startsWith("Error:"), rate);
            assertEquals(before, Storage.encode(data));
        }
        assertTrue(students.execute("edit S9 r/60").startsWith("Error:"));
    }

    @Test
    void studentModel_rejectsInvalidRatesWithoutDependingOnAssertions() {
        Student original = data.getStudent("S1");
        for (String value : List.of("", " ", "-0.01", "0.001", "NaN", "+10", "1e2")) {
            assertThrows(IllegalArgumentException.class, () -> original.withDefaultHourlyRate(value));
        }
        assertTrue(original.getDefaultHourlyRate().isEmpty());
    }

    @Test
    void schedule_defaultDuration_usesOneHourAndStudentRate() {
        students.execute("edit S1 r/60");
        assertTrue(lessons.execute("schedule" + SLOT).contains("$60.00"));
        assertEquals(60, data.getLesson("L1").getDurationMinutes());
        assertEquals(new BigDecimal("60.00"), data.getLesson("L1").getFee());
    }

    @Test
    void schedule_durationAndRounding_calculatesOneFixedTotal() {
        students.execute("edit S1 r/60.01");
        assertTrue(lessons.execute("SCHEDULE s1 DUR/30 S/e math T/1600 D/20-10-2026").contains("(30 min)"));
        assertEquals(new BigDecimal("30.01"), data.getLesson("L1").getFee());
        students.execute("edit S1 r/60");
        lessons.execute("schedule S1 d/20-10-2026 t/1800 s/E Math dur/90");
        assertEquals(new BigDecimal("90.00"), data.getLesson("L2").getFee());
        assertTrue(students.execute("view S1").contains("90 min"));
    }

    @Test
    void schedule_explicitFee_isTotalAndOverridesTheRate() {
        students.execute("edit S1 r/60");
        lessons.execute("schedule" + SLOT + " dur/90 f/25.50");
        assertEquals(new BigDecimal("25.50"), data.getLesson("L1").getFee());
        students.execute("edit S1 r/");
        lessons.execute("schedule S1 d/20-10-2026 t/1800 s/E Math dur/30 f/10");
        assertEquals(new BigDecimal("10"), data.getLesson("L2").getFee());
    }

    @Test
    void schedule_missingRateAndFee_reportsHowToProceedWithoutConsumingId() {
        assertEquals("Error: Set a default hourly rate with edit S1 r/RATE, or give f/FEE.",
                lessons.execute("schedule" + SLOT));
        assertTrue(data.getLessons().isEmpty());
        assertEquals(1, data.getNextLessonNumber());
    }

    @Test
    void schedule_zeroRate_isAFreeLesson() {
        students.execute("edit S1 r/0");
        assertTrue(lessons.execute("schedule" + SLOT + " dur/90").contains("$0.00"));
        assertEquals(0, data.getLesson("L1").getFee().signum());
    }

    @Test
    void schedule_invalidExplicitFee_neverFallsBackToRate() {
        students.execute("edit S1 r/60");
        String before = Storage.encode(data);
        for (String fee : List.of("", "-1", "1.001", "60 f/70")) {
            assertTrue(lessons.execute("schedule" + SLOT + " f/" + fee).startsWith("Error:"));
            assertEquals(before, Storage.encode(data));
        }
    }

    @Test
    void duration_invalidOrRepeated_valuesChangeNothing() {
        students.execute("edit S1 r/60");
        String before = Storage.encode(data);
        for (String value : List.of("", "0", "-1", "1.5", "1441", "999999999999999", "60 dur/90")) {
            assertTrue(lessons.execute("schedule" + SLOT + " dur/" + value).startsWith("Error:"), value);
            assertEquals(before, Storage.encode(data));
        }
        assertThrows(IllegalArgumentException.class,
                () -> data.addLesson("S1", "E Math", "20-10-2026", "1600", "10", Integer.MAX_VALUE));
    }

    @Test
    void duration_boundariesAreAcceptedAndSlotConflictsStillRejectWithoutConsumingIds() {
        students.execute("edit S1 r/60");
        lessons.execute("schedule" + SLOT + " dur/1");
        assertEquals(new BigDecimal("1.00"), data.getLesson("L1").getFee());
        assertTrue(lessons.execute("schedule" + SLOT + " dur/1440").startsWith("Error:"));
        assertEquals(2, data.getNextLessonNumber());
        lessons.execute("schedule S1 d/21-10-2026 t/1600 s/E Math dur/1440");
        assertEquals(1440, data.getLesson("L2").getDurationMinutes());
    }

    @Test
    void changesToRateAndDuration_preserveExistingFeeUntilExplicitlyEdited() {
        students.execute("edit S1 r/60");
        lessons.execute("schedule" + SLOT + " dur/90");
        students.execute("edit S1 r/100");
        lessons.execute("edit L1 dur/30");
        assertEquals(new BigDecimal("90.00"), data.getLesson("L1").getFee());
        assertEquals(30, data.getLesson("L1").getDurationMinutes());
        lessons.execute("schedule S1 d/20-10-2026 t/1800 s/E Math");
        assertEquals(new BigDecimal("100.00"), data.getLesson("L2").getFee());
        students.execute("edit S1 r/");
        lessons.execute("edit L1 dur/45 f/35");
        assertEquals(new BigDecimal("35"), data.getLesson("L1").getFee());
        String before = Storage.encode(data);
        assertTrue(lessons.execute("edit L1 dur/0 f/99").startsWith("Error:"));
        assertEquals(before, Storage.encode(data));
    }

    @Test
    void recordingAndPayments_preserveDurationAndUseSavedFee() {
        students.execute("edit S1 r/60");
        lessons.execute("schedule" + SLOT + " dur/90");
        lessons.execute("record L1 a/present note/Quadratics");
        students.execute("edit S1 r/100");
        PaymentCommands payments = new PaymentCommands(data, CLOCK);
        assertTrue(payments.execute("owed").contains("90.00"));
        assertTrue(payments.execute("fee_statement S1").contains("90.00"));
        payments.execute("mark L1 paid");
        assertEquals(90, data.getLesson("L1").getDurationMinutes());
        assertEquals("Quadratics", data.getLesson("L1").getNotes());
        assertEquals(Lesson.Attendance.PRESENT, data.getLesson("L1").getAttendance());
        assertEquals(Lesson.PAYMENT_PAID, data.getLesson("L1").getPaymentStatus());
        assertEquals(new BigDecimal("90.00"), data.getLesson("L1").getFee());
    }

    @Test
    void storage_legacyFile_hasNoRateAndOneHourDuration() {
        Storage.LoadResult result = Storage.decode(List.of(
                "S | S1 | Amirah | Sec 3 | 91234567 | r/60",
                "L | L1 | S1 | 20-10-2026 | 1600 | E Math | 25 | present | paid | old notes"));
        assertTrue(result.problems().isEmpty());
        assertTrue(result.data().getStudent("S1").getDefaultHourlyRate().isEmpty());
        assertEquals(List.of("r/60"), result.data().getStudent("S1").getSubjects());
        assertEquals(60, result.data().getLesson("L1").getDurationMinutes());
        assertEquals(new BigDecimal("25"), result.data().getLesson("L1").getFee());
    }

    @Test
    void storage_rateAndDurationRoundTrip_doNotRecalculateSavedFee() {
        students.execute("edit S1 r/60");
        lessons.execute("schedule" + SLOT + " dur/90 f/25");
        students.execute("edit S1 r/100");
        String encoded = Storage.encode(data);
        Storage.LoadResult result = Storage.decode(encoded.lines().toList());
        assertTrue(result.problems().isEmpty());
        assertEquals(encoded, Storage.encode(result.data()));
        assertEquals(new BigDecimal("25"), result.data().getLesson("L1").getFee());
        assertEquals(90, result.data().getLesson("L1").getDurationMinutes());
        assertEquals(new BigDecimal("100"), result.data().getStudent("S1").getDefaultHourlyRate().orElseThrow());
    }

    @Test
    void storage_invalidMetadata_reportsLinesAndKeepsFirstValidRate() {
        Storage.LoadResult result = Storage.decode(List.of(
                "R | S1 | -1", "r | s1 | 60", "R | S1 | 70", "R | S9 | 30", "R | S1",
                "S | S1 | Amirah | Sec 3 | 91234567 | E Math",
                "L | L1 | S1 | 20-10-2026 | 1600 | E Math | 25 | present | paid | notes |  | 0",
                "L | L2 | S1 | 20-10-2026 | 1800 | E Math | 25 | present | paid | notes |  | 90"));
        assertEquals(List.of(1, 3, 4, 5, 7), result.problems().stream()
                .map(problem -> Integer.parseInt(problem.split("[: ]")[1])).toList());
        assertEquals(new BigDecimal("60"), result.data().getStudent("S1").getDefaultHourlyRate().orElseThrow());
        assertEquals(1, result.data().getLessons().size());
        assertEquals(90, result.data().getLesson("L2").getDurationMinutes());
    }

    @Test
    void storage_autoSaveRestartAndClear_roundTripTheFeature() throws Exception {
        Storage storage = new Storage(tempDir.resolve("tutorledger.txt").toString());
        StorageCommands saving = new StorageCommands(data, storage);
        String before = saving.snapshot();
        students.execute("edit S1 r/60");
        lessons.execute("schedule" + SLOT + " dur/90");
        assertEquals("", saving.saveIfChanged(before));
        TutorLedgerData restarted = new TutorLedgerData();
        new StorageCommands(restarted, storage).loadOnStartup();
        assertEquals(90, restarted.getLesson("L1").getDurationMinutes());
        assertEquals(new BigDecimal("90.00"), restarted.getLesson("L1").getFee());
        before = saving.snapshot();
        students.execute("edit S1 r/");
        assertEquals("", saving.saveIfChanged(before));
        assertFalse(Files.readString(tempDir.resolve("tutorledger.txt")).contains("\nR |"));
        assertTrue(storage.load().data().getStudent("S1").getDefaultHourlyRate().isEmpty());
    }

    @Test
    void storage_reloadHandEditedRateAndDuration_keepsTheSavedFee() throws Exception {
        students.execute("edit S1 r/60");
        lessons.execute("schedule" + SLOT + " dur/90");
        Path file = tempDir.resolve("reload.txt");
        Storage storage = new Storage(file.toString());
        storage.save(data);
        String changed = Files.readString(file).replace("R | S1 | 60", "R | S1 | 100")
                .replace(" | 90" + System.lineSeparator(), " | 30" + System.lineSeparator());
        Files.writeString(file, changed);
        StorageCommands saving = new StorageCommands(data, storage);
        assertTrue(saving.execute("reload").startsWith("Reloaded"));
        assertEquals(new BigDecimal("100"), data.getStudent("S1").getDefaultHourlyRate().orElseThrow());
        assertEquals(30, data.getLesson("L1").getDurationMinutes());
        assertEquals(new BigDecimal("90.00"), data.getLesson("L1").getFee());
        assertEquals(changed, Files.readString(file));
    }

    @Test
    void weeklySchedule_defaultRateAndDuration_keepFixedFeesForEveryWeek() {
        students.execute("edit S1 r/60");
        String result = lessons.execute("schedule" + SLOT + " dur/90 w/3");
        assertTrue(result.contains("3 weekly lessons"));
        assertTrue(result.contains("(90 min)"));
        assertTrue(result.contains("$90.00 each"));
        students.execute("edit S1 r/100");
        assertEquals(3, data.getLessons().size());
        for (Lesson lesson : data.getLessons()) {
            assertEquals(90, lesson.getDurationMinutes());
            assertEquals(new BigDecimal("90.00"), lesson.getFee());
        }
        assertEquals(4, data.getNextLessonNumber());
    }

    @Test
    void weeklySchedule_explicitFeeOverridesRateAndClashesAreAtomic() {
        students.execute("edit S1 r/60");
        lessons.execute("schedule S1 d/27-10-2026 t/1600 s/E Math f/25");
        String before = Storage.encode(data);
        assertTrue(lessons.execute("schedule" + SLOT + " dur/90 w/3").startsWith("Error:"));
        assertEquals(before, Storage.encode(data));
        assertTrue(lessons.execute("schedule" + SLOT + " dur/0 w/3").startsWith("Error:"));
        assertEquals(before, Storage.encode(data));
        lessons.execute("schedule S1 d/20-10-2026 t/1800 s/E Math dur/90 f/40 w/2");
        assertEquals(new BigDecimal("40"), data.getLesson("L2").getFee());
        assertEquals(new BigDecimal("40"), data.getLesson("L3").getFee());
        assertEquals(90, data.getLesson("L3").getDurationMinutes());
    }

    @Test
    void cancellationAndDurationEdits_preserveBothFieldsAndExcludeTheFeeFromOwed() {
        students.execute("edit S1 r/60");
        lessons.execute("schedule" + SLOT + " dur/90");
        assertTrue(lessons.execute("cancel L1 r/90").startsWith("Cancelled"));
        lessons.execute("edit L1 dur/45");
        Lesson cancelled = data.getLesson("L1");
        assertEquals("90", cancelled.getCancellationReason());
        assertEquals(45, cancelled.getDurationMinutes());
        assertEquals(new BigDecimal("90.00"), cancelled.getFee());
        assertEquals(45, cancelled.withPaymentStatus("paid").getDurationMinutes());
        assertTrue(cancelled.withDurationMinutes(30).isCancelled());
        assertTrue(students.execute("view S1").contains("Amount owed: $0.00"));
        String before = Storage.encode(data);
        assertTrue(lessons.execute("edit L1 a/present dur/60").startsWith("Error:"));
        assertEquals(before, Storage.encode(data));
        // Cancellation frees the slot while retaining the original lesson and its fixed charge.
        assertTrue(lessons.execute("schedule" + SLOT + " dur/30").startsWith("Scheduled L2"));
    }

    @Test
    void reschedule_preservesRateBasedFeeAndDuration() {
        students.execute("edit S1 r/60");
        lessons.execute("schedule S1 d/21-10-2026 t/1600 s/E Math dur/90");
        students.execute("edit S1 r/100");
        assertTrue(lessons.execute("reschedule L1 d/22-10-2026 t/1800").startsWith("Rescheduled"));
        assertEquals(90, data.getLesson("L1").getDurationMinutes());
        assertEquals(new BigDecimal("90.00"), data.getLesson("L1").getFee());
    }

    @Test
    void storage_earlierRateBranchHeader_preservesDurationWithoutInventingCancellation() {
        Storage.LoadResult loaded = Storage.decode(List.of(
                "#   L | LESSON_ID | STUDENT_ID | DATE | TIME | SUBJECT | FEE"
                        + " | ATTENDANCE | PAYMENT | NOTES | MINUTES",
                "S | S1 | Amirah | Sec 3 | 91234567 | E Math", "R | S1 | 100",
                "L | L1 | S1 | 20-10-2026 | 1600 | E Math | 25 | present | paid | notes | 90"));
        assertTrue(loaded.problems().isEmpty());
        Lesson lesson = loaded.data().getLesson("L1");
        assertEquals(90, lesson.getDurationMinutes());
        assertFalse(lesson.isCancelled());
        assertEquals(new BigDecimal("25"), lesson.getFee());
        String combined = Storage.encode(loaded.data());
        Storage.LoadResult roundTrip = Storage.decode(combined.lines().toList());
        assertTrue(roundTrip.problems().isEmpty());
        assertEquals(combined, Storage.encode(roundTrip.data()));
    }

    @Test
    void storage_oldAndNewCancellationFormats_preserveNumericReasonsAndDurations() {
        Storage.LoadResult loaded = Storage.decode(List.of(
                "S | S1 | Amirah | Sec 3 | 91234567 | E Math",
                "L | L1 | S1 | 20-10-2026 | 1600 | E Math | 25 | not recorded | paid | notes | 90",
                "L | L2 | S1 | 20-10-2026 | 1800 | E Math | 42 | not recorded | unpaid | notes | sick | 90",
                "L | L3 | S1 | 20-10-2026 | 1900 | E Math | 50 | present | paid | notes |  | 30"));
        assertTrue(loaded.problems().isEmpty());
        assertEquals("90", loaded.data().getLesson("L1").getCancellationReason());
        assertEquals(60, loaded.data().getLesson("L1").getDurationMinutes());
        assertEquals("sick", loaded.data().getLesson("L2").getCancellationReason());
        assertEquals(90, loaded.data().getLesson("L2").getDurationMinutes());
        assertFalse(loaded.data().getLesson("L3").isCancelled());
        assertEquals(30, loaded.data().getLesson("L3").getDurationMinutes());
        String text = Storage.encode(loaded.data());
        assertEquals(text, Storage.encode(Storage.decode(text.lines().toList()).data()));
    }

    @Test
    void help_listsRateDurationAndAllNewUpstreamCommands() {
        String help = HelpCommand.getHelpText();
        assertTrue(help.contains("s/SUBJECT [dur/MINUTES] [f/FEE] [w/WEEKS]"));
        assertTrue(help.contains("log STUDENT_ID"));
        assertTrue(help.contains("reschedule LESSON_ID"));
        assertTrue(help.contains("cancel LESSON_ID r/REASON"));
        assertTrue(help.contains("[s/SUBJECT]... [r/RATE]"));
    }

    @Test
    void gradleTestRuntime_hasAssertionsEnabled() {
        assertTrue(Student.class.desiredAssertionStatus());
        assertTrue(Lesson.class.desiredAssertionStatus());
    }
}

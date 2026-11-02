package seedu.tutorledger;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Builds the small synthetic data set shown to a new user on the first launch.
 *
 * <p>The records are created through the same public data methods used by commands. This keeps the
 * sample subject to the same validation rules as records entered by a tutor, and makes it safe to
 * save the result using the normal storage format.
 */
public final class SampleData {
    private SampleData() {
        // Utility class; do not instantiate.
    }

    /**
     * Returns a fresh sample data set containing records for the main TutorLedger features.
     *
     * @param today the date used to keep the sample useful on any first launch
     * @return new synthetic records that can be modified without affecting another data set
     */
    public static TutorLedgerData create(LocalDate today) {
        Objects.requireNonNull(today, "today");
        TutorLedgerData data = new TutorLedgerData();
        data.addStudent("Amirah Binte Rahman", "Sec 3", "91234567",
                List.of("E Math", "A Math"), "60");
        data.addStudent("Tan Wei Ming", "Sec 4", "98765432", List.of("Physics"), "55");

        data.addLesson("S1", "E Math", Lesson.formatDate(today.minusDays(7)), "1600", "60",
                "present", "Quadratic equations");
        data.updateLessonPaymentStatus("L1", Lesson.PAYMENT_PAID);
        data.addLesson("S2", "Physics", Lesson.formatDate(today.plusDays(2)), "1000", "55");
        data.addHomework("S1", "Complete worksheet 4A", today.plusDays(7));
        data.addScore("S1", "WA1", "28", "40", Lesson.formatDate(today.minusDays(14)));
        data.addTask("S1", "Bring worksheet 4B", today.plusDays(5));
        return data;
    }

    /** Returns sample data relative to the computer's current date for callers without a shared clock. */
    public static TutorLedgerData create() {
        return create(LocalDate.now());
    }
}

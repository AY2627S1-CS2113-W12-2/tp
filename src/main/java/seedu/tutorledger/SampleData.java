package seedu.tutorledger;

import java.time.LocalDate;
import java.util.List;

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
     * @return new synthetic records that can be modified without affecting another data set
     */
    public static TutorLedgerData create() {
        TutorLedgerData data = new TutorLedgerData();
        data.addStudent("Amirah Binte Rahman", "Sec 3", "91234567",
                List.of("E Math", "A Math"), "60");
        data.addStudent("Tan Wei Ming", "Sec 4", "98765432", List.of("Physics"), "55");

        data.addLesson("S1", "E Math", "20-10-2026", "1600", "60",
                "present", "Quadratic equations");
        data.updateLessonPaymentStatus("L1", Lesson.PAYMENT_PAID);
        data.addLesson("S2", "Physics", "05-12-2026", "1000", "55");
        data.addHomework("S1", "Complete worksheet 4A", LocalDate.of(2026, 11, 15));
        data.addScore("S1", "WA1", "28", "40", "15-10-2026");
        data.addTask("S1", "Bring worksheet 4B", LocalDate.of(2026, 11, 10));
        return data;
    }
}

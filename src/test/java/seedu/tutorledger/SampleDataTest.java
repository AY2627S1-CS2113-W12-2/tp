package seedu.tutorledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;

import org.junit.jupiter.api.Test;

/** Verifies that the first-launch data is small, valid and useful for trying the commands. */
class SampleDataTest {
    @Test
    void create_containsLinkedRecordsForMainFeatures() {
        TutorLedgerData data = SampleData.create(LocalDate.of(2026, 11, 2));

        assertEquals(2, data.getStudents().size());
        assertEquals(2, data.getLessons().size());
        assertEquals(1, data.getHomework().size());
        assertEquals(1, data.getScores().size());
        assertEquals(1, data.getTasks().size());
        assertEquals("S1", data.getHomeworkById("H1").getStudentId());
        assertEquals("S1", data.getScores().iterator().next().getStudentId());
        assertEquals("S1", data.getTasks().iterator().next().getStudentId().orElseThrow());
        assertTrue(data.getLesson("L1").isRecorded());
        assertEquals(Lesson.PAYMENT_PAID, data.getLesson("L1").getPaymentStatus());
    }

    @Test
    void create_returnsIndependentDataSets() {
        LocalDate today = LocalDate.of(2026, 11, 2);
        TutorLedgerData first = SampleData.create(today);
        TutorLedgerData second = SampleData.create(today);

        first.deleteStudent("S2");

        assertEquals(2, second.getStudents().size());
        assertEquals(2, second.getLessons().size());
    }

    @Test
    void create_placesRecordsRelativeToToday() {
        LocalDate today = LocalDate.of(2030, 3, 10);
        TutorLedgerData data = SampleData.create(today);

        assertEquals(today.minusDays(1), data.getLesson("L1").getDate());
        assertEquals(today.plusDays(1), data.getLesson("L2").getDate());
        assertEquals(today.plusDays(7), data.getHomeworkById("H1").getDueDate());
    }

    /**
     * The Quick start tells a new user to try {@code lessons week}, which covers Monday to Sunday.
     * Whatever the day of the first launch, at least one sample lesson must fall in that week, the
     * recorded lesson must be in the past, and the upcoming lesson must be after today.
     */
    @Test
    void create_onEveryDayOfTheWeek_keepsASampleLessonInTheCurrentWeek() {
        LocalDate monday = LocalDate.of(2026, 11, 2);
        for (int offset = 0; offset < 7; offset++) {
            LocalDate today = monday.plusDays(offset);
            TutorLedgerData data = SampleData.create(today);
            LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate weekEnd = weekStart.plusDays(6);

            long inWeek = data.getLessons().stream()
                    .filter(lesson -> !lesson.getDate().isBefore(weekStart) && !lesson.getDate().isAfter(weekEnd))
                    .count();
            assertTrue(inWeek >= 1, "No sample lesson in the week of " + today.getDayOfWeek());
            assertTrue(data.getLesson("L1").getDate().isBefore(today), "L1 must already have happened");
            assertTrue(data.getLesson("L2").getDate().isAfter(today), "L2 must still be upcoming");
            assertTrue(data.getHomeworkById("H1").getDueDate().isAfter(today), "H1 must not start overdue");
        }
    }

    /** Runs the Quick start's {@code lessons week} and {@code next} on a Saturday and a Sunday first launch. */
    @Test
    void quickStartCommands_onAWeekendFirstLaunch_showTheSampleLessons() {
        for (String instant : new String[]{"2026-11-07T22:00:00Z", "2026-11-08T22:00:00Z"}) {
            Clock clock = Clock.fixed(Instant.parse(instant), ZoneOffset.UTC);
            TutorLedgerData data = SampleData.create(LocalDate.now(clock));
            LessonCommands lessons = new LessonCommands(data, clock);

            String week = lessons.execute("lessons week");
            String next = lessons.execute("next");

            assertFalse(week.contains(": 0 lessons"), instant + System.lineSeparator() + week);
            assertTrue(next.startsWith("Next lesson: L2"), instant + System.lineSeparator() + next);
        }
    }
}

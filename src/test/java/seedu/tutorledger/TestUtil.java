package seedu.tutorledger;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/** Holds the small helpers that many test classes share, so each test file does not repeat them. */
final class TestUtil {
    /** The line separator that command output uses between lines. */
    static final String NL = System.lineSeparator();

    /** Prevents instantiation, because this class only holds static helpers. */
    private TestUtil() {
    }

    /**
     * Returns a clock stopped at the given moment in UTC, so a test's notion of "today" never depends on the
     * day it is run.
     *
     * @param instant A moment such as {@code "2026-09-21T10:00:00Z"}.
     */
    static Clock clockAt(String instant) {
        return Clock.fixed(Instant.parse(instant), ZoneOffset.UTC);
    }
}

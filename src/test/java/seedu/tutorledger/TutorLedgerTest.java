package seedu.tutorledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

/** Checks the start-up set-up done by the entry point. */
class TutorLedgerTest {
    @Test
    void configureLogging_defaultHandler_showsOnlyWarningsAndErrors() {
        Logger logger = Logger.getAnonymousLogger();
        // A new console handler prints INFO messages, which is what Java's default set-up does.
        ConsoleHandler handler = new ConsoleHandler();
        logger.addHandler(handler);

        TutorLedger.configureLogging(logger);

        assertEquals(Level.WARNING, handler.getLevel());
        assertFalse(handler.isLoggable(new LogRecord(Level.INFO, "Parsing fields")));
        assertTrue(handler.isLoggable(new LogRecord(Level.WARNING, "Could not save")));
    }

    @Test
    void configureLogging_developerConfigFile_leftUntouched() {
        Logger logger = Logger.getAnonymousLogger();
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.FINE);
        logger.addHandler(handler);
        String previous = System.getProperty(TutorLedger.LOGGING_CONFIG_PROPERTY);
        System.setProperty(TutorLedger.LOGGING_CONFIG_PROPERTY, "debug-logging.properties");
        try {
            TutorLedger.configureLogging(logger);
        } finally {
            if (previous == null) {
                System.clearProperty(TutorLedger.LOGGING_CONFIG_PROPERTY);
            } else {
                System.setProperty(TutorLedger.LOGGING_CONFIG_PROPERTY, previous);
            }
        }

        assertEquals(Level.FINE, handler.getLevel());
    }
}

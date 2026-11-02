package seedu.tutorledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Checks session-history boundaries and ensures selection cannot mutate the history it reads. */
class CommandHistoryTest {
    @Test
    void controls_matchOnlyWholeCommandWordsIgnoringCase() {
        assertTrue(CommandHistory.handles("  HISTORY  "));
        assertTrue(CommandHistory.handles("RePeAt 1"));
        for (String input : List.of("", " ", "historyExtra", "repeated 1", "list")) {
            assertFalse(CommandHistory.handles(input), input);
        }
        assertFalse(CommandHistory.handles(null));
    }

    @Test
    void record_preservesAliasesAndInternalSpacingButExcludesControls() {
        CommandHistory history = new CommandHistory();
        assertEquals("No commands in history.", history.describe(""));
        history.record("  fl S1  pay/unpaid  ");
        history.record("unknown");
        for (String input : List.of("", " ", "history", "history extra", "repeat 1", " EXIT ")) {
            history.record(input);
        }
        history.record(null);
        String expected = "Command history (2 commands)" + System.lineSeparator()
                + "  1  fl S1  pay/unpaid" + System.lineSeparator() + "  2  unknown";
        assertEquals(expected, history.describe(""));
        assertEquals("fl S1  pay/unpaid", history.recall("1"));
        assertEquals("unknown", history.recall("02"));
        assertEquals(expected, history.describe(""));
    }

    @Test
    void invalidRequests_preserveEntriesAndRejectOverflow() {
        CommandHistory history = new CommandHistory();
        assertThrows(IllegalArgumentException.class, () -> history.recall("1"));
        history.record("list");
        String before = history.describe("");
        for (String number : List.of("", "0", "-1", "+1", "1.5", "1 extra", "two", "2",
                "99999999999999999999999999999999999")) {
            assertThrows(IllegalArgumentException.class, () -> history.recall(number), number);
            assertEquals(before, history.describe(""));
        }
        assertThrows(IllegalArgumentException.class, () -> history.describe("extra"));
        assertEquals("Command history (1 command)" + System.lineSeparator() + "  1  list", before);
    }
}

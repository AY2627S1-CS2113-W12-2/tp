package seedu.tutorledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

/** Checks the steps every command handler shares: owning input, splitting it and reporting rejections. */
class CommandHandlerTest {
    /** A handler with two words: {@code echo} returns its arguments, {@code fail} is always rejected. */
    private static final class EchoHandler extends CommandHandler {
        private EchoHandler() {
            super(Set.of("echo", "fail"));
        }

        @Override
        protected String run(CommandLine line) {
            if (line.word().equals("fail")) {
                throw new IllegalArgumentException("Bad " + line.arguments() + ".");
            }
            return line.typedWord() + ":" + line.arguments();
        }
    }

    private final EchoHandler handler = new EchoHandler();

    @Test
    void commandLine_splitsTheWordFromItsArguments() {
        CommandHandler.CommandLine line = CommandHandler.CommandLine.of("  EDIT   L3 t/1700  ");

        assertEquals("edit", line.word());
        assertEquals("EDIT", line.typedWord());
        assertEquals("L3 t/1700", line.arguments());
        assertEquals("", CommandHandler.CommandLine.of("   ").word());
        assertEquals("", CommandHandler.CommandLine.of(null).arguments());
    }

    @Test
    void handles_ownWordsInAnyCaseOnly() {
        assertTrue(handler.handles("echo hi"));
        assertTrue(handler.handles("  ECHO"));
        assertFalse(handler.handles("echoes hi"));
        assertFalse(handler.handles("list echo"));
        assertFalse(handler.handles("   "));
        assertFalse(handler.handles(null));
    }

    @Test
    void execute_runsOwnedCommandsAndReportsOthers() {
        assertEquals("Echo:a  b", handler.execute("Echo a  b"));
        assertEquals("echo:", handler.execute("echo"));
        assertEquals("Unknown command.", handler.execute("list"));
        assertEquals("Unknown command.", handler.execute(null));
    }

    @Test
    void execute_rejectedCommand_becomesAnErrorMessage() {
        assertEquals("Error: Bad input.", handler.execute("fail input"));
    }

    @Test
    void isSharedWordWithId_onlyClaimsTheWordFollowedByThatKindOfId() {
        Set<String> shared = Set.of("edit", "delete");

        assertTrue(CommandHandler.isSharedWordWithId("edit L3 t/1700", shared, "L"));
        assertTrue(CommandHandler.isSharedWordWithId("DELETE l44", shared, "L"));
        assertFalse(CommandHandler.isSharedWordWithId("edit S1 n/Tan", shared, "L"));
        // l/Sec 4 is a prefixed value, not a lesson ID.
        assertFalse(CommandHandler.isSharedWordWithId("edit l/Sec 4", shared, "L"));
        assertFalse(CommandHandler.isSharedWordWithId("edit", shared, "L"));
        assertFalse(CommandHandler.isSharedWordWithId("view L3", shared, "L"));
        assertFalse(CommandHandler.isSharedWordWithId(null, shared, "L"));
    }

    @Test
    void shouldSaveAfterwards_onlySaveAndReloadOptOut() {
        TutorLedgerData data = new TutorLedgerData();
        StorageCommands storage = new StorageCommands(data, new Storage("unused.txt"));
        for (CommandHandler each : TutorLedger.createHandlers(data, TestUtil.clockAt("2026-09-21T10:00:00Z"),
                storage)) {
            assertEquals(each != storage, each.shouldSaveAfterwards(), each.getClass().getSimpleName());
        }
    }
}

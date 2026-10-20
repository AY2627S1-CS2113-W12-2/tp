package seedu.tutorledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

/** Checks that aliases expand to their commands and never take over a real command word. */
class CommandAliasesTest {
    @Test
    void expand_alias_replacesOnlyTheFirstWord() {
        assertEquals("lessons week", CommandAliases.expand("ls week"));
        assertEquals("lessons", CommandAliases.expand("ls"));
        assertEquals("view S1", CommandAliases.expand("  V S1  "));
        assertEquals("schedule S1 d/22-09-2026 t/1600 s/A Math f/60",
                CommandAliases.expand("sch S1 d/22-09-2026 t/1600 s/A Math f/60"));
        // An alias appearing later in the line is part of a value, not a command.
        assertEquals("assign S1 hw ls d/29-09-2026", CommandAliases.expand("as S1 hw ls d/29-09-2026"));
    }

    @Test
    void expand_notAnAlias_returnsInputUnchanged() {
        assertEquals("lessons week", CommandAliases.expand("lessons week"));
        assertEquals("lsx", CommandAliases.expand("lsx"));
        assertEquals("   ", CommandAliases.expand("   "));
        assertNull(CommandAliases.expand(null));
    }

    @Test
    void all_noAliasIsAlreadyACommandWord() {
        TutorLedgerData data = new TutorLedgerData();
        StudentCommands studentCommands = new StudentCommands(data,
                Clock.fixed(Instant.parse("2026-09-21T10:00:00Z"), ZoneOffset.UTC));
        for (CommandAliases.Alias alias : CommandAliases.all()) {
            String word = alias.shortForm();
            assertFalse(LessonCommands.handles(word), word);
            assertFalse(PaymentCommands.handles(word), word);
            assertFalse(HomeworkCommands.handles(word), word);
            assertFalse(ScoreCommands.handles(word), word);
            assertFalse(StorageCommands.handles(word), word);
            assertFalse(LessonFilterCommand.handles(word), word);
            assertTrue(studentCommands.execute(word).startsWith("Unknown command"), word);
        }
    }

    @Test
    void all_everyAliasLeadsToARealCommand() {
        for (CommandAliases.Alias alias : CommandAliases.all()) {
            String expanded = CommandAliases.expand(alias.shortForm());
            boolean isHandled = LessonCommands.handles(expanded) || PaymentCommands.handles(expanded)
                    || HomeworkCommands.handles(expanded) || ScoreCommands.handles(expanded)
                    || LessonFilterCommand.handles(expanded)
                    || expanded.equals("view") || expanded.equals("help");
            assertTrue(isHandled, alias.shortForm() + " -> " + expanded);
        }
    }
}

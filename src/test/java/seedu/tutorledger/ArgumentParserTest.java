package seedu.tutorledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Checks how prefixed command arguments are split into values. */
class ArgumentParserTest {
    @Test
    void parseFields_valuesWithSpacesInAnyOrder_keepsEachValueWhole() {
        ArgumentParser.Fields fields = ArgumentParser.parseFields(
                "S/A Math d/22-09-2026 note/Started vectors", List.of("d", "s", "note"));

        assertEquals("A Math", fields.single("s", true));
        assertEquals("22-09-2026", fields.single("d", true));
        assertEquals("Started vectors", fields.single("note", true));
    }

    @Test
    void parseFields_repeatedPrefix_collectsEveryValue() {
        ArgumentParser.Fields fields = ArgumentParser.parseFields("s/E Math s/A Math", List.of("s"));

        assertEquals(List.of("E Math", "A Math"), fields.values("s"));
        assertThrows(IllegalArgumentException.class, () -> fields.single("s", true));
    }

    @Test
    void parseFields_noText_isEmpty() {
        ArgumentParser.Fields fields = ArgumentParser.parseFields("", List.of("s"));

        assertTrue(fields.isEmpty());
        assertFalse(fields.has("s"));
        assertEquals("fallback", fields.singleOr("s", "fallback"));
    }

    @Test
    void parseFields_unknownPrefixOrBareText_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> ArgumentParser.parseFields("x/1", List.of("s")));
        assertThrows(IllegalArgumentException.class,
                () -> ArgumentParser.parseFields("loose s/Math", List.of("s")));
        assertThrows(IllegalArgumentException.class,
                () -> ArgumentParser.parseFields("loose", List.of("s")));
    }

    @Test
    void single_blankValue_isNullUnlessRequired() {
        ArgumentParser.Fields fields = ArgumentParser.parseFields("note/", List.of("note"));

        assertTrue(fields.has("note"));
        assertNull(fields.single("note", false));
        assertThrows(IllegalArgumentException.class, () -> fields.single("note", true));
    }
}

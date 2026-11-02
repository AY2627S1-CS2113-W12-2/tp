package seedu.tutorledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

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

    @Test
    void parseFields_slashWordsInsideFreeText_keptAsText() {
        ArgumentParser.Fields fields = ArgumentParser.parseFields("a/present note/Speed in km/h and/or m/s",
                List.of("a", "note"), Set.of("note"));

        assertEquals("present", fields.single("a", true));
        assertEquals("Speed in km/h and/or m/s", fields.single("note", true));
    }

    @Test
    void parseFields_allowedPrefixAfterFreeText_stillStartsANewValue() {
        ArgumentParser.Fields fields = ArgumentParser.parseFields("note/Graphs and/or tables a/late",
                List.of("a", "note"), Set.of("note"));

        assertEquals("Graphs and/or tables", fields.single("note", true));
        assertEquals("late", fields.single("a", true));
    }

    @Test
    void parseFields_unknownPrefixOutsideFreeText_stillRejected() {
        List<String> allowed = List.of("t", "note");
        // Before any free-text value, and after a value that is not free text, a mistyped prefix is reported.
        IllegalArgumentException beforeNotes = assertThrows(IllegalArgumentException.class,
                () -> ArgumentParser.parseFields("tt/1600 note/Vectors", allowed, Set.of("note")));
        assertEquals("Unexpected prefix tt/.", beforeNotes.getMessage());
        assertThrows(IllegalArgumentException.class,
                () -> ArgumentParser.parseFields("t/1600 tt/1700 note/Vectors", allowed, Set.of("note")));
        // Without free-text prefixes, a slash word in a value is rejected as before.
        assertThrows(IllegalArgumentException.class,
                () -> ArgumentParser.parseFields("note/this and/or that", allowed));
    }

    @Test
    void parseFields_sonOfOrDaughterOfInsideName_keptInTheName() {
        List<String> allowed = List.of("n", "l", "p", "s", "r");
        Set<String> names = Set.of("n");

        ArgumentParser.Fields son = ArgumentParser.parseFields("n/Ravi s/o Kumar l/Sec 1 s/Math", allowed,
                Set.of(), names);
        ArgumentParser.Fields daughter = ArgumentParser.parseFields("n/Priya D/O Raman p/91234567", allowed,
                Set.of(), names);
        ArgumentParser.Fields capitals = ArgumentParser.parseFields("n/RAVI S/O KUMAR s/Math", allowed,
                Set.of(), names);
        ArgumentParser.Fields mixed = ArgumentParser.parseFields("n/Ravi S/o Kumar", allowed, Set.of(), names);

        assertEquals("Ravi s/o Kumar", son.single("n", true));
        assertEquals(List.of("Math"), son.values("s"));
        // The command has no d/ prefix, so d/o is part of the name in any case.
        assertEquals("Priya D/O Raman", daughter.single("n", true));
        assertFalse(daughter.has("d"));
        assertEquals("RAVI S/O KUMAR", capitals.single("n", true));
        assertEquals(List.of("Math"), capitals.values("s"));
        assertEquals("Ravi S/o Kumar", mixed.single("n", true));
    }

    /** A subject such as "O Level Math" right after a name must still be a subject, as before s/o was kept. */
    @Test
    void parseFields_capitalOAfterMixedCaseName_stillStartsASubject() {
        List<String> allowed = List.of("n", "s");
        Set<String> names = Set.of("n");

        ArgumentParser.Fields lower = ArgumentParser.parseFields("n/Ravi s/O Level Math", allowed, Set.of(), names);
        ArgumentParser.Fields upper = ArgumentParser.parseFields("n/Ravi S/O Level Math s/Physics", allowed,
                Set.of(), names);

        assertEquals("Ravi", lower.single("n", true));
        assertEquals("O Level Math", lower.single("s", true));
        assertEquals("Ravi", upper.single("n", true));
        assertEquals(List.of("O Level Math", "Physics"), upper.values("s"));
    }

    /** A name typed in capitals continues in capitals, so "O Level Math" after one is still a subject. */
    @Test
    void parseFields_capitalOAfterNameInCapitals_keptOnlyWhileTheNameStaysInCapitals() {
        List<String> allowed = List.of("n", "l", "s");
        Set<String> names = Set.of("n");

        ArgumentParser.Fields subject = ArgumentParser.parseFields("n/RAVI S/O Level Math l/Sec 4", allowed,
                Set.of(), names);
        ArgumentParser.Fields both = ArgumentParser.parseFields("n/RAVI S/O KUMAR s/O Level Math", allowed,
                Set.of(), names);
        ArgumentParser.Fields twice = ArgumentParser.parseFields("n/RAVI S/O KUMAR S/O LEE s/Math", allowed,
                Set.of(), names);

        assertEquals("RAVI", subject.single("n", true));
        assertEquals("O Level Math", subject.single("s", true));
        assertEquals("Sec 4", subject.single("l", true));
        assertEquals("RAVI S/O KUMAR", both.single("n", true));
        assertEquals("O Level Math", both.single("s", true));
        assertEquals("RAVI S/O KUMAR S/O LEE", twice.single("n", true));
        assertEquals(List.of("Math"), twice.values("s"));
    }

    /**
     * A name cannot end with s/o or d/o. The word stays in the name so that the name check rejects it, rather
     * than the command quietly saving a subject called "O".
     */
    @Test
    void parseFields_nameEndingWithSonOfOrDaughterOf_keepsTheWordInTheName() {
        List<String> allowed = List.of("n", "l", "s");
        Set<String> names = Set.of("n");
        String[][] cases = {
            {"n/Ravi s/o", "Ravi s/o"},
            {"n/Ravi s/o l/Sec 1", "Ravi s/o"},
            {"n/Ravi s/o s/Math", "Ravi s/o"},
            {"n/RAVI S/O", "RAVI S/O"},
            {"n/RAVI S/O l/Sec 1", "RAVI S/O"},
            {"n/Priya d/o", "Priya d/o"},
            {"n/Priya D/O l/Sec 1", "Priya D/O"},
            {"n/PRIYA D/O", "PRIYA D/O"},
        };

        for (String[] each : cases) {
            ArgumentParser.Fields fields = ArgumentParser.parseFields(each[0], allowed, Set.of(), names);
            assertEquals(each[1], fields.single("n", true), each[0]);
            assertTrue(fields.values("s").stream().noneMatch(value -> value.equalsIgnoreCase("o")), each[0]);
        }
    }

    @Test
    void parseFields_connectorsNextToOtherPrefixes_splitOnlyAtTheRealPrefixes() {
        List<String> allowed = List.of("n", "l", "p", "s");
        Set<String> names = Set.of("n");

        ArgumentParser.Fields middle = ArgumentParser.parseFields("l/Sec 1 n/Ravi s/o Kumar p/91234567", allowed,
                Set.of(), names);
        ArgumentParser.Fields chained = ArgumentParser.parseFields("n/Priya d/o Raman s/o Kumar s/Math", allowed,
                Set.of(), names);
        ArgumentParser.Fields thenSubject = ArgumentParser.parseFields("n/Ravi s/o Kumar S/O Level Math",
                allowed, Set.of(), names);
        // Without a space after it, s/o is not the word "s/o" but the start of a subject.
        ArgumentParser.Fields glued = ArgumentParser.parseFields("n/Ravi s/oKumar", allowed, Set.of(), names);
        // When the command also has a d/ prefix, D/O after a mixed-case name is that prefix, as with s/O.
        ArgumentParser.Fields withDate = ArgumentParser.parseFields("n/Priya d/o Raman d/01-10-2026 n/Tan D/O 1",
                List.of("n", "d"), Set.of(), names);

        assertEquals("Ravi s/o Kumar", middle.single("n", true));
        assertEquals("Sec 1", middle.single("l", true));
        assertEquals("91234567", middle.single("p", true));
        assertEquals("Priya d/o Raman s/o Kumar", chained.single("n", true));
        assertEquals(List.of("Math"), chained.values("s"));
        assertEquals("Ravi s/o Kumar", thenSubject.single("n", true));
        assertEquals("O Level Math", thenSubject.single("s", true));
        assertEquals("Ravi", glued.single("n", true));
        assertEquals("oKumar", glued.single("s", true));
        assertEquals(List.of("Priya d/o Raman", "Tan"), withDate.values("n"));
        assertEquals(List.of("01-10-2026", "O 1"), withDate.values("d"));
    }

    @Test
    void parseFields_sonOfOutsideName_stillStartsASubject() {
        List<String> allowed = List.of("n", "s");

        ArgumentParser.Fields before = ArgumentParser.parseFields("s/O Level Math n/Ravi", allowed, Set.of(),
                Set.of("n"));
        ArgumentParser.Fields joined = ArgumentParser.parseFields("n/Ravi s/oral English", allowed, Set.of(),
                Set.of("n"));
        // n/ is only a name when the command says so; the parser does not assume it.
        ArgumentParser.Fields notName = ArgumentParser.parseFields("n/Ravi s/o Kumar", allowed);

        assertEquals("O Level Math", before.single("s", true));
        assertEquals("Ravi", before.single("n", true));
        assertEquals("Ravi", joined.single("n", true));
        assertEquals("oral English", joined.single("s", true));
        assertEquals("Ravi", notName.single("n", true));
        assertEquals("o Kumar", notName.single("s", true));
    }
}

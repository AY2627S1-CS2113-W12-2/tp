package seedu.tutorledger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Splits command arguments written as prefixed values, such as {@code n/Tan Wei Ming l/Sec 3}.
 *
 * <p>The student and lesson commands share this parser, so a prefix such as {@code s/} is read
 * the same way by every command that accepts it.
 */
final class ArgumentParser {
    private static final Logger LOGGER = Logger.getLogger(ArgumentParser.class.getName());

    /** A prefix is a run of letters followed by a slash, at the start of the text or after a space. */
    private static final Pattern PREFIX = Pattern.compile("(?i)(?<!\\S)([a-z]+)/");

    /** Prevents instantiation, because this class only holds a static parsing method. */
    private ArgumentParser() {
    }

    /**
     * Separates prefixed values without splitting names or subjects containing spaces.
     *
     * @param text The part of the command that holds the prefixed values.
     * @param allowed The prefixes this command accepts, in lower case and without the slash.
     * @return The values found, grouped by prefix.
     * @throws IllegalArgumentException If the text has an unexpected prefix or a value with no prefix.
     */
    static Fields parseFields(String text, List<String> allowed) {
        return parseFields(text, allowed, Set.of());
    }

    /**
     * Separates prefixed values, letting free-text values such as notes contain words like "and/or".
     *
     * <p>Inside the value of a free-text prefix, a word ending in a slash that this command does not
     * accept, such as {@code and/} or {@code km/}, is kept as part of the text instead of being rejected.
     * A prefix the command does accept still starts a new value, so {@code note/Revision a/present}
     * records the attendance as usual. Outside free-text values, an unknown prefix is still rejected, so
     * a mistyped prefix such as {@code tt/1600} is reported rather than silently absorbed.
     *
     * @param text The part of the command that holds the prefixed values.
     * @param allowed The prefixes this command accepts, in lower case and without the slash.
     * @param freeText The allowed prefixes whose values are free text.
     * @return The values found, grouped by prefix.
     * @throws IllegalArgumentException If the text has an unexpected prefix or a value with no prefix.
     */
    static Fields parseFields(String text, List<String> allowed, Set<String> freeText) {
        return parseFields(text, allowed, freeText, Set.of());
    }

    /**
     * Separates prefixed values, also keeping the words "s/o" and "d/o" inside the values of name prefixes.
     *
     * <p>Inside a name, {@code d/o} is always part of the name when the command has no {@code d/} prefix.
     * {@code s/o} is harder, because {@code s/O Level Math} is a subject. It is kept in the name only when
     * written with a lower-case "o", which no subject starts with, or when the name is in capitals on both
     * sides of it, as in {@code RAVI S/O KUMAR}. Otherwise it starts a new value, as it did before names could
     * hold it, so {@code n/RAVI S/O Level Math} is RAVI with the subject O Level Math.
     *
     * @param text The part of the command that holds the prefixed values.
     * @param allowed The prefixes this command accepts, in lower case and without the slash.
     * @param freeText The allowed prefixes whose values are free text.
     * @param names The allowed prefixes whose values are people's names.
     * @return The values found, grouped by prefix.
     * @throws IllegalArgumentException If the text has an unexpected prefix or a value with no prefix.
     */
    static Fields parseFields(String text, List<String> allowed, Set<String> freeText, Set<String> names) {
        LOGGER.log(Level.FINE, "Parsing fields: text={0}, allowed={1}", new Object[]{text, allowed});
        assert text != null : "Callers pass an empty string, not null, when there are no values";
        // Matched keys are lower-cased letters, so an allowed prefix in any other form could never match.
        assert allowed != null && allowed.stream().allMatch(prefix -> prefix.matches("[a-z]+"))
                : "Allowed prefixes must be lower-case letters without the slash: " + allowed;
        assert freeText != null && allowed.containsAll(freeText)
                : "Free-text prefixes must also be allowed: " + freeText;
        assert names != null && allowed.containsAll(names) : "Name prefixes must also be allowed: " + names;
        Map<String, List<String>> values = new HashMap<>();
        Matcher matcher = PREFIX.matcher(text);
        int previousEnd = 0;
        String previousKey = null;
        while (matcher.find()) {
            if (previousKey == null && !text.substring(0, matcher.start()).isBlank()) {
                throw new IllegalArgumentException("Expected a prefixed value.");
            }
            String key = matcher.group(1).toLowerCase(Locale.ROOT);
            // Without this, "n/Ravi s/o Kumar" would store the name "Ravi" and a subject "o Kumar".
            if (previousKey != null && names.contains(previousKey)
                    && isPartOfName(text, previousEnd, matcher.start(), allowed.contains(key))) {
                LOGGER.log(Level.FINE, "Keeping {0}/o as part of the name", key);
                continue;
            }
            // This is checked before the previous value is stored, because that value has not ended yet.
            // previousEnd is left alone, so the word stays inside the free-text value being read.
            if (!allowed.contains(key) && previousKey != null && freeText.contains(previousKey)) {
                LOGGER.log(Level.FINE, "Keeping {0}/ as text inside {1}/", new Object[]{key, previousKey});
                continue;
            }
            if (previousKey != null) {
                values.get(previousKey).add(text.substring(previousEnd, matcher.start()).trim());
            }
            if (!allowed.contains(key)) {
                throw new IllegalArgumentException("Unexpected prefix " + key + "/.");
            }
            values.computeIfAbsent(key, ignored -> new ArrayList<>());
            previousKey = key;
            previousEnd = matcher.end();
        }
        if (previousKey == null) {
            if (!text.isBlank()) {
                throw new IllegalArgumentException("Expected a prefixed value.");
            }
        } else {
            values.get(previousKey).add(text.substring(previousEnd).trim());
        }
        // Every prefix found is followed by a value (possibly blank), which single() relies on.
        assert values.values().stream().noneMatch(List::isEmpty) : "A prefix was recorded without its value";
        LOGGER.log(Level.FINE, "Parsed fields result: {0}", values);
        return new Fields(values);
    }

    /**
     * Returns whether the "s/o" or "d/o" word at {@code start} belongs to the name that began at
     * {@code nameStart}, rather than starting a new value.
     *
     * @param isAllowedPrefix Whether the word's letter is also a prefix this command accepts.
     */
    private static boolean isPartOfName(String text, int nameStart, int start, boolean isAllowedPrefix) {
        if (!Student.isNameConnectorAt(text, start)) {
            return false;
        }
        if (!isAllowedPrefix) {
            return true;
        }
        if (text.charAt(start + 2) == 'o') {
            return true;
        }
        // A name typed in capitals stays in capitals, as in RAVI S/O KUMAR, while a subject such as
        // "O Level Math" does not. So the whole name, up to the next prefix, must be in capitals.
        Matcher nextPrefix = PREFIX.matcher(text);
        int nameEnd = nextPrefix.find(start + 2) ? nextPrefix.start() : text.length();
        String name = text.substring(nameStart, nameEnd);
        return name.equals(name.toUpperCase(Locale.ROOT));
    }

    /** The values typed for each prefix, keyed by the prefix in lower case. */
    record Fields(Map<String, List<String>> fields) {
        boolean isEmpty() {
            return fields.isEmpty();
        }

        boolean has(String key) {
            return fields.containsKey(key);
        }

        List<String> values(String key) {
            return fields.getOrDefault(key, List.of());
        }

        /**
         * Returns the one value given for a prefix, or null if it is optional and was left out or blank.
         *
         * @throws IllegalArgumentException If the prefix is repeated, or is required but missing or blank.
         */
        String single(String key, boolean isRequired) {
            LOGGER.log(Level.FINE, "Resolving single field for key {0} (required={1})", new Object[]{key, isRequired});
            List<String> entries = values(key);
            if (entries.size() > 1) {
                throw new IllegalArgumentException("Give " + key + "/ only once.");
            }
            if (entries.isEmpty() || entries.getFirst().isBlank()) {
                if (isRequired) {
                    throw new IllegalArgumentException("Give a non-blank " + key + "/ value.");
                }
                return null;
            }
            return entries.getFirst();
        }

        String singleOr(String key, String fallback) {
            LOGGER.log(Level.FINE, "Resolving optional field {0} with fallback {1}", new Object[]{key, fallback});
            return has(key) ? single(key, true) : fallback;
        }
    }
}

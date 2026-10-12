package seedu.tutorledger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
        LOGGER.log(Level.INFO, "Parsing fields: text={0}, allowed={1}", new Object[]{text, allowed});
        Map<String, List<String>> values = new HashMap<>();
        Matcher matcher = PREFIX.matcher(text);
        int previousEnd = 0;
        String previousKey = null;
        while (matcher.find()) {
            if (previousKey == null && !text.substring(0, matcher.start()).isBlank()) {
                throw new IllegalArgumentException("Expected a prefixed value.");
            }
            if (previousKey != null) {
                values.get(previousKey).add(text.substring(previousEnd, matcher.start()).trim());
            }
            String key = matcher.group(1).toLowerCase(Locale.ROOT);
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
        LOGGER.log(Level.INFO, "Parsed fields result: {0}", values);
        return new Fields(values);
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
        String single(String key, boolean required) {
            LOGGER.log(Level.FINE, "Resolving single field for key {0} (required={1})", new Object[]{key, required});
            List<String> entries = values(key);
            if (entries.size() > 1) {
                throw new IllegalArgumentException("Give " + key + "/ only once.");
            }
            if (entries.isEmpty() || entries.getFirst().isBlank()) {
                if (required) {
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

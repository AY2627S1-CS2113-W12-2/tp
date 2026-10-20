package seedu.tutorledger;

import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Expands short aliases for the most-used commands into their full command words (story US39).
 *
 * <p>An alias only ever replaces the first word of the input, so {@code ls week} becomes
 * {@code lessons week} and everything after the alias is passed on unchanged. Expanding the alias
 * once, before the input is routed to a feature handler, means no handler needs to know aliases exist.
 *
 * <p>A simpler alternative would be to add each alias to the command words of its handler, but that
 * would spread the aliases across several classes owned by different people. Keeping them in one list
 * also lets {@link HelpCommand} show them all without repeating them.
 */
final class CommandAliases {
    private static final Logger LOGGER = Logger.getLogger(CommandAliases.class.getName());

    /** Every alias with the command it stands for, in the order they are shown by {@code help}. */
    private static final List<Alias> ALIASES = List.of(
            new Alias("v", "view"),
            new Alias("sch", "schedule"),
            new Alias("ls", "lessons"),
            new Alias("rec", "record"),
            new Alias("fl", "filter"),
            new Alias("as", "assign"),
            new Alias("sub", "submit"),
            new Alias("hw", "homework"),
            new Alias("m", "mark"),
            new Alias("up", "unpaid"),
            new Alias("h", "help"));

    /** Prevents instantiation, because this class only holds the static alias list. */
    private CommandAliases() {
    }

    /**
     * Returns the input with a leading alias replaced by its full command word.
     * Input that does not start with an alias is returned unchanged.
     *
     * @param input One line typed by the user; may be null or blank.
     */
    static String expand(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }
        String[] words = input.strip().split("\\s+", 2);
        String firstWord = words[0].toLowerCase(Locale.ROOT);
        for (Alias alias : ALIASES) {
            if (alias.shortForm().equals(firstWord)) {
                String expanded = words.length == 2 ? alias.command() + " " + words[1] : alias.command();
                LOGGER.log(Level.FINE, "Expanded alias {0} to {1}", new Object[]{firstWord, alias.command()});
                return expanded;
            }
        }
        return input;
    }

    /** Returns every alias, in the order they should be listed. */
    static List<Alias> all() {
        return ALIASES;
    }

    /**
     * One short form and the full command word it expands to.
     *
     * @param shortForm The alias the user types, in lower case.
     * @param command The full command word it stands for.
     */
    record Alias(String shortForm, String command) {
        Alias {
            assert shortForm.matches("[a-z]+") && command.matches("[a-z_]+")
                    : "Aliases and commands must be single lower-case words: " + shortForm + " -> " + command;
            assert shortForm.length() < command.length() : "An alias should be shorter than its command";
        }
    }
}

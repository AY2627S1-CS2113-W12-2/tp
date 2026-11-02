package seedu.tutorledger;

import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The shared shape of every feature's command handler, such as {@link LessonCommands} or {@link PaymentCommands}.
 *
 * <p>A handler says which input it owns ({@link #handles}), and {@link #execute} does the steps every command
 * shares: splitting off the command word, tracing the command, and turning a rejected command into an
 * {@code Error: ...} message. A subclass only maps its command words to the methods that carry them out, in
 * {@link #run}. The main loop keeps the handlers in a list and gives each line to the first one that owns it.
 *
 * <p>A simpler alternative is for each class to repeat these steps itself, which is how they were first written.
 * That left eight copies of the same split, check and error handling, which had to be kept in step by hand.
 */
abstract class CommandHandler {
    /** The command words this handler owns, in lower case. */
    private final Set<String> commandWords;

    /** Traces commands under the subclass's own name, so its log messages can still be switched on alone. */
    private final Logger logger = Logger.getLogger(getClass().getName());

    /**
     * Creates a handler for the given command words.
     *
     * @param commandWords The first words this handler owns, in lower case.
     */
    protected CommandHandler(Set<String> commandWords) {
        assert commandWords.stream().allMatch(word -> word.equals(word.toLowerCase(Locale.ROOT)))
                : "Command words must be lower case: " + commandWords;
        this.commandWords = commandWords;
    }

    /**
     * Returns true if this handler owns the input. By default, that is when its first word, in any case, is one
     * of this handler's command words. Handlers that share a word such as {@code edit} narrow this further.
     *
     * @param input One line typed by the user, after aliases are expanded; may be null or blank.
     */
    boolean handles(String input) {
        return input != null && !input.isBlank() && commandWords.contains(CommandLine.of(input).word());
    }

    /**
     * Returns true if the input starts with one of the shared words, such as {@code edit}, followed by an ID
     * with the given letter, such as {@code L3}. Students, lessons and homework all have {@code edit} and
     * {@code delete}, so the ID decides which handler owns the command.
     *
     * @param input One line typed by the user; may be null or blank.
     * @param sharedWords The shared command words, in lower case.
     * @param idLetter The letter that starts this handler's IDs.
     */
    static boolean isSharedWordWithId(String input, Set<String> sharedWords, String idLetter) {
        if (input == null || input.isBlank()) {
            return false;
        }
        String[] words = input.strip().split("\\s+", 3);
        // "[^/]*" keeps a prefixed value such as l/Sec 4 from being mistaken for an ID.
        return sharedWords.contains(words[0].toLowerCase(Locale.ROOT)) && words.length > 1
                && words[1].matches("(?i)" + idLetter + "[^/]*");
    }

    /**
     * Runs one command and returns the text to show, including an {@code Error: ...} message if the command is
     * rejected. A rejected command changes nothing, because each command validates before it changes records.
     *
     * @param input One line typed by the user, after aliases are expanded.
     */
    final String execute(String input) {
        if (!handles(input)) {
            return "Unknown command.";
        }
        CommandLine line = CommandLine.of(input);
        logger.log(Level.FINE, "Running {0} with arguments: {1}", new Object[]{line.word(), line.arguments()});
        try {
            return run(line);
        } catch (IllegalArgumentException exception) {
            // A rejected command is the user's mistake and is already explained on screen, so it is only traced.
            logger.log(Level.FINE, "Rejected {0}: {1}", new Object[]{line.word(), exception.getMessage()});
            return "Error: " + exception.getMessage();
        }
    }

    /**
     * Carries out one command that this handler owns.
     *
     * @param line The command word and its arguments.
     * @return The text to show the user.
     * @throws IllegalArgumentException If the command is invalid; {@link #execute} shows its message.
     */
    protected abstract String run(CommandLine line);

    /**
     * Returns true if the main loop should save the data file after this handler's commands when they change
     * records. Only {@code save} and {@code reload}, which manage the file themselves, return false.
     */
    boolean shouldSaveAfterwards() {
        return true;
    }

    /**
     * One line of input, split into its command word and the text after it.
     *
     * @param word The first word in lower case, used to choose the command; empty for blank input.
     * @param typedWord The first word as the user typed it, for messages that repeat it.
     * @param arguments Everything after the first word, without surrounding whitespace.
     */
    record CommandLine(String word, String typedWord, String arguments) {
        /** Splits a line typed by the user; blank input gives an empty word and no arguments. */
        static CommandLine of(String input) {
            String[] words = input == null ? new String[]{""} : input.strip().split("\\s+", 2);
            String arguments = words.length == 2 ? words[1].trim() : "";
            return new CommandLine(words[0].toLowerCase(Locale.ROOT), words[0], arguments);
        }
    }
}

package seedu.tutorledger;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Keeps this session's commands so a tutor can re-run a query after updating records.
 * History is separate from tutoring data and is never written to the data file.
 */
final class CommandHistory {
    private static final String REPEAT_USAGE = "Use: repeat NUMBER (a positive history number).";
    /** Original command text, in execution order; list position plus one is its stable history number. */
    private final List<String> commands = new ArrayList<>();

    /** Returns whether the first word requests history or confirmed replay. */
    static boolean handles(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }
        String word = input.strip().split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        return word.equals("history") || word.equals("repeat");
    }

    /** Records ordinary commands, including failed ones, without recording controls or confirmation replies. */
    void record(String input) {
        if (input == null || input.isBlank() || handles(input) || input.strip().equalsIgnoreCase("exit")) {
            return;
        }
        commands.add(input.strip());
    }

    /** Lists entries without changing them; history takes no arguments. */
    String describe(String arguments) {
        if (!arguments.isBlank()) {
            throw new IllegalArgumentException("Use: history.");
        }
        if (commands.isEmpty()) {
            return "No commands in history.";
        }
        StringBuilder text = new StringBuilder("Command history (" + commands.size()
                + (commands.size() == 1 ? " command)" : " commands)"));
        for (int index = 0; index < commands.size(); index++) {
            text.append(System.lineSeparator()).append("  ").append(index + 1).append("  ")
                    .append(commands.get(index));
        }
        return text.toString();
    }

    /**
     * Resolves a one-based history number without recording or executing the selected command.
     *
     * @throws IllegalArgumentException If the number is malformed or does not identify an entry.
     */
    String recall(String arguments) {
        int number;
        try {
            if (!arguments.matches("[0-9]+")) {
                throw new NumberFormatException();
            }
            number = Integer.parseInt(arguments);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(REPEAT_USAGE);
        }
        if (number < 1) {
            throw new IllegalArgumentException(REPEAT_USAGE);
        }
        if (commands.isEmpty()) {
            throw new IllegalArgumentException("No commands in history.");
        }
        if (number > commands.size()) {
            throw new IllegalArgumentException("History number must be between 1 and " + commands.size() + ".");
        }
        return commands.get(number - 1);
    }
}

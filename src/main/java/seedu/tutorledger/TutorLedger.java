package seedu.tutorledger;

import java.time.Clock;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Command-line entry point for the student book. */
public class TutorLedger {
    /** System property that names a logging configuration file, as defined by java.util.logging. */
    static final String LOGGING_CONFIG_PROPERTY = "java.util.logging.config.file";

    /**
     * Keeps routine log messages off the tutor's screen.
     *
     * <p>By default Java prints every INFO message to the terminal, so each command would be followed by
     * lines of log text. Only warnings and errors are shown instead. A developer who wants the full trace
     * can still start the app with {@code -Djava.util.logging.config.file=FILE}, which is left untouched.
     *
     * @param rootLogger The logger every other logger passes its messages up to.
     */
    static void configureLogging(Logger rootLogger) {
        assert rootLogger != null : "The root logger is needed to configure logging";
        if (System.getProperty(LOGGING_CONFIG_PROPERTY) != null) {
            return;
        }
        for (Handler handler : rootLogger.getHandlers()) {
            handler.setLevel(Level.WARNING);
        }
    }

    /**
     * Loads the saved data, then reads commands until the user exits or standard input closes.
     * Any command that changes data is saved straight away, so nothing needs saving on exit.
     */
    public static void main(String[] args) {
        run(new Storage(Storage.DEFAULT_FILE_PATH), true);
    }

    /**
     * Runs the command loop against the given data file.
     * {@link #main} always uses the default file; tests pass a temporary one instead, so running the
     * tests never reads or overwrites the records a developer keeps in {@code data/}.
     *
     * @param storage The data file to load at start-up and save to after each change.
     */
    static void run(Storage storage) {
        run(storage, false);
    }

    /** Runs the command loop, optionally creating first-launch sample records when storage is absent. */
    private static void run(Storage storage, boolean shouldSeedSampleData) {
        assert storage != null : "The command loop needs a data file to load and save";
        configureLogging(Logger.getLogger(""));
        TutorLedgerData data = new TutorLedgerData();
        Clock clock = Clock.systemDefaultZone();
        StorageCommands storageCommands = new StorageCommands(data, storage, shouldSeedSampleData, clock);
        List<CommandHandler> handlers = createHandlers(data, clock, storageCommands);
        CommandHistory history = new CommandHistory();
        System.out.println("TutorLedger student book. Type exit to close.");
        System.out.println(storageCommands.loadOnStartup());
        Scanner input = new Scanner(System.in);
        while (input.hasNextLine()) {
            String selected = selectCommand(input.nextLine(), history, input);
            if (selected == null) {
                continue;
            }
            // Recalled commands use the same alias expansion and validation as freshly typed commands.
            String line = CommandAliases.expand(selected);
            if (line.strip().equalsIgnoreCase("exit")) {
                break;
            }
            history.record(selected);
            runCommand(line, handlers, storageCommands);
        }
    }

    /**
     * Creates every command handler, in the order they are offered input. Each handler owns different command
     * words, except that {@code edit} and {@code delete} go to the lesson or homework handler only for an L or H
     * ID. The student handler is last, because it takes everything else, including unknown words.
     */
    static List<CommandHandler> createHandlers(TutorLedgerData data, Clock clock,
            StorageCommands storageCommands) {
        return List.of(storageCommands,
                new PaymentCommands(data, clock),
                new HomeworkCommands(data, clock),
                new LessonCommands(data, clock),
                new TopicCommands(data, clock),
                new ScoreCommands(data, clock),
                new TaskCommands(data, clock),
                new LessonFilterCommand(data),
                new StudentCommands(data, clock));
    }

    /**
     * Gives one command to the first handler that owns it, prints the result, and saves the data file if the
     * command changed any records.
     */
    private static void runCommand(String line, List<CommandHandler> handlers, StorageCommands storageCommands) {
        CommandHandler handler = handlers.stream()
                .filter(candidate -> candidate.handles(line))
                .findFirst()
                .orElseThrow(() -> new AssertionError("The student handler accepts every command: " + line));
        // save and reload manage the file themselves, so they are not followed by an automatic save.
        // In particular, reload must not save, or lines it skipped would be erased before they are fixed.
        if (!handler.shouldSaveAfterwards()) {
            System.out.println(handler.execute(line));
            return;
        }
        String before = storageCommands.snapshot();
        System.out.println(handler.execute(line));
        String saveWarning = storageCommands.saveIfChanged(before);
        if (!saveWarning.isEmpty()) {
            System.out.println(saveWarning);
        }
    }

    /** Returns ordinary input or a confirmed command; null means a history request was handled or cancelled. */
    private static String selectCommand(String line, CommandHistory history, Scanner input) {
        if (!CommandHistory.handles(line)) {
            return line;
        }
        String[] words = line.strip().split("\\s+", 2);
        String arguments = words.length == 2 ? words[1] : "";
        try {
            if (words[0].equalsIgnoreCase("history")) {
                System.out.println(history.describe(arguments));
                return null;
            }
            String selected = history.recall(arguments);
            System.out.println("Selected command: " + selected);
            if (!isRepeatConfirmed(input)) {
                System.out.println("Repeat cancelled.");
                return null;
            }
            return selected;
        } catch (IllegalArgumentException exception) {
            System.out.println("Error: " + exception.getMessage());
            return null;
        }
    }

    /** Reads confirmation only; replies never enter command dispatch or history. End-of-input cancels. */
    private static boolean isRepeatConfirmed(Scanner input) {
        while (true) {
            System.out.println("Run this command? (yes/no):");
            if (!input.hasNextLine()) {
                return false;
            }
            String answer = input.nextLine().strip();
            if (answer.equalsIgnoreCase("yes") || answer.equalsIgnoreCase("y")) {
                return true;
            }
            if (answer.equalsIgnoreCase("no") || answer.equalsIgnoreCase("n")) {
                return false;
            }
            System.out.println("Please enter yes or no.");
        }
    }
}

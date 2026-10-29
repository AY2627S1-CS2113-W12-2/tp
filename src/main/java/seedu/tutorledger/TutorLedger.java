package seedu.tutorledger;

import java.time.Clock;
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
        run(new Storage(Storage.DEFAULT_FILE_PATH));
    }

    /**
     * Runs the command loop against the given data file.
     * {@link #main} always uses the default file; tests pass a temporary one instead, so running the
     * tests never reads or overwrites the records a developer keeps in {@code data/}.
     *
     * @param storage The data file to load at start-up and save to after each change.
     */
    static void run(Storage storage) {
        assert storage != null : "The command loop needs a data file to load and save";
        configureLogging(Logger.getLogger(""));
        TutorLedgerData data = new TutorLedgerData();
        Clock clock = Clock.systemDefaultZone();
        StudentCommands commands = new StudentCommands(data, clock);
        HomeworkCommands homeworkCommands = new HomeworkCommands(data, clock);
        PaymentCommands paymentCommands = new PaymentCommands(data, clock);
        LessonCommands lessonCommands = new LessonCommands(data, clock);
        TopicCommands topicCommands = new TopicCommands(data, clock);
        ScoreCommands scoreCommands = new ScoreCommands(data, clock);
        TaskCommands taskCommands = new TaskCommands(data, clock);
        LessonFilterCommand filterCommand = new LessonFilterCommand(data);
        StorageCommands storageCommands = new StorageCommands(data, storage);
        System.out.println("TutorLedger student book. Type exit to close.");
        System.out.println(storageCommands.loadOnStartup());
        Scanner input = new Scanner(System.in);
        while (input.hasNextLine()) {
            // Short aliases such as "ls" are turned into full command words before anything else sees them.
            String line = CommandAliases.expand(input.nextLine());
            if (line.strip().equalsIgnoreCase("exit")) {
                break;
            }
            // save and reload manage the file themselves, so they are not followed by an automatic save.
            // In particular, reload must not save, or lines it skipped would be erased before they are fixed.
            if (StorageCommands.handles(line)) {
                System.out.println(storageCommands.execute(line));
                continue;
            }
            String before = storageCommands.snapshot();
            // Each feature handler receives only the commands it owns.
            if (PaymentCommands.handles(line)) {
                System.out.println(paymentCommands.execute(line));
            } else if (HomeworkCommands.handles(line)) {
                System.out.println(homeworkCommands.execute(line));
            } else if (LessonCommands.handles(line)) {
                System.out.println(lessonCommands.execute(line));
            } else if (TopicCommands.handles(line)) {
                System.out.println(topicCommands.execute(line));
            } else if (ScoreCommands.handles(line)) {
                System.out.println(scoreCommands.execute(line));
            } else if (TaskCommands.handles(line)) {
                System.out.println(taskCommands.execute(line));
            } else if (LessonFilterCommand.handles(line)) {
                System.out.println(filterCommand.execute(line));
            } else {
                System.out.println(commands.execute(line));
            }
            String saveWarning = storageCommands.saveIfChanged(before);
            if (!saveWarning.isEmpty()) {
                System.out.println(saveWarning);
            }
        }
    }
}

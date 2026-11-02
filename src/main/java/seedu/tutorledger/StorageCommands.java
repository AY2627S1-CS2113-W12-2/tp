package seedu.tutorledger;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Runs the storage commands {@code save} and {@code reload}, and saves automatically after any
 * other command that changes data (features F5.1 and F5.2).
 *
 * <p>Like the other command classes, each method returns the text to show, and problems are
 * reported as messages instead of crashing the program.
 */
public class StorageCommands {
    /** Traces storage commands and file errors at FINE level, shown only when debug logging is enabled. */
    private static final Logger LOGGER = Logger.getLogger(StorageCommands.class.getName());

    /** Command words this class understands, used by the main loop to route input here. */
    private static final Set<String> COMMAND_WORDS = Set.of("save", "reload");

    private final TutorLedgerData data;
    private final Storage storage;
    private final boolean shouldSeedSampleData;
    private final Clock clock;
    /** Prevents a later data-changing command from overwriting a file that could not be read. */
    private boolean isAutomaticSavingEnabled = true;

    /**
     * Creates the handler for the storage commands.
     *
     * @param data The records to save, and to replace on reload.
     * @param storage The data file that is saved to automatically.
     */
    public StorageCommands(TutorLedgerData data, Storage storage) {
        this(data, storage, false);
    }

    /**
     * Creates the handler for the storage commands.
     *
     * @param data The records to save, and to replace on reload.
     * @param storage The data file that is saved to automatically.
     * @param seedSampleData Whether a missing file should be populated with first-launch sample records.
     */
    public StorageCommands(TutorLedgerData data, Storage storage, boolean shouldSeedSampleData) {
        this(data, storage, shouldSeedSampleData, Clock.systemDefaultZone());
    }

    /**
     * Creates the handler with the clock used by the other command handlers.
     *
     * @param data The records to save, and to replace on reload.
     * @param storage The data file that is saved to automatically.
     * @param seedSampleData Whether a missing file should be populated with first-launch sample records.
     * @param clock Source of today's date for relative first-launch sample records.
     */
    public StorageCommands(TutorLedgerData data, Storage storage, boolean shouldSeedSampleData, Clock clock) {
        assert data != null : "StorageCommands needs the records to save";
        assert storage != null : "StorageCommands needs a data file to save to";
        assert clock != null : "StorageCommands needs a clock for sample data";
        this.data = data;
        this.storage = storage;
        this.shouldSeedSampleData = shouldSeedSampleData;
        this.clock = clock;
    }

    /** Returns true if the first word of the input is a storage command. */
    public static boolean handles(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }
        String command = input.strip().split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        return COMMAND_WORDS.contains(command);
    }

    /** Executes one storage command and returns its display text. */
    public String execute(String input) {
        if (!handles(input)) {
            return "Unknown storage command.";
        }
        String[] words = input.strip().split("\\s+", 2);
        String command = words[0].toLowerCase(Locale.ROOT);
        String arguments = words.length == 2 ? words[1].trim() : "";
        LOGGER.log(Level.FINE, "Running storage command {0} with arguments: {1}", new Object[]{command, arguments});
        // Words typed after reload are ignored, as the user guide says.
        return command.equals("save") ? save(arguments) : reload();
    }

    /**
     * Loads the data file when TutorLedger starts, and returns a message describing what happened.
     * If first-launch seeding is enabled and the file does not exist yet, a small synthetic data set is
     * saved and used instead, so a new user can try the commands immediately. An existing file, including
     * an intentionally empty one, is always treated as the user's source of truth.
     */
    public String loadOnStartup() {
        try {
            Storage.LoadResult result = storage.load();
            data.replaceAllWith(result.data());
            isAutomaticSavingEnabled = true;
            return "Loaded " + describeContents(data) + " from " + storage.getDisplayPath() + "."
                    + describeProblems(result.problems());
        } catch (NoSuchFileException exception) {
            isAutomaticSavingEnabled = true;
            if (!shouldSeedSampleData) {
                LOGGER.log(Level.FINE, "No data file at startup; starting empty");
                return "No saved data yet. TutorLedger will create " + storage.getDisplayPath()
                        + " when you add something.";
            }
            LOGGER.log(Level.FINE, "No data file at startup; loading sample data");
            data.replaceAllWith(SampleData.create(LocalDate.now(clock)));
            try {
                storage.save(data);
                return "No saved data yet. Loaded sample data (" + describeContents(data) + ") and saved it to "
                        + storage.getDisplayPath() + ".";
            } catch (IOException saveException) {
                LOGGER.log(Level.FINE, "Could not save sample data at startup", saveException);
                return "No saved data yet. Loaded sample data (" + describeContents(data)
                        + ") but could not save it to " + storage.getDisplayPath()
                        + " (" + describe(saveException) + ").";
            }
        } catch (IOException exception) {
            isAutomaticSavingEnabled = false;
            LOGGER.log(Level.FINE, "Could not read data file at startup", exception);
            return "Could not read " + storage.getDisplayPath() + " (" + describe(exception) + ")."
                    + System.lineSeparator() + "Starting with no data.";
        }
    }

    /**
     * Returns the current records in their saved form, to compare against after a command runs.
     *
     * @see #saveIfChanged(String)
     */
    public String snapshot() {
        return Storage.encode(data);
    }

    /**
     * Saves to the data file if the records differ from {@code snapshotBefore}.
     *
     * <p>Comparing snapshots means any command that changes data is saved, including commands added
     * later, without this class keeping a list of which commands change data. Commands that only
     * display data are not saved, so they never overwrite edits made to the file by hand.
     *
     * @param snapshotBefore The result of {@link #snapshot()} taken before the command ran.
     * @return An empty string if all went well, otherwise a warning to show the user.
     */
    public String saveIfChanged(String snapshotBefore) {
        if (snapshot().equals(snapshotBefore)) {
            return "";
        }
        if (!isAutomaticSavingEnabled) {
            return "Warning: your change could not be saved to " + storage.getDisplayPath()
                    + " because automatic saving is disabled after the file could not be read."
                    + " Fix the file and run reload, or use save explicitly.";
        }
        LOGGER.log(Level.FINE, "Data changed, saving automatically");
        try {
            storage.save(data);
            return "";
        } catch (IOException exception) {
            LOGGER.log(Level.FINE, "Automatic save failed", exception);
            return "Warning: your change could not be saved to " + storage.getDisplayPath()
                    + " (" + describe(exception) + ").";
        }
    }

    /** Handles {@code save [FILE_PATH]}. */
    private String save(String filePath) {
        try {
            if (filePath.isEmpty()) {
                storage.save(data);
                isAutomaticSavingEnabled = true;
                return "Saved " + describeContents(data) + System.lineSeparator()
                        + "to " + storage.getDisplayPath();
            }
            Storage.saveTo(Path.of(filePath), data);
            return "Saved a copy to " + filePath + System.lineSeparator()
                    + "TutorLedger will keep saving to " + storage.getDisplayPath() + ".";
        } catch (InvalidPathException exception) {
            return "Error: " + filePath + " is not a valid file path.";
        } catch (IOException exception) {
            LOGGER.log(Level.FINE, "save command failed", exception);
            String target = filePath.isEmpty() ? storage.getDisplayPath() : filePath;
            return "Error: could not save to " + target + " (" + describe(exception) + ").";
        }
    }

    /** Handles {@code reload}: replaces the current records with the contents of the data file. */
    private String reload() {
        Storage.LoadResult result;
        try {
            result = storage.load();
        } catch (NoSuchFileException exception) {
            return "No data file found at " + storage.getDisplayPath() + "." + System.lineSeparator()
                    + "Your current data has not been changed.";
        } catch (IOException exception) {
            isAutomaticSavingEnabled = false;
            return "Could not read " + storage.getDisplayPath() + " (" + describe(exception) + ")."
                    + System.lineSeparator() + "Your current data has not been changed.";
        }
        data.replaceAllWith(result.data());
        isAutomaticSavingEnabled = true;
        // The file was just read in full, so saving now would write back exactly the records it gave.
        assert snapshot().equals(Storage.encode(result.data())) : "Reload did not copy every record";
        return "Reloaded " + storage.getDisplayPath() + System.lineSeparator()
                + "Loaded " + describeContents(data) + "." + describeProblems(result.problems());
    }

    /** Describes the records, e.g. "10 students, 42 lessons and 7 homework items". */
    private static String describeContents(TutorLedgerData records) {
        return count(records.getStudents().size(), "student") + ", "
                + count(records.getLessons().size(), "lesson") + " and "
                + count(records.getHomework().size(), "homework item");
    }

    /** Lists the skipped lines under a heading, or returns nothing if no line was skipped. */
    private static String describeProblems(List<String> problems) {
        if (problems.isEmpty()) {
            return "";
        }
        StringBuilder text = new StringBuilder(System.lineSeparator()).append("Skipped ")
                .append(count(problems.size(), "line")).append(" that could not be read:");
        for (String problem : problems) {
            text.append(System.lineSeparator()).append("  ").append(problem);
        }
        text.append(System.lineSeparator())
                .append("Fix the line and run reload, or it is removed the next time TutorLedger saves.");
        return text.toString();
    }

    /** Turns a file error into a short reason a user can act on. */
    private static String describe(IOException exception) {
        if (exception instanceof AccessDeniedException) {
            return "access denied";
        }
        if (exception instanceof FileSystemException fileException && fileException.getReason() != null) {
            return fileException.getReason();
        }
        return exception.getMessage();
    }

    private static String count(int amount, String noun) {
        return amount + " " + noun + (amount == 1 ? "" : "s");
    }
}

package seedu.tutorledger;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Runs the storage commands {@code save} and {@code reload}, and saves automatically after any
 * other command that changes data (features F5.1 and F5.2).
 *
 * <p>Like the other command classes, each method returns the text to show, and problems are
 * reported as messages instead of crashing the program.
 */
public class StorageCommands {
    /** Command words this class understands, used by the main loop to route input here. */
    private static final Set<String> COMMAND_WORDS = Set.of("save", "reload");

    private final TutorLedgerData data;
    private final Storage storage;

    /**
     * Creates the handler for the storage commands.
     *
     * @param data The records to save, and to replace on reload.
     * @param storage The data file that is saved to automatically.
     */
    public StorageCommands(TutorLedgerData data, Storage storage) {
        this.data = data;
        this.storage = storage;
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
        // Words typed after reload are ignored, as the user guide says.
        return command.equals("save") ? save(arguments) : reload();
    }

    /**
     * Loads the data file when TutorLedger starts, and returns a message describing what happened.
     * If the file does not exist yet, TutorLedger simply starts with no records.
     */
    public String loadOnStartup() {
        try {
            Storage.LoadResult result = storage.load();
            data.replaceAllWith(result.data());
            return "Loaded " + describeContents(data) + " from " + storage.getDisplayPath() + "."
                    + describeProblems(result.problems());
        } catch (NoSuchFileException exception) {
            return "No saved data yet. TutorLedger will create " + storage.getDisplayPath()
                    + " when you add something.";
        } catch (IOException exception) {
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
        try {
            storage.save(data);
            return "";
        } catch (IOException exception) {
            return "Warning: your change could not be saved to " + storage.getDisplayPath()
                    + " (" + describe(exception) + ").";
        }
    }

    /** Handles {@code save [FILE_PATH]}. */
    private String save(String filePath) {
        try {
            if (filePath.isEmpty()) {
                storage.save(data);
                return "Saved " + describeContents(data) + System.lineSeparator()
                        + "to " + storage.getDisplayPath();
            }
            Storage.saveTo(Path.of(filePath), data);
            return "Saved a copy to " + filePath + System.lineSeparator()
                    + "TutorLedger will keep saving to " + storage.getDisplayPath() + ".";
        } catch (InvalidPathException exception) {
            return "Error: " + filePath + " is not a valid file path.";
        } catch (IOException exception) {
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
            return "Could not read " + storage.getDisplayPath() + " (" + describe(exception) + ")."
                    + System.lineSeparator() + "Your current data has not been changed.";
        }
        data.replaceAllWith(result.data());
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

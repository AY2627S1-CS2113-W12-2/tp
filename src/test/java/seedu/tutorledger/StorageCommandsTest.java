package seedu.tutorledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Checks the save and reload commands, automatic saving, and loading at startup. */
class StorageCommandsTest {
    @TempDir
    Path tempDir;

    private Path dataFile;
    private TutorLedgerData data;
    private StorageCommands commands;

    @BeforeEach
    void setUp() {
        dataFile = tempDir.resolve("data").resolve("tutorledger.txt");
        data = new TutorLedgerData();
        commands = new StorageCommands(data, new Storage(dataFile.toString()));
        data.addStudent("Amirah Binte Rahman", "Sec 3", "91234567");
    }

    @Test
    void handles_storageCommandWords_onlyThoseAreHandled() {
        assertTrue(StorageCommands.handles("save"));
        assertTrue(StorageCommands.handles("  SAVE backups/term1.txt"));
        assertTrue(StorageCommands.handles("reload"));
        assertFalse(StorageCommands.handles("saved"));
        assertFalse(StorageCommands.handles(""));
        assertFalse(StorageCommands.handles(null));
    }

    @Test
    void save_noPath_writesTheDataFileAndReportsCounts() {
        String output = commands.execute("save");

        assertEquals("Saved 1 student, 0 lessons and 0 homework items" + System.lineSeparator()
                + "to " + dataFile, output);
        assertTrue(Files.exists(dataFile));
    }

    @Test
    void save_withPath_writesACopyAndKeepsTheDataFileUnchanged() throws IOException {
        Path backup = tempDir.resolve("backups").resolve("term1.txt");

        String output = commands.execute("save " + backup);

        assertEquals("Saved a copy to " + backup + System.lineSeparator()
                + "TutorLedger will keep saving to " + dataFile + ".", output);
        assertEquals(Storage.encode(data), Files.readString(backup));
        assertFalse(Files.exists(dataFile));
    }

    @Test
    void save_pathIsAFolder_reportsAnError() throws IOException {
        Files.createDirectories(tempDir.resolve("folder"));

        String output = commands.execute("save " + tempDir.resolve("folder"));

        assertTrue(output.startsWith("Error: could not save to "), output);
    }

    @Test
    void reload_missingFile_keepsCurrentData() {
        String output = commands.execute("reload");

        assertEquals("No data file found at " + dataFile + "." + System.lineSeparator()
                + "Your current data has not been changed.", output);
        assertEquals(1, data.getStudents().size());
    }

    @Test
    void reload_editedFile_replacesDataAndReportsSkippedLines() throws IOException {
        commands.execute("save");
        Files.writeString(dataFile, Files.readString(dataFile).replace("Sec 3", "Sec 4")
                + "S | S2 | Tan Wei Ming | Sec 4 | 98765432" + System.lineSeparator()
                + "L | L1 | S1 | 31-02-2026 | 1600 | A Math | 60 | not recorded | unpaid | "
                + System.lineSeparator());
        int badLineNumber = (int) Files.readString(dataFile).lines().count();

        String output = commands.execute("reload extra words are ignored");

        assertEquals("Reloaded " + dataFile + System.lineSeparator()
                + "Loaded 2 students, 0 lessons and 0 homework items." + System.lineSeparator()
                + "Skipped 1 line that could not be read:" + System.lineSeparator()
                + "  Line " + badLineNumber + ": Date must be a real date in DD-MM-YYYY format."
                + System.lineSeparator()
                + "Fix the line and run reload, or it is removed the next time TutorLedger saves.", output);
        assertEquals("Sec 4", data.getStudent("S1").getLevel());
        assertEquals("Tan Wei Ming", data.getStudent("S2").getName());
        // The ID counter follows the reloaded file, so the next student added is S3.
        assertEquals("S3", data.addStudent("Daniel Lim", "Sec 2", "81234567").getStudentId());
    }

    @Test
    void saveIfChanged_onlySavesWhenACommandChangedData() throws IOException {
        String beforeReadOnly = commands.snapshot();
        assertEquals("", commands.saveIfChanged(beforeReadOnly));
        assertFalse(Files.exists(dataFile), "A command that changes nothing must not write the file.");

        String beforeAdd = commands.snapshot();
        data.addStudent("Tan Wei Ming", "Sec 4", "98765432");
        assertEquals("", commands.saveIfChanged(beforeAdd));
        assertEquals(Storage.encode(data), Files.readString(dataFile));
    }

    @Test
    void saveIfChanged_fileCannotBeWritten_returnsAWarning() throws IOException {
        // A folder where the data file should be makes the write fail.
        Files.createDirectories(dataFile);
        String before = commands.snapshot();
        data.addStudent("Tan Wei Ming", "Sec 4", "98765432");

        String warning = commands.saveIfChanged(before);

        assertTrue(warning.startsWith("Warning: your change could not be saved to " + dataFile), warning);
    }

    @Test
    void loadOnStartup_missingThenPresentFile_reportsWhatHappened() {
        assertEquals("No saved data yet. TutorLedger will create " + dataFile + " when you add something.",
                commands.loadOnStartup());

        commands.execute("save");
        TutorLedgerData restarted = new TutorLedgerData();
        StorageCommands restartedCommands = new StorageCommands(restarted, new Storage(dataFile.toString()));

        assertEquals("Loaded 1 student, 0 lessons and 0 homework items from " + dataFile + ".",
                restartedCommands.loadOnStartup());
        assertEquals("Amirah Binte Rahman", restarted.getStudent("S1").getName());
    }
}

package seedu.tutorledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

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
        assertTrue(commands.handles("save"));
        assertTrue(commands.handles("  SAVE backups/term1.txt"));
        assertTrue(commands.handles("reload"));
        assertFalse(commands.handles("saved"));
        assertFalse(commands.handles(""));
        assertFalse(commands.handles(null));
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

    @Test
    void loadOnStartup_withSampleSeeding_populatesAndPersistsSampleData() {
        TutorLedgerData freshData = new TutorLedgerData();
        StorageCommands seededCommands = new StorageCommands(freshData, new Storage(dataFile.toString()), true);

        String output = seededCommands.loadOnStartup();

        String expectedPrefix = "No saved data yet. Loaded sample data (2 students, 2 lessons and 1 "
                + "homework item)";
        assertTrue(output.startsWith(expectedPrefix), output);
        assertEquals(2, freshData.getStudents().size());
        assertTrue(Files.exists(dataFile));
    }

    @Test
    void loadOnStartup_withSampleSeeding_usesTheSharedClockForRelativeDates() {
        TutorLedgerData freshData = new TutorLedgerData();
        Clock clock = TestUtil.clockAt("2030-03-10T10:00:00Z");
        StorageCommands seededCommands = new StorageCommands(freshData, new Storage(dataFile.toString()), true, clock);

        seededCommands.loadOnStartup();

        assertEquals(LocalDate.of(2030, 3, 9), freshData.getLesson("L1").getDate());
        assertEquals(LocalDate.of(2030, 3, 11), freshData.getLesson("L2").getDate());
    }

    @Test
    void loadOnStartup_existingEmptyFile_doesNotSeedSampleData() throws IOException {
        Files.createDirectories(dataFile.getParent());
        Files.writeString(dataFile, "");
        TutorLedgerData freshData = new TutorLedgerData();
        StorageCommands seededCommands = new StorageCommands(freshData, new Storage(dataFile.toString()), true);

        assertEquals("Loaded 0 students, 0 lessons and 0 homework items from " + dataFile + ".",
                seededCommands.loadOnStartup());
        assertTrue(freshData.getStudents().isEmpty());
    }

    @Test
    void reloadAndStartup_unreadableFile_reportItAndKeepTheCurrentData() throws IOException {
        // A folder in place of the data file cannot be read on any operating system.
        Files.createDirectories(dataFile);

        String reload = commands.execute("reload");
        String startup = commands.loadOnStartup();

        assertTrue(reload.startsWith("Could not read " + dataFile + " ("), reload);
        assertTrue(reload.endsWith("Your current data has not been changed."), reload);
        assertTrue(startup.startsWith("Could not read " + dataFile + " ("), startup);
        assertTrue(startup.endsWith("Starting with no data."), startup);
        // Neither message replaces the records already in memory with an empty set.
        assertEquals(1, data.getStudents().size());
    }

    @Test
    void startupReadFailure_doesNotAutomaticallyOverwriteTheUnreadableFile() throws IOException {
        AtomicInteger saveAttempts = new AtomicInteger();
        Storage unreadableStorage = new Storage(dataFile.toString()) {
            @Override
            public LoadResult load() throws IOException {
                throw new IOException("file is locked");
            }

            @Override
            public void save(TutorLedgerData records) {
                saveAttempts.incrementAndGet();
            }
        };
        StorageCommands unreadableCommands = new StorageCommands(data, unreadableStorage);

        unreadableCommands.loadOnStartup();
        String before = unreadableCommands.snapshot();
        data.addStudent("New Person", "Sec 2", "87654321");

        String warning = unreadableCommands.saveIfChanged(before);

        assertEquals(0, saveAttempts.get());
        assertTrue(warning.contains("automatic saving is disabled"), warning);
    }

    @Test
    void save_invalidPath_reportsAnErrorWithoutSaving() {
        // A NUL character is not allowed in a file path on any operating system.
        String path = "backup" + (char) 0 + ".txt";

        assertEquals("Error: " + path + " is not a valid file path.", commands.execute("save " + path));
        assertFalse(Files.exists(dataFile));
    }

    @Test
    void execute_notAStorageCommand_isRejected() {
        assertEquals("Unknown command.", commands.execute("list"));
        assertEquals("Unknown command.", commands.execute("   "));
    }
}

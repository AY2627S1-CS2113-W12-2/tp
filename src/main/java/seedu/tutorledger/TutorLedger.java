package seedu.tutorledger;

import java.time.Clock;
import java.util.Scanner;

/** Command-line entry point for the student book. */
public class TutorLedger {
    /**
     * Loads the saved data, then reads commands until the user exits or standard input closes.
     * Any command that changes data is saved straight away, so nothing needs saving on exit.
     */
    public static void main(String[] args) {
        TutorLedgerData data = new TutorLedgerData();
        Clock clock = Clock.systemDefaultZone();
        StudentCommands commands = new StudentCommands(data, clock);
        HomeworkCommands homeworkCommands = new HomeworkCommands(data, clock);
        PaymentCommands paymentCommands = new PaymentCommands(data, clock);
        LessonCommands lessonCommands = new LessonCommands(data, clock);
        TopicCommands topicCommands = new TopicCommands(data, clock);
        ScoreCommands scoreCommands = new ScoreCommands(data, clock);
        LessonFilterCommand filterCommand = new LessonFilterCommand(data);
        StorageCommands storageCommands = new StorageCommands(data, new Storage(Storage.DEFAULT_FILE_PATH));
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

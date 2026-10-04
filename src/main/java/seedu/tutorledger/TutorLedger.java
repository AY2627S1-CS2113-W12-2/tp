package seedu.tutorledger;

import java.time.Clock;
import java.util.Scanner;

/** Command-line entry point for the student book. */
public class TutorLedger {
    /**
     * Reads commands until the user exits or standard input closes.
     */
    public static void main(String[] args) {
        TutorLedgerData data = new TutorLedgerData();
        StudentCommands commands = new StudentCommands(data, Clock.systemDefaultZone());
        System.out.println("TutorLedger student book. Type exit to close.");
        Scanner input = new Scanner(System.in);
        while (input.hasNextLine()) {
            String line = input.nextLine();
            if (line.strip().equalsIgnoreCase("exit")) {
                break;
            }
            System.out.println(commands.execute(line));
        }
    }
}

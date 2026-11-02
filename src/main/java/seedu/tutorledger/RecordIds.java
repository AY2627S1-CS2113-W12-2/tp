package seedu.tutorledger;

/** Validates string IDs before storage changes, reserving one integer value for the next-ID counter. */
final class RecordIds {
    /** Highest record number that still has a representable successor. */
    private static final int MAX_RECORD_NUMBER = Integer.MAX_VALUE - 1;

    private RecordIds() {
        // Utility class; IDs remain strings in the records and collection keys.
    }

    /** Returns the successor of a supported record ID, rejecting malformed or oversized IDs. */
    static int nextAfter(String id, String prefix) {
        return parse(id, prefix, MAX_RECORD_NUMBER) + 1;
    }

    /** Reads a saved NEXT value; the maximum integer represents an exhausted counter. */
    static int parseCounter(String id, String prefix) {
        return parse(id, prefix, Integer.MAX_VALUE);
    }

    /** Checks an entire allocation before mutation, including every member of a weekly schedule. */
    static void requireCapacity(int first, int count, String prefix) {
        if (first < 1 || count < 1 || first > Integer.MAX_VALUE - count) {
            throw new IllegalArgumentException("No more " + prefix + " IDs are available for this request.");
        }
    }

    /** Converts only the numeric suffix; callers retain the original string ID. */
    private static int parse(String id, String prefix, int maximum) {
        if (id == null || !id.matches("(?i)" + prefix + "[1-9][0-9]*")) {
            throw new IllegalArgumentException(prefix + " ID must look like " + prefix + "1.");
        }
        String rangeError = "The numeric part of " + prefix + " IDs must be between 1 and " + maximum + ".";
        try {
            int number = Integer.parseInt(id.substring(1));
            if (number > maximum) {
                throw new IllegalArgumentException(rangeError);
            }
            return number;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(rangeError);
        }
    }
}

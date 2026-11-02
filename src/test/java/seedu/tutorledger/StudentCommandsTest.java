package seedu.tutorledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Checks the student command workflow and its links to other v1 records. */
class StudentCommandsTest {
    private TutorLedgerData data;
    private StudentCommands commands;

    @BeforeEach
    void setUp() {
        data = new TutorLedgerData();
        Clock clock = TestUtil.clockAt("2026-09-21T10:00:00Z");
        commands = new StudentCommands(data, clock);
    }

    @Test
    void addAndListSupportPrimaryAndJuniorCollegeLevels() {
        assertTrue(commands.execute("add n/Alice Tan l/pRi5 p/91234567 r/40").contains("(Pri 5)"));
        assertTrue(commands.execute("add n/Bob Tan l/jc 2 p/81234567").contains("(JC 2)"));
        assertTrue(commands.execute("list").contains("Alice Tan (Pri 5)"));
        assertTrue(commands.execute("list").contains("Bob Tan (JC 2)"));
        assertTrue(commands.execute("view S1").contains("(Pri 5)"));
        assertTrue(commands.execute("view S2").contains("(JC 2)"));
    }

    @Test
    void editAcrossSchoolStagesPreservesStudentDetailsRateAndHistory() {
        commands.execute("add n/Alice Tan l/Pri6 p/91234567 s/Math r/40.50");
        Student original = data.getStudent("S1");
        Lesson lesson = data.addLesson("S1", "Math", "15-09-2026", "1600", "40.50");
        Homework homework = data.addHomework("S1", "Revision", LocalDate.of(2026, 9, 22));
        Score score = data.addScore("S1", "Test", "8", "10", "15-09-2026");
        for (String level : List.of("Sec1", "jC1", "pri2")) {
            assertTrue(commands.execute("edit S1 l/" + level).startsWith("Edited S1"));
            Student updated = data.getStudent("S1");
            assertEquals("S1", updated.getStudentId());
            assertEquals("Alice Tan", updated.getName());
            assertEquals("91234567", updated.getPhoneNumber());
            assertEquals(List.of("Math"), updated.getSubjects());
            assertEquals(new BigDecimal("40.50"), updated.getDefaultHourlyRate().orElseThrow());
            assertSame(lesson, data.getLesson("L1"));
            assertSame(homework, data.getHomeworkById("H1"));
            assertSame(score, data.getScores().iterator().next());
        }
        assertEquals("Pri 2", data.getStudent("S1").getLevel());
        assertEquals("Pri 6", original.getLevel());
    }

    @Test
    void invalidSchoolLevelsDoNotConsumeIdsOrPartiallyEditRecords() {
        commands.execute("add n/Alice Tan l/Pri 5 p/91234567 r/40");
        String before = Storage.encode(data);
        for (String level : List.of("", "Pri0", "Pri7", "Sec6", "JC0", "JC3", "P5", "Poly 1",
                "JC 1 extra", "Pri  5", "Pri5 l/JC1")) {
            assertTrue(commands.execute("add n/Bob Tan l/" + level + " p/81234567 r/60").startsWith("Error:"));
            assertEquals(before, Storage.encode(data));
            assertTrue(commands.execute("edit S1 l/" + level + " p/87654321 r/99").startsWith("Error:"));
            assertEquals(before, Storage.encode(data));
        }
        assertTrue(commands.execute("add n/Bob Tan l/JC2 p/81234567").startsWith("Added S2"));
    }

    @Test
    void addAndListAcceptPrefixesInAnyOrderAndFindNamesIgnoringCase() {
        assertTrue(commands.execute("ADD P/91234567 L/Sec3 N/Tan Wei Ming").contains("Added S1"));
        assertTrue(commands.execute("add n/Tan Wei Ling l/Sec 2 p/81234567").contains("Added S2"));
        assertTrue(commands.execute("list wEi").contains("2 students match"));
        assertEquals("Tan Wei Ming", data.getStudent("s1").getName());
        assertTrue(commands.execute("add n/tan wei ming l/Sec 4 p/91234567").contains("already exists"));
        assertEquals(3, data.getNextStudentNumber());
    }

    @Test
    void addWithSubjectsCreatesTheWholeRecordInOneLine() {
        String output = commands.execute("add n/Tan Wei Ming l/Sec 3 p/91234567 s/E Math s/A Math s/e math");
        assertEquals("Added S1: Tan Wei Ming (Sec 3), 91234567" + System.lineSeparator()
                + "Subjects: E Math, A Math" + System.lineSeparator()
                + "You now have 1 student.", output);
        assertEquals(2, data.getStudent("S1").getSubjects().size());
        // A blank subject is rejected before the student is added, so no half-entered record is left.
        assertTrue(commands.execute("add n/Tan Wei Ling l/Sec 2 p/81234567 s/").startsWith("Error:"));
        assertEquals(1, data.getStudents().size());
        assertEquals(2, data.getNextStudentNumber());
    }

    @Test
    void subjectAndEditAddReplaceAndClearWithoutPartialChanges() {
        commands.execute("add n/Tan Wei Ming l/Sec 3 p/91234567");
        commands.execute("subject s1 s/E Math S/A Math s/e math");
        assertEquals(2, data.getStudent("S1").getSubjects().size());
        commands.execute("edit S1 p/81234567 s/Pure Physics");
        assertEquals("81234567", data.getStudent("S1").getPhoneNumber());
        assertEquals("Pure Physics", data.getStudent("S1").getSubjects().getFirst());
        assertTrue(commands.execute("edit S1 l/Sec 6 s/Chemistry").startsWith("Error:"));
        assertEquals("Sec 3", data.getStudent("S1").getLevel());
        assertEquals("Pure Physics", data.getStudent("S1").getSubjects().getFirst());
        commands.execute("edit S1 s/");
        assertTrue(data.getStudent("S1").getSubjects().isEmpty());
    }

    @Test
    void viewShowsLinkedRecordsAndExcludesFutureFeesAndSubmittedHomework() {
        commands.execute("add n/Tan Wei Ming l/Sec 3 p/91234567");
        data.putLesson(new Lesson("L1", "S1", "E Math", "15-09-2026", "1600",
                "present", "60", "Quadratics"));
        data.putLesson(new Lesson("L2", "S1", "E Math", "22-09-2026", "1600",
                "not recorded", "70", ""));
        data.putHomework(new Homework("H1", "S1", "Worksheet 3B", "22-09-2026", "outstanding"));
        data.putHomework(new Homework("H2", "S1", "Old work", "20-09-2026", "submitted"));
        String view = commands.execute("view s1");
        assertTrue(view.indexOf("L2") < view.indexOf("L1"));
        assertTrue(view.contains("Quadratics"));
        assertTrue(view.contains("H1"));
        assertFalse(view.contains("H2"));
        assertTrue(view.contains("Amount owed: $60.00 (1 lesson)"));
    }

    @Test
    void deleteCascadesAndIdsRemainUnused() {
        commands.execute("add n/Tan Wei Ming l/Sec 3 p/91234567");
        data.putLesson(new Lesson("L1", "S1", "E Math", "15-09-2026", "1600",
                "present", "60", ""));
        data.putHomework(new Homework("H1", "S1", "Worksheet", "22-09-2026", "outstanding"));
        assertTrue(commands.execute("delete s1 confirm").contains("1 lesson and 1 homework"));
        assertTrue(data.getLessons().isEmpty());
        assertTrue(data.getHomework().isEmpty());
        assertTrue(commands.execute("add n/Tan Wei Ling l/Sec 2 p/81234567").contains("Added S2"));
    }

    @Test
    void missingValuesUnknownIdsAndDuplicateEditDoNotChangeRecords() {
        assertTrue(commands.execute("add n/Tan Wei Ming l/Sec 3").startsWith("Error:"));
        assertTrue(commands.execute("view S9").startsWith("Error:"));
        commands.execute("add n/Tan Wei Ming l/Sec 3 p/91234567");
        commands.execute("add n/Tan Wei Ling l/Sec 2 p/81234567");
        assertTrue(commands.execute("edit S2 n/tan wei ming p/91234567").contains("already exists"));
        assertEquals("Tan Wei Ling", data.getStudent("S2").getName());
        assertEquals("81234567", data.getStudent("S2").getPhoneNumber());
        assertTrue(commands.execute("subject S2 s/").startsWith("Error:"));
        assertTrue(commands.execute("delete S9").startsWith("Error:"));
    }

    @Test
    void importedCounterPreservesDeletedIdsAfterReload() {
        data.importStudent(new Student("S4", "Tan Wei Ming", "Sec 3", "91234567"));
        data.deleteStudent("S4");
        data.setNextStudentNumber(8);
        assertTrue(commands.execute("add n/Tan Wei Ling l/Sec 2 p/81234567").contains("Added S8"));
    }

    @Test
    void view_noLessonsOrOutstandingHomework_saysNoneUnderEachHeading() {
        commands.execute("add n/Alice Tan l/Pri5 p/91234567");
        data.putHomework(new Homework("H1", "S1", "Done already", LocalDate.of(2026, 9, 20),
                Homework.STATUS_SUBMITTED));

        String newLine = System.lineSeparator();
        assertEquals("S1  Alice Tan  (Pri 5)  91234567" + newLine
                + "Subjects: none" + newLine
                + "Default hourly rate: not set" + newLine
                + "Lessons" + newLine
                + "  none" + newLine
                + "Outstanding homework" + newLine
                + "  none" + newLine
                + "Amount owed: $0.00 (0 lessons)", commands.execute("view S1"));
    }

    /** Singapore names often contain s/o (son of) or d/o (daughter of), which look like prefixes. */
    @Test
    void addEditListAndView_namesWithSonOfOrDaughterOf_keepTheWholeName() {
        String added = commands.execute("add n/Ravi s/o Kumar l/Sec 1 p/91234567 s/Math");
        String daughter = commands.execute("add n/Priya d/o Raman l/Sec 2 p/81234567");

        assertTrue(added.startsWith("Added S1: Ravi s/o Kumar (Sec 1), 91234567"), added);
        assertEquals("Ravi s/o Kumar", data.getStudent("S1").getName());
        assertEquals(List.of("Math"), data.getStudent("S1").getSubjects());
        assertTrue(daughter.startsWith("Added S2: Priya d/o Raman (Sec 2)"), daughter);
        assertTrue(commands.execute("list ravi s/o").contains("S1  Ravi s/o Kumar (Sec 1)  Math"));
        assertTrue(commands.execute("list d/o sort/owed").contains("S2  Priya d/o Raman"));

        String edited = commands.execute("edit S2 n/Priya D/O Raman Pillai");
        assertTrue(edited.startsWith("Edited S2: Priya D/O Raman Pillai"), edited);
        assertTrue(commands.execute("view S2").startsWith("S2  Priya D/O Raman Pillai"));
    }

    @Test
    void add_sonOfWithoutAFollowingName_isRejectedWithoutAddingAnyone() {
        String result = commands.execute("add n/Ravi s/o l/Sec 1 p/91234567");

        assertTrue(result.startsWith("Error: Name must contain only letters"), result);
        assertTrue(result.contains("s/o"), result);
        assertTrue(data.getStudents().isEmpty());
        assertTrue(commands.execute("add n/Ravi s/x Kumar l/Sec 1 p/91234567").contains("Subjects: x Kumar"));
    }

    /** Before s/o could be part of a name, a subject could follow n/ directly; that must keep working. */
    @Test
    void addAndEdit_subjectStartingWithCapitalOAfterName_staysASubject() {
        String added = commands.execute("add n/Ravi s/O Level Math l/Sec 4 p/91234567");

        assertTrue(added.startsWith("Added S1: Ravi (Sec 4), 91234567"), added);
        assertEquals(List.of("O Level Math"), data.getStudent("S1").getSubjects());

        String edited = commands.execute("edit S1 n/Ravi Kumar S/O Level Physics");
        assertTrue(edited.contains("Edited S1: Ravi Kumar (Sec 4)"), edited);
        assertEquals(List.of("O Level Physics"), data.getStudent("S1").getSubjects());
    }

    @Test
    void add_nameInCapitalsWithSonOf_keepsTheWholeName() {
        String added = commands.execute("add n/RAVI S/O KUMAR l/Sec 1 p/91234567 s/Math");

        assertTrue(added.startsWith("Added S1: RAVI S/O KUMAR (Sec 1)"), added);
        assertEquals(List.of("Math"), data.getStudent("S1").getSubjects());
        assertTrue(commands.execute("list RAVI S/O").contains("S1  RAVI S/O KUMAR"));
    }

    /** Before this was fixed, the subject was swallowed and the student was saved as "RAVI S/O Level Math". */
    @Test
    void addAndEdit_capitalOSubjectAfterNameInCapitals_staysASubject() {
        String added = commands.execute("add n/RAVI S/O Level Math l/Sec 4 p/91234567");
        String both = commands.execute("add n/PRIYA D/O RAMAN s/O Level Math l/Sec 4 p/81234567");

        assertTrue(added.startsWith("Added S1: RAVI (Sec 4), 91234567"), added);
        assertEquals(List.of("O Level Math"), data.getStudent("S1").getSubjects());
        assertTrue(both.startsWith("Added S2: PRIYA D/O RAMAN (Sec 4)"), both);
        assertEquals(List.of("O Level Math"), data.getStudent("S2").getSubjects());

        String edited = commands.execute("edit S1 n/RAVI S/O KUMAR s/O Level Physics");
        assertTrue(edited.startsWith("Edited S1: RAVI S/O KUMAR (Sec 4)"), edited);
        assertEquals(List.of("O Level Physics"), data.getStudent("S1").getSubjects());
    }

    @Test
    void addAndEdit_mixedCaseConnectors_followTheDocumentedRule() {
        String upperS = commands.execute("add n/Ravi S/o Kumar l/Sec 1 p/91234567 s/Math");
        String upperD = commands.execute("add n/Priya D/o Raman l/Sec 1 p/81234567");
        String capitalO = commands.execute("add n/Tan s/O Kumar l/Sec 1 p/71234567");

        // A lower-case "o" always marks the word as part of the name.
        assertTrue(upperS.startsWith("Added S1: Ravi S/o Kumar (Sec 1)"), upperS);
        assertEquals(List.of("Math"), data.getStudent("S1").getSubjects());
        // add has no d/ prefix, so d/o in any case is part of the name.
        assertTrue(upperD.startsWith("Added S2: Priya D/o Raman (Sec 1)"), upperD);
        // s/O after a mixed-case name is a subject, and the result says so.
        assertTrue(capitalO.contains("Added S3: Tan (Sec 1)") && capitalO.contains("Subjects: O Kumar"), capitalO);
        assertTrue(commands.execute("list s/O").contains("S1  Ravi S/o Kumar"));
    }

    @Test
    void addAndEdit_nameEndingWithSonOfOrDaughterOf_isRejectedWithoutChanges() {
        commands.execute("add n/Ravi s/o Kumar l/Sec 1 p/91234567 s/Math");
        String[] rejected = {
            "add n/Priya d/o l/Sec 2 p/81234567",
            "add n/PRIYA D/O l/Sec 2 p/81234567",
            "add n/Ravi s/o l/Sec 2 p/81234567 s/Math",
            "add n/RAVI S/O l/Sec 2 p/81234567",
            "add n/Ravi s/o s/Math l/Sec 2 p/81234567",
            "edit S1 n/Ravi s/o",
            "edit S1 n/Priya d/o",
            "edit S1 n/RAVI S/O",
            "edit S1 n/Ravi s/o s/Physics",
        };

        for (String command : rejected) {
            String result = commands.execute(command);
            assertTrue(result.startsWith("Error: Name must contain only letters"), command + " -> " + result);
        }
        assertEquals(1, data.getStudents().size());
        assertEquals("Ravi s/o Kumar", data.getStudent("S1").getName());
        assertEquals(List.of("Math"), data.getStudent("S1").getSubjects());
    }

    @Test
    void storage_namesWithSonOfOrDaughterOf_roundTrip() {
        commands.execute("add n/Ravi s/o Kumar l/Sec 1 p/91234567");

        Storage.LoadResult reloaded = Storage.decode(Storage.encode(data).lines().toList());

        assertTrue(reloaded.problems().isEmpty(), reloaded.problems().toString());
        assertEquals("Ravi s/o Kumar", reloaded.data().getStudent("S1").getName());
    }
}

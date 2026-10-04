package seedu.duke;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

class StudentTest {
    @Test
    void acceptsValidStudentDetails() {
        Student student = new Student("S123", "Tan Wei Ming", "Sec 3", "91234567",
                "Pure Chemistry", "22-09-2026", "30-09-2026", "1600",
                "62.50", "late", "Started vectors", "Worksheet 3B");

        assertEquals("S123", student.getStudentId());
        assertEquals("Tan Wei Ming", student.getName());
        assertEquals("Sec 3", student.getLevel());
        assertEquals("91234567", student.getPhoneNumber());
        assertEquals(LocalDate.of(2026, 9, 22), student.getDate());
        assertEquals(LocalDate.of(2026, 9, 30), student.getDueDate());
        assertEquals(LocalTime.of(16, 0), student.getTime());
        assertEquals(new BigDecimal("62.50"), student.getFee());
        assertEquals(Student.Attendance.LATE, student.getAttendance());
        assertEquals("Started vectors", student.getNotes());
        assertEquals("Worksheet 3B", student.getDescription());
    }

    @Test
    void acceptsOptionalLevelSpaceAndZeroFee() {
        Student student = new Student("S124", "O'Neil-Smith", "Sec4", "81234567",
                "Mathematics", "01-01-2026", "02-01-2026", "0000",
                "0", "PRESENT", "", "");

        assertEquals("Sec 4", student.getLevel());
        assertEquals(BigDecimal.ZERO, student.getFee());
        assertEquals(Student.Attendance.PRESENT, student.getAttendance());
    }

    @Test
    void rejectsInvalidName() {
        assertThrows(IllegalArgumentException.class, () -> createStudent("Tan Wei 3"));
    }

    @Test
    void rejectsInvalidPhoneNumber() {
        assertThrows(IllegalArgumentException.class, () -> createStudentWithPhone("1234567"));
    }

    @Test
    void rejectsImpossibleDate() {
        assertThrows(IllegalArgumentException.class, () -> createStudentWithDate("31-02-2026"));
    }

    @Test
    void rejectsInvalidTime() {
        assertThrows(IllegalArgumentException.class, () -> createStudentWithTime("2460"));
    }

    @Test
    void rejectsFeeWithMoreThanTwoDecimalPlaces() {
        assertThrows(IllegalArgumentException.class, () -> createStudentWithFee("1.234"));
    }

    @Test
    void rejectsUnknownAttendance() {
        assertThrows(IllegalArgumentException.class, () -> createStudentWithAttendance("excused"));
    }

    private Student createStudent(String name) {
        return createStudent(name, "91234567", "22-09-2026", "1600", "62.50", "late");
    }

    private Student createStudent(String name, String phoneNumber, String date, String time, String fee,
            String attendance) {
        return new Student("S123", name, "Sec 3", phoneNumber, "Pure Chemistry",
                date, "30-09-2026", time, fee, attendance, "Notes", "Description");
    }

    private Student createStudentWithPhone(String phoneNumber) {
        return createStudent("Tan Wei Ming", phoneNumber, "22-09-2026", "1600", "62.50", "late");
    }

    private Student createStudentWithDate(String date) {
        return createStudent("Tan Wei Ming", "91234567", date, "1600", "62.50", "late");
    }

    private Student createStudentWithTime(String time) {
        return createStudent("Tan Wei Ming", "91234567", "22-09-2026", time, "62.50", "late");
    }

    private Student createStudentWithFee(String fee) {
        return createStudent("Tan Wei Ming", "91234567", "22-09-2026", "1600", fee, "late");
    }

    private Student createStudentWithAttendance(String attendance) {
        return createStudent("Tan Wei Ming", "91234567", "22-09-2026", "1600", "62.50", attendance);
    }
}

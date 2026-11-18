package app;

import booking.BookingException;
import booking.TimeInterval;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;

/** End-to-end tests of the PoC through AppContext and the CLI. Run: java -cp out app.ClinicCLITest */
public class ClinicCLITest {
    public static void main(String[] args) throws IOException {
        importSearchBookAndExportWorkTogether();
        cliScriptRunsTheWholeDemo();
        System.out.println("ClinicCLITest passed!");
    }

    /** Import -> search -> book -> search again -> export, using the real wired objects. */
    static void importSearchBookAndExportWorkTogether() throws IOException {
        AppContext ctx = new AppContext();
        ctx.seedDemoData();
        ctx.getImporter().importAvailability(sampleFile());
        String amelie = ctx.getProfessionals().getAll().get(0).getProfessionalId();
        TimeInterval day = new TimeInterval(LocalDateTime.of(2026, 11, 2, 0, 0), LocalDateTime.of(2026, 11, 3, 0, 0));

        List<TimeInterval> before = ctx.getBookingService().findAvailableSlots(amelie, "SRV-1", day);
        check(before.size() == 2, "Amélie has two 30-minute times on Nov 2");

        ctx.getBookingService().bookAppointment("PAT-1", amelie, "SRV-1", LocalDateTime.of(2026, 11, 2, 9, 0), null);
        List<TimeInterval> after = ctx.getBookingService().findAvailableSlots(amelie, "SRV-1", day);
        check(after.size() == 1 && after.get(0).getStartsAt().getHour() == 9 && after.get(0).getStartsAt().getMinute() == 30,
                "the booked 09:00 time is no longer offered");

        expectCode("SLOT_UNAVAILABLE", () -> ctx.getBookingService()
                .bookAppointment("PAT-2", amelie, "SRV-1", LocalDateTime.of(2026, 11, 2, 9, 0), null));
        expectCode("REFERRAL_REQUIRED", () -> ctx.getBookingService()
                .bookAppointment("PAT-2", amelie, "SRV-4", LocalDateTime.of(2026, 11, 2, 9, 30), null));
        expectCode("PATIENT_NOT_FOUND", () -> ctx.getBookingService()
                .bookAppointment("PAT-99", amelie, "SRV-1", LocalDateTime.of(2026, 11, 2, 9, 30), null));

        Path out = Files.createTempFile("after-booking", ".csv");
        check(ctx.getExporter().exportAvailability(out) == 8, "booking does not change published availability");
    }

    /** Drives the menu exactly like a user would. */
    static void cliScriptRunsTheWholeDemo() throws IOException {
        Path sample = sampleFile();
        Path export = Files.createTempFile("cli-export", ".csv");
        String script = String.join("\n",
                "4", sample.toString(),                      // import
                "6",                                         // list
                "2", "PRO-1", "SRV-1", "2026-11-02", "2026-11-02", // search
                "3", "Lavoie", "", "PRO-1", "SRV-1", "2026-11-02 09:00", "", // book
                "1", "N", "Jean", "Dupont", "Dr.", "Pediatrics", "2026-11-01", "2026-11-30", "WEEKLY", "MON,WED",
                "09:00", "12:00",                            // make availability
                "5", export.toString(),                      // export
                "0") + "\n";
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        AppContext ctx = new AppContext();
        ctx.seedDemoData();
        new ClinicCLI(ctx, new Scanner(script), new PrintStream(bytes, true, StandardCharsets.UTF_8)).run();
        String output = bytes.toString(StandardCharsets.UTF_8);

        check(output.contains("IMPORTED: 8 slot(s) added, 0 row(s) rejected"), "import message");
        check(output.contains("PRO-1  Dr. Amélie Tremblay (Family Medicine)"), "professional list keeps accents");
        check(output.contains("2 available time(s)"), "search message");
        check(output.contains("CONFIRMED: APT-1  Émilie Lavoie with Dr. Amélie Tremblay"), "booking message");
        check(output.contains("CREATED: 9 availability slot(s) for Dr. Jean Dupont (Pediatrics)"),
                "5 Mondays + 4 Wednesdays in November 2026 = 9 slots");
        check(output.contains("EXPORTED: 17 slot(s)"), "8 imported + 9 created are exported");
        check(output.trim().endsWith("Goodbye."), "menu exits cleanly");
    }

    static Path sampleFile() throws IOException {
        Path p = Files.createTempFile("sample", ".csv");
        Files.write(p, ("First Name,Last Name,Title,Specialty,Date,Start,End\n"
                + "Amélie,Tremblay,Dr.,Family Medicine,2026-11-02,09:00,09:30\n"
                + "Amélie,Tremblay,Dr.,Family Medicine,2026-11-02,09:30,10:00\n"
                + "Marc,Bouchard,Dr.,Cardiology,2026-11-02,10:00,11:00\n"
                + "Sophie,Gagnon,Nurse Practitioner,Primary Care,2026-11-02,11:00,11:30\n"
                + "Nadia,Haddad,Dr.,Dermatology,2026-11-03,13:00,13:30\n"
                + "Nadia,Haddad,Dr.,Dermatology,2026-11-03,13:30,14:00\n"
                + "David,Roy,Dr.,Orthopedics,2026-11-04,08:00,08:45\n"
                + "Marc,Bouchard,Dr.,Cardiology,2026-11-04,14:00,15:00\n").getBytes(StandardCharsets.UTF_8));
        return p;
    }

    static void expectCode(String code, Runnable action) {
        try {
            action.run();
        } catch (BookingException e) {
            check(code.equals(e.getCode()), "expected " + code + " but got " + e.getCode());
            return;
        }
        throw new AssertionError("expected BookingException " + code);
    }

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}

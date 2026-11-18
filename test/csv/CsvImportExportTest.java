package csv;

import app.AppContext;
import booking.HealthcareProfessional;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/** Tests for importAvailability / exportAvailability (SSDs I1, I2, E1, E2). Run: java -cp out csv.CsvImportExportTest */
public class CsvImportExportTest {
    static final String HEADER = "First Name,Last Name,Title,Specialty,Date,Start,End";
    static final String SAMPLE = HEADER + "\n"
            + "Amélie,Tremblay,Dr.,Family Medicine,2026-11-02,09:00,09:30\n"
            + "Amélie,Tremblay,Dr.,Family Medicine,2026-11-02,09:30,10:00\n"
            + "Marc,Bouchard,Dr.,Cardiology,2026-11-02,10:00,11:00\n"
            + "Sophie,Gagnon,Nurse Practitioner,Primary Care,2026-11-02,11:00,11:30\n"
            + "Nadia,Haddad,Dr.,Dermatology,2026-11-03,13:00,13:30\n"
            + "Nadia,Haddad,Dr.,Dermatology,2026-11-03,13:30,14:00\n"
            + "David,Roy,Dr.,Orthopedics,2026-11-04,08:00,08:45\n"
            + "Marc,Bouchard,Dr.,Cardiology,2026-11-04,14:00,15:00\n";

    public static void main(String[] args) throws IOException {
        importSampleAddsEveryRowAndKeepsAccents();
        badRowsAreRejectedByLineNumberButValidRowsAreKept();
        missingFileImportsNothing();
        wrongHeaderImportsNothing();
        windows1252FileKeepsAccents();
        exportThenImportGivesTheSameFile();
        exportWithNoAvailabilityWritesOnlyTheHeader();
        exportToMissingFolderFails();
        System.out.println("CsvImportExportTest passed!");
    }

    static void importSampleAddsEveryRowAndKeepsAccents() throws IOException {
        AppContext ctx = context();
        ImportResult r = ctx.getImporter().importAvailability(write(SAMPLE, StandardCharsets.UTF_8));
        check(r.getImportedCount() == 8, "all 8 sample rows should be imported");
        check(r.getRejectedLines().isEmpty(), "no sample row should be rejected");
        check(ctx.getProfessionals().getAll().size() == 5, "5 distinct professionals are in the sample");
        HealthcareProfessional amelie = ctx.getProfessionals().getAll().get(0);
        check(amelie.getFirstName().equals("Amélie"), "accented first name must be preserved");
        check(amelie.getSpecialty().getName().equals("Family Medicine"), "specialty must be kept");
    }

    static void badRowsAreRejectedByLineNumberButValidRowsAreKept() throws IOException {
        String csv = HEADER + "\n"
                + "Amélie,Tremblay,Dr.,Family Medicine,2026-11-02,09:00,09:30\n" // line 2 ok
                + "Marc,Bouchard,Dr.,Cardiology,2026-11-02,10:00\n"            // line 3 missing column
                + "Sophie,Gagnon,NP,Primary Care,2026-13-02,11:00,11:30\n"     // line 4 bad date
                + "Nadia,Haddad,Dr.,Dermatology,2026-11-03,14:00,13:30\n"      // line 5 end before start
                + "Amélie,Tremblay,Dr.,Family Medicine,2026-11-02,09:15,09:45\n" // line 6 overlaps line 2
                + "David,Roy,Dr.,,2026-11-04,08:00,08:45\n"                    // line 7 missing specialty
                + "\n"                                                          // line 8 blank, ignored
                + "Olivier,Côté,Dr.,Pediatrics,2026-11-05,10:00,10:30\n";       // line 9 ok
        AppContext ctx = context();
        ImportResult r = ctx.getImporter().importAvailability(write(csv, StandardCharsets.UTF_8));
        check(r.getImportedCount() == 2, "the 2 valid rows should still be imported");
        check(r.getRejectedLines().equals(Arrays.asList(3, 4, 5, 6, 7)), "rows 3-7 should be rejected: " + r.getRejectedLines());
        check(ctx.getSlots().getAll().size() == 2, "rejected rows must not create slots");
    }

    static void missingFileImportsNothing() {
        AppContext ctx = context();
        expectCode("FILE_NOT_FOUND", () -> ctx.getImporter().importAvailability(Path.of("no_such_file.csv")));
        check(ctx.getSlots().getAll().isEmpty(), "nothing may be imported");
    }

    static void wrongHeaderImportsNothing() throws IOException {
        AppContext ctx = context();
        Path p = write("Name,Day,From,To\nAmélie,2026-11-02,09:00,09:30\n", StandardCharsets.UTF_8);
        expectCode("INVALID_CSV_HEADER", () -> ctx.getImporter().importAvailability(p));
        check(ctx.getSlots().getAll().isEmpty(), "nothing may be imported");
    }

    static void windows1252FileKeepsAccents() throws IOException {
        AppContext ctx = context();
        String csv = HEADER + "\nOlivier,Côté,Dr.,Pédiatrie,2026-11-05,10:00,10:30\n";
        ImportResult r = ctx.getImporter().importAvailability(write(csv, Charset.forName("windows-1252")));
        check(r.getImportedCount() == 1, "Excel-style Windows-1252 file should import");
        check(ctx.getProfessionals().getAll().get(0).getLastName().equals("Côté"), "accent must survive Windows-1252");
    }

    static void exportThenImportGivesTheSameFile() throws IOException {
        AppContext first = context();
        first.getImporter().importAvailability(write(SAMPLE, StandardCharsets.UTF_8));
        Path out1 = Files.createTempFile("export1", ".csv");
        check(first.getExporter().exportAvailability(out1) == 8, "8 slots should be exported");

        AppContext second = context();
        ImportResult r = second.getImporter().importAvailability(out1);
        check(r.getImportedCount() == 8 && r.getRejectedLines().isEmpty(), "exported file must import back fully");
        Path out2 = Files.createTempFile("export2", ".csv");
        second.getExporter().exportAvailability(out2);
        check(Files.readAllLines(out1, StandardCharsets.UTF_8).equals(Files.readAllLines(out2, StandardCharsets.UTF_8)),
                "export -> import -> export must give an identical file");
        List<String> lines = Files.readAllLines(out1, StandardCharsets.UTF_8);
        check(lines.get(0).equals(HEADER), "export must start with the import header");
        check(lines.get(1).equals("Amélie,Tremblay,Dr.,Family Medicine,2026-11-02,09:00,09:30"), "row format: " + lines.get(1));
    }

    static void exportWithNoAvailabilityWritesOnlyTheHeader() throws IOException {
        Path out = Files.createTempFile("empty", ".csv");
        check(context().getExporter().exportAvailability(out) == 0, "nothing to export");
        check(Files.readAllLines(out, StandardCharsets.UTF_8).equals(List.of(HEADER)), "only the header row");
    }

    static void exportToMissingFolderFails() {
        expectCode("CANNOT_WRITE_FILE",
                () -> context().getExporter().exportAvailability(Path.of("no_such_folder", "out.csv")));
    }

    // ------------------------------------------------------------ helpers
    static AppContext context() {
        AppContext ctx = new AppContext();
        ctx.seedDemoData();
        return ctx;
    }

    static Path write(String content, Charset charset) throws IOException {
        Path p = Files.createTempFile("availability", ".csv");
        Files.write(p, content.getBytes(charset));
        return p;
    }

    static void expectCode(String code, Runnable action) {
        try {
            action.run();
        } catch (CsvException e) {
            check(code.equals(e.getCode()), "expected " + code + " but got " + e.getCode());
            return;
        }
        throw new AssertionError("expected CsvException " + code);
    }

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}

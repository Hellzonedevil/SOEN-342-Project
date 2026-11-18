package csv;

import booking.AvailabilityRepository;
import booking.AvailabilitySlot;
import booking.HealthcareProfessional;
import booking.ProfessionalRepository;
import booking.TimeInterval;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * exportAvailability(filePath) - SSDs E1 and E2.
 * Writes every availability slot with the same columns as the import file, so the file can be imported back.
 */
public class AvailabilityCsvExporter {
    private final AvailabilityRepository slots;
    private final ProfessionalRepository professionals;

    public AvailabilityCsvExporter(AvailabilityRepository slots, ProfessionalRepository professionals) {
        this.slots = slots;
        this.professionals = professionals;
    }

    /** Returns the number of slots written. Availability in the system is never changed by an export. */
    public int exportAvailability(Path filePath) {
        if (filePath == null) {
            throw new CsvException("CANNOT_WRITE_FILE");
        }
        List<AvailabilitySlot> all = slots.getAll();
        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            writer.write(CsvFormat.headerLine());
            writer.newLine();
            for (AvailabilitySlot slot : all) {
                writer.write(toRow(slot));
                writer.newLine();
            }
        } catch (IOException e) {
            throw new CsvException("CANNOT_WRITE_FILE");
        }
        return all.size();
    }

    private String toRow(AvailabilitySlot slot) {
        HealthcareProfessional p = professionals.findById(slot.getProfessionalId());
        TimeInterval interval = slot.getInterval();
        String specialty = p.getSpecialty() == null ? "General" : p.getSpecialty().getName();
        return String.join(",",
                CsvFormat.escape(p.getFirstName()),
                CsvFormat.escape(p.getLastName()),
                CsvFormat.escape(p.getTitle()),
                CsvFormat.escape(specialty),
                interval.getStartsAt().toLocalDate().format(CsvFormat.DATE),
                interval.getStartsAt().toLocalTime().format(CsvFormat.TIME),
                interval.getEndsAt().toLocalTime().format(CsvFormat.TIME));
    }
}

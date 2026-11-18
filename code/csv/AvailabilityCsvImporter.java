package csv;

import app.ServiceCatalog;
import booking.AvailabilityException;
import booking.AvailabilityService;
import booking.HealthcareProfessional;
import booking.ProfessionalRepository;
import booking.Specialty;
import booking.TimeInterval;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * importAvailability(filePath) - SSDs I1 and I2.
 * File-level problems (missing file, wrong header) import nothing.
 * A bad row is skipped and reported by line number; valid rows are still imported.
 */
public class AvailabilityCsvImporter {
    private final AvailabilityService availability;
    private final ProfessionalRepository professionals;
    private final ServiceCatalog services;

    public AvailabilityCsvImporter(AvailabilityService availability, ProfessionalRepository professionals,
            ServiceCatalog services) {
        this.availability = availability;
        this.professionals = professionals;
        this.services = services;
    }

    public ImportResult importAvailability(Path filePath) {
        if (filePath == null || !Files.isRegularFile(filePath)) {
            throw new CsvException("FILE_NOT_FOUND");
        }
        List<String> lines = readLines(filePath);
        if (lines.isEmpty() || !isValidHeader(lines.get(0))) {
            throw new CsvException("INVALID_CSV_HEADER");
        }
        ImportResult result = new ImportResult();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) {
                continue; // blank lines (for example a trailing newline) are not rows
            }
            parseRow(line, i + 1, result);
        }
        return result;
    }

    /** Validates one row and adds it as an availability slot, or records why it was rejected. */
    void parseRow(String line, int lineNumber, ImportResult result) {
        List<String> f = CsvFormat.split(line);
        if (f.size() != CsvFormat.HEADER.length) {
            result.reject(lineNumber, "expected 7 columns but found " + f.size());
            return;
        }
        for (int c = 0; c < f.size(); c++) {
            if (f.get(c).isEmpty()) {
                result.reject(lineNumber, "missing " + CsvFormat.HEADER[c]);
                return;
            }
        }
        LocalDate date;
        LocalTime start;
        LocalTime end;
        try {
            date = LocalDate.parse(f.get(4), CsvFormat.DATE);
            start = LocalTime.parse(f.get(5), CsvFormat.TIME);
            end = LocalTime.parse(f.get(6), CsvFormat.TIME);
        } catch (DateTimeParseException e) {
            result.reject(lineNumber, "invalid date or time (expected yyyy-MM-dd and HH:mm)");
            return;
        }
        if (!start.isBefore(end)) {
            result.reject(lineNumber, "end time must be after start time");
            return;
        }
        Specialty specialty = services.findOrCreateSpecialty(f.get(3));
        HealthcareProfessional professional = professionals.findOrCreate(f.get(0), f.get(1), f.get(2), specialty);
        try {
            availability.addSlot(professional.getProfessionalId(),
                    new TimeInterval(LocalDateTime.of(date, start), LocalDateTime.of(date, end)));
            result.addImported();
        } catch (AvailabilityException e) {
            result.reject(lineNumber, e.getCode().equals("AVAILABILITY_OVERLAP")
                    ? "overlaps an existing slot of " + professional.getFirstName() + " " + professional.getLastName()
                    : e.getCode());
        }
    }

    /** Reads UTF-8 (accented names such as Amélie or Côté); falls back to Windows-1252, which Excel often uses. */
    private List<String> readLines(Path filePath) {
        try {
            return Files.readAllLines(filePath, StandardCharsets.UTF_8);
        } catch (MalformedInputException e) {
            try {
                return Files.readAllLines(filePath, Charset.forName("windows-1252"));
            } catch (IOException e2) {
                throw new CsvException("FILE_NOT_FOUND");
            }
        } catch (IOException e) {
            throw new CsvException("FILE_NOT_FOUND");
        }
    }

    private boolean isValidHeader(String line) {
        String cleaned = line.startsWith("﻿") ? line.substring(1) : line; // Excel adds a BOM to UTF-8 files
        List<String> fields = CsvFormat.split(cleaned);
        if (fields.size() != CsvFormat.HEADER.length) {
            return false;
        }
        for (int i = 0; i < fields.size(); i++) {
            if (!fields.get(i).equalsIgnoreCase(CsvFormat.HEADER[i])) {
                return false;
            }
        }
        return true;
    }
}

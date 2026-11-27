package app;

import booking.Appointment;
import booking.AvailabilityException;
import booking.AvailabilitySlot;
import booking.BookingException;
import booking.HealthcareProfessional;
import booking.Patient;
import booking.PatientSearchCriteria;
import booking.RepetitionRule;
import booking.Specialty;
import booking.TimeInterval;
import csv.CsvException;
import csv.ImportResult;
import persistence.PersistenceException;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.Set;

/** Command-line front end. It reads input, calls the services, and prints their results. */
public class ClinicCLI {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final AppContext context;
    private final Scanner input;
    private final PrintStream out;

    public ClinicCLI(AppContext context, Scanner input, PrintStream out) {
        this.context = context;
        this.input = input;
        this.out = out;
    }

    public static void main(String[] args) {
        try (AppContext context = new AppContext(Paths.get("clinic.db"))) {
            context.seedDemoData();
            PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
            new ClinicCLI(context, new Scanner(System.in, StandardCharsets.UTF_8), out).run();
        } catch (PersistenceException exception) {
            System.err.println("ERROR: " + exception.getMessage());
        }
    }

    public void run() {
        out.println("=== Appointment Tracking System - Iteration III ===");
        while (true) {
            out.println();
            out.println("1. Make availability");
            out.println("2. Search availability");
            out.println("3. Book appointment");
            out.println("4. Import availability from CSV");
            out.println("5. Export availability to CSV");
            out.println("6. List professionals and services");
            out.println("7. Cancel appointment");
            out.println("8. Reschedule appointment");
            out.println("0. Exit");
            String choice = ask("Choice: ");
            if (choice == null || choice.equals("0")) {
                out.println("Goodbye.");
                return;
            }
            try {
                switch (choice) {
                    case "1" -> makeAvailability();
                    case "2" -> searchAvailability();
                    case "3" -> bookAppointment();
                    case "4" -> importCsv();
                    case "5" -> exportCsv();
                    case "6" -> listReferenceData();
                    case "7" -> cancelAppointment();
                    case "8" -> rescheduleAppointment();
                    default -> out.println("Please choose a number from the menu.");
                }
            } catch (BookingException e) {
                out.println("ERROR: " + e.getCode());
            } catch (AvailabilityException e) {
                out.println("ERROR: " + e.getCode());
            } catch (CsvException e) {
                out.println("ERROR: " + e.getCode());
            } catch (DateTimeParseException e) {
                out.println("ERROR: INVALID_INPUT (dates are yyyy-MM-dd, times are HH:mm)");
            } catch (IllegalArgumentException e) {
                out.println("ERROR: INVALID_INPUT (" + e.getMessage() + ")");
            } catch (EndOfInput e) {
                out.println("Goodbye.");
                return;
            }
        }
    }

    /** Make availability: professional + date range + repeat pattern + start/end time (SSD A1). */
    void makeAvailability() {
        HealthcareProfessional professional = chooseProfessional(true);
        if (professional == null) {
            return;
        }
        LocalDate startDate = LocalDate.parse(require("Start date (yyyy-MM-dd): "));
        LocalDate endDate = LocalDate.parse(require("End date (yyyy-MM-dd): "));
        RepetitionRule rule = askRepetitionRule();
        LocalTime startTime = LocalTime.parse(require("Start time (HH:mm): "));
        LocalTime endTime = LocalTime.parse(require("End time (HH:mm): "));
        List<AvailabilitySlot> created = context.getAvailabilityService()
                .createAvailability(professional.getProfessionalId(), startDate, endDate, rule, startTime, endTime);
        out.println("CREATED: " + created.size() + " availability slot(s) for " + describe(professional));
        for (AvailabilitySlot slot : created) {
            out.println("  " + slot.getSlotId() + "  " + format(slot.getInterval()));
        }
    }

    /** Search availability: professional + service + date range; booked times are not offered (SSD A3). */
    void searchAvailability() {
        HealthcareProfessional professional = chooseProfessional(false);
        if (professional == null) {
            return;
        }
        Service service = chooseService();
        if (service == null) {
            return;
        }
        LocalDate from = LocalDate.parse(require("From date (yyyy-MM-dd): "));
        LocalDate to = LocalDate.parse(require("To date (yyyy-MM-dd): "));
        TimeInterval period = new TimeInterval(from.atStartOfDay(), to.plusDays(1).atStartOfDay());
        List<TimeInterval> slots = context.getBookingService()
                .findAvailableSlots(professional.getProfessionalId(), service.getServiceId(), period);
        if (slots.isEmpty()) {
            out.println("No available times for " + describe(professional) + " in that period.");
            return;
        }
        out.println(slots.size() + " available time(s) for " + service.getName() + " with " + describe(professional) + ":");
        for (TimeInterval slot : slots) {
            out.println("  " + format(slot));
        }
    }

    /** Book appointment: search the patient (C1 searchPatients), then bookAppointment without endsAt. */
    void bookAppointment() {
        Patient patient = choosePatient();
        if (patient == null) {
            return;
        }
        HealthcareProfessional professional = chooseProfessional(false);
        if (professional == null) {
            return;
        }
        Service service = chooseService();
        if (service == null) {
            return;
        }
        LocalDateTime startsAt = LocalDateTime.parse(require("Start (yyyy-MM-dd HH:mm): "), DATE_TIME);
        String referralId = ask("Referral ID (leave blank if none): ");
        Appointment appointment = context.getBookingService().bookAppointment(patient.getPatientId(),
                professional.getProfessionalId(), service.getServiceId(), startsAt,
                referralId == null || referralId.isBlank() ? null : referralId);
        out.println("CONFIRMED: " + appointment.getAppointmentId() + "  " + patient.getFirstName() + " "
                + patient.getLastName() + " with " + describe(professional) + ", " + service.getName() + ", "
                + format(appointment.getInterval()));
    }

    void importCsv() {
        Path path = Paths.get(require("CSV file to import: "));
        ImportResult result = context.getImporter().importAvailability(path);
        out.println("IMPORTED: " + result.getImportedCount() + " slot(s) added, "
                + result.getRejectedLines().size() + " row(s) rejected");
        for (String reason : result.getRejectionReasons()) {
            out.println("  rejected " + reason);
        }
    }

    void exportCsv() {
        Path path = Paths.get(require("CSV file to write: "));
        int count = context.getExporter().exportAvailability(path);
        out.println("EXPORTED: " + count + " slot(s) written to " + path);
    }

    void cancelAppointment() {
        Appointment appointment = chooseAppointment();
        if (appointment == null) {
            return;
        }
        Appointment cancelled = context.getBookingService().cancelAppointment(appointment.getAppointmentId());
        out.println("CANCELLED: " + cancelled.getAppointmentId());
    }

    void rescheduleAppointment() {
        Appointment appointment = chooseAppointment();
        if (appointment == null) {
            return;
        }
        LocalDateTime newStartsAt = LocalDateTime.parse(require("New start (yyyy-MM-dd HH:mm): "), DATE_TIME);
        Appointment rescheduled = context.getBookingService().rescheduleAppointment(appointment.getAppointmentId(), newStartsAt);
        out.println("RESCHEDULED: " + rescheduled.getAppointmentId() + "  " + format(rescheduled.getInterval()));
    }

    private Appointment chooseAppointment() {
        List<Appointment> appointments = context.getAppointments().getAll();
        if (appointments.isEmpty()) {
            out.println("No appointments have been booked.");
            return null;
        }
        for (Appointment appointment : appointments) {
            out.println("  " + appointment.getAppointmentId() + "  " + appointment.getStatus() + "  " + format(appointment.getInterval()));
        }
        String id = require("Appointment ID: ");
        for (Appointment appointment : appointments) {
            if (appointment.getAppointmentId().equalsIgnoreCase(id)) {
                return appointment;
            }
        }
        throw new BookingException("APPOINTMENT_NOT_FOUND");
    }

    void listReferenceData() {
        out.println("Professionals:");
        if (context.getProfessionals().getAll().isEmpty()) {
            out.println("  (none yet - import a CSV file or make availability)");
        }
        for (HealthcareProfessional p : context.getProfessionals().getAll()) {
            out.println("  " + p.getProfessionalId() + "  " + describe(p));
        }
        out.println("Services:");
        for (Service s : context.getServices().getAllServices()) {
            out.println("  " + s.getServiceId() + "  " + s.getName() + " (" + s.getDurationMinutes() + " min"
                    + (s.isRequiresReferral() ? ", referral required" : "") + ")");
        }
    }

    // ---------------------------------------------------------------- helpers

    private HealthcareProfessional chooseProfessional(boolean allowNew) {
        List<HealthcareProfessional> all = context.getProfessionals().getAll();
        for (HealthcareProfessional p : all) {
            out.println("  " + p.getProfessionalId() + "  " + describe(p));
        }
        String prompt = allowNew ? "Professional ID (or N for a new professional): " : "Professional ID: ";
        if (all.isEmpty() && !allowNew) {
            out.println("No professionals yet - import a CSV file or make availability first.");
            return null;
        }
        String id = require(prompt);
        if (allowNew && id.equalsIgnoreCase("N")) {
            String first = require("First name: ");
            String last = require("Last name: ");
            String title = require("Title (e.g. Dr.): ");
            Specialty specialty = context.getServices().findOrCreateSpecialty(require("Specialty: "));
            return context.getProfessionals().findOrCreate(first, last, title, specialty);
        }
        HealthcareProfessional p = context.getProfessionals().findById(id);
        if (p == null) {
            out.println("ERROR: PROFESSIONAL_NOT_FOUND");
        }
        return p;
    }

    private Service chooseService() {
        for (Service s : context.getServices().getAllServices()) {
            out.println("  " + s.getServiceId() + "  " + s.getName() + " (" + s.getDurationMinutes() + " min)");
        }
        Service s = context.getServices().findService(require("Service ID: "));
        if (s == null) {
            out.println("ERROR: INVALID_BOOKING_DATA (unknown service)");
        }
        return s;
    }

    private Patient choosePatient() {
        String last = ask("Patient last name (part is fine): ");
        String phone = ask("or telephone (leave blank to skip): ");
        List<Patient> found = context.getBookingService()
                .searchPatients(new PatientSearchCriteria(null, null, phone, null, last));
        if (found.isEmpty()) {
            out.println("No matching patient.");
            return null;
        }
        for (Patient p : found) {
            out.println("  " + p.getPatientId() + "  " + p.getFirstName() + " " + p.getLastName() + "  " + p.getPhoneNumber());
        }
        if (found.size() == 1) {
            return found.get(0);
        }
        String id = require("Patient ID: ");
        for (Patient p : found) {
            if (p.getPatientId().equalsIgnoreCase(id)) {
                return p;
            }
        }
        out.println("ERROR: PATIENT_NOT_FOUND");
        return null;
    }

    private RepetitionRule askRepetitionRule() {
        String type = require("Repeat WEEKLY or MONTHLY: ").toUpperCase(Locale.ROOT);
        if (type.startsWith("W")) {
            Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
            for (String d : require("Weekdays (e.g. MON,WED,FRI): ").split(",")) {
                days.add(parseDay(d.trim()));
            }
            return RepetitionRule.weekly(days);
        }
        if (type.startsWith("M")) {
            return RepetitionRule.monthly(Integer.parseInt(require("Day of month (1-31): ")));
        }
        throw new IllegalArgumentException("repeat pattern must be WEEKLY or MONTHLY");
    }

    private static DayOfWeek parseDay(String text) {
        String t = text.toUpperCase(Locale.ROOT);
        for (DayOfWeek d : DayOfWeek.values()) {
            if (t.length() >= 3 && d.name().startsWith(t.substring(0, 3))) {
                return d;
            }
        }
        throw new IllegalArgumentException("unknown weekday " + text);
    }

    private static String describe(HealthcareProfessional p) {
        String specialty = p.getSpecialty() == null ? "" : " (" + p.getSpecialty().getName() + ")";
        return p.getTitle() + " " + p.getFirstName() + " " + p.getLastName() + specialty;
    }

    private static String format(TimeInterval i) {
        return i.getStartsAt().format(DATE_TIME) + "-" + i.getEndsAt().format(TIME);
    }

    /** Returns the next trimmed line, or null when input has ended. */
    private String ask(String prompt) {
        out.print(prompt);
        if (!input.hasNextLine()) {
            return null;
        }
        return input.nextLine().trim();
    }

    /** Like ask, but the answer may not be blank. */
    private String require(String prompt) {
        String answer = ask(prompt);
        if (answer == null) {
            throw new EndOfInput();
        }
        if (answer.isEmpty()) {
            throw new IllegalArgumentException("a value is required");
        }
        return answer;
    }

    private static class EndOfInput extends RuntimeException { }
}

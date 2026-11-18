package booking;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AvailabilityServiceTest {
    private static final String PROFESSIONAL_ID = "PRO-1";
    private static final String SERVICE_ID = "SVC-1";

    public static void main(String[] args) {
        weeklyRecurrenceGeneratesSearchableAppointmentIntervals();
        monthlyRecurrenceSkipsUnavailableCalendarDates();
        overlappingAndInvalidAvailabilityAreRejectedWithoutPartialWrites();
        professionalFindOrCreateReusesAndCreatesRecords();
        bookingSearchDoesNotReturnTheBookedCandidate();
        System.out.println("AvailabilityServiceTest passed!");
    }

    private static void weeklyRecurrenceGeneratesSearchableAppointmentIntervals() {
        AvailabilityRepository repository = new AvailabilityRepository();
        AvailabilityService service = createService(repository);
        service.createAvailability(PROFESSIONAL_ID, LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 11),
                RepetitionRule.weekly(new java.util.HashSet<>(Arrays.asList(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY))),
                LocalTime.of(9, 0), LocalTime.of(12, 0));

        check(repository.getAll().size() == 2, "weekly rule should create two schedule intervals");
        TimeInterval search = interval(2026, 1, 7, 9, 0, 12, 0);
        List<TimeInterval> candidates = service.findAvailableSlots(PROFESSIONAL_ID, SERVICE_ID, search);
        check(candidates.size() == 3, "three one-hour candidates should fit the Wednesday schedule");
        check(service.isAvailable(PROFESSIONAL_ID, SERVICE_ID, interval(2026, 1, 7, 10, 0, 11, 0)),
                "candidate within a published schedule should be available");
        check(!service.isAvailable(PROFESSIONAL_ID, SERVICE_ID,
                new TimeInterval(LocalDateTime.of(2026, 1, 7, 10, 0), LocalDateTime.of(2026, 1, 7, 11, 0, 1))),
                "candidate duration must exactly match the service duration");
        check(!service.isAvailable(PROFESSIONAL_ID, SERVICE_ID, interval(2026, 1, 7, 9, 15, 10, 15)),
                "candidate starts must align to the service slot grid");
        check(!service.isAvailable(PROFESSIONAL_ID, SERVICE_ID, interval(2026, 1, 7, 8, 0, 9, 0)),
                "candidate outside the published schedule should not be available");
        check(service.findAvailableSlots(PROFESSIONAL_ID, SERVICE_ID,
                interval(2026, 1, 8, 9, 0, 12, 0)).isEmpty(), "non-matching weekday should have no candidates");
    }

    private static void monthlyRecurrenceSkipsUnavailableCalendarDates() {
        AvailabilityRule rule = new AvailabilityRule(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31),
                RepetitionRule.monthly(31), LocalTime.of(9, 0), LocalTime.of(10, 0));
        List<TimeInterval> intervals = rule.generateIntervals();
        check(intervals.size() == 2, "day 31 should occur in January and March, but not February");
        check(intervals.get(0).getStartsAt().toLocalDate().equals(LocalDate.of(2026, 1, 31)),
                "first monthly interval should be January 31");
        check(intervals.get(1).getStartsAt().toLocalDate().equals(LocalDate.of(2026, 3, 31)),
                "second monthly interval should be March 31");
    }

    private static void overlappingAndInvalidAvailabilityAreRejectedWithoutPartialWrites() {
        AvailabilityRepository repository = new AvailabilityRepository();
        AvailabilityService service = createService(repository);
        service.addSlot(PROFESSIONAL_ID, interval(2026, 1, 12, 11, 30, 12, 30));

        expectCode("AVAILABILITY_OVERLAP", () -> service.createAvailability(PROFESSIONAL_ID,
                LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 12),
                RepetitionRule.weekly(Collections.singleton(DayOfWeek.MONDAY)),
                LocalTime.of(11, 0), LocalTime.of(13, 0)));
        check(repository.getAll().size() == 1, "failed recurrence must not partially add schedule intervals");

        expectCode("INVALID_AVAILABILITY_DATA", () -> new AvailabilityRule(LocalDate.of(2026, 1, 5),
                LocalDate.of(2026, 1, 5), RepetitionRule.weekly(Collections.singleton(DayOfWeek.MONDAY)),
                LocalTime.of(12, 0), LocalTime.of(12, 0)));
    }

    private static void professionalFindOrCreateReusesAndCreatesRecords() {
        ProfessionalRepository repository = new ProfessionalRepository();
        HealthcareProfessional existing = new HealthcareProfessional("PRO-1", "Alex", "Khoury", "Dr.");
        repository.add(existing);

        HealthcareProfessional matched = repository.findOrCreate(" alex ", "KHOURY", "dr.");
        check(matched == existing, "matching professional should be reused");
        check(repository.getAll().size() == 1, "reusing a professional should not add another record");

        HealthcareProfessional created = repository.findOrCreate("Sam", "Lee", "Dr.");
        check("PRO-2".equals(created.getProfessionalId()), "new professional should receive an unused identifier");
        check(repository.findById(created.getProfessionalId()) == created,
                "new professional should be stored in the repository");

        expectIllegalArgument(() -> repository.findOrCreate(" ", "Lee", "Dr."));
    }

    private static void bookingSearchDoesNotReturnTheBookedCandidate() {
        AvailabilityRepository availabilityRepository = new AvailabilityRepository();
        AvailabilityService availability = createService(availabilityRepository);
        availability.createAvailability(PROFESSIONAL_ID, LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 5),
                RepetitionRule.weekly(Collections.singleton(DayOfWeek.MONDAY)),
                LocalTime.of(9, 0), LocalTime.of(12, 0));

        PatientRepository patients = new PatientRepository();
        Patient patient = new Patient("PAT-1", "Alex", "Khoury", "555-0100", null, LocalDate.of(1990, 1, 1));
        patients.add(patient);
        BookingCatalog catalog = new BookingCatalog() {
            @Override public boolean professionalExists(String professionalId) { return PROFESSIONAL_ID.equals(professionalId); }
            @Override public int getDurationMinutes(String serviceId) { return 60; }
            @Override public boolean serviceExists(String serviceId) { return SERVICE_ID.equals(serviceId); }
            @Override public boolean requiresReferral(String serviceId) { return false; }
            @Override public boolean referralIsEligible(String referralId, String patientId, String professionalId, String serviceId) { return true; }
        };
        BookingService booking = new BookingService(patients, new AppointmentRepository(), availability, catalog,
                new BookingAuditLog());

        TimeInterval day = interval(2026, 1, 5, 9, 0, 12, 0);
        check(booking.findAvailableSlots(PROFESSIONAL_ID, SERVICE_ID, day).size() == 3,
                "all candidates should be available before booking");
        Appointment booked = booking.bookAppointment("PAT-1", PROFESSIONAL_ID, SERVICE_ID,
                LocalDateTime.of(2026, 1, 5, 10, 0), null);
        check(booked.getLocationId() == null, "booking without a location parameter should leave location unspecified");
        List<TimeInterval> remaining = booking.findAvailableSlots(PROFESSIONAL_ID, SERVICE_ID, day);
        check(remaining.size() == 2, "booking should remove only the conflicting candidate from search");
        check(!containsInterval(remaining, interval(2026, 1, 5, 10, 0, 11, 0)),
                "booked candidate should not be returned");
    }

    private static AvailabilityService createService(AvailabilityRepository repository) {
        ProfessionalRepository professionals = new ProfessionalRepository();
        professionals.add(new HealthcareProfessional(PROFESSIONAL_ID, "Alex", "Khoury", "Physiotherapist"));
        return new AvailabilityService(professionals, repository, serviceId -> 60);
    }

    private static TimeInterval interval(int year, int month, int day, int startHour, int startMinute,
            int endHour, int endMinute) {
        return new TimeInterval(LocalDateTime.of(year, month, day, startHour, startMinute),
                LocalDateTime.of(year, month, day, endHour, endMinute));
    }

    private static boolean containsInterval(List<TimeInterval> intervals, TimeInterval expected) {
        for (TimeInterval interval : intervals) {
            if (interval.getStartsAt().equals(expected.getStartsAt())
                    && interval.getEndsAt().equals(expected.getEndsAt())) {
                return true;
            }
        }
        return false;
    }

    private static void expectCode(String expectedCode, Runnable action) {
        try {
            action.run();
        } catch (AvailabilityException exception) {
            check(expectedCode.equals(exception.getCode()), "expected error code " + expectedCode);
            return;
        }
        throw new AssertionError("expected AvailabilityException " + expectedCode);
    }

    private static void expectIllegalArgument(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException exception) {
            return;
        }
        throw new AssertionError("expected IllegalArgumentException");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}

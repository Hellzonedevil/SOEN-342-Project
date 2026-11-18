package booking;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class AvailabilityService implements AvailabilityPort {
    private final ProfessionalRepository professionals;
    private final AvailabilityRepository availability;
    private final ServiceDurationProvider serviceDurations;

    public AvailabilityService(ProfessionalRepository professionals, AvailabilityRepository availability,
            ServiceDurationProvider serviceDurations) {
        if (professionals == null || availability == null || serviceDurations == null) {
            throw new IllegalArgumentException("Availability dependencies are required");
        }
        this.professionals = professionals;
        this.availability = availability;
        this.serviceDurations = serviceDurations;
    }

    public List<AvailabilitySlot> createAvailability(String professionalId, LocalDate startDate, LocalDate endDate,
            RepetitionRule repetitionRule, LocalTime startTime, LocalTime endTime) {
        requireProfessional(professionalId);
        AvailabilityRule rule = new AvailabilityRule(startDate, endDate, repetitionRule, startTime, endTime);
        return availability.createAll(professionalId, rule.generateIntervals());
    }

    public AvailabilitySlot addSlot(String professionalId, TimeInterval interval) {
        requireProfessional(professionalId);
        if (interval == null) {
            throw new AvailabilityException("INVALID_AVAILABILITY_DATA");
        }
        return availability.create(professionalId, interval);
    }

    @Override
    public List<TimeInterval> findAvailableSlots(String professionalId, String serviceId,
            TimeInterval requestedPeriod) {
        requireProfessional(professionalId);
        if (isBlank(serviceId)) {
            throw new AvailabilityException("INVALID_AVAILABILITY_DATA");
        }
        if (requestedPeriod == null) {
            throw new AvailabilityException("INVALID_AVAILABILITY_DATA");
        }
        Duration duration = Duration.ofMinutes(getDurationMinutes(serviceId));
        List<TimeInterval> result = new ArrayList<>();

        for (AvailabilitySlot slot : availability.findByProfessionalId(professionalId)) {
            TimeInterval schedule = slot.getInterval();
            LocalDateTime candidateStart = schedule.getStartsAt();
            while (!candidateStart.plus(duration).isAfter(schedule.getEndsAt())) {
                LocalDateTime candidateEnd = candidateStart.plus(duration);
                if (!candidateStart.isBefore(requestedPeriod.getStartsAt())
                        && !candidateEnd.isAfter(requestedPeriod.getEndsAt())) {
                    result.add(new TimeInterval(candidateStart, candidateEnd));
                }
                candidateStart = candidateEnd;
            }
        }
        return result;
    }

    @Override
    public boolean isAvailable(String professionalId, String serviceId, TimeInterval proposed) {
        requireProfessional(professionalId);
        if (isBlank(serviceId)) {
            throw new AvailabilityException("INVALID_AVAILABILITY_DATA");
        }
        if (proposed == null) {
            throw new AvailabilityException("INVALID_AVAILABILITY_DATA");
        }
        int durationMinutes = getDurationMinutes(serviceId);
        Duration requiredDuration = Duration.ofMinutes(durationMinutes);
        if (!Duration.between(proposed.getStartsAt(), proposed.getEndsAt()).equals(requiredDuration)) {
            return false;
        }
        for (AvailabilitySlot slot : availability.findByProfessionalId(professionalId)) {
            TimeInterval schedule = slot.getInterval();
            Duration offset = Duration.between(schedule.getStartsAt(), proposed.getStartsAt());
            if (schedule.contains(proposed) && !offset.isNegative() && offset.getNano() == 0
                    && offset.getSeconds() % requiredDuration.getSeconds() == 0) {
                return true;
            }
        }
        return false;
    }

    private int getDurationMinutes(String serviceId) {
        int durationMinutes = serviceDurations.getDurationMinutes(serviceId);
        if (durationMinutes <= 0) {
            throw new AvailabilityException("INVALID_SERVICE_DURATION");
        }
        return durationMinutes;
    }

    private void requireProfessional(String professionalId) {
        if (isBlank(professionalId)) {
            throw new AvailabilityException("INVALID_AVAILABILITY_DATA");
        }
        if (!professionals.existsById(professionalId)) {
            throw new AvailabilityException("PROFESSIONAL_NOT_FOUND");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

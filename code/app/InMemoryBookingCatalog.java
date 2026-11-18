package app;

import booking.BookingCatalog;
import booking.ProfessionalRepository;

/**
 * Adapter that answers BookingService's catalog questions from the in-memory repositories.
 * It is also given to AvailabilityService as its ServiceDurationProvider.
 */
public class InMemoryBookingCatalog implements BookingCatalog {
    private final ProfessionalRepository professionals;
    private final ServiceCatalog services;

    public InMemoryBookingCatalog(ProfessionalRepository professionals, ServiceCatalog services) {
        this.professionals = professionals;
        this.services = services;
    }

    @Override
    public boolean professionalExists(String professionalId) {
        return professionalId != null && professionals.existsById(professionalId);
    }

    @Override
    public boolean serviceExists(String serviceId) {
        return services.findService(serviceId) != null;
    }

    @Override
    public boolean requiresReferral(String serviceId) {
        Service s = services.findService(serviceId);
        return s != null && s.isRequiresReferral();
    }

    @Override
    public int getDurationMinutes(String serviceId) {
        Service s = services.findService(serviceId);
        return s == null ? 0 : s.getDurationMinutes();
    }

    /** Referrals (C3) are designed but not part of the Iteration II PoC, so no referral can be eligible yet. */
    @Override
    public boolean referralIsEligible(String referralId, String patientId, String professionalId, String serviceId) {
        return false;
    }
}

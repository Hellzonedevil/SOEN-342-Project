package app;

import booking.AppointmentRepository;
import booking.AvailabilityRepository;
import booking.AvailabilityService;
import booking.BookingAuditLog;
import booking.BookingService;
import booking.Patient;
import booking.PatientRepository;
import booking.ProfessionalRepository;
import csv.AvailabilityCsvExporter;
import csv.AvailabilityCsvImporter;

import java.time.LocalDate;

/** Creates every object once and wires them together (the only place that knows all the parts). */
public class AppContext {
    private final PatientRepository patients = new PatientRepository();
    private final ProfessionalRepository professionals = new ProfessionalRepository();
    private final ServiceCatalog services = new ServiceCatalog();
    private final AvailabilityRepository slots = new AvailabilityRepository();
    private final AppointmentRepository appointments = new AppointmentRepository();
    private final InMemoryBookingCatalog catalog = new InMemoryBookingCatalog(professionals, services);
    private final AvailabilityService availabilityService = new AvailabilityService(professionals, slots, catalog);
    private final BookingService bookingService =
            new BookingService(patients, appointments, availabilityService, catalog, new BookingAuditLog());
    private final AvailabilityCsvImporter importer =
            new AvailabilityCsvImporter(availabilityService, professionals, services);
    private final AvailabilityCsvExporter exporter = new AvailabilityCsvExporter(slots, professionals);

    /**
     * Availability starts empty (the system is loaded fresh and filled by the CSV import).
     * Patients and services are not part of the CSV, so a few are added here so that booking can be demonstrated.
     */
    public void seedDemoData() {
        services.addService(new Service("SRV-1", "General consultation", 30, false));
        services.addService(new Service("SRV-2", "Cardiology consultation", 60, false));
        services.addService(new Service("SRV-3", "Orthopedic assessment", 45, false));
        services.addService(new Service("SRV-4", "Specialist consultation (referral required)", 30, true));
        patients.add(new Patient("PAT-1", "Émilie", "Lavoie", "514-555-0101", "LAVE99010101", LocalDate.of(1999, 1, 1)));
        patients.add(new Patient("PAT-2", "Olivier", "Côté", "514-555-0102", "COTO85061502", LocalDate.of(1985, 6, 15)));
        patients.add(new Patient("PAT-3", "Fatima", "Benali", "438-555-0103", null, null));
    }

    public BookingService getBookingService() { return bookingService; }
    public AvailabilityService getAvailabilityService() { return availabilityService; }
    public AvailabilityCsvImporter getImporter() { return importer; }
    public AvailabilityCsvExporter getExporter() { return exporter; }
    public PatientRepository getPatients() { return patients; }
    public ProfessionalRepository getProfessionals() { return professionals; }
    public ServiceCatalog getServices() { return services; }
    public AvailabilityRepository getSlots() { return slots; }
    public AppointmentRepository getAppointments() { return appointments; }
}

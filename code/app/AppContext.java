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
import persistence.SqliteDatabase;

import java.nio.file.Path;
import java.sql.Connection;
import java.time.LocalDate;

/** this creates and wires the application objects for either in-memory or SQLite-backed use. */
public class AppContext implements AutoCloseable {
    private final SqliteDatabase database;
    private final PatientRepository patients;
    private final ProfessionalRepository professionals;
    private final ServiceCatalog services;
    private final AvailabilityRepository slots;
    private final AppointmentRepository appointments;
    private final InMemoryBookingCatalog catalog;
    private final AvailabilityService availabilityService;
    private final BookingService bookingService;
    private final AvailabilityCsvImporter importer;
    private final AvailabilityCsvExporter exporter;

    /** we wanna keep the in-memory setup used by the existing focused examples and tests */
    public AppContext() {
        this((SqliteDatabase) null);
    }

    /** Creates a persistent application context backed by the selected SQLite file. */
    public AppContext(Path databaseFile) {
        this(new SqliteDatabase(databaseFile));
    }

    private AppContext(SqliteDatabase database) {
        this.database = database;
        Connection connection = database == null ? null : database.getConnection();
        patients = new PatientRepository(connection);
        professionals = new ProfessionalRepository(connection);
        services = new ServiceCatalog(connection);
        slots = new AvailabilityRepository(connection);
        appointments = new AppointmentRepository(connection, patients);
        catalog = new InMemoryBookingCatalog(professionals, services);
        availabilityService = new AvailabilityService(professionals, slots, catalog);
        bookingService = new BookingService(patients, appointments, availabilityService, catalog, new BookingAuditLog());
        importer = new AvailabilityCsvImporter(availabilityService, professionals, services);
        exporter = new AvailabilityCsvExporter(slots, professionals);
    }

    /** we're addingh the small demo patient and service set if it is not already stored. */
    public void seedDemoData() {
        addServiceIfMissing(new Service("SRV-1", "General consultation", 30, false));
        addServiceIfMissing(new Service("SRV-2", "Cardiology consultation", 60, false));
        addServiceIfMissing(new Service("SRV-3", "Orthopedic assessment", 45, false));
        addServiceIfMissing(new Service("SRV-4", "Specialist consultation (referral required)", 30, true));
        addPatientIfMissing(new Patient("PAT-1", "Émilie", "Lavoie", "514-555-0101", "LAVE99010101", LocalDate.of(1999, 1, 1)));
        addPatientIfMissing(new Patient("PAT-2", "Olivier", "Côté", "514-555-0102", "COTO85061502", LocalDate.of(1985, 6, 15)));
        addPatientIfMissing(new Patient("PAT-3", "Fatima", "Benali", "438-555-0103", null, null));
    }

    private void addServiceIfMissing(Service service) {
        if (services.findService(service.getServiceId()) == null) {
            services.addService(service);
        }
    }

    private void addPatientIfMissing(Patient patient) {
        if (patients.findById(patient.getPatientId()) == null) {
            patients.add(patient);
        }
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

    @Override
    public void close() {
        if (database != null) {
            database.close();
        }
    }
}

package app;

import booking.Specialty;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** In-memory catalog of the clinic's services and specialties (Information Expert for both). */
public class ServiceCatalog {
    private final Map<String, Service> services = new LinkedHashMap<>();
    private final Map<String, Specialty> specialtiesByName = new LinkedHashMap<>();
    private int nextSpecialtyId = 1;

    public void addService(Service service) {
        services.put(service.getServiceId(), service);
    }

    public Service findService(String serviceId) {
        return serviceId == null ? null : services.get(serviceId.trim());
    }

    public List<Service> getAllServices() {
        return new ArrayList<>(services.values());
    }

    public Specialty findSpecialty(String specialtyId) {
        for (Specialty s : specialtiesByName.values()) {
            if (s.getSpecialtyId().equals(specialtyId)) {
                return s;
            }
        }
        return null;
    }

    /** Used by the CSV import: the same specialty name always maps to the same Specialty object. */
    public Specialty findOrCreateSpecialty(String name) {
        String key = name.trim().toLowerCase(Locale.ROOT);
        Specialty existing = specialtiesByName.get(key);
        if (existing != null) {
            return existing;
        }
        Specialty created = new Specialty("SPC-" + nextSpecialtyId++, name.trim());
        specialtiesByName.put(key, created);
        return created;
    }
}

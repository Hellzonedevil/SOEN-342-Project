package booking;

import java.util.ArrayList, java.util.LinkedHashMap, java.util.List, java.util.Map;

// this is basically an in-memory patient lookup 
// (NOTE: WE STILL NEED THE PATIENT REGISTRATION TO DO THIS, BUT FOR NOW I FIGURED WE CAN JUST ADD PATIENTS DIRECTLY)
public class PatientRepository {
    private final Map<String, Patient> patients = new LinkedHashMap<>();

    public void add(Patient patient) {
        if (patient == null || patient.getPatientId() == null || patient.getPatientId().isBlank()) {
            throw new IllegalArgumentException("Patient must have an identifier");
        }
        if (patients.containsKey(patient.getPatientId())) {
            throw new IllegalArgumentException("Duplicate patient identifier");
        }
        patients.put(patient.getPatientId(), patient);
    }

    public Patient findById(String patientId) {
        return patients.get(patientId);
    }

    public List<Patient> search(PatientSearchCriteria criteria) {
        if (criteria == null || criteria.isEmpty()) {
            throw new BookingException("INVALID_SEARCH_CRITERIA");
        }
        List<Patient> matches = new ArrayList<>();
        for (Patient patient : patients.values()) {
            if (criteria.matches(patient)) {
                matches.add(patient);
            }
        }
        return matches;
    }
}

package booking;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ProfessionalRepository {
    private final Map<String, HealthcareProfessional> professionals = new LinkedHashMap<>();
    private int nextGeneratedId = 1;

    public void add(HealthcareProfessional professional) {
        if (professional == null || isBlank(professional.getProfessionalId())) {
            throw new IllegalArgumentException("Professional must have an identifier");
        }
        if (professionals.containsKey(professional.getProfessionalId())) {
            throw new IllegalArgumentException("Duplicate professional identifier");
        }
        professionals.put(professional.getProfessionalId(), professional);
    }

    public HealthcareProfessional findById(String professionalId) {
        return professionals.get(professionalId);
    }

    public boolean existsById(String professionalId) {
        return professionals.containsKey(professionalId);
    }

    public HealthcareProfessional findOrCreate(String firstName, String lastName, String title) {
        String normalizedFirstName = normalizeRequired(firstName, "First name");
        String normalizedLastName = normalizeRequired(lastName, "Last name");
        String normalizedTitle = normalizeRequired(title, "Title");

        for (HealthcareProfessional professional : professionals.values()) {
            if (professional.getFirstName() != null
                    && professional.getLastName() != null
                    && professional.getTitle() != null
                    && professional.getFirstName().equalsIgnoreCase(normalizedFirstName)
                    && professional.getLastName().equalsIgnoreCase(normalizedLastName)
                    && professional.getTitle().equalsIgnoreCase(normalizedTitle)) {
                return professional;
            }
        }

        String professionalId;
        do {
            professionalId = "PRO-" + nextGeneratedId++;
        } while (professionals.containsKey(professionalId));

        HealthcareProfessional created = new HealthcareProfessional(
                professionalId, normalizedFirstName, normalizedLastName, normalizedTitle);
        add(created);
        return created;
    }
    
    public List<HealthcareProfessional> getAll() {
        return new ArrayList<>(professionals.values());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String normalizeRequired(String value, String fieldName) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}

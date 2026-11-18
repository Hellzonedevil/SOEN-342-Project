package booking;

public class HealthcareProfessional {
    private final String professionalId;
    private final String firstName;
    private final String lastName;
    private final String title;
    private final Specialty specialty; // may be null when the specialty is not known

    public HealthcareProfessional(String professionalId, String firstName, String lastName, String title) {
        this(professionalId, firstName, lastName, title, null);
    }

    public HealthcareProfessional(String professionalId, String firstName, String lastName, String title,
            Specialty specialty) {
        this.professionalId = professionalId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.title = title;
        this.specialty = specialty;
    }

    public String getProfessionalId() { return professionalId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getTitle() { return title; }
    public Specialty getSpecialty() { return specialty; }

    /** Information Expert (C3 design): a professional knows which specialty it holds. */
    public boolean holds(Specialty s) {
        return s != null && specialty != null && specialty.getSpecialtyId().equals(s.getSpecialtyId());
    }
}

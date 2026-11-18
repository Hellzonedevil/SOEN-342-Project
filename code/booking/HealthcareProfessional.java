package booking;

public class HealthcareProfessional {
    private final String professionalId;
    private final String firstName;
    private final String lastName;
    private final String title;

    public HealthcareProfessional(String professionalId, String firstName, String lastName, String title) {
        this.professionalId = professionalId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.title = title;
    }

    public String getProfessionalId() { return professionalId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getTitle() { return title; }
}

package booking;

/** A medical specialty (for example Cardiology). Held by healthcare professionals and requested by referrals. */
public class Specialty {
    private final String specialtyId;
    private final String name;

    public Specialty(String specialtyId, String name) {
        if (specialtyId == null || specialtyId.isBlank() || name == null || name.isBlank()) {
            throw new IllegalArgumentException("Specialty needs an identifier and a name");
        }
        this.specialtyId = specialtyId;
        this.name = name.trim();
    }

    public String getSpecialtyId() { return specialtyId; }
    public String getName() { return name; }
}

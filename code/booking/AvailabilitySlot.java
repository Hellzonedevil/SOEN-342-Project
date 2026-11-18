package booking;

public class AvailabilitySlot {
    private final String slotId;
    private final String professionalId;
    private final TimeInterval interval;

    public AvailabilitySlot(String slotId, String professionalId, TimeInterval interval) {
        this.slotId = slotId;
        this.professionalId = professionalId;
        this.interval = interval;
    }

    public String getSlotId() { return slotId; }
    public String getProfessionalId() { return professionalId; }
    public TimeInterval getInterval() { return interval; }
}

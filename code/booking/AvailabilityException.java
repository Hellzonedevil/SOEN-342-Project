package booking;

public class AvailabilityException extends RuntimeException {
    private final String code;

    public AvailabilityException(String code) {
        super(code);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

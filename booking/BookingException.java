package booking;

// basically this is just a custom named failure outcome that BookingService throws before creating an invalid appointment
public class BookingException extends RuntimeException {
    public BookingException(String code) {
        super(code);
    }

    public String getCode() {
        return getMessage();
    }
}

package csv;

/** File-level CSV failure: FILE_NOT_FOUND, INVALID_CSV_HEADER or CANNOT_WRITE_FILE (see SSDs I2 and E2). */
public class CsvException extends RuntimeException {
    private final String code;

    public CsvException(String code) {
        super(code);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

package csv;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Shared CSV layout so that an exported file can always be imported back (round trip). */
final class CsvFormat {
    static final String[] HEADER = {"First Name", "Last Name", "Title", "Specialty", "Date", "Start", "End"};
    static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private CsvFormat() { }

    /** Splits one CSV line, honouring double quotes ("Smith, Jr." stays one field). */
    static List<String> split(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                fields.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString().trim());
        return fields;
    }

    /** Quotes a field only when it contains a comma, a quote or a line break. */
    static String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    static String headerLine() {
        return String.join(",", HEADER);
    }
}

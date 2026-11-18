package csv;

import java.util.ArrayList;
import java.util.List;

/** Outcome of importAvailability: how many slots were added and which lines were skipped (SSD I1). */
public class ImportResult {
    private int importedCount;
    private final List<Integer> rejectedLines = new ArrayList<>();
    private final List<String> rejectionReasons = new ArrayList<>();

    public void addImported() {
        importedCount++;
    }

    public void reject(int lineNumber, String reason) {
        rejectedLines.add(lineNumber);
        rejectionReasons.add("line " + lineNumber + ": " + reason);
    }

    public int getImportedCount() { return importedCount; }
    public List<Integer> getRejectedLines() { return new ArrayList<>(rejectedLines); }
    public List<String> getRejectionReasons() { return new ArrayList<>(rejectionReasons); }
}

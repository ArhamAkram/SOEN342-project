package ats.csv;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** How many rows were imported, and which were skipped and why (with their line numbers). */
public class ImportResult {
    private int imported;
    private final List<String> skipped = new ArrayList<>();

    void imported() {
        imported++;
    }

    void skipped(int lineNumber, String reason) {
        skipped.add("line " + lineNumber + ": " + reason);
    }

    public int getImportedCount() {
        return imported;
    }

    public List<String> getSkipped() {
        return Collections.unmodifiableList(skipped);
    }

    @Override
    public String toString() {
        return imported + " imported, " + skipped.size() + " skipped";
    }
}

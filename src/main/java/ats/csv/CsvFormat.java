package ats.csv;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Minimal RFC 4180-style CSV reading and writing (quoted fields, doubled quotes). */
public final class CsvFormat {
    static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter TIME_IN = DateTimeFormatter.ofPattern("H:mm");

    private CsvFormat() {
    }

    /** Splits one line into fields. Quoted fields may contain commas and doubled quotes. */
    public static List<String> parseLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else if (c == '"') {
                    inQuotes = false;
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
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

    /** Joins fields into one line, quoting any field that needs it. */
    public static String formatLine(List<String> fields) {
        List<String> out = new ArrayList<>();
        for (String f : fields) {
            String value = f == null ? "" : f;
            if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
                value = "\"" + value.replace("\"", "\"\"") + "\"";
            }
            out.add(value);
        }
        return String.join(",", out);
    }

    /** Accepts 9:00 or 09:00. */
    public static LocalTime parseTime(String text) {
        return LocalTime.parse(text.trim(), TIME_IN);
    }

    /** True for an empty value or a dash used to mean "none" (the sample files use an en dash). */
    public static boolean isNone(String value) {
        if (value == null) {
            return true;
        }
        String v = value.trim();
        return v.isEmpty() || v.equals("-") || v.equals("–") || v.equals("—") || v.equalsIgnoreCase("none");
    }

    static String stripBom(String line) {
        return line.startsWith("﻿") ? line.substring(1) : line;
    }
}

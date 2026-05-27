package uy.edu.um.importer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class CSVParser {

    public static void parseCSV(String csvFilePath, LineProcessor processor) {
        try (BufferedReader reader = new BufferedReader(new FileReader(csvFilePath))) {

            reader.readLine(); // descarta encabezado

            String line;
            StringBuilder currentRecord = new StringBuilder();
            boolean inQuotedField = false;

            while ((line = reader.readLine()) != null) {

                if (currentRecord.length() > 0) {
                    currentRecord.append(' ').append(line);
                } else {
                    currentRecord.append(line);
                }

                inQuotedField = false;
                boolean recordComplete = true;

                for (int i = 0; i < currentRecord.length(); i++) {
                    char c = currentRecord.charAt(i);
                    if (c == '"') inQuotedField = !inQuotedField;
                }

                if (inQuotedField) recordComplete = false;

                if (recordComplete) {
                    try {
                        processor.processLine(currentRecord.toString());
                    } catch (Exception e) {
                        // línea malformada, se omite
                    }
                    currentRecord.setLength(0);
                }
            }

            // procesar último registro si quedó pendiente
            if (currentRecord.length() > 0) {
                try {
                    processor.processLine(currentRecord.toString());
                } catch (Exception e) { }
            }

        } catch (IOException e) {
            System.err.println("Error leyendo CSV: " + e.getMessage());
        }
    }

    public static String[] splitCSV(String line) {
        if (line.indexOf('"') == -1) {
            return line.split(",", -1);
        }

        java.util.List<String> fields = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }

    public interface LineProcessor {
        void processLine(String line) throws Exception;
    }
}
package com.wharvex.budget;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class PendingTransactionsCsv {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);

    List<PendingTransaction> load(Path path) throws IOException {
        List<List<String>> rows = parse(Files.readString(path));
        if (rows.isEmpty()) {
            return List.of();
        }

        Map<String, Integer> columns = columns(rows.getFirst());
        int statusColumn = requiredColumn(columns, "Status");
        int dateColumn = requiredColumn(columns, "Date");
        int amountColumn = requiredColumn(columns, "Amount");
        int descriptionColumn = requiredColumn(columns, "Simple Description");
        List<PendingTransaction> transactions = new ArrayList<>();
        for (int rowNumber = 1; rowNumber < rows.size(); rowNumber++) {
            List<String> row = rows.get(rowNumber);
            if (row.stream().allMatch(String::isBlank)) {
                continue;
            }
            if (value(row, statusColumn, rowNumber).trim().equalsIgnoreCase("pending")) {
                transactions.add(new PendingTransaction(
                        LocalDate.parse(value(row, dateColumn, rowNumber).trim(), DATE_FORMAT),
                        value(row, descriptionColumn, rowNumber).trim(),
                        new BigDecimal(value(row, amountColumn, rowNumber).trim()
                                .replace("$", "").replace(",", ""))));
            }
        }
        return transactions;
    }

    private Map<String, Integer> columns(List<String> header) {
        Map<String, Integer> columns = new HashMap<>();
        for (int index = 0; index < header.size(); index++) {
            columns.put(header.get(index).trim().toLowerCase(Locale.ROOT), index);
        }
        return columns;
    }

    private int requiredColumn(Map<String, Integer> columns, String name) {
        Integer index = columns.get(name);
        if (index == null) {
            throw new IllegalArgumentException("Pending transactions CSV is missing column: " + name);
        }
        return index;
    }

    private String value(List<String> row, int column, int rowNumber) {
        if (column >= row.size()) {
            throw new IllegalArgumentException("Pending transactions CSV row " + (rowNumber + 1)
                    + " is missing a required value.");
        }
        return row.get(column);
    }

    private List<List<String>> parse(String csv) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < csv.length(); index++) {
            char character = csv.charAt(index);
            if (character == '"') {
                if (quoted && index + 1 < csv.length() && csv.charAt(index + 1) == '"') {
                    value.append(character);
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (character == ',' && !quoted) {
                row.add(value.toString());
                value.setLength(0);
            } else if ((character == '\n' || character == '\r') && !quoted) {
                if (character == '\r' && index + 1 < csv.length() && csv.charAt(index + 1) == '\n') {
                    index++;
                }
                row.add(value.toString());
                rows.add(row);
                row = new ArrayList<>();
                value.setLength(0);
            } else {
                value.append(character);
            }
        }
        if (quoted) {
            throw new IllegalArgumentException("Pending transactions CSV has an unterminated quoted value.");
        }
        if (!row.isEmpty() || !value.isEmpty()) {
            row.add(value.toString());
            rows.add(row);
        }
        return rows;
    }
}

package com.wharvex.budget;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Month;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class JdbcFinanceRepository {
    private static final String ACCOUNTS_SQL = """
            SELECT account_id, account_name, balance
            FROM account
            ORDER BY account_id
            """;

    private static final String SCHEDULES_SQL = """
            SELECT e.expense_id, et.expense_type_name, e.subtract_from_account_id,
                   e.add_to_account_id, e.amount, e.end_date, d.month, d.day,
                   ed.is_last_of_month
            FROM expense e
            JOIN expense_type et ON et.expense_type_id = e.expense_type_id
            JOIN expense_day ed ON ed.expense_id = e.expense_id
            JOIN day d ON d.day_id = ed.day_id
            ORDER BY e.expense_id, d.month, d.day
            """;
    private static final String INCOMES_SQL = """
            SELECT income_id, amount, start_date, end_date, interval::text AS recurrence_interval
            FROM income
            ORDER BY income_id
            """;
    private static final Pattern INTERVAL_PART = Pattern.compile(
            "([+-]?\\d+)\\s*(years?|yrs?|mons?|months?|days?)", Pattern.CASE_INSENSITIVE);

    FinanceData load(String jdbcUrl, String username, String password) throws SQLException {
        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
            return new FinanceData(loadAccounts(connection), loadSchedules(connection), loadIncomes(connection));
        }
    }

    private List<Account> loadAccounts(Connection connection) throws SQLException {
        List<Account> accounts = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(ACCOUNTS_SQL);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                BigDecimal balance = resultSet.getBigDecimal("balance");
                if (balance == null) {
                    throw new SQLException("Account " + resultSet.getLong("account_id") + " has no balance.");
                }
                accounts.add(new Account(
                        resultSet.getLong("account_id"),
                        resultSet.getString("account_name"),
                        balance));
            }
        }
        return accounts;
    }

    private List<ExpenseSchedule> loadSchedules(Connection connection) throws SQLException {
        List<ExpenseSchedule> schedules = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(SCHEDULES_SQL);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                Object o = resultSet.getObject("add_to_account_id");
                Long destinationAccountId = null;
                if (o != null) {
                    destinationAccountId = ((Number) o).longValue();
                }
                schedules.add(new ExpenseSchedule(
                        resultSet.getLong("expense_id"),
                        resultSet.getString("expense_type_name"),
                        resultSet.getLong("subtract_from_account_id"),
                        destinationAccountId,
                        resultSet.getBigDecimal("amount"),
                        resultSet.getObject("end_date", LocalDate.class),
                        Month.of(resultSet.getInt("month")),
                        resultSet.getInt("day"),
                        resultSet.getBoolean("is_last_of_month")));
            }
        }
        return schedules;
    }

    private List<IncomeSchedule> loadIncomes(Connection connection) throws SQLException {
        List<IncomeSchedule> incomes = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(INCOMES_SQL);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                long incomeId = resultSet.getLong("income_id");
                LocalDate startDate = resultSet.getObject("start_date", LocalDate.class);
                String interval = resultSet.getString("recurrence_interval");
                if (startDate == null || interval == null) {
                    throw new SQLException("Income " + incomeId + " must have a start date and interval.");
                }
                try {
                    incomes.add(new IncomeSchedule(
                            incomeId,
                            resultSet.getBigDecimal("amount"),
                            startDate,
                            resultSet.getObject("end_date", LocalDate.class),
                            parseInterval(interval)));
                } catch (IllegalArgumentException exception) {
                    throw new SQLException("Income " + incomeId + " has an invalid interval: " + interval, exception);
                }
            }
        }
        return incomes;
    }

    private Period parseInterval(String interval) {
        Matcher matcher = INTERVAL_PART.matcher(interval);
        int years = 0;
        int months = 0;
        int days = 0;
        boolean found = false;
        while (matcher.find()) {
            found = true;
            int amount = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2).toLowerCase();
            if (unit.startsWith("y")) {
                years = Math.addExact(years, amount);
            } else if (unit.startsWith("mon")) {
                months = Math.addExact(months, amount);
            } else {
                days = Math.addExact(days, amount);
            }
        }
        Period parsed = Period.of(years, months, days);
        if (!found || parsed.isZero() || parsed.isNegative() || interval.contains(":")) {
            throw new IllegalArgumentException("Interval must be a positive whole number of days, months, or years.");
        }
        return parsed;
    }
}

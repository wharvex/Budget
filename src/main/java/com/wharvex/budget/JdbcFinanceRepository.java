package com.wharvex.budget;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;

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

    FinanceData load(String jdbcUrl, String username, String password) throws SQLException {
        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
            return new FinanceData(loadAccounts(connection), loadSchedules(connection));
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
}


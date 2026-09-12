package com.wharvex.budget;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@Command(
        name = "budget-forecast",
        mixinStandardHelpOptions = true,
        description = "Projects scheduled expenses and account balances from a PostgreSQL budget database.")
public final class BudgetForecastCli implements Callable<Integer> {
    @Option(names = "--db-url", defaultValue = "${env:BUDGET_DB_URL}", required = true,
            description = "PostgreSQL JDBC URL (or BUDGET_DB_URL).")
    private String jdbcUrl;

    @Option(names = "--db-user", defaultValue = "${env:BUDGET_DB_USER}",
            description = "Database user (or BUDGET_DB_USER).")
    private String username;

    @Option(names = "--db-password", defaultValue = "${env:BUDGET_DB_PASSWORD}",
            description = "Database password (or BUDGET_DB_PASSWORD).")
    private String password;

    @Option(names = "--from",
            description = "First date to project, in ISO-8601 format. Defaults to today.")
    private LocalDate from = LocalDate.now();

    @Option(names = "--days", defaultValue = "90",
            description = "Number of calendar days to include, starting from --from. Default: ${DEFAULT-VALUE}.")
    private int days;

    @Option(names = "--account",
            description = "Only display changes for this account name. Repeat to select multiple accounts.")
    private List<String> accountNames;

    @Override
    public Integer call() {
        if (days < 1) {
            throw new IllegalArgumentException("--days must be at least 1.");
        }

        FinanceData data;
        try {
            data = new JdbcFinanceRepository().load(jdbcUrl, username, password);
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to load budget data: " + exception.getMessage(), exception);
        }

        LocalDate through = from.plusDays(days - 1L);
        Map<Long, Account> accountsById = data.accounts().stream()
                .collect(Collectors.toMap(Account::id, account -> account));
        Map<String, Account> accountsByName = data.accounts().stream()
                .collect(Collectors.toMap(Account::name, account -> account, (first, ignored) -> first));
        List<Account> selectedAccounts = selectedAccounts(data.accounts(), accountsByName);

        List<ProjectedEvent> events = new ProjectionService().project(data, from, through);
        printHeader();
        for (ProjectedEvent event : events) {
            printEvent(event, accountsById, selectedAccounts);
        }
        if (events.isEmpty()) {
            System.out.printf("No scheduled expenses from %s through %s.%n", from, through);
        }
        return 0;
    }

    private List<Account> selectedAccounts(List<Account> accounts, Map<String, Account> accountsByName) {
        if (accountNames == null || accountNames.isEmpty()) {
            return accounts;
        }
        return accountNames.stream().map(name -> {
            Account account = accountsByName.get(name);
            if (account == null) {
                throw new IllegalArgumentException("Unknown account: " + name);
            }
            return account;
        }).toList();
    }

    private void printHeader() {
        System.out.printf("%-10s  %-24s  %-20s  %14s  %14s%n",
                "DATE", "EXPENSE", "ACCOUNT", "CHANGE", "BALANCE");
    }

    private void printEvent(
            ProjectedEvent event,
            Map<Long, Account> accountsById,
            List<Account> selectedAccounts) {
        Map<Long, BigDecimal> changes = new HashMap<>();
        changes.put(event.subtractFromAccountId(), event.amount().negate());
        if (event.addToAccountId() != null) {
            changes.merge(event.addToAccountId(), event.amount(), BigDecimal::add);
        }
        for (Account account : selectedAccounts) {
            BigDecimal change = changes.get(account.id());
            if (change != null) {
                System.out.printf("%-10s  %-24s  %-20s  %14s  %14s%n",
                        event.date(),
                        event.expenseType(),
                        accountsById.get(account.id()).name(),
                        formatCurrency(change),
                        formatCurrency(event.balancesAfter().get(account.id())));
            }
        }
    }

    private String formatCurrency(BigDecimal amount) {
        return String.format("$%,.2f", amount);
    }

    public static void main(String[] args) {
        int exitCode = new picocli.CommandLine(new BudgetForecastCli()).execute(args);
        System.exit(exitCode);
    }
}

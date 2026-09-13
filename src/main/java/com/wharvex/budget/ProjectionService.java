package com.wharvex.budget;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class ProjectionService {
    private static final String CHECKING_ACCOUNT_NAME = "Checking";
    private static final String CREDIT_CARD_ACCOUNT_NAME = "Credit Card";

    List<ProjectedEvent> project(FinanceData data, LocalDate from, LocalDate through) {
        if (through.isBefore(from)) {
            throw new IllegalArgumentException("The end date must not be before the start date.");
        }

        Map<Long, BigDecimal> balances = new HashMap<>();
        for (Account account : data.accounts()) {
            balances.put(account.id(), account.balance());
        }
        long checkingAccountId = accountId(data.accounts(), CHECKING_ACCOUNT_NAME);
        long creditCardAccountId = accountId(data.accounts(), CREDIT_CARD_ACCOUNT_NAME);
        BigDecimal checkingBalance = balances.get(checkingAccountId);
        BigDecimal creditCardPendingBalance = balances.get(creditCardAccountId);
        BigDecimal creditCardPendingExcludingRecentChargesBalance = creditCardPendingBalance;
        List<PendingCharge> recentCreditCardCharges = new ArrayList<>();

        List<ExpenseSchedule> schedules = data.schedules().stream()
                .sorted(Comparator.comparingLong(ExpenseSchedule::expenseId))
                .toList();
        List<IncomeSchedule> incomes = data.incomes().stream()
                .sorted(Comparator.comparingLong(IncomeSchedule::incomeId))
                .toList();
        List<ProjectedEvent> events = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(through); date = date.plusDays(1)) {
            LocalDate recentChargeStart = date.minusDays(4);
            for (PendingCharge charge : recentCreditCardCharges) {
                if (charge.date().isBefore(recentChargeStart)) {
                    creditCardPendingExcludingRecentChargesBalance =
                            creditCardPendingExcludingRecentChargesBalance.add(charge.amount());
                }
            }
            recentCreditCardCharges.removeIf(charge -> charge.date().isBefore(recentChargeStart));
            for (IncomeSchedule income : incomes) {
                if (!income.occursOn(date)) {
                    continue;
                }
                changeBalance(balances, checkingAccountId, income.amount());
                checkingBalance = checkingBalance.add(income.amount());
                events.add(new ProjectedEvent(
                        date,
                        income.incomeId(),
                        "Income",
                        income.amount().negate(),
                        checkingAccountId,
                        null,
                        Map.copyOf(balances),
                        checkingBalance,
                        creditCardPendingBalance,
                        creditCardPendingExcludingRecentChargesBalance));
            }
            for (ExpenseSchedule schedule : schedules) {
                if (!schedule.occursOn(date)) {
                    continue;
                }

                changeBalance(balances, schedule.subtractFromAccountId(), schedule.amount().negate());
                if (schedule.addToAccountId() != null) {
                    changeBalance(balances, schedule.addToAccountId(), schedule.amount());
                }
                checkingBalance = changeIfAffected(
                        checkingBalance, checkingAccountId, schedule);
                creditCardPendingBalance = changeIfAffected(
                        creditCardPendingBalance, creditCardAccountId, schedule);
                if (schedule.subtractFromAccountId() == creditCardAccountId) {
                    recentCreditCardCharges.add(new PendingCharge(date, schedule.amount().negate()));
                } else {
                    creditCardPendingExcludingRecentChargesBalance = changeIfAffected(
                            creditCardPendingExcludingRecentChargesBalance, creditCardAccountId, schedule);
                }
                events.add(new ProjectedEvent(
                        date,
                        schedule.expenseId(),
                        schedule.expenseType(),
                        schedule.amount(),
                        schedule.subtractFromAccountId(),
                        schedule.addToAccountId(),
                        Map.copyOf(balances),
                        checkingBalance,
                        creditCardPendingBalance,
                        creditCardPendingExcludingRecentChargesBalance));
            }
        }
        return events;
    }

    private long accountId(List<Account> accounts, String accountName) {
        return accounts.stream()
                .filter(account -> account.name().equals(accountName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Account was not loaded: " + accountName))
                .id();
    }

    private BigDecimal changeIfAffected(
            BigDecimal balance, long accountId, ExpenseSchedule schedule) {
        if (schedule.subtractFromAccountId() == accountId) {
            balance = balance.subtract(schedule.amount());
        }
        if (schedule.addToAccountId() != null && schedule.addToAccountId() == accountId) {
            balance = balance.add(schedule.amount());
        }
        return balance;
    }

    private void changeBalance(Map<Long, BigDecimal> balances, long accountId, BigDecimal change) {
        BigDecimal currentBalance = balances.get(accountId);
        if (currentBalance == null) {
            throw new IllegalStateException("Expense refers to an account that was not loaded: " + accountId);
        }
        balances.put(accountId, currentBalance.add(change));
    }

    private record PendingCharge(LocalDate date, BigDecimal amount) {
    }
}

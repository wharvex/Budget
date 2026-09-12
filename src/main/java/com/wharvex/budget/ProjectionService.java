package com.wharvex.budget;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class ProjectionService {
    List<ProjectedEvent> project(FinanceData data, LocalDate from, LocalDate through) {
        if (through.isBefore(from)) {
            throw new IllegalArgumentException("The end date must not be before the start date.");
        }

        Map<Long, BigDecimal> balances = new HashMap<>();
        for (Account account : data.accounts()) {
            balances.put(account.id(), account.balance());
        }

        List<ExpenseSchedule> schedules = data.schedules().stream()
                .sorted(Comparator.comparingLong(ExpenseSchedule::expenseId))
                .toList();
        List<ProjectedEvent> events = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(through); date = date.plusDays(1)) {
            for (ExpenseSchedule schedule : schedules) {
                if (!schedule.occursOn(date)) {
                    continue;
                }

                changeBalance(balances, schedule.subtractFromAccountId(), schedule.amount().negate());
                if (schedule.addToAccountId() != null) {
                    changeBalance(balances, schedule.addToAccountId(), schedule.amount());
                }
                events.add(new ProjectedEvent(
                        date,
                        schedule.expenseId(),
                        schedule.expenseType(),
                        schedule.amount(),
                        schedule.subtractFromAccountId(),
                        schedule.addToAccountId(),
                        Map.copyOf(balances)));
            }
        }
        return events;
    }

    private void changeBalance(Map<Long, BigDecimal> balances, long accountId, BigDecimal change) {
        BigDecimal currentBalance = balances.get(accountId);
        if (currentBalance == null) {
            throw new IllegalStateException("Expense refers to an account that was not loaded: " + accountId);
        }
        balances.put(accountId, currentBalance.add(change));
    }
}


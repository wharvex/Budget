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
    private static final long CREDIT_CARD_PAYMENT_EXPENSE_ID = 0;
    private static final String CREDIT_CARD_PAYMENT_EXPENSE_TYPE = "Credit Card Payment";

    List<ProjectedEvent> project(FinanceData data, LocalDate from, LocalDate through) {
        if (through.isBefore(from)) {
            throw new IllegalArgumentException("The end date must not be before the start date.");
        }

        // Make balances map.
        Map<Long, BigDecimal> balances = new HashMap<>();
        for (Account account : data.accounts()) {
            balances.put(account.id(), account.balance());
        }

        // Get account IDs and initial balances.
        long checkingAccountId = getAccountId(data.accounts(), CHECKING_ACCOUNT_NAME);
        long creditCardAccountId = getAccountId(data.accounts(), CREDIT_CARD_ACCOUNT_NAME);
        BigDecimal checkingBalance = balances.get(checkingAccountId);
        BigDecimal creditCardBalance = balances.get(creditCardAccountId);
        BigDecimal creditCardBalancePlusPending = creditCardBalance;

        // Add CSV pending charges to the projection pending charges list and the plus-pending balance.
        List<PendingCharge> pendingCreditCardCharges = new ArrayList<>();
        for (PendingTransaction pendingTransaction : data.pendingTransactions()) {
            pendingCreditCardCharges.add(new PendingCharge(pendingTransaction.date(), pendingTransaction.amount()));
            creditCardBalancePlusPending = creditCardBalancePlusPending.add(pendingTransaction.amount());
        }

        // Get schedules and create the events list.
        List<ExpenseSchedule> schedules = getSortedSchedules(data);
        List<IncomeSchedule> incomes = getSortedIncomes(data);
        List<ProjectedEvent> events = new ArrayList<>();

        // Iterate through each date in the projection range.
        for (LocalDate date = from; !date.isAfter(through); date = date.plusDays(1)) {

            // Update the pending charge cutoff and move charges older than it to the main credit card balance.
            LocalDate pendingChargeCutoff = date.minusDays(4);
            creditCardBalance = addPendingChargesToBalance(creditCardBalance, pendingCreditCardCharges,
                    pendingChargeCutoff);
            pendingCreditCardCharges.removeIf(charge -> charge.date().isBefore(pendingChargeCutoff));

            // Process incomes for the current date.
            for (IncomeSchedule income : incomes) {
                if (!income.occursOn(date)) {
                    continue;
                }
                addAmountToStoredBalance(balances, checkingAccountId, income.amount());
                checkingBalance = checkingBalance.add(income.amount());
                events.add(new ProjectedEvent(date, income.incomeId(), "Income", income.amount().negate(),
                        checkingAccountId, null, Map.copyOf(balances), checkingBalance, creditCardBalancePlusPending,
                        creditCardBalance));
            }

            // Process expenses for the current date.
            for (ExpenseSchedule schedule : schedules) {
                if (!schedule.occursOn(date)) {
                    continue;
                }

                // Subtract the expense from checking or add it as a pending charge & update CC Pending if it's a CC
                // expense.
                if (schedule.subtractFromAccountId() == checkingAccountId) {
                    checkingBalance = addAmountToStoredBalance(balances, schedule.subtractFromAccountId(),
                            schedule.amount().negate());
                } else {
                    pendingCreditCardCharges.add(new PendingCharge(date, schedule.amount().negate()));
                    creditCardBalancePlusPending = creditCardBalancePlusPending.add(schedule.amount().negate());
                }

                // Add to an account if this is a transfer. This is where the savings account gets incremented.
                if (schedule.addToAccountId() != null) {
                    addAmountToStoredBalance(balances, schedule.addToAccountId(), schedule.amount());
                }

                // Record as an event for later printout.
                events.add(new ProjectedEvent(date, schedule.expenseId(), schedule.expenseType(), schedule.amount(),
                        schedule.subtractFromAccountId(), schedule.addToAccountId(), Map.copyOf(balances),
                        checkingBalance, creditCardBalancePlusPending, creditCardBalance));
            }

            // Pay off CC if it's the 23rd of the month and the balance is negative, and record the payment as an event.
            if (date.getDayOfMonth() == 23 && creditCardBalance.signum() < 0) {
                BigDecimal paymentAmount = creditCardBalance.negate();
                checkingBalance = addAmountToStoredBalance(balances, checkingAccountId, paymentAmount.negate());
                creditCardBalance = addAmountToStoredBalance(balances, creditCardAccountId, paymentAmount);
                creditCardBalancePlusPending = creditCardBalancePlusPending.add(paymentAmount);
                events.add(new ProjectedEvent(date, CREDIT_CARD_PAYMENT_EXPENSE_ID, CREDIT_CARD_PAYMENT_EXPENSE_TYPE,
                        paymentAmount, checkingAccountId, creditCardAccountId, Map.copyOf(balances), checkingBalance,
                        creditCardBalancePlusPending, creditCardBalance));
            }
        }
        return events;
    }

    private BigDecimal addPendingChargesToBalance(BigDecimal creditCardBalance,
            List<PendingCharge> pendingCreditCardCharges, LocalDate pendingChargeCutoff) {
        for (PendingCharge charge : pendingCreditCardCharges) {
            if (charge.date().isBefore(pendingChargeCutoff)) {
                creditCardBalance = creditCardBalance.add(charge.amount());
            }
        }
        return creditCardBalance;
    }

    private List<ExpenseSchedule> getSortedSchedules(FinanceData data) {
        return data.schedules().stream().sorted(Comparator.comparingLong(ExpenseSchedule::expenseId)).toList();
    }

    private List<IncomeSchedule> getSortedIncomes(FinanceData data) {
        return data.incomes().stream().sorted(Comparator.comparingLong(IncomeSchedule::incomeId)).toList();
    }

    private long getAccountId(List<Account> accounts, String accountName) {
        return accounts.stream().filter(account -> account.name().equals(accountName)).findFirst()
                .orElseThrow(() -> new IllegalStateException("Account was not loaded: " + accountName)).id();
    }

    private BigDecimal addAmountToStoredBalance(Map<Long, BigDecimal> balances, long accountId, BigDecimal change) {
        BigDecimal currentBalance = balances.get(accountId);
        if (currentBalance == null) {
            throw new IllegalStateException("Expense refers to an account that was not loaded: " + accountId);
        }
        BigDecimal newBalance = currentBalance.add(change);
        balances.put(accountId, newBalance);
        return newBalance;
    }

    private record PendingCharge(LocalDate date, BigDecimal amount) {
    }
}

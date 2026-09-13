package com.wharvex.budget;

import java.util.List;

record FinanceData(
        List<Account> accounts,
        List<ExpenseSchedule> schedules,
        List<IncomeSchedule> incomes,
        List<PendingTransaction> pendingTransactions) {
    FinanceData(List<Account> accounts, List<ExpenseSchedule> schedules, List<IncomeSchedule> incomes) {
        this(accounts, schedules, incomes, List.of());
    }

    FinanceData(List<Account> accounts, List<ExpenseSchedule> schedules) {
        this(accounts, schedules, List.of());
    }
}

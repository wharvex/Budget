package com.wharvex.budget;

import java.util.List;

record FinanceData(List<Account> accounts, List<ExpenseSchedule> schedules, List<IncomeSchedule> incomes) {
    FinanceData(List<Account> accounts, List<ExpenseSchedule> schedules) {
        this(accounts, schedules, List.of());
    }
}

package com.wharvex.budget;

import java.util.List;

record FinanceData(List<Account> accounts, List<ExpenseSchedule> schedules) {
}


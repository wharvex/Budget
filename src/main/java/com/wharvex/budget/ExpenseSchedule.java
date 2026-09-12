package com.wharvex.budget;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

record ExpenseSchedule(
        long expenseId,
        String expenseType,
        long subtractFromAccountId,
        Long addToAccountId,
        BigDecimal amount,
        LocalDate endDate,
        Month month,
        int day,
        boolean lastOfMonth) {

    boolean occursOn(LocalDate date) {
        if (endDate != null && date.isAfter(endDate)) {
            return false;
        }
        if (date.getMonth() != month) {
            return false;
        }
        return lastOfMonth ? date.getDayOfMonth() == date.lengthOfMonth() : date.getDayOfMonth() == day;
    }
}


package com.wharvex.budget;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;

record IncomeSchedule(
        long incomeId,
        BigDecimal amount,
        LocalDate startDate,
        LocalDate endDate,
        Period interval) {

    IncomeSchedule {
        Objects.requireNonNull(startDate, "startDate");
        Objects.requireNonNull(interval, "interval");
        if (interval.isZero() || interval.isNegative()) {
            throw new IllegalArgumentException("Income interval must be positive.");
        }
    }

    boolean occursOn(LocalDate date) {
        if (date.isBefore(startDate) || (endDate != null && date.isAfter(endDate))) {
            return false;
        }
        for (LocalDate occurrence = startDate; !occurrence.isAfter(date); occurrence = occurrence.plus(interval)) {
            if (occurrence.equals(date)) {
                return true;
            }
        }
        return false;
    }
}

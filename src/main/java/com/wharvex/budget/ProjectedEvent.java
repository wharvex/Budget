package com.wharvex.budget;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

record ProjectedEvent(
        LocalDate date,
        long expenseId,
        String expenseType,
        BigDecimal amount,
        long subtractFromAccountId,
        Long addToAccountId,
        Map<Long, BigDecimal> balancesAfter) {
}


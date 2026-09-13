package com.wharvex.budget;

import java.math.BigDecimal;
import java.time.LocalDate;

record PendingTransaction(LocalDate date, String description, BigDecimal amount) {
}

package com.wharvex.budget;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProjectionServiceTest {
    @Test
    void projectsExpensesAndTransfersInScheduleOrder() {
        FinanceData data = new FinanceData(
                List.of(
                        new Account(1, "Checking", new BigDecimal("100.00")),
                        new Account(2, "Savings", new BigDecimal("500.00")),
                        new Account(3, "Credit Card", new BigDecimal("-20.00"))),
                List.of(
                        schedule(1, "Utilities", 1, null, "25.00", Month.JANUARY, 2, false, null),
                        schedule(2, "Savings transfer", 1, 2L, "50.00", Month.JANUARY, 2, false, null)));

        List<ProjectedEvent> events = new ProjectionService().project(
                data, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 3));

        assertEquals(2, events.size());
        assertEquals(new BigDecimal("75.00"), events.get(0).balancesAfter().get(1L));
        assertEquals(new BigDecimal("25.00"), events.get(1).balancesAfter().get(1L));
        assertEquals(new BigDecimal("550.00"), events.get(1).balancesAfter().get(2L));
    }

    @Test
    void expandsLastDaySchedulesAndHonorsEndDate() {
        FinanceData data = new FinanceData(
                List.of(
                        new Account(1, "Checking", new BigDecimal("100.00")),
                        new Account(2, "Credit Card", new BigDecimal("-20.00"))),
                List.of(schedule(1, "Rent", 1, null, "10.00", Month.FEBRUARY, 1, true,
                        LocalDate.of(2028, 2, 29))));

        List<ProjectedEvent> events = new ProjectionService().project(
                data, LocalDate.of(2027, 1, 1), LocalDate.of(2029, 3, 1));

        assertEquals(
                List.of(LocalDate.of(2027, 2, 28), LocalDate.of(2028, 2, 29)),
                events.stream().map(ProjectedEvent::date).toList());
    }

    @Test
    void tracksCreditCardPendingBalancesSeparatelyFromRecentCharges() {
        FinanceData data = new FinanceData(
                List.of(
                        new Account(1, "Checking", new BigDecimal("100.00")),
                        new Account(2, "Credit Card", new BigDecimal("-20.00"))),
                List.of(
                        schedule(1, "Recent charge", 2, null, "10.00", Month.JANUARY, 1, false, null),
                        schedule(2, "Credit card payment", 1, 2L, "5.00", Month.JANUARY, 3, false, null),
                        schedule(3, "Later expense", 1, null, "1.00", Month.JANUARY, 6, false, null)));

        List<ProjectedEvent> events = new ProjectionService().project(
                data, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 6));

        assertEquals(new BigDecimal("95.00"), events.get(1).checkingBalanceAfter());
        assertEquals(new BigDecimal("-25.00"), events.get(1).creditCardPendingBalanceAfter());
        assertEquals(new BigDecimal("-15.00"),
                events.get(1).creditCardPendingExcludingRecentChargesAfter());
        assertEquals(new BigDecimal("-25.00"),
                events.get(2).creditCardPendingExcludingRecentChargesAfter());
    }

    private ExpenseSchedule schedule(
            long id,
            String type,
            long fromAccount,
            Long toAccount,
            String amount,
            Month month,
            int day,
            boolean lastOfMonth,
            LocalDate endDate) {
        return new ExpenseSchedule(
                id, type, fromAccount, toAccount, new BigDecimal(amount), endDate, month, day, lastOfMonth);
    }
}

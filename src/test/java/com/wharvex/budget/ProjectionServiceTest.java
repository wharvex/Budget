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
                        new Account(2, "Savings", new BigDecimal("500.00"))),
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
                List.of(new Account(1, "Checking", new BigDecimal("100.00"))),
                List.of(schedule(1, "Rent", 1, null, "10.00", Month.FEBRUARY, 1, true,
                        LocalDate.of(2028, 2, 29))));

        List<ProjectedEvent> events = new ProjectionService().project(
                data, LocalDate.of(2027, 1, 1), LocalDate.of(2029, 3, 1));

        assertEquals(
                List.of(LocalDate.of(2027, 2, 28), LocalDate.of(2028, 2, 29)),
                events.stream().map(ProjectedEvent::date).toList());
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


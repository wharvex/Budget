package com.wharvex.budget;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BudgetForecastCliTest {
    @Test
    void printsSavingsBalanceColumn() {
        BudgetForecastCli cli = new BudgetForecastCli();
        ProjectedEvent event = new ProjectedEvent(
                LocalDate.of(2026, 1, 2),
                1,
                "Savings transfer",
                new BigDecimal("50.00"),
                1,
                2L,
                Map.of(1L, new BigDecimal("50.00"), 2L, new BigDecimal("550.00")),
                new BigDecimal("50.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO);
        Map<Long, Account> accounts = Map.of(
                1L, new Account(1, "Checking", new BigDecimal("100.00")),
                2L, new Account(2, "Savings", new BigDecimal("500.00")));
        PrintStream originalOut = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(output));
            cli.printHeader();
            cli.printEvent(event, accounts, List.of(accounts.get(1L)), 2L);
        } finally {
            System.setOut(originalOut);
        }

        String[] lines = output.toString().split(System.lineSeparator());
        assertTrue(lines[0].contains("SAVINGS"));
        assertTrue(lines[1].indexOf("$50.00") < lines[1].indexOf("$550.00"));
    }
}

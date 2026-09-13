package com.wharvex.budget;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static java.nio.file.Files.writeString;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PendingTransactionsCsvTest {
    @Test
    void loadsPendingRowsUsingTheExpectedColumns(@TempDir Path directory) throws Exception {
        Path csv = directory.resolve("pending.csv");
        writeString(csv, """
                status,date,original_description,split_type,category,currency,amount,user_description,memo,classification,account_name,simple_description
                pending,08/29/2026,"RETURN, DETAIL NOT YET AVAILABLE",,Travel,$,0.00,,,Personal,M&T Bank - Credit Card - M&T Visa Credit Card,Return - Detail Not Yet Available
                cleared,08/30/2026,Ignored,,Travel,$,-12.34,,,Personal,Credit Card,Ignored
                pending,08/31/2026,Charge,,Travel,$,"-1,234.56",,,Personal,Credit Card,Charge
                """);

        assertEquals(List.of(
                new PendingTransaction(
                        LocalDate.of(2026, 8, 29),
                        "Return - Detail Not Yet Available",
                        new BigDecimal("0.00")),
                new PendingTransaction(
                        LocalDate.of(2026, 8, 31),
                        "Charge",
                        new BigDecimal("-1234.56"))),
                new PendingTransactionsCsv().load(csv));
    }
}

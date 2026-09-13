-- budget-15-dml-insert-quarterly-expense.sql
WITH inserted_expense AS (
    INSERT INTO expense(subtract_from_account_id, add_to_account_id, expense_type_id, amount)
    SELECT
        (
            SELECT
                account_id
            FROM
                account
            WHERE
                account_name = 'Credit Card'),
        NULL,
        (
            SELECT
                expense_type_id
            FROM
                expense_type
            WHERE
                expense_type_name = 'Miscellaneous'),
        300.00
    RETURNING
        expense_id)
INSERT INTO expense_day(expense_id, day_id, is_last_of_month)
SELECT
    inserted_expense.expense_id,
    day.day_id,
    FALSE
FROM
    inserted_expense
CROSS JOIN day
WHERE
    day.month IN (4, 7, 10, 1)
    AND day.day = 6;

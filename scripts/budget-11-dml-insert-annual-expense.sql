-- budget-11-dml-insert-annual-expense.sql
WITH inserted_expense AS (
    INSERT INTO expense (
        subtract_from_account_id,
        add_to_account_id,
        expense_type_id,
        amount
    )
    SELECT
        (SELECT account_id FROM account WHERE account_name = 'Checking'),
        (SELECT account_id FROM account WHERE account_name = 'Credit Card'),
        (SELECT expense_type_id FROM expense_type WHERE expense_type_name = 'Insurance'),
        1200.00
    RETURNING expense_id
)
INSERT INTO expense_day (expense_id, day_id)
SELECT inserted_expense.expense_id, day.day_id
FROM inserted_expense
CROSS JOIN day
WHERE day.month = 2
  AND day.day = 12;

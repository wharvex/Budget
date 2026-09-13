-- Create a gas charge every two weeks, varying deterministically from $30 to $40.
WITH gas_occurrences AS (
    SELECT
        day_id,
        30.00 + ((day_id * 7) % 11) AS amount
    FROM
        day
    WHERE
        (day_id - 10) % 14 = 0),
inserted_expenses AS (
    INSERT INTO expense(
        subtract_from_account_id,
        add_to_account_id,
        expense_type_id,
        amount)
    SELECT
        (
            SELECT account_id
            FROM account
            WHERE account_name = 'Checking'),
        (
            SELECT account_id
            FROM account
            WHERE account_name = 'Credit Card'),
        (
            SELECT expense_type_id
            FROM expense_type
            WHERE expense_type_name = 'Gas'),
        amount
    FROM
        (SELECT DISTINCT amount FROM gas_occurrences) AS gas_amounts
    RETURNING
        expense_id,
        amount)
INSERT INTO expense_day(expense_id, day_id, is_last_of_month)
SELECT
    inserted_expenses.expense_id,
    gas_occurrences.day_id,
    FALSE
FROM
    inserted_expenses
JOIN gas_occurrences
    ON gas_occurrences.amount = inserted_expenses.amount;

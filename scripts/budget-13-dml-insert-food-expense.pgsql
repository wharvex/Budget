-- Create a food charge every three days, varying deterministically from $20 to $80.
WITH food_occurrences AS (
    SELECT
        day_id,
        20.00 + ((day_id * 17) % 61) AS amount
    FROM
        day
    WHERE
        (day_id - 1) % 3 = 0),
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
            WHERE expense_type_name = 'Food'),
        amount
    FROM
        (SELECT DISTINCT amount FROM food_occurrences) AS food_amounts
    RETURNING
        expense_id,
        amount)
INSERT INTO expense_day(expense_id, day_id, is_last_of_month)
SELECT
    inserted_expenses.expense_id,
    food_occurrences.day_id,
    FALSE
FROM
    inserted_expenses
JOIN food_occurrences
    ON food_occurrences.amount = inserted_expenses.amount;

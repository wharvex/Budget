-- p-budget-16-dml-insert-transfer-expense.pgsql
WITH inserted_expense AS (
INSERT INTO expense(subtract_from_account_id, add_to_account_id, expense_type_id, amount)
   SELECT
      (
         SELECT
            account_id
         FROM
            account
         WHERE
            account_name = 'Checking'),
(
            SELECT
               account_id
            FROM
               account
            WHERE
               account_name = 'Savings'),
(
               SELECT
                  expense_type_id
               FROM
                  expense_type
               WHERE
                  expense_type_name = 'Savings Transfer'),
               300.00
            RETURNING
               expense_id)
      INSERT INTO expense_day(expense_id, day_id, is_last_of_month)
   SELECT
      inserted_expense.expense_id,
      day.day_id,
      TRUE
   FROM
      inserted_expense
   CROSS JOIN day
WHERE
   day.day = 28;


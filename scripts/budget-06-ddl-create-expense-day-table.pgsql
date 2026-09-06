-- budget-06-ddl-create-expense-day-table.sql
CREATE TABLE expense_day(
    expense_id integer NOT NULL REFERENCES expense(expense_id),
    day_id integer NOT NULL REFERENCES day(day_id),
    PRIMARY KEY (expense_id, day_id),
    is_last_of_month boolean NOT NULL DEFAULT FALSE
);


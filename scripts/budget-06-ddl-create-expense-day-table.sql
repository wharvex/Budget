CREATE TABLE expense_day (
    expense_id INTEGER NOT NULL REFERENCES expense(expense_id),
    day_id INTEGER NOT NULL REFERENCES day(day_id),
    PRIMARY KEY (expense_id, day_id)
);

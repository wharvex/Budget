INSERT INTO day (day_id, month, day, is_last_of_month)
SELECT
    EXTRACT(DOY FROM calendar_date)::INTEGER,
    EXTRACT(MONTH FROM calendar_date)::INTEGER,
    EXTRACT(DAY FROM calendar_date)::INTEGER,
    calendar_date = (DATE_TRUNC('month', calendar_date) + INTERVAL '1 month - 1 day')::DATE
FROM GENERATE_SERIES(
    DATE '2001-01-01',
    DATE '2001-12-31',
    INTERVAL '1 day'
) AS calendar(calendar_date);

SELECT SETVAL(
    PG_GET_SERIAL_SEQUENCE('day', 'day_id'),
    (SELECT MAX(day_id) FROM day)
);

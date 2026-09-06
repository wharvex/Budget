-- budget-02-dml-insert-days.sql
INSERT INTO day(day_id, month, day)
SELECT
    EXTRACT(DOY FROM calendar_date)::integer,
    EXTRACT(MONTH FROM calendar_date)::integer,
    EXTRACT(DAY FROM calendar_date)::integer
FROM
    GENERATE_SERIES(DATE '2001-01-01', DATE '2001-12-31', INTERVAL '1 day') AS calendar(calendar_date);

SELECT
    SETVAL(PG_GET_SERIAL_SEQUENCE('day', 'day_id'),(
            SELECT
                MAX(day_id)
            FROM day));


# Budget

The SQL scripts define a PostgreSQL-backed personal-finance budget. Load the
schema and your private data:

```sh
cat reset_scripts/*.pgsql | psql -U budget -d budget -1
cat scripts/*ddl*.pgsql | psql -U budget -d budget -1
psql -U budget -d budget -f scripts/budget-02-dml-insert-days.pgsql
cat personal_scripts/*.pgsql | psql -U budget -d budget -1
```

## Forecast CLI

`budget-forecast` is a Java 21/Gradle application that projects recurring
expenses and the balances of the affected accounts. An expense that has an
`add_to_account_id` is treated as a transfer: the source account is debited
and the destination account is credited on the scheduled date.

Supply PostgreSQL credentials as command-line options or environment variables:

```sh
./gradlew run --args="--db-url jdbc:postgresql://localhost:5432/budget --db-user budget --days 120"
```

```sh
export BUDGET_DB_URL=jdbc:postgresql://localhost:5432/budget
export BUDGET_DB_USER=budget
export BUDGET_DB_PASSWORD=...
./gradlew run --args="--from 2026-10-01 --days 90 --account Checking"
```

`--from` defaults to today. `--days` defaults to 90 and includes the start
date. Repeat `--account` to limit displayed rows while still applying every
scheduled expense to the projected balances.

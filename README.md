# Budget

## Database

### Run Instructions

Download and install PostgreSQL.

Add the "C:\Program Files\PostgreSQL\18\bin" folder (or the equivalent for your installation) to your PATH.

Run the following commands in powershell (using the password provided or your own):

```
psql -U postgres -c "CREATE USER budget WITH PASSWORD 'budget';"
psql -U postgres -c "CREATE DATABASE budget OWNER budget;"
```

Clone this Git repo to your local machine.

Make copies of any DML scripts you want to use to enter your own personal finance information and put them in the `personal_scripts` folder.

Run the following in Powershell or bash to reset or initially populate the database (omit the `reset_scripts` line on initial setup).

```sh
cat reset_scripts/*.pgsql | psql -U budget -d budget -1
cat scripts/*ddl*.pgsql | psql -U budget -d budget -1
psql -U budget -d budget -f scripts/budget-02-dml-insert-days.pgsql
cat personal_scripts/*.pgsql | psql -U budget -d budget -1
```

## CLI

### Background

`budget-forecast` is a Java 21/Gradle application that projects recurring income, expenses, and the balances of the affected accounts.

Income is applied to Checking according to its start date, end date, and interval.

An expense that has an `add_to_account_id` is treated as a transfer: the source account is debited and the destination account is credited on the scheduled date.

On the 23rd of each month, the forecast also includes a non-persisted Credit Card Payment from Checking for the pending credit-card balance excluding charges from the previous five days.

### Run Instructions

First, set the following environment variables:

```
BUDGET_DB_URL=jdbc:postgresql://localhost:5432/budget
BUDGET_DB_USER=budget
BUDGET_DB_PASSWORD=...
```

Then run:

```sh
./gradlew run
```

Add arguments as needed.

```sh
./gradlew run --args="--days 90 --pending-transactions-csv path/to/transactions.csv"
```

Use `--pending-transactions-csv path/to/transactions.csv` to include pending credit-card transactions exported with the `status`, `date`, `simple description`, and `amount` columns.

Only rows whose status is `pending` are loaded.

Example CSV data:

```csv
Status,Date,Original Description,Split Type,Category,Currency,Amount,User Description,Memo,Classification,Account Name,Simple Description
pending,09/12/2026, Exxon WESTERN AVE. BBE,,Gasoline/Fuel,$,40.15, , ,Personal,M&T Bank - Credit Card - M&T Visa Credit Card, ExxonMobil
```

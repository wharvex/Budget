# Budget

```
cat reset_scripts/*.sql | psql -U budget -d budget -1
cat scripts/*ddl*.sql | psql -U budget -d budget -1
psql -U budget -d budget -f scripts/budget-02-dml-insert-days.sql
```

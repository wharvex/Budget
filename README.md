# Budget

```
cat reset_scripts/*.pgsql | psql -U budget -d budget -1
cat scripts/*ddl*.pgsql | psql -U budget -d budget -1
psql -U budget -d budget -f scripts/budget-02-dml-insert-days.pgsql
```

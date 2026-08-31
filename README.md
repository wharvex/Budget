# Budget

```
cat reset_scripts/*.sql | psql -U budget -d budget -1
cat scripts/*ddl*.sql | psql -U budget -d budget -1
cat scripts/*dml*.sql | psql -U budget -d budget -1
```

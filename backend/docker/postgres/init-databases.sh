#!/usr/bin/env bash
# Runs once, on the first start of the postgres container (empty data volume).
# For each service it creates a role and a database that only that role can use.
set -euo pipefail

for service in parking psp card paypal crypto bank; do
    password_var="${service^^}_DB_PASSWORD"
    password="${!password_var}"

    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres \
        -v role="${service}_user" -v db="${service}_db" -v pw="$password" <<'SQL'
CREATE ROLE :"role" LOGIN PASSWORD :'pw';
CREATE DATABASE :"db" OWNER :"role";
REVOKE ALL ON DATABASE :"db" FROM PUBLIC;
SQL
done

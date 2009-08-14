#!/bin/sh

## may not work for you
##  depends on whether root is the DB owner

echo "clear-db.sh..."
echo "CLEARING DB TABLES IN atomserver_dev (PROMPTS FOR PASSWORD)"
psql -U atomserver atomserver_dev < src/main/resources/org/atomserver/sql/delete_all.sql


#!/usr/bin/env bash
# rebuilds legacy-final.catalog: the original app's migrations V1..V14 replayed with psql in ONE
# transaction on the postgis test database, catalog.sql run on its schema, then ROLLBACK -- the
# database is left as it was. takes the test-suite lock first, so it never runs under a suite.
# needs the five CHAWPI_TEST_DB_* vars and CHAWPI_TEST_GIS_DB_PORT (the original needs postgis).
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
migrations="${SAPGIS_MIGRATIONS:-/Users/jorge/IdeaProjects/sapgis/backend/src/main/resources/db/migration}"
: "${CHAWPI_TEST_DB_HOST:?}" "${CHAWPI_TEST_DB_NAME:?}" "${CHAWPI_TEST_DB_USERNAME:?}" "${CHAWPI_TEST_DB_PASSWORD:?}" "${CHAWPI_TEST_GIS_DB_PORT:?}"
case "$CHAWPI_TEST_DB_NAME" in
  *_test) ;;
  *) echo "refusing: $CHAWPI_TEST_DB_NAME does not end in _test" >&2; exit 1 ;;
esac
out="$here/legacy-final.catalog"
{
  echo '\o /dev/null'
  echo "SELECT pg_advisory_lock(hashtext('chawpi-test-suite'));"
  echo "BEGIN;"
  # flyway ran them with the original's schema first on the search path
  echo "SET search_path TO sapgis, public;"
  for v in $(seq 1 14); do
    cat "$migrations"/V${v}__*.sql
    echo
  done
  echo "SET search_path TO public;"
  echo "\\o $out.tmp"
  sed 's/__SCHEMA__/sapgis/g' "$here/catalog.sql"
  echo '\o'
  echo "ROLLBACK;"
} | PGPASSWORD="$CHAWPI_TEST_DB_PASSWORD" psql -X -q -At -v ON_ERROR_STOP=1 \
  -h "$CHAWPI_TEST_DB_HOST" -p "$CHAWPI_TEST_GIS_DB_PORT" -U "$CHAWPI_TEST_DB_USERNAME" -d "$CHAWPI_TEST_DB_NAME"
mv "$out.tmp" "$out"
wc -l "$out"

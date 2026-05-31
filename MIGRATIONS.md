# Migration Bootstrap (No Existing Scripts)

This repository currently has no SQL migration history.

## Current Safe Mode
- `JPA_DDL_AUTO=update` keeps services running as before.
- `FLYWAY_ENABLED=false` avoids forcing migration before scripts exist.

## Goal State
- Move schema evolution to versioned migrations (Flyway).
- Set `JPA_DDL_AUTO=validate` in production after baseline is in place.

## Step-by-Step
1. Generate baseline SQL per service database from current schema.
2. Save as `V1__baseline.sql` under each service: `src/main/resources/db/migration`.
3. Set `FLYWAY_ENABLED=true` and keep `FLYWAY_BASELINE_ON_MIGRATE=true` for first rollout.
4. Deploy once, then set `FLYWAY_BASELINE_ON_MIGRATE=false`.
5. Every schema change must be a new versioned migration (`V2__...`, `V3__...`).

## Suggested Baseline Extraction
- From MySQL: `mysqldump --no-data --routines --events --triggers <db_name>`
- Clean output to deterministic DDL only.
- Do not include data inserts in baseline except required static seed rows.

## Service Paths
- `diwan-users/src/main/resources/db/migration`
- `diwan-transactions/src/main/resources/db/migration`
- `diwan-medical/src/main/resources/db/migration`
- `diwan-smarthome/src/main/resources/db/migration`
- `diwan-logging/src/main/resources/db/migration`
- `diwan-gateway/src/main/resources/db/migration`

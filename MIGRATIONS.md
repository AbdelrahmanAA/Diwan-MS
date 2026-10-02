# Database migrations (Flyway)

Every service with a database (`diwan-users`, `-transactions`, `-medical`, `-smarthome`, `-logging`, `-gateway`)
owns its schema through Flyway scripts in `src/main/resources/db/migration`. Hibernate only **validates** that
the entities match the schema (`spring.jpa.hibernate.ddl-auto=validate`); it no longer creates or alters tables.

## How it works
- `V1__baseline.sql` is the schema as Hibernate had created it (extracted with `mysqldump --no-data`).
  - **New/empty database:** Flyway runs V1, then every later script.
  - **Existing database** (no `flyway_schema_history` yet): `baseline-on-migrate` marks it as version 1,
    skips V1 and runs only newer scripts. No manual step is needed on the first deploy.
- Startup fails if an entity does not match the schema. That is intended: it means a migration is missing.

## Changing the schema
1. Add `V<next>__<description>.sql` next to the existing scripts (never edit a script that has been applied).
2. Change the entity in the same commit. Start the service against a scratch database to check that
   Flyway applies it and Hibernate validation passes.
3. `V2__outbox_event.sql` in `diwan-users` is an example.

## Settings (all optional, environment variables)
| Variable | Default | Meaning |
|---|---|---|
| `FLYWAY_ENABLED` | `true` | Turn migrations off (not recommended) |
| `JPA_DDL_AUTO` | `validate` | `update` only as a temporary local escape hatch |
| `FLYWAY_BASELINE_ON_MIGRATE` | `true` | Baseline a non-empty schema without history. Set `false` once every environment is baselined |
| `FLYWAY_BASELINE_VERSION` | `1` | Version assigned to the baseline |

## First rollout to an environment that was created by `ddl-auto=update`
Hibernate `update` never drops or retypes columns, so an old database can have drifted from the entities.
If the first start fails with a schema-validation error, add a `V2__...sql` that fixes the difference
(or compare with `V1__baseline.sql`), then redeploy.

## Notes
- The bundled Flyway is 10.20.x. It logs a warning that MySQL 8.4 is newer than the versions it has been tested
  with; migrations work.
- Do not put seed/reference data that the application manages itself (e.g. the gateway's routes and features,
  which `RouteDataInitializer` upserts on startup) into migrations.

## Known pitfalls

### Room schema bump fails the first build (2026-11-23, task 37e800)
When: you change a Room entity and raise `@Database(version)`, so `app/schemas/.../<n>.json` does not exist yet.
Symptom: `kspDebugKotlin` fails with `JsonDecodingException: Expected start of the object '{', but had 'EOF'`.
Cause: the debug and release KSP tasks race to write the new schema file.
Do: run the build again (the file now exists) and commit the generated `<n>.json` together with a migration.

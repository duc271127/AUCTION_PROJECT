# AUCTION_PROJECT

## Run backend

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Backend listens on `0.0.0.0:8081` by default, so other machines in the same network can connect to it.
Backend needs JDK 17+.
Current public server address in this repo is `lungs-decree.with.playit.plus:1125`.

## Run UI

```powershell
backend\mvnw.cmd -f ui\pom.xml javafx:run
```

UI is configured to connect directly to `http://lungs-decree.with.playit.plus:1125`.
Use `AUCTION_API_BASE_URL` or the JVM property `auction.api.baseUrl` only when you intentionally want a different backend.
UI needs JDK 21+.

## Notes for new clones

- Do not commit or reuse local files under `backend/data/`, `backend/uploads/`, or `backend/backend-restart*.log`.
- If backend fails to start because of H2 lock or stale local data, delete the files in `backend/data/` and run backend again.

# Render Deployment

This repository is configured for:

- GitHub Releases for the JavaFX desktop UI
- Render for the Spring Boot backend
- Render Postgres for the backend database

## What gets deployed where

- `backend/` deploys to Render as a Docker-based web service
- `ui/` is distributed as release bundles from GitHub Releases

## Why the backend uses a paid Render web service

The backend stores uploaded images on the filesystem under `app.upload.dir`.
Render Free web services have an ephemeral filesystem and cannot attach a persistent disk.
This app therefore uses a `starter` web service with a disk mounted at `/app/uploads`.

## Initial Render setup

1. Push this repository to GitHub.
2. In Render, connect your GitHub account.
3. Create a new Blueprint from this repository.
4. Confirm that Render reads `render.yaml` from the repository root.
5. Let Render create:
   - `auction-backend`
   - `auction-db`

## Continuous deployment behavior

The backend service is configured with `autoDeployTrigger: checksPass`.
That means Render deploys the linked branch only after GitHub Actions checks pass.

The backend health check is:

- `GET /api/hello`

## UI release flow

Tag a release to publish desktop bundles:

```bash
git tag v1.0.0
git push origin v1.0.0
```

The GitHub Actions release workflow publishes:

- `backend-dist.zip`
- `ui-linux-dist.zip`
- `ui-windows-dist.zip`

## Point the UI to Render

After the first backend deploy succeeds, copy the Render backend URL and set it in one of these ways:

- Edit `ui/src/main/resources/auction-client.properties`
- Or set `AUCTION_API_BASE_URL` when launching the desktop app

Example Render URL:

```text
https://auction-backend.onrender.com
```

## Notes

- Render docs indicate Java apps should run on Render via Docker, not a native Java runtime.
- Render docs also indicate persistent disks are available only on paid services.

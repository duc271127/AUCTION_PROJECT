# GitHub Release Usage

This project now uses GitHub Releases to publish runnable bundles.

## One-time GitHub setup

1. Push the repository to GitHub.
2. Open `Settings` -> `Actions` -> `General`.
3. In `Actions permissions`, allow GitHub Actions to run for the repository.
4. In `Workflow permissions`, choose `Read and write permissions`.
5. Save the settings.

Why this matters:

- the release workflow needs `contents: write` so it can publish a GitHub Release and attach files
- no extra secret is required for the current workflow because it uses the built-in `GITHUB_TOKEN`

## What stays unchanged

- The UI still points by default to the current shared backend configured in:
  - `ui/src/main/resources/auction-client.properties`
  - `ui/src/main/java/com/auction/client/config/EndpointConfig.java`
- You do not need to switch to localhost to use the release bundles.

## What the release workflow publishes

When you push a tag like `v1.0.0`, GitHub Actions publishes:

- `backend-dist.zip`
- `ui-linux-dist.zip`
- `ui-windows-dist.zip`

The workflow file is:

- `.github/workflows/release-cd.yml`

The repository also keeps the existing CI workflows:

- `.github/workflows/backend-ci.yml`
- `.github/workflows/ui-ci.yml`

Recommended flow:

1. push code to your normal branch
2. let CI pass
3. create a release tag
4. let `Release CD` publish the release bundles

## How to create a release

```bash
git tag v1.0.0
git push origin v1.0.0
```

If you do not want to publish a GitHub Release yet, you can still run `Release CD` manually from the `Actions` tab.
That manual run uploads build artifacts to the workflow run, but it does not create a tagged GitHub Release.

## How to run from the GitHub UI

### Publish a real release

1. Open your repository on GitHub.
2. Open `Releases`.
3. Click `Draft a new release`.
4. In `Choose a tag`, create a new tag such as `v1.0.0`.
5. Publish the release.
6. GitHub will push the tag to the repository.
7. The `Release CD` workflow starts automatically.
8. Wait for the workflow to finish, then refresh the release page.
9. Download:
   - `backend-dist.zip`
   - `ui-linux-dist.zip`
   - `ui-windows-dist.zip`

### Build artifacts only

1. Open `Actions`.
2. Select `Release CD`.
3. Click `Run workflow`.
4. Choose the branch.
5. Run it.
6. Download artifacts from that workflow run if needed.

## How to use the UI bundle

1. Download the bundle for your operating system from GitHub Releases.
2. Extract the zip file.
3. Run:
   - Windows: `run-ui.cmd`
   - Linux/macOS: `./run-ui.sh`

By default, the extracted `auction-client.properties` keeps the current shared backend URL from the repository.

If your backend endpoint changes later, update:

- `auction-client.properties`

Or launch with overrides:

- `AUCTION_API_BASE_URL`
- `AUCTION_WS_URL`

## How to use the backend bundle

Only use `backend-dist.zip` if your team wants to move or recreate the backend server.

Run:

- Windows: `run-backend.cmd`
- Linux/macOS: `./run-backend.sh`

Requirements:

- Java 21

The backend bundle reads configuration from:

- `application.properties`
- or environment variables such as `AUCTION_DB_URL`, `AUCTION_DB_USERNAME`, `AUCTION_DB_PASSWORD`, `AUCTION_SERVER_PORT`

## Current default server

The current repository default UI endpoint is:

```text
http://lungs-decree.with.playit.plus:1125
```

That means the released UI bundles still target the same shared backend unless your team changes `auction-client.properties` later.

## Verification checklist

After setup, check these items:

1. `Backend CI - JUnit Test` appears in `Actions`.
2. `UI CI` appears in `Actions`.
3. `Release CD` appears in `Actions`.
4. A pushed tag like `v1.0.0` triggers `Release CD`.
5. The release page contains the three zip files.

# VEX APK Signer Cloud Build API (prototype)

This adds an authenticated API and disposable Docker worker for standard Android Studio Java/Kotlin Gradle projects.

## Important current limitation

This first backend version compiles **release APK/AAB artifacts but does not sign them**. It must not be advertised as a signed APK/AAB service yet. Secure signing is a separate stage that needs protected keystore enrollment and secret handling. Never put a keystore or passwords inside a project ZIP.

## API

- `GET /health`
- `POST /v1/builds` multipart fields: `project_zip` and `output=apk|aab|both`; requires `Authorization: Bearer <VEX_API_TOKEN>`
- `GET /v1/builds/{build_id}`
- `GET /v1/builds/{build_id}/artifacts` (returns a ZIP after success)

## Build and run on a dedicated Linux VPS

1. Install Docker Engine and Docker Compose on a dedicated VPS.
2. Create a long random API token and export it as `VEX_API_TOKEN`. Never commit it to Git.
3. From this directory, build the worker image: `docker build -f worker.Dockerfile -t vex-apk-worker:latest .`
4. Start the API: `docker compose up --build -d`
5. Put the API behind HTTPS (for example, a reverse proxy with a valid TLS certificate). Do not expose port 8080 directly to the public internet.
6. Verify `GET /health` through the HTTPS endpoint.

## Security warning

The API control plane requires access to the Docker socket to create disposable workers. Treat the API host as highly privileged and dedicate it to this service. The Docker socket is **never mounted inside a build worker**. Workers receive only a read-only ZIP mount and a dedicated output directory, have CPU/memory/process limits, drop Linux capabilities, use a read-only root filesystem, and run without privilege escalation.

Uploaded Gradle projects execute arbitrary build scripts. The prototype worker currently has outbound network access so Gradle can download dependencies; this is not a hardened production egress policy. Run only on a dedicated host, add firewall/egress controls, rate limiting, durable job storage, per-user authorization, quotas, cleanup/retention, monitoring, and security review before public use.

The prototype keeps job state in process memory; restarting the API loses status records. It is not production-ready.

## Supported project type

Standard Android Studio Java/Kotlin Gradle projects with one `gradlew` wrapper. Flutter, React Native, CMake, and custom build systems are not supported by this first worker.

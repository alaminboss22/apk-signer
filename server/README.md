# VEX APK Signer Cloud Build Runner

The Android application currently selects a ZIP and lets the user choose APK, AAB, or both. A cloud build backend is not deployed yet, so the app does not claim to compile or sign files until that service exists.

## Planned API

- `POST /v1/builds`: authenticated multipart ZIP upload and `output=apk|aab|both`; returns a build ID.
- `GET /v1/builds/{id}`: returns queued/running/succeeded/failed status and sanitized log output.
- `GET /v1/builds/{id}/artifacts`: returns short-lived authenticated download URLs after success.

## Security

Follow the repository `SECURITY.md`. Build untrusted Gradle projects in disposable isolated workers with strict CPU, memory, disk, time, and network limits. Do not mount host filesystems, Docker sockets, production credentials, or signing keystores into compilation workers. Sign in a separate protected stage after compilation.

## Initial supported project type

Standard Android Studio Java/Kotlin Gradle projects. Flutter, React Native, native CMake, and arbitrary custom build systems need separate support and validation.

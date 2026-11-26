# VEX APK Signer

An Android starter app for selecting an Android project ZIP and choosing an APK, AAB, or combined build output.

## Current status

- Android Studio Kotlin + Jetpack Compose starter UI is committed.
- ZIP selection and output-format selection are implemented.
- GitHub Actions builds a debug APK and uploads it as an artifact.
- Cloud compilation, secure signing, build progress, and artifact downloads are **not implemented/deployed yet**. The app must not be treated as a working online APK/AAB signer until the backend is built and connected.

## Build

Open this repository in Android Studio and run the `app` configuration, or use the Actions tab and run **Android CI**. Download the `VEX-APK-Signer-Debug-APK` artifact from a successful workflow run.

## Scope

First target: standard Android Studio Java/Kotlin Gradle projects. See `server/README.md` for the planned cloud API and `SECURITY.md` for mandatory isolation and signing requirements.

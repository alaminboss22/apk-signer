# VEX APK Signer

An Android starter app and an initial cloud build API prototype for standard Android Studio Java/Kotlin Gradle projects.

## Current status

- Android Studio Kotlin + Jetpack Compose starter UI is committed.
- ZIP selection and output-format selection are implemented.
- GitHub Actions builds a debug APK and uploads it as an artifact.
- Initial authenticated Docker-worker API source is in `server/`.
- The cloud API is **not deployed or connected to the Android app**.
- The current worker compiles release APK/AAB artifacts but **does not sign them**. Secure keystore enrollment and a separate signing stage are still required.

Do not treat the current Android app as a working online signer yet.

## Build

Open this repository in Android Studio and run the `app` configuration, or use the Actions tab and download the `VEX-APK-Signer-Debug-APK` artifact from a successful workflow run.

## Backend

See `server/README.md` for prototype deployment steps and important security limitations. A dedicated Linux VPS with Docker and HTTPS is required; GitHub alone does not host this API.

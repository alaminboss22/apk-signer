# VEX APK Signer

Android starter app plus GitHub Actions workflows for building Android projects.

## Current status

- Android Kotlin + Jetpack Compose starter UI is committed.
- ZIP selection and APK/AAB output selection are implemented in the app.
- Android CI builds a debug APK of this VEX app.
- **Free GitHub Actions ZIP build workflow** is available at Actions → Build Android Project ZIP → Run workflow.
- The ZIP build workflow compiles release APK/AAB outputs and uploads them as a downloadable artifact.
- The ZIP build workflow currently produces **unsigned** release outputs. Secure signing is not implemented.
- The Android app's Start Build button is **not yet connected** to GitHub Actions. Run the workflow from the GitHub website for now.
- The Docker API under `server/` is a separate prototype and is not deployed.

## Free ZIP build instructions (phone browser)

1. Upload your Android Studio project ZIP to a file host that provides a direct HTTPS download URL. The URL must download the ZIP itself without an interactive login page. Keep private projects private; anyone with access to a public URL may download that ZIP.
2. Open this repository on GitHub and sign in.
3. Open the **Actions** tab.
4. Select **Build Android Project ZIP** in the workflow list.
5. Tap **Run workflow**, paste the direct HTTPS ZIP URL, choose `apk`, `aab`, or `both`, then run it.
6. Wait for the run to finish successfully.
7. Open the run and download the `VEX-Android-Project-Outputs` artifact. Extract the downloaded ZIP to get the APK/AAB.

Limits: ZIP download is capped at 200 MB; extracted content at 2 GB; one Gradle wrapper is required; builds time out after 35 minutes. Standard Android Studio Java/Kotlin Gradle projects are the target. Flutter and other build systems may need changes.

## Security and signing

Gradle build scripts execute code. Only build ZIPs you trust. The workflow does not receive signing passwords, keystores, or write permissions to the repository. Do not add a keystore or password to the ZIP or commit it to this public repository.

The generated release APK/AAB is not signed by this workflow. Do not install or publish it as a production release until a separate, secure signing process is added. Keep your upload keystore and passwords private.

## Android app

The current app UI is a starter and does not yet upload a ZIP to GitHub Actions or display workflow progress/downloads. The workflow is manually started through GitHub's website for now.

# PicoShell Agent Bundle

`PicoShell-Agent-2026` is a local-first Android agent shell built with Compose Multiplatform. It turns the original JSON spec bundle into a working mobile app with model management, prompt execution, runtime controls, and persistence.

## What The App Does

The app provides a mobile control surface for an on-device or hybrid agent workflow:

- Run prompts through a PicoClaw binary when one is configured
- Route prompts through an offline GGUF model lane
- Fall back to a cloud endpoint when local execution is not enough
- Manage model sources from the app
- Tune cache behavior with `Standard`, `PagedCache`, and `TurboQuant`
- Keep execution history and model state in a local SQLDelight database
- Expose Telegram and voice-control configuration from the UI

## Current Model Behavior

The app supports three real model acquisition paths:

1. Import a local `.gguf` file from Android document storage
2. Download a real model file when a catalog entry points to a direct artifact URL or a resolvable Hugging Face GGUF repository
3. Paste a direct `.gguf` file link or a Hugging Face model repository link into the app and download the resolved model into app-managed storage

Imported `.gguf` files are linked in place through Android's Storage Access Framework. The app persists read access to the selected document URI and does not create a duplicate copy inside app storage.

When a Hugging Face repository exposes multiple `.gguf` siblings, the app inspects the repository metadata and picks a preferred quantized artifact automatically, currently favoring `Q4_K_M` and similar mobile-friendly variants first.

If a catalog entry points to a repository that cannot be resolved into a GGUF artifact, the app does not pretend that the model was downloaded. In that case it:

1. Verifies the upstream page is reachable
2. Writes a local manifest under app storage
3. Marks the model as `Staged`, not `Ready`

If a pasted link cannot be resolved into a GGUF artifact, the app rejects the download and shows the resolution error in the UI instead of creating a fake local model entry.

## Tech Stack

- Framework: Compose Multiplatform
- Language: Kotlin 2.3.x
- Concurrency: Coroutines + Flow
- Persistence: SQLDelight
- Dependency Injection: Koin
- Networking: Ktor
- Build: Gradle Wrapper
- CI: GitHub Actions

## Project Layout

- `composeApp`: Android application module, manifest, activity, Koin startup
- `core`: Binary execution contracts and JVM process runner
- `domain`: Shared models and repository interfaces
- `data`: Seed catalog, SQLDelight schema, repository implementations
- `services`: Agent execution, cache strategy logic, downloads, local model import, cloud and Telegram integration
- `ui`: Theme, presenter, reusable components, dashboard/models/services screens

## Requirements

You need the following to build and run the project locally:

- JDK 17
- Android SDK installed locally
- Android platform `android-36`
- Android build-tools `36.0.0`
- Android platform-tools

Recommended local environment:

- Android Studio with Android SDK Manager
- `ANDROID_HOME` or `ANDROID_SDK_ROOT` pointing to the SDK location

## Local Setup

1. Install JDK 17.
2. Install Android SDK packages for API 36 and build-tools 36.0.0.
3. Open the project root in Android Studio, or use the Gradle wrapper from the terminal.
4. If Android Studio creates `local.properties`, keep it local. It is already ignored.

Example SDK packages:

```text
platform-tools
platforms;android-36
build-tools;36.0.0
```

## Build Commands

Debug APK:

```powershell
.\gradlew.bat :composeApp:assembleDebug
```

Release APK:

```powershell
.\gradlew.bat :composeApp:assembleRelease
```

Generated debug APK:

`composeApp/build/outputs/apk/debug/composeApp-debug.apk`

## Signed Release Setup

The Android app can load signing data from either:

- Gradle properties passed on the command line
- A local ignored file at `signing/release-signing.properties`

The signing keys expected by the build are:

- `RELEASE_STORE_FILE`
- `RELEASE_STORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

If `signing/release-signing.properties` exists, a normal release build command is enough:

```powershell
.\gradlew.bat :composeApp:assembleRelease
```

Example `signing/release-signing.properties`:

```properties
RELEASE_STORE_FILE=signing/release-keystore.jks
RELEASE_STORE_PASSWORD=your-store-password
RELEASE_KEY_ALIAS=your-key-alias
RELEASE_KEY_PASSWORD=your-key-password
```

Example:

```powershell
.\gradlew.bat :composeApp:assembleRelease `
  -PRELEASE_STORE_FILE="C:\keys\release.jks" `
  -PRELEASE_STORE_PASSWORD="your-store-password" `
  -PRELEASE_KEY_ALIAS="your-key-alias" `
  -PRELEASE_KEY_PASSWORD="your-key-password"
```

For GitHub Actions, the workflow expects these secret names:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

## Running The App

You can run the app from Android Studio on a device or emulator, or install the debug APK manually.

To get meaningful behavior inside the app:

- Import a local `.gguf` model, or stage a compatible catalog model from a direct artifact or Hugging Face GGUF repository
- Paste a direct `.gguf` URL or a Hugging Face model repository URL into the Models screen to download a model immediately
- Optionally configure a PicoClaw command if your Android environment exposes one
- Optionally configure a cloud fallback endpoint
- Optionally configure a Telegram handle or channel

Without any extra configuration:

- The UI runs
- Seed catalog data is visible
- Local import works
- Prompt execution falls back to the mocked offline lane and stored state

## Runtime Notes

Important operational details:

- Imported local models are linked by document URI, not copied into app storage
- Direct artifact downloads are stored in app-managed storage
- Hugging Face model repo links are auto-resolved to GGUF artifacts when the repo publishes compatible files
- Non-resolvable catalog links still produce staged manifests instead of fake downloads
- Voice control is a configuration lane right now, not a full speech-recognition pipeline
- Telegram integration currently produces deep links, not a fully authenticated bot session
- PicoClaw execution depends on a valid command that is actually available in the Android runtime environment

## CI

GitHub Actions is configured in `.github/workflows/android.yml`.

The workflow:

- Builds the debug APK on pushes, PRs, and manual runs
- Uploads `debug.apk` as an artifact
- Always prepares release signing material in CI
- Uses repository signing secrets when available
- Otherwise generates an ephemeral PKCS12 release keystore on the runner and signs the release build with it
- Continues publishing the debug APK even if signed release generation fails
- Uploads a `build-metadata` artifact with APK hashes, signer fingerprints, and build details
- Writes the same build summary into the GitHub Actions job summary
- Publishes available APKs to the GitHub Releases page for non-PR runs

Release workflow secrets:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Release publishing behavior:

- Non-PR runs publish APKs to a rolling prerelease tag named `ci-<branch>-latest`
- The debug APK is always the primary deliverable
- The release APK is signed with repository secrets when configured, otherwise with a generated ephemeral CI key
- The GitHub release also attaches `build-summary.md` with APK SHA-256 values and signer certificate digests

Important CI signing note:

- Generated CI signing keys are ephemeral and only stable for that one run
- Use the repository signing secrets if you need consistent package signing across releases and upgrades

## Step-By-Step Coverage

The implementation follows the original plan:

1. Bootstrap project
2. Create core process runner
3. Create domain models
4. Setup SQLDelight DB
5. Implement repositories
6. Implement agent executor
7. Implement download manager
8. Build design system
9. Build interactive components
10. Build screens
11. Integrate services
12. Setup CI

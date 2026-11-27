# Building Sippy (Android)

## Toolchain (pinned in `gradle/libs.versions.toml`)

| What | Version | Note |
|---|---|---|
| Gradle | 9.9.2 (wrapper) | `./gradlew` — the wrapper jar is committed |
| AGP | 9.5.2 | JDK 21; AGP 9 compiles Kotlin itself, so `org.jetbrains.kotlin.android` must **not** be applied |
| Kotlin | 2.4.21 | Compose + serialization compiler plugins only |
| compileSdk / targetSdk / buildTools | 37 / 37 / 37.0.0 | Kyant0 `backdrop` requires compileSdk 37 |
| minSdk | 26 | Android 8.0 |

Verified dependency pins (2026-11-27): compose 1.12.1 · material3 1.5.0 ·
activity-compose 1.13.0 · core-ktx 1.19.1 · lifecycle 2.11.1 · datastore 1.3.0 ·
work 2.12.0 · kotlinx-serialization-json 1.9.0 · coroutines 1.10.2 ·
`io.github.kyant0:backdrop` 2.0.1 · `io.github.kyant0:shapes` 1.2.1.

## Commands

```bash
cd sippy/android
./gradlew testDebugUnitTest     # pure-JVM tests (no device, no emulator)
./gradlew assembleDebug         # debug APK → app/build/outputs/apk/debug/
./gradlew assembleRelease       # signed release APK (needs the committed key)
```

## Release signing — the key is committed

The release key lives in this repository, in `sippy/android/signing/`:

- `sippy-release.jks` — the keystore
- `signing.properties` — four lines:

  ```properties
  storeFile=sippy-release.jks
  storePassword=<password>
  keyAlias=sippy
  keyPassword=<password>
  ```

`app/build.gradle.kts` reads that file, and only that file. There is no
environment-variable path and **no fallback to the debug key**: if either file
is missing, every release task fails and names the missing file.

### Creating the key (once, ever)

```bash
cd sippy/android/signing
keytool -genkeypair -v -keystore sippy-release.jks -alias sippy \
  -keyalg RSA -keysize 4096 -validity 10000
# then write signing.properties (four lines, see above)
```

### Committing it

```bash
git add sippy/android/signing/sippy-release.jks sippy/android/signing/signing.properties
git commit -m "android: commit the release signing key (public by design)"
```

`.gitignore` ignores every other `*.jks` / `*.keystore` and `local.properties`;
these two paths are explicitly un-ignored.

### Warning — read before you touch the key

The key is **public by design**: it is committed, it cannot be kept secret once
pushed, and it is only ever allowed to sign Sippy releases. Anyone holding it can
build an APK that Android would treat as an update to Sippy, so never reuse it for
another app, another project or a fork.

**It also cannot be made private later without breaking updates.** Android only
installs an update signed with the same certificate as the installed app. Swap
the key and every existing install has to be uninstalled first, losing local
data. To make that hard to do by accident, CI enforces trust on first use: the
first publish writes the certificate's SHA-256 into `releases/latest.json` as
`certSha256`, and every later build compares the APK's certificate against it
and fails with both values if they differ.

## What CI does

- `ci.yml` — every push and PR: `testDebugUnitTest` + `assembleDebug`. No
  signing material needed, no secrets, uploads nothing.
- `release.yml` — pushes to `main`, plus `workflow_dispatch` as a dry run. It
  compares `version.properties` with `releases/latest.json` first: if the
  `versionCode` is not newer, the build jobs are skipped and nothing is
  published. Otherwise it checks the committed signing files exist, runs the
  tests, builds and signs the APK, verifies package/versions/certificate with
  `aapt2` + `apksigner`, smoke-tests it on an API 34 emulator (boot + crash
  buffer + in-place upgrade), then publishes the GitHub Release and rewrites
  `releases/latest.json`.

## Releasing

One step, no tags to remember:

1. Bump `versionCode` (and `versionName` with it) in
   `sippy/android/version.properties` — e.g. `2` / `1.1.0`.
2. Commit and push to `main`.

The pipeline makes the `v1.1.0` tag and the release, uploads `sippy-1.1.0.apk`,
rewrites `releases/latest.json` (versionCode, SHA-256, `certSha256`) and
redeploys the site. Installed phones offer the update on their next check — the
app compares `versionCode`. A push that does not bump the version builds nothing.

To rebuild without publishing, run **Android release** → *Run workflow*: a manual
run always builds and smoke-tests, and publishes only if you tick `publish`.

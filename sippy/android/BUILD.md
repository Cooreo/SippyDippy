# Building Sippy (Android)

## Toolchain (pinned in `gradle/libs.versions.toml`)

| What | Version | Notes |
|---|---|---|
| Gradle | 9.9.2 (wrapper) | `./gradlew` — the wrapper jar is committed, as always |
| AGP | 9.5.2 | requires JDK 21 |
| Kotlin | 2.4.21 (compose & serialization compiler plugins) | AGP 9 compiles Kotlin itself: the `org.jetbrains.kotlin.android` plugin must **not** be applied — AGP 9 rejects it |
| compileSdk / targetSdk | 37 | Kyant0 `backdrop` requires compileSdk 37 |
| buildTools | 37.0.0 | |
| minSdk | 26 | |

Every pin below was resolved against `dl.google.com` / Maven Central on
2026-11-27 (the versions originally guessed from the reference repos — material3
1.9.0, datastore 1.1.7, work 2.10.5 — do not exist and were replaced):

| Artifact | Version | Source |
|---|---|---|
| `androidx.compose.ui:ui` (+ foundation, runtime, material-ripple) | 1.12.1 | google() |
| `androidx.compose.material3:material3` | 1.5.0 | google() |
| `androidx.activity:activity-compose` | 1.13.0 | google() |
| `androidx.core:core-ktx` | 1.19.1 | google() |
| `androidx.lifecycle:*` | 2.11.1 | google() |
| `androidx.datastore:datastore-preferences` | 1.3.0 | google() |
| `androidx.work:work-runtime-ktx` | 2.12.0 | google() |
| `org.jetbrains.kotlinx:kotlinx-serialization-json` | 1.9.0 | Central |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | 1.10.2 | Central |
| `io.github.kyant0:backdrop` | 2.0.1 | Central |
| `io.github.kyant0:shapes` | 1.2.1 | Central |

The Android SDK platform is installed as `android-37.0` / `android-37.1` /
`android-37.2` on current runner images; `compileSdk = 37` resolves to the
base 37 platform, and AGP downloads it if the image is older.

## Commands

```bash
cd sippy/android
./gradlew testDebugUnitTest     # pure-JVM tests (no device, no emulator)
./gradlew assembleDebug         # debug APK → app/build/outputs/apk/debug/
```

A **release** build reads its signing material from the environment only:

```bash
export ANDROID_KEYSTORE_PATH=/absolute/path/sippy-release.jks
export ANDROID_KEYSTORE_PASSWORD=…
export ANDROID_KEY_ALIAS=sippy
export ANDROID_KEY_PASSWORD=…
./gradlew assembleRelease
```

If any of the four is missing, any release task fails on purpose with a message
naming the missing variables. There is no debug-key fallback for release.

## One-time repository setup (before the first dry run)

1. Create the keystore (once, ever — it is the app's identity):
   ```bash
   keytool -genkeypair -v -keystore sippy-release.jks -alias sippy \
     -keyalg RSA -keysize 4096 -validity 10000 \
     -storetype JKS
   ```
2. Add the four repository secrets: `ANDROID_KEYSTORE_BASE64`
   (`base64 -w0 sippy-release.jks`), `ANDROID_KEYSTORE_PASSWORD`,
   `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`.
3. Fill `sippy/android/signing/expected-cert-sha256.txt` with the certificate
   SHA-256, lowercase hex, no colons:
   ```bash
   keytool -list -v -keystore sippy-release.jks -alias sippy \
     | grep -i 'SHA256:' | head -1 \
     | sed 's/.*SHA256: //' | tr -d ':' | tr 'A-F' 'a-f'
   ```
   Commit that one line. `release.yml` compares it with the certificate of the
   APK it just built and fails by design while the file still holds
   `PLACEHOLDER-REPLACE-ME` — the check exists so an APK signed with a second
   key can never reach a release (it would install as a different app or fail
   with an update conflict).
4. Nothing else. `ci.yml` needs no secrets; `release.yml` is a dry run on
   `workflow_dispatch` (build + emulator smoke test, publishes nothing) and
   publishes only for `v*` tags.

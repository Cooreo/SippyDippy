# Building Sippy (Android)

## Toolchain (pinned in `gradle/libs.versions.toml`)

| What | Version | Notes |
|---|---|---|
| Gradle | 9.9.2 (wrapper) | `./gradlew` — the wrapper jar is committed, as always |
| AGP | 9.5.2 | requires JDK 21 |
| Kotlin | 2.4.21 (+ compose & serialization plugins) | |
| compileSdk / targetSdk | 37 | Kyant0 `backdrop` requires compileSdk 37 |
| buildTools | 37.0.0 | |
| minSdk | 26 | |

⚠ = the version could not be checked from the build sandbox (maven.google.com and
repo1.maven.org are unreachable there). If CI reports that one of these does not
exist, drop it to the newest version the error message lists — nothing else changes.

| Artifact | Pinned | Fallback if unresolvable |
|---|---|---|
| `androidx.compose.material3:material3` | 1.9.0 | newest `1.9.x`, else `1.8.x` |
| `androidx.datastore:datastore-preferences` | 1.1.7 | newest `1.1.x` |
| `org.jetbrains.kotlinx:kotlinx-serialization-json` | 1.9.0 | newest `1.8+` |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | 1.10.2 | newest `1.10.x` |
| `androidx.work:work-runtime-ktx` | 2.10.5 | newest `2.10.x` |

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
   SHA-256. The release workflow prints the value it measured (as
   `APK cert SHA-256: …`) — copy it in, in lowercase hex, no colons. Until it
   matches, `release.yml` fails by design.

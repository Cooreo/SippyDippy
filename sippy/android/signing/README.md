# Release signing key — public by design

This directory holds the **only** key that signs Sippy releases:

| File | What it is |
|---|---|
| `sippy-release.jks` | RSA 4096 keystore, alias `sippy`, 10000-day validity |
| `signing.properties` | `storeFile`, `storePassword`, `keyAlias`, `keyPassword` for that keystore |

Both files are **committed on purpose**. Sippy is a sideloaded app built by
GitHub Actions with no secrets to configure, so the key has to be in the
repository. Treat it as public: anyone can read it, and anyone can build an APK
that Android will accept as an update to Sippy.

## Rules

1. **Public by design.** This is not a leaked secret; it is a published one.
   There is nothing here worth protecting, and no attempt is made to hide it.
2. **Sippy releases only.** Never sign another app, another project or anything
   you distribute with this key. It identifies the applicationId
   `com.cooreo.sippy` and nothing else.
3. **Never reuse it.** Not for a fork, not for a second app, not "just once".
   Generate a fresh key for anything else.
4. **It cannot be made private later.** Replacing the key breaks the update
   path: Android only installs an update whose signature matches the installed
   app. A new key means every user has to uninstall Sippy first and lose their
   local data. The CI check in `release.yml` exists to make that impossible to
   do by accident: the first publish pins this certificate's SHA-256 in
   `releases/latest.json`, and every later build must match it.

If the key is ever lost, rotate deliberately: generate a new keystore, delete
`certSha256` from `releases/latest.json` in the same commit as the first build
with the new key, and tell users a one-time reinstall is required.

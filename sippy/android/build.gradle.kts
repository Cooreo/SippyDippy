// Sippy — Android root build. Plugin versions live in gradle/libs.versions.toml
// (pinned, per sippy/shared/docs/api-notes.md §5).
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

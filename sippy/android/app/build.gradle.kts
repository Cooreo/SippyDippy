import java.util.Properties
import java.util.Locale

// AGP 9 compiles Kotlin itself (built-in Kotlin): the org.jetbrains.kotlin.android
// plugin must NOT be applied — AGP rejects it outright. Only the compiler
// plugins (Compose, serialization) are applied here.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// ---------------------------------------------------------------------------
// versionCode / versionName are the single source of truth: version.properties
// (bumped by hand in each release commit, checked by CI).
// ---------------------------------------------------------------------------
val versionFile = rootProject.file("version.properties")
val versionProps = Properties().apply {
    require(versionFile.exists()) { "version.properties is missing at ${versionFile.path}" }
    versionFile.inputStream().use { load(it) }
}
val sippyVersionCode = versionProps.getProperty("versionCode").trim().toInt()
val sippyVersionName = versionProps.getProperty("versionName").trim()

// ---------------------------------------------------------------------------
// Signing — environment only. Never a keystore in the repo, never the debug
// key for a release build.
// ---------------------------------------------------------------------------
val signingKeys = listOf(
    "ANDROID_KEYSTORE_PATH",
    "ANDROID_KEYSTORE_PASSWORD",
    "ANDROID_KEY_ALIAS",
    "ANDROID_KEY_PASSWORD",
)
val signingEnv = signingKeys.associateWith { System.getenv(it) }
val missingSigningKeys = signingKeys.filter { signingEnv[it].isNullOrBlank() }
val signingComplete = missingSigningKeys.isEmpty()

android {
    namespace = "com.cooreo.sippy"
    compileSdk = 37
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "com.cooreo.sippy"
        minSdk = 26
        targetSdk = 37
        versionCode = sippyVersionCode
        versionName = sippyVersionName
        vectorDrawables.useSupportLibrary = true
        // Only English is shipped; keep the APK small and predictable.
        resourceConfigurations += listOf("en")
    }

    signingConfigs {
        create("release") {
            if (signingComplete) {
                storeFile = file(signingEnv.getValue("ANDROID_KEYSTORE_PATH")!!)
                storePassword = signingEnv.getValue("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = signingEnv.getValue("ANDROID_KEY_ALIAS")
                keyPassword = signingEnv.getValue("ANDROID_KEY_PASSWORD")
                // v1: APK only (no AAB), so JAR signing stays on for old devices.
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        debug {
            // Never collides with a release install.
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            // v1: R8 stays OFF (brief). It comes back later, with tests.
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/DEPENDENCIES",
                "/META-INF/LICENSE*",
                "/META-INF/NOTICE*",
                "DebugProbesKt.bin",
            )
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    lint {
        // CI does not run lint in v1 (it needs network for the full check).
        abortOnError = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

// A release build without the four signing secrets must FAIL, never silently
// produce an unsigned or debug-signed APK.
gradle.taskGraph.whenReady {
    val releaseWorkRequested = allTasks.any { task ->
        val n = task.name.lowercase(Locale.ROOT)
        n.contains("release") && (n.startsWith("assemble") || n.startsWith("bundle") || n.startsWith("package"))
    }
    if (releaseWorkRequested && !signingComplete) {
        throw GradleException(
            "Release signing is not configured. Missing environment variable(s): " +
                missingSigningKeys.joinToString(", ") +
                ". Set all of ANDROID_KEYSTORE_PATH, ANDROID_KEYSTORE_PASSWORD, " +
                "ANDROID_KEY_ALIAS, ANDROID_KEY_PASSWORD (see sippy/android/BUILD.md).",
        )
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.ripple)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    // Glass (Kyant0 backdrop only — brief).
    implementation(libs.kyant.backdrop)
    implementation(libs.kyant.shapes)

    testImplementation(libs.junit)
}

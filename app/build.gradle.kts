import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// The Google Maps key lives in the git-ignored local.properties as MAPS_API_KEY=...
val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}
val mapsApiKey: String = localProps.getProperty("MAPS_API_KEY", "")

android {
    namespace = "com.cardash.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.cardash.app"
        // The OnePlus 5 runs Android 13; the Navigation SDK needs 24+.
        minSdk = 28
        // The Navigation SDK requires targetSdk 36. Wi-Fi/Bluetooth toggling is normally blocked
        // for modern targets, but Device Owner apps are exempt, which is why setup.sh makes CarDash
        // Device Owner.
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
        // Lets the UI fall back to the plain "Open Google Maps" tile when no key is configured.
        buildConfigField("boolean", "HAS_MAPS_KEY", (mapsApiKey.isNotBlank()).toString())
        // The Places SDK takes the key in code (it's in the manifest anyway).
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsApiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Sideloaded personal app: sign release with the debug key so `setup.sh` can install it.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // Required by the Navigation SDK.
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        // Wi-Fi/Bluetooth toggles are deprecated APIs, but allowed for Device Owner apps.
        disable += setOf("ExpiredTargetSdkVersion", "OldTargetApi")
    }
}

dependencies {
    // CarDash Link: the protocol shared with the main phone's app.
    implementation(project(":linkproto"))

    val composeBom = platform("androidx.compose:compose-bom:2026.06.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.12.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")

    // In-dashboard Google map + turn-by-turn (Navigation SDK; includes the Maps SDK, so
    // play-services-maps must not be added). First 1,000 destinations/month are free.
    implementation("com.google.android.libraries.navigation:navigation:8.0.0")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:2.1.5")
    // Place search by name (Places API (New) Text Search: 5,000 free/month). The Navigation SDK
    // already contains the Maps SDK, so the Places SDK's copy is excluded.
    implementation("com.google.android.libraries.places:places:6.0.2") {
        exclude(group = "com.google.android.gms", module = "play-services-maps")
    }

    // Front/back camera recording with no preview (runs in a camera foreground service).
    val camerax = "1.6.2"
    implementation("androidx.camera:camera-camera2:$camerax")
    implementation("androidx.camera:camera-lifecycle:$camerax")
    implementation("androidx.camera:camera-video:$camerax")
    implementation("androidx.lifecycle:lifecycle-service:2.10.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}

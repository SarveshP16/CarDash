plugins {
    id("com.android.library")
}

// Shared by CarDash (car phone) and CarDash Link (main phone): messages, pairing and share parsing.
// Plain JDK + org.json only, so both sides and the unit tests use exactly the same code.
android {
    namespace = "com.cardash.link.proto"
    compileSdk = 37

    defaultConfig {
        minSdk = 28
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    // Android's org.json is a stub in local unit tests; use the real one there.
    testImplementation("org.json:json:20240303")
}

package com.cardash.link.proto

/** Third-party software in the apps, for their "Open-source licences" screens. */
object OpenSource {
    data class Notice(val name: String, val licence: String, val url: String)

    private const val APACHE = "Apache License 2.0"
    private const val APACHE_URL = "https://www.apache.org/licenses/LICENSE-2.0"
    private const val GOOGLE_MAPS_TERMS = "Google Maps Platform Terms of Service"
    private const val GOOGLE_MAPS_TERMS_URL = "https://cloud.google.com/maps-platform/terms"

    private val common = listOf(
        Notice("Jetpack Compose and AndroidX libraries", APACHE, APACHE_URL),
        Notice("Material icons", APACHE, APACHE_URL),
        Notice("Kotlin standard library and kotlinx.coroutines", APACHE, APACHE_URL),
        Notice("Lexend font", "SIL Open Font License 1.1", "https://openfontlicense.org"),
    )

    val carDash: List<Notice> = common + listOf(
        Notice("CameraX", APACHE, APACHE_URL),
        Notice("Google Navigation SDK and Maps SDK for Android", GOOGLE_MAPS_TERMS, GOOGLE_MAPS_TERMS_URL),
        Notice("Places SDK for Android", GOOGLE_MAPS_TERMS, GOOGLE_MAPS_TERMS_URL),
        Notice(
            "Java core library desugaring",
            "GNU GPL v2 with Classpath Exception",
            "https://github.com/google/desugar_jdk_libs",
        ),
    )

    val link: List<Notice> = common
}

import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun setting(name: String, fallback: String): String =
    (project.findProperty(name) as String?)?.takeIf { it.isNotBlank() }
        ?: localProperties.getProperty(name)?.takeIf { it.isNotBlank() }
        ?: fallback

android {
    namespace = "com.abaye.derechkohanim"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.abaye.derechkohanim"
        minSdk = 30
        targetSdk = 36
        versionCode = 4
        versionName = "0.2.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["mapsApiKey"] = setting("MAPS_API_KEY", "")
    }

    // Hebrew is the default locale, English the only alternative shipped.
    androidResources {
        localeFilters += listOf("he", "en")
    }

    // The one place the API address lives. Defaults to the live server, so any build - debug
    // or release, on a device or the emulator - talks to production out of the box. For local
    // development against the dev server on port 3001, override it and permit cleartext to the
    // loopback host (already allowlisted in network_security_config.xml):
    //     ./gradlew installDebug -PAPI_BASE_URL=http://127.0.0.1:3001/api/
    // (reach it from a real device over USB with `adb reverse tcp:3001 tcp:3001`).
    // HTTPS to the server is trusted on NetFree devices too: network_security_config.xml
    // bundles the NetFree roots alongside the system store.
    val apiBaseUrl = setting("API_BASE_URL", "https://derech-kohanim.dinobycloud.com/api/")

    // The backend's Google OAuth *Web* client id, used as the serverClientId so the ID token
    // the device returns is one the API can verify. It belongs to the deployment, not the
    // source tree, so it is supplied at build time (local.properties: GOOGLE_SERVER_CLIENT_ID).
    // Empty here means Google sign-in stays disabled rather than failing. Sign-in also needs an
    // Android OAuth client registered in the same project (package + signing SHA-1).
    val googleServerClientId = setting("GOOGLE_SERVER_CLIENT_ID", "")

    // The MapLibre style/source URL for the fallback renderer that works on NetFree. EMPTY by
    // default, which makes DkMapLibre load the bundled "Waze Day" style (res/raw/waze_day_style.json)
    // - a custom MapLibre GL style tuned to the Waze-day palette over OpenFreeMap's OpenMapTiles
    // vector tiles, with Hebrew glyphs. Set this to a non-empty URL to override with a remote style
    // instead (e.g. a future self-hosted style JSON - or a `pmtiles://` source; MapLibre >= 11.7
    // supports PMTiles natively, all of Israel from one static file) - a config-only change:
    //     ./gradlew installDebug -PMAPLIBRE_STYLE_URL=https://maps.example.org/styles/dk
    val mapLibreStyleUrl = setting("MAPLIBRE_STYLE_URL", "")

    // Google Analytics 4 via the Measurement Protocol - events are POSTed over the app's own
    // OkHttp/TLS stack (which already trusts the NetFree roots), NOT Firebase, so it works on
    // filtered networks. Both belong to the deployment, not the source tree, so they are supplied
    // at build time (local.properties or -P). BOTH must be non-empty for analytics to do anything:
    // when either is blank the analytics client is a complete no-op and makes no network calls.
    //     ./gradlew installRelease -PGA_MEASUREMENT_ID=G-XXXXXXXXXX -PGA_API_SECRET=xxxxxxxx
    // GA_MEASUREMENT_ID is the GA4 "G-..." measurement id; GA_API_SECRET is the Measurement
    // Protocol api_secret created under Admin > Data Streams > Measurement Protocol API secrets.
    val gaMeasurementId = setting("GA_MEASUREMENT_ID", "")
    val gaApiSecret = setting("GA_API_SECRET", "")

    buildTypes {
        debug {
            isMinifyEnabled = false
            buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
            buildConfigField("String", "GOOGLE_SERVER_CLIENT_ID", "\"$googleServerClientId\"")
            buildConfigField("String", "MAPLIBRE_STYLE_URL", "\"$mapLibreStyleUrl\"")
            buildConfigField("String", "GA_MEASUREMENT_ID", "\"$gaMeasurementId\"")
            buildConfigField("String", "GA_API_SECRET", "\"$gaApiSecret\"")
            buildConfigField("boolean", "USE_MOCK_DATA", setting("USE_MOCK_DATA", "false"))
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
            buildConfigField("String", "GOOGLE_SERVER_CLIENT_ID", "\"$googleServerClientId\"")
            buildConfigField("String", "MAPLIBRE_STYLE_URL", "\"$mapLibreStyleUrl\"")
            buildConfigField("String", "GA_MEASUREMENT_ID", "\"$gaMeasurementId\"")
            buildConfigField("String", "GA_API_SECRET", "\"$gaApiSecret\"")
            buildConfigField("boolean", "USE_MOCK_DATA", setting("USE_MOCK_DATA", "false"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.androidx.datastore.preferences)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    implementation(libs.maps.compose)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)

    // MapLibre GL Android - the second (OpenFreeMap) renderer, used as a NetFree fallback. It
    // draws through the app's own OkHttp/TLS stack, so it honours network_security_config.xml
    // and works on NetFree where Google Maps (Play Services, own pinned trust) fails.
    implementation(libs.maplibre.android)

    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.google.identity.googleid)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
}

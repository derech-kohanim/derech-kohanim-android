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
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["mapsApiKey"] = setting("MAPS_API_KEY", "")
    }

    // Hebrew is the default locale, English the only alternative shipped.
    androidResources {
        localeFilters += listOf("he", "en")
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            // Real server data is the default. The dev server runs on port 3001. The base
            // points at 127.0.0.1:3001, which reaches the host from BOTH a real device (over
            // USB via `adb reverse tcp:3001 tcp:3001`) and the emulator. 127.0.0.1 is in the
            // cleartext allowlist of the network security config, so NetFree does not block
            // it. Override with -PAPI_BASE_URL=... (e.g. a host LAN IP on the same subnet).
            buildConfigField("String", "API_BASE_URL", "\"${setting("API_BASE_URL", "http://127.0.0.1:3001/api/")}\"")
            buildConfigField("boolean", "USE_MOCK_DATA", setting("USE_MOCK_DATA", "false"))
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("String", "API_BASE_URL", "\"${setting("API_BASE_URL", "https://api.derechkohanim.org/api/")}\"")
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

    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.google.identity.googleid)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
}

import java.util.Properties

plugins {
    // AGP 9 has built-in Kotlin support — do NOT apply kotlin.android separately.
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Credentials are read from local.properties (not committed). See local.properties.example.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun env(key: String, default: String = ""): String =
    (localProps.getProperty(key) ?: System.getenv(key) ?: default)

android {
    namespace = "com.mapzone.mapzonealertview"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.mapzone.AlertViewApp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "VIETMAP_API_KEY", "\"${env("VIETMAP_API_KEY")}\"")
        buildConfigField("String", "VIETMAP_TILEMAP_KEY", "\"${env("VIETMAP_TILEMAP_KEY")}\"")
        buildConfigField("String", "BASE_URL", "\"${env("BASE_URL")}\"")
        buildConfigField("String", "SEGMENT_URL", "\"${env("SEGMENT_URL", "")}\"")
        buildConfigField("String", "SPEED_ALERT_API_KEY_ID", "\"${env("SPEED_ALERT_API_KEY_ID")}\"")
        buildConfigField("String", "SPEED_ALERT_API_KEY", "\"${env("SPEED_ALERT_API_KEY")}\"")
        buildConfigField("String", "APP_BUNDLE_ID", "\"${env("APP_BUNDLE_ID", "com.mapzone.mapzonealertview")}\"")
        buildConfigField("String", "VEHICLE_ID", "\"${env("VEHICLE_ID", "10")}\"")
    }

    val releaseStorePath = env("RELEASE_STORE_FILE")
    val hasReleaseKeystore = releaseStorePath.isNotEmpty() && rootProject.file(releaseStorePath).exists()
    signingConfigs {
        create("release") {
            if (hasReleaseKeystore) {
                storeFile = rootProject.file(releaseStorePath)
                storePassword = env("RELEASE_STORE_PASSWORD")
                keyAlias = env("RELEASE_KEY_ALIAS")
                keyPassword = env("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            if (hasReleaseKeystore) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // The route-based Alert View SDK (published library).
    implementation(libs.mapzone.alert.view.android)

    // Compose UI
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.material)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)

    // Search API (Vietmap autocomplete/place)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    // Vietmap Maps + Navigation (base map, route building, SnapToRoute, LocationEngine)
    implementation(libs.vietmap.maps.sdk)
    implementation(libs.vietmap.navigation)
    implementation(libs.vietmap.navigation.ui)
    implementation(libs.vietmap.services.core)
    implementation(libs.vietmap.services.directions.models)
    implementation(libs.vietmap.services.turf)
    implementation(libs.vietmap.services)
    implementation(libs.vietmap.services.geojson)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

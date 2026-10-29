import java.net.URI

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.nur.quran"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.nur.quran"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // All builds use the shared Heroku backend. Override only the HTTPS API origin.
        val renderApiBaseUrl = providers.gradleProperty("quranNur.apiBaseUrl")
            .orElse("https://qurannur-backend-d7d7312f118d.herokuapp.com").get()
        val renderUri = URI(renderApiBaseUrl)
        require(renderUri.scheme == "https" && !renderUri.host.isNullOrBlank() &&
            renderUri.userInfo == null && renderUri.rawQuery == null && renderUri.rawFragment == null) {
            "quranNur.apiBaseUrl must be an HTTPS URL without credentials, query, or fragment"
        }
        buildConfigField("String", "QURAN_NUR_API_BASE_URL", "\"$renderApiBaseUrl\"")

    }

    // Release signing credentials live OUTSIDE the repo:
    // ~/.gradle/gradle.properties (QURAN_NUR_STORE_FILE/_PASSWORD/_ALIAS)
    // or the same names as environment variables. Unsigned release builds
    // still work when they are absent (not installable — debug instead).
    val releaseStoreFile = (project.findProperty("QURAN_NUR_STORE_FILE") as String?)
        ?: System.getenv("QURAN_NUR_STORE_FILE")
    val releaseStorePassword = (project.findProperty("QURAN_NUR_STORE_PASSWORD") as String?)
        ?: System.getenv("QURAN_NUR_STORE_PASSWORD")
    val releaseKeyAlias = (project.findProperty("QURAN_NUR_KEY_ALIAS") as String?)
        ?: System.getenv("QURAN_NUR_KEY_ALIAS")
    if (releaseStoreFile != null && releaseStorePassword != null && releaseKeyAlias != null) {
        signingConfigs {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseStorePassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseSigning = signingConfigs.findByName("release")
            if (releaseSigning != null) signingConfig = releaseSigning
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        // BuildConfig generation is off by default on AGP 8 — the QURAN_NUR_* fields
        // above need it (no code read BuildConfig before this).
        buildConfig = true
    }
    // SyncService/SyncWorker log through android.util.Log; JVM tests must not
    // throw "not mocked" — return default values for framework stubs instead.
    testOptions.unitTests.isReturnDefaultValues = true
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.ui.text.google.fonts)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.gson)
    implementation(libs.okhttp.logging)

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    // Media Player
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)

    // Preferences DataStore
    implementation(libs.datastore.preferences)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // DocumentFile (SAF linked audio)
    implementation(libs.androidx.documentfile)

    // WorkManager (background audio downloads)
    implementation(libs.androidx.work.runtime)

    // Google Credential Manager (compatible with this project's compileSdk 34).
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

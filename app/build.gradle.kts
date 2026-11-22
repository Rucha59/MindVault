import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
}

// Release signing secrets live in keystore.properties (git-ignored), never in this file.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

// Supabase URL and anon key live in local.properties (git-ignored). Never the service_role key.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
fun buildConfigString(value: String): String = "\"" + value.trim().replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    namespace = "com.mindvault"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mindvault"
        minSdk = 26
        targetSdk = 36
        versionCode = 1 // must increase with every Play upload
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Blank values are allowed: the app then runs in guest-only mode and hides sign-in.
        buildConfigField("String", "SUPABASE_URL", buildConfigString(localProperties.getProperty("supabase.url", "")))
        buildConfigField("String", "SUPABASE_ANON_KEY", buildConfigString(localProperties.getProperty("supabase.anonKey", "")))
        // Google OAuth *Web* client ID (public). Never the client secret. Blank hides the Google button.
        // Email/password sign-in stays in the code but hidden until real email delivery (custom SMTP) exists.
        buildConfigField("boolean", "AUTH_EMAIL_ENABLED", localProperties.getProperty("auth.emailEnabled", "false").trim().toBoolean().toString())
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", buildConfigString(localProperties.getProperty("google.webClientId", "")))
    }

    signingConfigs {
        create("release") {
            if (keystoreProperties.isNotEmpty()) {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (keystoreProperties.isNotEmpty()) {
                signingConfig = signingConfigs.getByName("release")
            }
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
        buildConfig = true
    }
    testOptions { unitTests.isReturnDefaultValues = true }

    // Lets migration tests read the schema JSON that Room exports.
    sourceSets {
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }
}

// Room exports each schema version as JSON here. Commit this folder to Git.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    // Needed for filled/outlined icon pairs (Bookmarks, Style) required by the design system.
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Accounts (Phase A): Supabase Auth with the OkHttp Ktor engine (reuses the project's OkHttp 4.12).
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth)
    // Phase A3: only used to call the delete_my_account database function (sync in Phase C reuses it).
    implementation(libs.supabase.postgrest)
    implementation(libs.ktor.client.okhttp)

    // Google sign-in (Phase A2): Credential Manager gets a Google ID token, which Supabase verifies.
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.room.testing)
}
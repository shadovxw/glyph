import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Secrets live in local.properties (never committed) and reach the app through BuildConfig.
val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

fun configString(key: String): String =
    (localProperties.getProperty(key) ?: "").replace("\\", "\\\\").replace("\"", "\\\"")

android {
    namespace = "me.shadovxw.glyph"
    compileSdk = 37

    defaultConfig {
        applicationId = "me.shadovxw.glyph"
        minSdk = 31
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"

        // github.accounts=Personal:TOKEN,Work:TOKEN  (one personal access token per account)
        buildConfigField("String", "GITHUB_ACCOUNTS", "\"${configString("github.accounts")}\"")
        // homelab.url=http://192.168.1.10:8080  (the Homelab tile checks this host and port, and opens it)
        buildConfigField("String", "HOMELAB_URL", "\"${configString("homelab.url")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.foundation)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.health.connect)
    implementation(libs.zxing.core) // draws the hotspot QR
}

import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.room)

}

fun String.asBuildConfigString(): String = "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

fun configValue(name: String, localPropertyName: String, defaultValue: String): String {
    return providers.gradleProperty(name).orNull
        ?: System.getenv(name)
        ?: localProperties.getProperty(localPropertyName)
        ?: defaultValue
}

val apiBaseUrl = configValue(
    name = "API_BASE_URL",
    localPropertyName = "api.baseUrl",
    defaultValue = "http://10.0.2.2:8080/"
)
val apiHost = configValue(
    name = "API_HOST",
    localPropertyName = "api.host",
    defaultValue = ""
)
val apiCertPin = configValue(
    name = "API_CERT_PIN",
    localPropertyName = "api.certPin",
    defaultValue = ""
)
val releaseApiBaseUrl = configValue(
    name = "RELEASE_API_BASE_URL",
    localPropertyName = "release.apiBaseUrl",
    defaultValue = "https://skautaibackend-production.up.railway.app/"
)
val releaseApiHost = configValue(
    name = "RELEASE_API_HOST",
    localPropertyName = "release.apiHost",
    defaultValue = "skautaibackend-production.up.railway.app"
)
val passwordResetHost = configValue(
    name = "PASSWORD_RESET_HOST",
    localPropertyName = "passwordReset.host",
    defaultValue = releaseApiHost
)
val releaseApiCertPin = configValue(
    name = "RELEASE_API_CERT_PIN",
    localPropertyName = "release.apiCertPin",
    defaultValue = ""
)
val privacyPolicyUrl = configValue(
    name = "PRIVACY_POLICY_URL",
    localPropertyName = "privacy.policyUrl",
    defaultValue = "https://skautaibackend-production.up.railway.app/privacy.html"
)
val supportEmail = configValue(
    name = "SUPPORT_EMAIL",
    localPropertyName = "support.email",
    defaultValue = "support@skautuinventorius.lt"
)
val privacyEmail = configValue(
    name = "PRIVACY_EMAIL",
    localPropertyName = "privacy.email",
    defaultValue = "privacy@skautuinventorius.lt"
)

android {
    namespace = "lt.skautai.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "lt.skautai.android"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["passwordResetHost"] = passwordResetHost
        buildConfigField("String", "API_BASE_URL", apiBaseUrl.asBuildConfigString())
        buildConfigField("String", "API_HOST", apiHost.asBuildConfigString())
        buildConfigField("String", "API_CERT_PIN", apiCertPin.asBuildConfigString())
        buildConfigField("String", "PRIVACY_POLICY_URL", privacyPolicyUrl.asBuildConfigString())
        buildConfigField("String", "SUPPORT_EMAIL", supportEmail.asBuildConfigString())
        buildConfigField("String", "PRIVACY_EMAIL", privacyEmail.asBuildConfigString())
    }

    buildTypes {
        debug {
            manifestPlaceholders["usesCleartextTraffic"] = "true"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            manifestPlaceholders["usesCleartextTraffic"] = "false"
            buildConfigField("String", "API_BASE_URL", releaseApiBaseUrl.asBuildConfigString())
            buildConfigField("String", "API_HOST", releaseApiHost.asBuildConfigString())
            buildConfigField("String", "API_CERT_PIN", releaseApiCertPin.asBuildConfigString())
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
        buildConfig = true
        compose = true
    }
    packaging {
        resources {
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material")
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.datastore.preferences)
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.androidx.hilt.compiler)


    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // WorkManager
    implementation(libs.work.runtime.ktx)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.zxing.embedded)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.crashlytics)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

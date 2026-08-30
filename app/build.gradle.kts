import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val localPropertiesFile = rootProject.file("local.properties")
val localProperties = Properties()

if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { inputStream ->
        localProperties.load(inputStream)
    }
}

fun localProperty(name: String, fallback: String): String {
    val value = localProperties.getProperty(name)
    return if (value.isNullOrBlank()) fallback else value.trim()
}

fun String.asBuildConfigString(): String {
    return "\"" + replace("\\", "\\\\")
        .replace("\"", "\\\"") + "\""
}

android {
    namespace = "com.example.phonghochaui"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.phonghochaui"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "SUPABASE_URL",
            localProperty(
                "SUPABASE_URL",
                "https://your-project-ref.supabase.co/"
            ).asBuildConfigString()
        )
        buildConfigField(
            "String",
            "SUPABASE_PUBLISHABLE_KEY",
            localProperty(
                "SUPABASE_PUBLISHABLE_KEY",
                "YOUR_PUBLISHABLE_KEY"
            ).asBuildConfigString()
        )
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.gson.converter)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.androidx.navigation.fragment)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.compose)
    id("com.google.android.gms.oss-licenses-plugin")
    kotlin("plugin.serialization") version "2.1.10"
}

android {
    namespace = "ru.morozovit.ultimatesecurity"
    compileSdk = 35

    defaultConfig {
        applicationId = "ru.morozovit.ultimatesecurity"
        minSdk = 24
        targetSdk = 35
        versionCode = 26
        versionName = "1.7.3"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug").apply {
                enableV3Signing = true
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.gson)
    implementation(libs.androidx.exifinterface)
    implementation(libs.core)
    implementation(libs.biometric)
    implementation(kotlin("reflect"))
    implementation(libs.cloudy)
    // compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.ui.viewbinding)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.adaptive)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.constraintlayout.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.accompanist.drawablepainter)
    implementation(libs.compose.video)

    implementation(libs.play.services.oss.licenses)

    implementation(project(":androidUtils"))
    implementation(project(":utils"))
}
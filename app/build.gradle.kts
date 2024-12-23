plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.compose)
    id("com.google.android.gms.oss-licenses-plugin")
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
            signingConfig = signingConfigs.getByName("debug")
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
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.gson)
    implementation(libs.androidx.exifinterface)
    implementation(libs.core)
    implementation(libs.shimmer)
    implementation(libs.androidx.biometric.v110)
    implementation(libs.biometric)
    implementation(kotlin("reflect"))
    implementation(libs.jasypt)
    // compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.adaptive)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.constraintlayout.compose)

    implementation(libs.play.services.oss.licenses)
}
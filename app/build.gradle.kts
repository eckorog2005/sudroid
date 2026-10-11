plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.rlamb.android.sudroid"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.rlamb.android.sudroid"
        minSdk = 24
        targetSdk = 37
        versionCode = 4
        versionName = "1.2"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

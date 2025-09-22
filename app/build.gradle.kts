plugins {
    id("com.android.application")
}

android {
    namespace = "com.drhowdydoo.layoutinspector"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.drhowdydoo.layoutinspector"
        minSdk = 29
        targetSdk = 36
        versionCode = 11
        versionName = "1.2.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.14.0-alpha04")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("io.github.amrdeveloper:treeview:1.2.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
}
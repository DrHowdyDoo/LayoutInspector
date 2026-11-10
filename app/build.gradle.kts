plugins {
    id("com.android.application")
}

android {
    namespace = "com.drhowdydoo.layoutinspector"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.drhowdydoo.layoutinspector"
        minSdk = 29
        targetSdk = 37
        versionCode = 13
        versionName = "1.2.4"

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

    implementation("androidx.appcompat:appcompat:1.8.0")
    implementation("com.google.android.material:material:1.14.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.2")
    implementation("io.github.amrdeveloper:treeview:1.2.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
}
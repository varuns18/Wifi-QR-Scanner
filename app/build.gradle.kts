plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.ramphal.wifiqrscanner"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ramphal.wifiqrscanner"
        minSdk = 21
        targetSdk = 34
        versionCode = 5
        versionName = "5.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk.debugSymbolLevel = "FULL"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    ndkVersion = "28.0.12433566 rc1"
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation(libs.barcode.scanning)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(libs.play.services.ads)
    implementation(libs.zxing.android.embedded)

    // CameraX core library
    implementation (libs.camera.core)

    // Guava library (includes ListenableFuture)
    implementation (libs.guava)

    implementation(libs.lifecycle.process)
    implementation (platform(libs.kotlin.bom))
}
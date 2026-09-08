plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "pl.nygus.live"
    compileSdk = 35

    defaultConfig {
        applicationId = "pl.nygus.live"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
}
implementation("com.github.pedroSG94.RootEncoder:library:2.8.1")

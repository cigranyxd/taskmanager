plugins {

    alias(libs.plugins.kotlin.android)
    id("com.android.application")
    id("com.google.gms.google-services")

}

android {
    namespace = "com.example.szemelyes_penzugyi_menedzser"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.szemelyes_penzugyi_menedzser"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.database.ktx)
    implementation(libs.androidx.material3.android)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.places)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.auth.ktx)
    implementation(platform(libs.google.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.mpandroidchart)
    implementation (libs.androidx.appcompat.v161)
    implementation (libs.firebase.firestore)
    implementation (libs.play.services.auth)
    implementation (libs.play.services.base)
    implementation (libs.play.services.auth)


}


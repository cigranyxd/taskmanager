plugins {
    alias(libs.plugins.kotlin.android)
    id("com.android.application")
    id("com.google.gms.google-services") // Firebase szolgáltatásokhoz
}

android {
    namespace = "com.example.szemelyes_penzugyi_menedzser"
    compileSdk = 35  // Frissítve: használj API 35-öt!

    defaultConfig {
        applicationId = "com.example.szemelyes_penzugyi_menedzser"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Alap AndroidX csomagok
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")

    // ConstraintLayout (MotionLayout is included)
    implementation(libs.androidx.constraintlayout)
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Firebase csomagok
    implementation(platform("com.google.firebase:firebase-bom:32.3.1"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-analytics")

    // Google Services (bejelentkezéshez és API-khoz)
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("com.google.android.gms:play-services-base:18.2.0")
    implementation("com.google.android.libraries.places:places:3.3.0")

    // Lifecycle komponensek
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")

    // UI és Material 3
    implementation("androidx.compose.material3:material3:1.1.2")

    // MPAndroidChart (grafikonokhoz)
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // JSON kezeléshez
    implementation("com.google.code.gson:gson:2.10.1")
    implementation(libs.androidx.work.runtime.ktx)

    // Teszteléshez
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Debugging és UI tesztelés
    debugImplementation("androidx.compose.ui:ui-tooling:1.5.3")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.5.3")
}

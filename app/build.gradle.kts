plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
    id ("kotlin-parcelize")
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.nihongo"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.nihongo"
        minSdk = 24
        targetSdk = 35
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }



    packaging {
        resources {
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/NOTICE.md"
            excludes += "META-INF/DEPENDENCIES"
        }
    }
}

dependencies {
    // Thêm thư viện Gson
    implementation("com.google.code.gson:gson:2.10.1")
    
    // ROOM
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.text)
    kapt(libs.room.compiler)

    // Firestore (chỉ Firestore, không Auth)
    implementation(platform("com.google.firebase:firebase-bom:32.1.1"))  // Đảm bảo Firebase BOM được thêm vào
    implementation("com.google.firebase:firebase-firestore-ktx:25.1.3")  // Phiên bản sẽ được quản lý bởi BOM
    implementation("com.google.firebase:firebase-firestore:25.1.3")

    implementation("com.google.android.gms:play-services-auth:21.1.0")

    // Compose UI
    implementation(platform(libs.androidx.compose.bom))
    implementation("androidx.compose.ui:ui")
    // Example for build.gradle.kts
    implementation("androidx.compose.material3:material3:1.2.1") // Use a recent version
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Navigation
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.navigation.compose)

    // Lifecycle & Activity
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Gmail Sender / Cloudinary nếu cần
    implementation("com.sun.mail:android-mail:1.6.7")
    implementation("com.sun.mail:android-activation:1.6.7")
    implementation("com.cloudinary:cloudinary-android:2.3.1")

    implementation("io.coil-kt:coil-compose:2.2.2")

    implementation("androidx.compose.foundation:foundation:1.8.0") // hoặc phiên bản mới nhất

    implementation ("com.google.android.exoplayer:exoplayer:2.18.1")

    implementation ("com.google.accompanist:accompanist-flowlayout:0.32.0")
    implementation("androidx.compose.ui:ui-text-google-fonts:1.5.0")
    //Admin
    implementation("com.google.firebase:firebase-messaging:23.4.1")
    implementation("com.google.auth:google-auth-library-oauth2-http:1.18.0")
    implementation("com.onesignal:OneSignal:[5.1.6, 5.1.99]")

}


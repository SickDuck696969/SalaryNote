plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // Sử dụng KSP thay cho kapt để không bị lỗi Java
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.example.tinhluong" // Lưu ý: Đổi tên này nếu package của bạn khác
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.tinhluong" // Lưu ý: Đổi tên này nếu package của bạn khác
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
        compose = true
    }
    composeOptions {
        // Tương thích với Kotlin 1.9.20
        kotlinCompilerExtensionVersion = "1.5.4"
    }
}

dependencies {
    // Android Core & Lifecycle
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Jetpack Compose
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // Fix lỗi "Theme.Material3.DayNight.NoActionBar not found"
    implementation("com.google.android.material:material:1.11.0")

    // ViewModel cho Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")

    // Room Database
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")

    // Khai báo KSP để build Room (Thay thế cho kapt)
    // Thêm dòng này vào:
    add("ksp", "androidx.room:room-compiler:$room_version")
}
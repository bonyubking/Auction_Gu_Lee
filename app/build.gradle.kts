plugins {
    id("com.android.application")
    id("kotlin-android")
    id("com.google.gms.google-services")
    id("kotlin-kapt")
}

android {
    namespace = "com.example.auction_gu_lee"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.auction_gu_lee"
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

    buildFeatures {
        dataBinding = true // 데이터 바인딩 활성화
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

    implementation("com.github.bumptech.glide:glide:4.14.2")
    kapt("com.github.bumptech.glide:compiler:4.14.2")
    // Room dependencies 추가
    implementation(platform("com.google.firebase:firebase-bom:32.0.0")) // 최신 버전으로 변경

    // Firebase 관련 라이브러리 추가 (필요한 기능에 따라 추가)
    implementation("com.google.firebase:firebase-database-ktx")  // Firebase Realtime Database
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation(libs.firebase.storage.ktx)
    implementation(libs.firebase.firestore.ktx)
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    // For control over item selection of both touch and mouse driven selection
    implementation("androidx.recyclerview:recyclerview-selection:1.1.0")

    // 코루틴 사용 시 필요한 의존성
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.0")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
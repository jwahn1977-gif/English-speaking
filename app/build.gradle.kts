plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.jwahn.englishspeaking"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.jwahn.englishspeaking"
        minSdk = 26
        targetSdk = 35
        versionCode = 10
        versionName = "0.3.7"
    }

    // 서명 키: 저장소에 넣지 않는다.
    // 로컬: keystore/family.jks (gitignore), CI: GitHub Secrets에서 복원.
    // 키가 없으면 debug 키로 서명(설치는 되지만 빌드 환경마다 서명이 달라짐).
    val ksFile = rootProject.file(System.getenv("KEYSTORE_FILE") ?: "keystore/family.jks")
    val ksPass = System.getenv("KEYSTORE_PASSWORD") ?: (project.findProperty("keystorePassword") as String?) ?: "englishspeaking"
    val ksAlias = System.getenv("KEY_ALIAS") ?: "family"
    signingConfigs {
        if (ksFile.exists()) {
            create("family") {
                storeFile = ksFile
                storePassword = ksPass
                keyAlias = ksAlias
                keyPassword = System.getenv("KEY_PASSWORD") ?: ksPass
            }
        }
    }

    buildTypes {
        val signing = signingConfigs.findByName("family") ?: signingConfigs.getByName("debug")
        release {
            isMinifyEnabled = false
            signingConfig = signing
        }
        debug {
            signingConfig = signing
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
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.maxHeapSize = "3g"
            // Robolectric이 안드로이드 런타임 jar를 받을 저장소 (기본: Maven Central)
            (project.findProperty("robolectricRepo") as String?)?.let { url ->
                it.systemProperty("robolectric.dependency.repo.url", url)
            }
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jsoup:jsoup:1.18.3")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("androidx.browser:browser:1.8.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("androidx.work:work-testing:2.10.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.juani.paywallreader"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.juani.paywallreader"
        minSdk = 26
        targetSdk = 36
        versionCode = 4
        versionName = "1.0-update4"

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
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.05.01")
    val material3ExpressiveVersion = "1.5.0-alpha20"
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Core + Lifecycle
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.activity:activity-compose:1.13.0")

    // Material 3
    implementation("androidx.compose.material3:material3:$material3ExpressiveVersion")
    implementation("androidx.compose.material3:material3-window-size-class:$material3ExpressiveVersion")
    implementation("androidx.compose.material3.adaptive:adaptive-navigation3:1.3.0-alpha09")
    implementation("androidx.compose.material:material-icons-extended")

    // Compose UI + Tooling
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Navigation 3
    implementation("androidx.navigation3:navigation3-runtime:1.1.2")
    implementation("androidx.navigation3:navigation3-ui:1.1.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-navigation3:2.10.0")

    // Room
    val roomVersion = "2.8.4"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // JSON serialization (for default sources)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}

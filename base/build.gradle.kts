plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.parcelize")
    alias(libs.plugins.room)
    id("org.jetbrains.kotlin.android")
    kotlin("kapt")
}

val appConfig = (findProperty("app") as? Map<*, *>) ?: mapOf(
    "compileSdk" to 36,
    "minSdk" to 24
)
val analyticsConfig = (findProperty("analytics") as? Map<*, *>) ?: mapOf(
    "defaultUserChannel" to "default"
)

android {
    namespace = "com.remax.base"
    compileSdk = appConfig["compileSdk"] as Int

    defaultConfig {
        minSdk = appConfig["minSdk"] as Int
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "DEFAULT_USER_CHANNEL", "\"${analyticsConfig["defaultUserChannel"]}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            consumerProguardFiles("proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}
dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    api(libs.utilcodex)
    api(libs.androidx.core.ktx)
    api(libs.androidx.lifecycle.runtime.ktx)
    api(libs.androidx.room.runtime)
    api(libs.androidx.databinding.runtime)
    api(libs.androidx.constraintlayout)
    api(libs.material)
    kapt(libs.androidx.room.compiler)
    api(libs.androidx.room.ktx)
    api(libs.androidx.fragment.ktx)
    api(libs.androidx.work.runtime)
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.coroutines.android)
    
    // Glide图片加载库
    api(libs.glide)
    kapt(libs.glide.compiler)
    
    // Lottie动画库
    api(libs.lottie)

    api(platform(libs.firebase.bom))
    api(libs.firebase.config)
    api(libs.firebase.analytics)
    api(libs.firebase.crashlytics)

    // Gson for JSON parsing
    api(libs.gson)
}
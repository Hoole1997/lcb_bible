plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

val appConfig = (findProperty("app") as? Map<*, *>) ?: mapOf(
    "compileSdk" to 36,
    "minSdk" to 24
)
val analyticsConfig = (findProperty("analytics") as? Map<*, *>) ?: mapOf(
    "adjustAppToken" to "",
    "thinkingDataAppId" to "",
    "thinkingDataServerUrl" to ""
)

android {
    namespace = "com.remax.analytics"
    compileSdk = appConfig["compileSdk"] as Int

    defaultConfig {
        minSdk = appConfig["minSdk"] as Int

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        buildConfigField("String", "ADJUST_APP_TOKEN", "\"${analyticsConfig["adjustAppToken"]}\"")
        buildConfigField("String", "THINKING_DATA_APP_ID", "\"${analyticsConfig["thinkingDataAppId"]}\"")
        buildConfigField("String", "THINKING_DATA_SERVER_URL", "\"${analyticsConfig["thinkingDataServerUrl"]}\"")
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


dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(project(":base"))

    // adjust
    implementation("com.adjust.sdk:adjust-android:5.4.3")
    implementation("com.android.installreferrer:installreferrer:2.2")
    implementation("com.google.android.gms:play-services-ads-identifier:18.0.1")

    // thinkingData
    implementation("cn.thinkingdata.android:ThinkingAnalyticsSDK:3.0.2")
}
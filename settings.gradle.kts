import java.util.Properties

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://repo.itextsupport.com/android") }
        // Pangle SDK
        maven {
            url = uri("https://artifact.bytedance.com/repository/pangle")
            content {
                includeGroup("com.pangle.global")
            }
        }
        // TopOn SDK
        maven {
            url = uri("https://jfrog.anythinktech.com/artifactory/overseas_sdk")
            content {
                includeGroup("com.thinkup.sdk")
                includeGroup("com.smartdigimkttech.sdk")
            }
        }
        // Mintegral SDK
        maven {
            url = uri("https://dl-maven-android.mintegral.com/repository/mbridge_android_sdk_oversea")
            content {
                includeGroup("com.mbridge.msdk.oversea")
            }
        }
        // IronSource SDK
        maven {
            url = uri("https://android-sdk.is.com/")
            content {
                includeGroup("com.ironsource.sdk")
            }
        }
        // Bigo Ads SDK
        maven {
            url = uri("https://api.ad.bigossp.com/repository/maven-public/")
            content {
                includeGroup("com.bigossp")
            }
        }
        // Vungle SDK
        maven {
            url = uri("https://sdk.vungle.com/public/")
            content {
                includeGroup("com.vungle")
            }
        }
        maven { url = uri("https://artifacts.applovin.com/android") }
        maven { url = uri("https://repo.dgtverse.cn/repository/maven-public/") }
    }
}

rootProject.name = "KJVBible"
include(":app")

include(":base")
include(":analytics")
include(":core")
include(":bill")


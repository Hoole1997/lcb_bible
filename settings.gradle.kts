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
        maven {
            url = uri("https://maven.pkg.github.com/toukaRemax/remax_sdk")
            credentials {
                val buildConfigFile = rootDir.resolve("build.config.properties")
                val props = Properties()
                if (buildConfigFile.exists()) {
                    buildConfigFile.inputStream().use { props.load(it) }
                }
                username = props.getProperty("github.user") ?: System.getenv("GITHUB_ACTOR") ?: ""
                password = props.getProperty("github.token") ?: System.getenv("GITHUB_TOKEN") ?: ""
            }
        }
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
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
    }
}

rootProject.name = "KJVBible"
include(":app")

include(":base")
include(":analytics")

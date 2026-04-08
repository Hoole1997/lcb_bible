// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
}

val taskNames = gradle.startParameter.taskNames
val configFile = when {
    taskNames.any {
        it.contains(
            "online",
            ignoreCase = true
        )
    } -> file("app/src/online/buildEnv.gradle")

    else -> file("app/src/internal/buildEnv.gradle")
}

apply {
    from(configFile)
}

import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

android {
    namespace = "dev.roxy.kryptonplayer.feature.player"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()

        val localProperties = Properties().apply {
            val localPropsFile = rootProject.file("local.properties")
            if (localPropsFile.exists()) {
                load(localPropsFile.inputStream())
            }
        }
        val envFile = rootProject.file(".env")
        if (envFile.exists()) {
            envFile.forEachLine { line ->
                val trimmed = line.trim()
                if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && trimmed.contains("=")) {
                    val key = trimmed.substringBefore("=").trim()
                    val value = trimmed.substringAfter("=").trim()
                    if (!localProperties.containsKey(key)) {
                        localProperties.setProperty(key, value)
                    }
                }
            }
        }
        val rawUrl = (System.getenv("ARCH_API_URL") ?: localProperties.getProperty("ARCH_API_URL") ?: "").trim()
        val parsedHost = try {
            if (rawUrl.isNotEmpty()) {
                val stripped = if (rawUrl.contains("://")) rawUrl.substringAfter("://") else rawUrl
                stripped.substringBefore("/").substringBefore(":")
            } else {
                "archplayer.local"
            }
        } catch (_: Exception) {
            "archplayer.local"
        }
        manifestPlaceholders["syncHost"] = parsedHost
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.android.jvm.get().toInt())
        targetCompatibility = JavaVersion.toVersion(libs.versions.android.jvm.get().toInt())
    }

    buildFeatures {
        viewBinding = true
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(libs.versions.android.jvm.get()))
    }
}

dependencies {

    implementation(project(":core:common"))
    implementation(project(":core:ui"))
    implementation(project(":core:data"))
    implementation(project(":core:domain"))
    implementation(project(":core:model"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewModel.ktx)
    implementation(libs.google.android.material)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.icons)
    implementation(libs.androidx.compose.material.iconsExtended)
    implementation(libs.androidx.constraintlayout)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)
    implementation(libs.lottie.compose)

    // Reorderable list for drag-and-drop
    implementation(libs.reorderable)

    // Media3
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.exoplayer.rtsp)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.ui.compose)
    implementation(libs.androidx.media3.session)
    implementation(libs.github.anilbeesetti.nextlib.media3ext)
    implementation(libs.github.anilbeesetti.nextlib.mediainfo)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.guava)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    ksp(libs.kotlin.metadata.jvm)
    kspAndroidTest(libs.hilt.compiler)

    implementation("com.google.zxing:core:3.5.3")

    testImplementation(libs.junit4)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.androidx.test.espresso.core)
}

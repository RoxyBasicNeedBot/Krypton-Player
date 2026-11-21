import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.firebaseCrashlytics)
}

android {
    namespace = "dev.roxy.kryptonplayer"

    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localProperties.load(localPropertiesFile.inputStream())
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
    val rawArchApiUrl = (System.getenv("ARCH_API_URL") ?: localProperties.getProperty("ARCH_API_URL") ?: "https://your-worker.domain.workers.dev/").trim()
    val syncHost = try {
        if (rawArchApiUrl.isNotEmpty()) {
            val stripped = if (rawArchApiUrl.contains("://")) rawArchApiUrl.substringAfter("://") else rawArchApiUrl
            stripped.substringBefore("/").substringBefore(":")
        } else {
            "archplayer.local"
        }
    } catch (_: Exception) {
        "archplayer.local"
    }

    val isBuildingBundle = gradle.startParameter.taskNames.any { it.lowercase().contains("bundle") }
    val isReleaseBuild = gradle.startParameter.taskNames.any { it.lowercase().contains("release") }

    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        applicationId = "dev.roxy.vip.archplayer"
        val envVersionCode = System.getenv("APP_VERSION_CODE")?.toIntOrNull()
        val envVersionName = System.getenv("APP_VERSION_NAME")?.takeIf { it.isNotBlank() }
        versionCode = envVersionCode ?: 57
        versionName = envVersionName ?: "0.16.4"
        buildConfigField("boolean", "ENABLE_SELF_UPDATE", "true")

        manifestPlaceholders["syncHost"] = syncHost

        ndk {
            abiFilters.add("armeabi-v7a")
            abiFilters.add("arm64-v8a")
            if (!isReleaseBuild) {
                abiFilters.add("x86")
                abiFilters.add("x86_64")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.android.jvm.get().toInt())
        targetCompatibility = JavaVersion.toVersion(libs.versions.android.jvm.get().toInt())
    }

    val releaseKeyStorePass: String = (System.getenv("KEYSTORE_PASSWORD") ?: localProperties.getProperty("KEYSTORE_PASSWORD") ?: "").ifBlank { "your_keystore_password_here" }
    val releaseKeyPass: String = (System.getenv("KEY_PASSWORD") ?: localProperties.getProperty("KEY_PASSWORD") ?: "").ifBlank { "your_key_password_here" }

    signingConfigs {
        create("release") {
            storeFile = file("${project.rootDir}/app/release.keystore")
            storePassword = releaseKeyStorePass
            keyAlias = "archplayer"
            keyPassword = releaseKeyPass
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = true
        }
        getByName("debug") {
            val relKey = file("${project.rootDir}/app/release.keystore")
            val hasReleaseCreds = releaseKeyStorePass != "your_keystore_password_here" &&
                releaseKeyPass != "your_key_password_here"
            if (relKey.exists() && hasReleaseCreds) {
                storeFile = relKey
                storePassword = releaseKeyStorePass
                keyAlias = "archplayer"
                keyPassword = releaseKeyPass
            } else {
                storeFile = file("${project.rootDir}/app/debug.keystore")
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = true
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            ndk {
                debugSymbolLevel = "NONE"
            }
        }

        getByName("debug") {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
            applicationIdSuffix = ".debug"
        }

        create("release-with-debug-signing") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            applicationIdSuffix = ".release"
            matchingFallbacks.add("release")
        }
    }

    splits {
        abi {
            //noinspection WrongGradleMethod
            isEnable = !isBuildingBundle && isReleaseBuild
            reset()
            include("armeabi-v7a", "arm64-v8a")
            isUniversalApk = true
        }
    }

    bundle {
        abi {
            enableSplit = true
        }
        density {
            enableSplit = true
        }
        language {
            enableSplit = true
        }
    }

    packaging {
        jniLibs.useLegacyPackaging = true
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
            excludes.add("META-INF/DEPENDENCIES")
            excludes.add("META-INF/LICENSE")
            excludes.add("META-INF/LICENSE.txt")
            excludes.add("META-INF/license.txt")
            excludes.add("META-INF/LICENSE.md")
            excludes.add("META-INF/NOTICE")
            excludes.add("META-INF/NOTICE.txt")
            excludes.add("META-INF/notice.txt")
            excludes.add("META-INF/NOTICE.md")
            excludes.add("META-INF/ASL2.0")
            excludes.add("META-INF/*.kotlin_module")
            excludes.add("META-INF/CONTRIBUTORS.md")
        }
    }

    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(libs.versions.android.jvm.get()))
    }
}

dependencies {

    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:media"))
    implementation(project(":core:model"))
    implementation(project(":core:ui"))
    implementation(project(":feature:videopicker"))
    implementation(project(":feature:player"))
    implementation(project(":feature:settings"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.iconsExtended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtimeCompose)

    implementation(libs.google.android.material)
    implementation(libs.play.services.auth)
    implementation(libs.play.services.wearable)
    implementation(libs.androidx.core.splashscreen)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.config)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.inappmessaging.display)
    implementation(libs.firebase.perf)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.appcheck.playintegrity)
    implementation(libs.firebase.appcheck.debug)

    // Credential Manager (Modern Google Sign-In)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Media3
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    ksp(libs.kotlin.metadata.jvm)
    kspAndroidTest(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.accompanist.permissions)

    implementation(libs.github.anilbeesetti.nextlib.mediainfo)

    testImplementation(libs.junit4)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.testManifest)
}

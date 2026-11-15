import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "dev.anilbeesetti.nextplayer.core.data"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()

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

        fun getSecret(name: String, fallback: String = ""): String {
            val envVal = System.getenv(name) ?: ""
            val propVal = localProperties.getProperty(name) ?: ""
            val raw = when {
                envVal.isNotBlank() -> envVal
                propVal.isNotBlank() -> propVal
                else -> fallback
            }
            return raw.replace("\r", "").replace("\n", "").trim()
        }

        val rawArchApiUrl = getSecret("ARCH_API_URL", "https://your-worker.domain.workers.dev/")
        val archApiUrl = if (rawArchApiUrl.contains("streamlit.app") || rawArchApiUrl.isBlank() || rawArchApiUrl.contains("onrender.com")) {
            "https://your-worker.domain.workers.dev/"
        } else {
            rawArchApiUrl
        }
        buildConfigField("String", "ARCH_API_URL", "\"$archApiUrl\"")

        val archClientKey = getSecret("ARCH_CLIENT_KEY", "")
        buildConfigField("String", "ARCH_CLIENT_KEY", "\"$archClientKey\"")

        val apiKey = getSecret("TMDB_API_KEY", "")
        buildConfigField("String", "TMDB_API_KEY", "\"$apiKey\"")

        buildConfigField("boolean", "ENABLE_SELF_UPDATE", "true")

        val fanartApiKey = getSecret("FANART_API_KEY", "")
        buildConfigField("String", "FANART_API_KEY", "\"$fanartApiKey\"")

        val openSubtitlesApiKey = getSecret("OPENSUBTITLES_API_KEY", "")
        buildConfigField("String", "OPENSUBTITLES_API_KEY", "\"$openSubtitlesApiKey\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.android.jvm.get().toInt())
        targetCompatibility = JavaVersion.toVersion(libs.versions.android.jvm.get().toInt())
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(libs.versions.android.jvm.get()))
    }
}

dependencies {

    implementation(project(":core:database"))
    implementation(project(":core:media"))
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:datastore"))
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)

    // Media3
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.datasource)

    // Hilt
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.gson)
    implementation(libs.mlkit.translate)
    implementation(libs.mlkit.language.id)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    ksp(libs.kotlin.metadata.jvm)
    kspAndroidTest(libs.hilt.compiler)

    testImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.androidx.test.espresso.core)
}

import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

fun gradleOrEnvironment(name: String): String? =
    providers.gradleProperty(name)
        .orElse(providers.environmentVariable(name))
        .orNull
        ?.takeIf { it.isNotBlank() }

fun releaseSigningValue(name: String): String? =
    gradleOrEnvironment(name)
        ?: keystoreProperties.getProperty(name)?.takeIf { it.isNotBlank() }

val youtubeApiKey = gradleOrEnvironment("YOUTUBE_API_KEY")
    ?: localProperties.getProperty("YOUTUBE_API_KEY", "")

val playUploadStoreFile = releaseSigningValue("PLAY_UPLOAD_STORE_FILE")
val playUploadStorePassword = releaseSigningValue("PLAY_UPLOAD_STORE_PASSWORD")
val playUploadKeyAlias = releaseSigningValue("PLAY_UPLOAD_KEY_ALIAS")
val playUploadKeyPassword = releaseSigningValue("PLAY_UPLOAD_KEY_PASSWORD")

val playUploadSigningValues = listOf(
    playUploadStoreFile,
    playUploadStorePassword,
    playUploadKeyAlias,
    playUploadKeyPassword,
)
val hasAnyPlayUploadSigningConfig = playUploadSigningValues.any { !it.isNullOrBlank() }
val hasCompletePlayUploadSigningConfig = playUploadSigningValues.all { !it.isNullOrBlank() }

if (hasAnyPlayUploadSigningConfig && !hasCompletePlayUploadSigningConfig) {
    throw GradleException(
        "Configurazione Play upload signing incompleta. " +
            "Imposta PLAY_UPLOAD_STORE_FILE, PLAY_UPLOAD_STORE_PASSWORD, " +
            "PLAY_UPLOAD_KEY_ALIAS e PLAY_UPLOAD_KEY_PASSWORD.",
    )
}

android {
    namespace = "io.github.falker47.socialviewer"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.falker47.socialviewer"
        minSdk = 26
        targetSdk = 37
        versionCode = 2
        versionName = "0.1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField(
            "String",
            "YOUTUBE_API_KEY",
            "\"" + youtubeApiKey
                .replace("\\", "\\\\")
                .replace("\"", "\\\"") + "\"",
        )
    }

    val playUploadSigning = if (hasCompletePlayUploadSigningConfig) {
        signingConfigs.create("playUpload") {
            storeFile = rootProject.file(playUploadStoreFile!!)
            storePassword = playUploadStorePassword
            keyAlias = playUploadKeyAlias
            keyPassword = playUploadKeyPassword
        }
    } else {
        null
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            playUploadSigning?.let { signingConfig = it }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

val verifyPlayPublishConfig = tasks.register("verifyPlayPublishConfig") {
    group = "verification"
    description = "Fail fast unless the Google Play publish configuration is complete."

    doLast {
        if (youtubeApiKey.isBlank()) {
            throw GradleException(
                "YOUTUBE_API_KEY mancante: una build pubblicabile non deve disabilitare YouTube.",
            )
        }
        if (!hasCompletePlayUploadSigningConfig) {
            throw GradleException(
                "Play upload signing non configurato. " +
                    "Vedi RELEASE.md e keystore.properties.example.",
            )
        }

        val configuredStoreFile = rootProject.file(playUploadStoreFile!!)
        if (!configuredStoreFile.isFile) {
            throw GradleException(
                "Keystore di upload non trovato: ${configuredStoreFile.absolutePath}",
            )
        }
    }
}

tasks.register("playReleaseBundle") {
    group = "build"
    description = "Validate publish configuration, then build the signed Google Play AAB."
    dependsOn(verifyPlayPublishConfig, "bundleRelease")
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")

    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}

import java.net.URI

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Only developer-owned base URLs enter the APK; LLM credentials stay on Python.
fun configuredChatUrl(propertyName: String, allowHttp: Boolean): String {
    val value = providers.gradleProperty(propertyName).orElse("").get().trim()
    if (value.isNotEmpty()) {
        val uri = URI(value)
        require(!uri.host.isNullOrBlank() && uri.rawUserInfo == null && uri.rawQuery == null &&
            uri.rawFragment == null && (uri.port == -1 || uri.port in 1..65535) &&
            (uri.scheme == "https" || (allowHttp && uri.scheme == "http"))) {
            "$propertyName must be a base URL without credentials; release requires HTTPS"
        }
    }
    return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}

android {
    namespace = "com.silverlingo.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.silverlingo.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "1.1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("String", "CHAT_BASE_URL", configuredChatUrl("CHAT_DEV_BASE_URL", true))
        }
        release {
            buildConfigField("String", "CHAT_BASE_URL", configuredChatUrl("CHAT_PROD_BASE_URL", false))
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
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    sourceSets["main"].assets.srcDir(layout.buildDirectory.dir("generated/learningAssets"))
}

val syncLearningAssets by tasks.registering(Sync::class) {
    from(rootProject.file("../silver-ringo-c/web"))
    into(layout.buildDirectory.dir("generated/learningAssets/kiosk"))
}
tasks.named("preBuild").configure { dependsOn(syncLearningAssets) }

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.security.crypto)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.webkit:webkit:1.12.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    debugImplementation(libs.androidx.ui.tooling)
}

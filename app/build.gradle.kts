import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun signing(env: String, key: String): String? = System.getenv(env) ?: keystoreProps.getProperty(key)

android {
    namespace = "com.minimaa"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.minimaa"
        minSdk = 26
        targetSdk = 34
        versionCode = 16
        versionName = "16"
    }
    signingConfigs {
        create("release") {
            val path = signing("KEYSTORE_PATH", "storeFile")
            if (path != null) {
                storeFile = rootProject.file(path)
                storePassword = signing("KEYSTORE_PASSWORD", "storePassword")
                keyAlias = signing("KEY_ALIAS", "keyAlias")
                keyPassword = signing("KEY_PASSWORD", "keyPassword")
            }
        }
    }
    buildTypes {
        release {
            if (signing("KEYSTORE_PATH", "storeFile") != null) signingConfig = signingConfigs.getByName("release")
        }
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.5")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.5")
    implementation("com.google.dagger:hilt-android:2.52")
    ksp("com.google.dagger:hilt-android-compiler:2.52")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
}

import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Dropbox App Key কোডে না রেখে local.properties থেকে পড়া হয় (ফাইলটি GitHub-এ যায় না)
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val dropboxAppKey: String = localProps.getProperty("DROPBOX_APP_KEY", "")

android {
    namespace = "com.example.filebrowser"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.filebrowser"
        minSdk = 24   // Android 7.0 (Nougat)
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "DROPBOX_APP_KEY", "\"$dropboxAppKey\"")
        manifestPlaceholders["dropboxScheme"] = "db-$dropboxAppKey"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // Android 7-এ java.time ইত্যাদি নেই; লাইব্রেরি থেকে সেগুলো ব্যবহার হলে এটি সামলায়
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE*",
                "META-INF/NOTICE*",
                "META-INF/INDEX.LIST",
                "META-INF/versions/9/module-info.class"
            )
        }
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.2")

    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")

    // FTP সার্ভার
    implementation("org.apache.ftpserver:ftpserver-core:1.2.0")

    // Google Drive
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("com.google.api-client:google-api-client-android:2.7.0")
    implementation("com.google.apis:google-api-services-drive:v3-rev20240521-2.0.0")
    implementation("com.google.http-client:google-http-client-gson:1.45.0")

    // Dropbox (Android-এর লগইন helper সহ; এটি dropbox-core-sdk-ও টানে)
    implementation("com.dropbox.core:dropbox-android-sdk:7.0.0")
}

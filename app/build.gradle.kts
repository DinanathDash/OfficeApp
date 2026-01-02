plugins {
    alias(libs.plugins.android.application)
}

import java.util.Properties
import java.util.Base64
import java.io.FileInputStream
import java.io.File

android {
    namespace = "com.dinanathdash.officeapp"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        val versionPropsFile = project.rootProject.file("version.properties")
        val versionProps = Properties()
        if (versionPropsFile.exists()) {
            versionProps.load(FileInputStream(versionPropsFile))
        }

        applicationId = "com.dinanathdash.officeapp"
        minSdk = 31
        targetSdk = 36
        versionCode = (versionProps["VERSION_CODE"] as String? ?: "1").toInt()
        versionName = "${versionProps["VERSION_MAJOR"]}.${versionProps["VERSION_MINOR"]}.${versionProps["VERSION_PATCH"]}"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val keystoreFile = project.rootProject.file("release.keystore")
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD") ?: project.findProperty("RELEASE_KEYSTORE_PASSWORD") as String?
                keyAlias = System.getenv("RELEASE_KEY_ALIAS") ?: project.findProperty("RELEASE_KEY_ALIAS") as String?
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD") ?: project.findProperty("RELEASE_KEY_PASSWORD") as String?
            } else if (System.getenv("RELEASE_KEYSTORE_BASE64") != null) {
                // For CI/CD: Create keystore from base64 env var
                val decodedKeystore = Base64.getDecoder().decode(System.getenv("RELEASE_KEYSTORE_BASE64"))
                val tempKeystore = File.createTempFile("release", ".keystore")
                tempKeystore.writeBytes(decodedKeystore)
                storeFile = tempKeystore
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }

    packaging {
        resources {
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/license.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
            excludes += "META-INF/notice.txt"
            excludes += "META-INF/ASL2.0"
        }
    }
    
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.gson)
    implementation(libs.poi)
    implementation(libs.poi.ooxml) {
        exclude(group = "org.apache.poi", module = "poi-ooxml-lite")
    }
    implementation(libs.poi.ooxml.full)
    implementation(libs.poi.scratchpad)
    implementation(libs.pdfbox)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.airbnb.android:lottie:6.4.0")
}
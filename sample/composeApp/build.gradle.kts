import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.android.application)
    id("app.cash.sqldelight") version "2.1.0"

}

kotlin {
    jvmToolchain(17)

    jvm()
    androidTarget()
//    wasmJs {
//        browser()
//        binaries.executable()
//    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(project(":core"))
            implementation(libs.kermit)
            implementation(libs.kotlinx.serialization.json)
            implementation("io.github.kdroidfilter:gplayscrapper-core:0.3.1")

            implementation(libs.ktor.client.core)
            implementation(libs.platform.tools.release.fetcher)
            implementation("io.coil-kt.coil3:coil-compose:3.1.0")
            implementation("io.coil-kt.coil3:coil-network-okhttp:3.1.0")
        }


        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation("app.cash.sqldelight:sqlite-driver:2.1.0")

        }

        androidMain.dependencies {
            implementation(libs.androidx.activityCompose)
            implementation("app.cash.sqldelight:android-driver:2.1.0")
            implementation(libs.kotlinx.coroutines.android)
            implementation("io.github.kdroidfilter:netfreetools.certificates:1.0.1")

        }

        wasmJsMain.dependencies {
            implementation(libs.sqldelight.web.driver)
            implementation(npm("@cashapp/sqldelight-sqljs-worker", "2.1.0"))
            implementation(npm("sql.js", libs.versions.sqlJs.get()))
            implementation(devNpm("copy-webpack-plugin", libs.versions.webPackPlugin.get()))
        }


    }
}

android {
    namespace = "io.github.kdroidfilter.database.sample"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.kdroidfilter.database.sample"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.get()
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            modules("java.sql")
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "sample"
            packageVersion = "1.0.0"
        }
    }
}

sqldelight {
    databases {
        create("Database") {
            packageName.set("io.github.kdroidfilter.database.sample")
        }
    }
}

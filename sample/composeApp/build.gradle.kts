import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.android.application)
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
            implementation(project(":core"))
            implementation(project(":dao"))
            implementation(project(":localization"))
            implementation(project(":downloader"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(libs.kermit)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.android.appstore.kit.gplay.scrapper)
            implementation(libs.ktor.client.core)
            implementation(libs.platform.tools.release.fetcher)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.okhttp)
        }


        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.sqldelight.sqlite.driver)

        }

        androidMain.dependencies {
            implementation(libs.androidx.activityCompose)
            implementation(libs.sqldelight.android.driver)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.netfreetools.certificates)

        }

//        wasmJsMain.dependencies {
//            implementation(libs.sqldelight.web.driver)
//            implementation(npm("@cashapp/sqldelight-sqljs-worker", "2.1.0"))
//            implementation(npm("sql.js", libs.versions.sqlJs.get()))
//            implementation(devNpm("copy-webpack-plugin", libs.versions.webPackPlugin.get()))
//        }


    }
}

android {
    namespace = "io.github.kdroidfilter.database.sample"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.kdroidfilter.database.sample"
        minSdk = 26
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


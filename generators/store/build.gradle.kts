plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.kotlinx.serialization)
}


kotlin {
    jvmToolchain(17)
    jvm()

    sourceSets {

        jvmTest.dependencies {
            implementation(kotlin("test"))
        }

        jvmMain.dependencies {
            implementation(project(":core"))
            implementation(project(":dao"))
            implementation(project(":downloader"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kermit)
            implementation(libs.storekit.gplaycore)
            implementation(libs.platform.tools.release.fetcher)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.sqlite.jdbc)
            implementation(libs.maven.slf4j.provider)
            implementation(libs.sqldelight.sqlite.driver)
        }


    }

    //https://kotlinlang.org/docs/native-objc-interop.html#export-of-kdoc-comments-to-generated-objective-c-headers
    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        compilations["main"].compileTaskProvider.configure {
            compilerOptions {
                freeCompilerArgs.add("-Xexport-kdoc")
            }
        }
    }

}


tasks.register<JavaExec>("runStoreExtractor") {
    group = "extraction"
    description = "Runs the SQLite store extractor"

    dependsOn(kotlin.jvm().compilations["main"].compileTaskProvider)
    classpath = files(
        kotlin.jvm().compilations["main"].output.allOutputs,
        kotlin.jvm().compilations["main"].runtimeDependencyFiles
    )
    mainClass.set("SqliteStoreExtractorKt")

}

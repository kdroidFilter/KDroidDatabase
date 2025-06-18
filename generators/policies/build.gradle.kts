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
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kermit)
            implementation(libs.platform.tools.release.fetcher)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.sqlite.jdbc)
            implementation(libs.maven.slf4j.provider)
            implementation(libs.storekit.aptoide.api)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.serialization)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.cio)

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

tasks.register<JavaExec>("runPolicyExtractor") {
    group = "extraction"
    description = "Runs the SQLite policy extractor"

    dependsOn(kotlin.jvm().compilations["main"].compileTaskProvider)
    classpath = files(
        kotlin.jvm().compilations["main"].output.allOutputs,
        kotlin.jvm().compilations["main"].runtimeDependencyFiles
    )
    mainClass.set("SqlitePolicyExtractorKt")

}

tasks.register<JavaExec>("validateJson") {
    group = "validation"
    description = "Validates all JSON policy files for correctness"

    dependsOn(kotlin.jvm().compilations["main"].compileTaskProvider)
    classpath = files(
        kotlin.jvm().compilations["main"].output.allOutputs,
        kotlin.jvm().compilations["main"].runtimeDependencyFiles
    )
    mainClass.set("JsonValidatorKt")
}

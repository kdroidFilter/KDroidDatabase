import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.sql.delight)
    alias(libs.plugins.vannitktech.maven.publish)
}

val ref = System.getenv("GITHUB_REF") ?: ""
version = if (ref.startsWith("refs/tags/")) {
    val tag = ref.removePrefix("refs/tags/")
    if (tag.startsWith("v")) tag.substring(1) else tag
} else "dev"


kotlin {
    jvmToolchain(17)
    androidTarget { publishLibraryVariants("release") }
    jvm()


    sourceSets {

        androidMain.dependencies {
            implementation(libs.sqldelight.android.driver)
            implementation(libs.kotlinx.coroutines.android)
        }

        jvmMain.dependencies {
            implementation(libs.sqlite.jdbc)
            implementation(libs.maven.slf4j.provider)
            implementation(libs.kotlinx.coroutines.swing)
        }

        jvmTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)

        }

        commonMain.dependencies {
            api(project(":core"))
            implementation(project(":localization"))
            api(libs.gplay.scrapper.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kermit)
            implementation(libs.platform.tools.release.fetcher)
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

android {
    namespace = "io.github.kdroidfilter.database.dao"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
    }
}

sqldelight {
    databases {
        create("Database") {
            packageName.set("io.github.kdroidfilter.database.store")
        }
    }
}

mavenPublishing {
    coordinates(
        groupId = "io.github.kdroidfilter.database.dao",
        artifactId = "core",
        version = version.toString()
    )

    pom {
        name.set("KDroid Database")
        description.set("DAO of the Kdroid Database")
        inceptionYear.set("2025")
        url.set("https://github.com/kdroidFilter/KDroidDatabase")

        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
            }
        }

        developers {
            developer {
                id.set("kdroidfilter")
                name.set("Elie Gambache")
                email.set("elyahou.hadass@gmail.com")
            }
        }

        scm {
            connection.set("scm:git:git://github.com/kdroidFilter/KDroidDatabase.git")
            developerConnection.set("scm:git:ssh://git@github.com/kdroidFilter/KDroidDatabase.git")
            url.set("https://github.com/kdroidFilter/KDroidDatabase")
        }
    }

    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    signAllPublications()
}

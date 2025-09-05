plugins {
    alias(libs.plugins.multiplatform).apply(false)
    alias(libs.plugins.android.library).apply(false)
    alias(libs.plugins.kotlinx.serialization).apply(false)
    alias(libs.plugins.android.application).apply(false)
    alias(libs.plugins.sql.delight).apply(false)
}

// Task to run both generators
tasks.register("runGenerators") {
    group = "extraction"
    description = "Runs both the SQLite policy extractor and the SQLite store extractor"

    dependsOn(":generators:policies:runPolicyExtractor", ":generators:store:runStoreExtractor")
}

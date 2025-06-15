package io.github.kdroidfilter.database.downloader

import co.touchlab.kermit.Logger
import io.github.kdroidfilter.platformtools.releasefetcher.downloader.Downloader
import io.github.kdroidfilter.platformtools.releasefetcher.github.GitHubReleaseFetcher
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileOutputStream
import java.net.URL

/**
 * Class responsible for downloading KDroid database files from GitHub releases.
 */
class DatabaseDownloader {
    private val logger = Logger.withTag("DatabaseDownloader")
    private val downloader = Downloader()
    private val owner = "kdroidFilter"
    private val repo = "KDroidDatabase"

    /**
     * Downloads the latest store databases for all three languages (en, fr, he) from GitHub releases.
     * @param outputDir The directory where the database files will be saved
     * @return Map of language codes to download success status
     */
    fun downloadLatestStoreDatabases(outputDir: String): Map<String, Boolean> = runBlocking {
        val languages = listOf("en", "fr", "he")
        val results = mutableMapOf<String, Boolean>()

        try {
            logger.i { "🔄 Attempting to download the latest store databases for all languages..." }
            val fetcher = GitHubReleaseFetcher(owner = owner, repo = repo)
            val latestRelease = fetcher.getLatestRelease()

            if (latestRelease != null && latestRelease.assets.isNotEmpty()) {
                languages.forEach { lang ->
                    try {
                        // Look for language-specific database file
                        val assetName = "store-database-$lang.db"
                        val asset = latestRelease.assets.find { it.name == assetName }

                        if (asset != null) {
                            val outputDirFile = File(outputDir)
                            if (!outputDirFile.exists()) {
                                outputDirFile.mkdirs()
                            }

                            val outputDbFile = File(outputDirFile, assetName)
                            val downloadUrl = asset.browser_download_url

                            logger.i { "📥 Downloading $lang store database from: $downloadUrl" }

                            // Download the file
                            downloadFile(downloadUrl, outputDbFile.absolutePath)

                            // Verify the file was downloaded successfully
                            if (outputDbFile.exists() && outputDbFile.length() > 0) {
                                logger.i { "✅ Store database $lang downloaded successfully to ${outputDbFile.absolutePath}" }
                                results[lang] = true
                            } else {
                                logger.w { "⚠️ Downloaded file for $lang is empty or does not exist" }
                                results[lang] = false
                            }
                        } else {
                            logger.w { "⚠️ No store database asset found for language: $lang" }
                            results[lang] = false
                        }
                    } catch (e: Exception) {
                        logger.e(e) { "❌ Failed to download store database for $lang: ${e.message}" }
                        results[lang] = false
                    }
                }
            } else {
                logger.w { "⚠️ No store database assets found in the latest release" }
                languages.forEach { results[it] = false }
            }
        } catch (e: Exception) {
            logger.e(e) { "❌ Failed to download store databases: ${e.message}" }
            languages.forEach { results[it] = false }
        }

        return@runBlocking results
    }

    /**
     * Downloads the latest policies database from GitHub releases.
     * @param outputDir The directory where the database file will be saved
     * @return Boolean indicating whether the download was successful
     */
    fun downloadLatestPoliciesDatabase(outputDir: String): Boolean = runBlocking {
        try {
            logger.i { "🔄 Attempting to download the latest policies database..." }
            val fetcher = GitHubReleaseFetcher(owner = owner, repo = repo)
            val latestRelease = fetcher.getLatestRelease()

            if (latestRelease != null && latestRelease.assets.isNotEmpty()) {
                // Look for policies database file
                val assetName = "policies-database.db"
                val asset = latestRelease.assets.find { it.name == assetName }

                if (asset != null) {
                    val outputDirFile = File(outputDir)
                    if (!outputDirFile.exists()) {
                        outputDirFile.mkdirs()
                    }

                    val outputDbFile = File(outputDirFile, assetName)
                    val downloadUrl = asset.browser_download_url

                    logger.i { "📥 Downloading policies database from: $downloadUrl" }

                    // Download the file
                    downloadFile(downloadUrl, outputDbFile.absolutePath)

                    // Verify the file was downloaded successfully
                    if (outputDbFile.exists() && outputDbFile.length() > 0) {
                        logger.i { "✅ Policies database downloaded successfully to ${outputDbFile.absolutePath}" }
                        return@runBlocking true
                    } else {
                        logger.w { "⚠️ Downloaded policies database file is empty or does not exist" }
                        return@runBlocking false
                    }
                } else {
                    logger.w { "⚠️ No policies database asset found in the latest release" }
                    return@runBlocking false
                }
            } else {
                logger.w { "⚠️ No assets found in the latest release" }
                return@runBlocking false
            }
        } catch (e: Exception) {
            logger.e(e) { "❌ Failed to download policies database: ${e.message}" }
            return@runBlocking false
        }
    }

    /**
     * Downloads a file from a URL to a local file path.
     * @param url The URL to download from
     * @param outputPath The local file path to save the downloaded file
     */
    private fun downloadFile(url: String, outputPath: String) {
        val outputFile = File(outputPath)

        // Create parent directories if they don't exist
        outputFile.parentFile?.mkdirs()

        // Download the file
        URL(url).openStream().use { input ->
            FileOutputStream(outputFile).use { output ->
                input.copyTo(output)
            }
        }
    }
}

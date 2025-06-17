package io.github.kdroidfilter.database.downloader

import co.touchlab.kermit.Logger
import io.github.kdroidfilter.platformtools.releasefetcher.github.GitHubReleaseFetcher
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import kotlin.math.min
import kotlin.math.pow

/**
 * Class responsible for downloading KDroid database files from GitHub releases.
 */
class DatabaseDownloader {
    private val logger = Logger.withTag("DatabaseDownloader")

    /**
     * Downloads the latest store database for a specific language from GitHub releases.
     * @param outputDir The directory where the database file will be saved
     * @param language The language code (en, fr, he) for which to download the database
     * @return Boolean indicating whether the download was successful
     */
    suspend fun downloadLatestStoreDatabaseForLanguage(outputDir: String, language: String): Boolean {
        try {
            logger.i { "🔄 Attempting to download the latest store database for language: $language..." }
            val fetcher = GitHubReleaseFetcher(owner = GitHubConstants.OWNER, repo = GitHubConstants.REPO)
            val latestRelease = fetcher.getLatestRelease()

            if (latestRelease != null && latestRelease.assets.isNotEmpty()) {
                // Look for language-specific database file
                val assetName = "store-database-$language.db"
                val asset = latestRelease.assets.find { it.name == assetName }

                if (asset != null) {
                    val outputDirFile = File(outputDir)
                    if (!outputDirFile.exists()) {
                        outputDirFile.mkdirs()
                    }

                    val outputDbFile = File(outputDirFile, assetName)
                    val downloadUrl = asset.browser_download_url

                    logger.i { "📥 Downloading $language store database from: $downloadUrl" }

                    // Download the file
                    val downloadSuccess = downloadFile(downloadUrl, outputDbFile.absolutePath)

                    // Verify the file was downloaded successfully
                    if (downloadSuccess && outputDbFile.exists() && outputDbFile.length() > 0) {
                        logger.i {
                            "✅ Store database $language downloaded successfully to ${outputDbFile.absolutePath}"
                        }
                        return true
                    } else {
                        logger.w { "⚠️ Downloaded file for $language is empty or does not exist" }
                        return false
                    }
                } else {
                    logger.w { "⚠️ No store database asset found for language: $language" }
                    return false
                }
            } else {
                logger.w { "⚠️ No store database assets found in the latest release" }
                return false
            }
        } catch (e: Exception) {
            logger.e(e) { "❌ Failed to download store database for $language: ${e.message}" }
            return false
        }
    }

    /**
     * Downloads the latest store databases for all three languages (en, fr, he) from GitHub releases.
     * @param outputDir The directory where the database files will be saved
     * @return Map of language codes to download success status
     */
    suspend fun downloadLatestStoreDatabases(outputDir: String): Map<String, Boolean> {
        val languages = listOf("en", "fr", "he")
        val results = mutableMapOf<String, Boolean>()

        try {
            logger.i { "🔄 Attempting to download the latest store databases for all languages..." }
            val fetcher = GitHubReleaseFetcher(owner = GitHubConstants.OWNER, repo = GitHubConstants.REPO)
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
                            val downloadSuccess = downloadFile(downloadUrl, outputDbFile.absolutePath)

                            // Verify the file was downloaded successfully
                            if (downloadSuccess && outputDbFile.exists() && outputDbFile.length() > 0) {
                                logger.i {
                                    "✅ Store database $lang downloaded successfully to ${outputDbFile.absolutePath}"
                                }
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

        return results
    }

    /**
     * Downloads the latest policies database from GitHub releases.
     * @param outputDir The directory where the database file will be saved
     * @return Boolean indicating whether the download was successful
     */
    suspend fun downloadLatestPoliciesDatabase(outputDir: String): Boolean {
        try {
            logger.i { "🔄 Attempting to download the latest policies database..." }
            val fetcher = GitHubReleaseFetcher(owner = GitHubConstants.OWNER, repo = GitHubConstants.REPO)
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
                    val downloadSuccess = downloadFile(downloadUrl, outputDbFile.absolutePath)

                    // Verify the file was downloaded successfully
                    if (downloadSuccess && outputDbFile.exists() && outputDbFile.length() > 0) {
                        logger.i { "✅ Policies database downloaded successfully to ${outputDbFile.absolutePath}" }
                        return true
                    } else {
                        logger.w { "⚠️ Downloaded policies database file is empty or does not exist" }
                        return false
                    }
                } else {
                    logger.w { "⚠️ No policies database asset found in the latest release" }
                    return false
                }
            } else {
                logger.w { "⚠️ No assets found in the latest release" }
                return false
            }
        } catch (e: Exception) {
            logger.e(e) { "❌ Failed to download policies database: ${e.message}" }
            return false
        }
    }

    /**
     * Downloads a file from a URL to a local file path with HTTP status verification and retry mechanism.
     * @param url The URL to download from
     * @param outputPath The local file path to save the downloaded file
     * @param maxRetries Maximum number of retry attempts (default: 3)
     * @param initialBackoffMs Initial backoff time in milliseconds (default: 1000)
     * @return Boolean indicating whether the download was successful
     */
    private fun downloadFile(
        url: String, 
        outputPath: String, 
        maxRetries: Int = 3, 
        initialBackoffMs: Long = 1000
    ): Boolean {
        val outputFile = File(outputPath)

        // Create parent directories if they don't exist
        outputFile.parentFile?.mkdirs()

        var attempt = 0
        var lastException: Exception? = null

        while (attempt <= maxRetries) {
            try {
                if (attempt > 0) {
                    // Calculate backoff time with exponential increase and some randomization
                    val backoffMs = (initialBackoffMs * 2.0.pow(attempt - 1)).toLong()
                    val jitteredBackoff = backoffMs + (backoffMs * Math.random() * 0.1).toLong()
                    val cappedBackoff = min(jitteredBackoff, 30_000) // Cap at 30 seconds

                    logger.d { "⏱️ Retry attempt $attempt after ${cappedBackoff}ms delay" }
                    Thread.sleep(cappedBackoff)
                }

                // Use HttpURLConnection for better HTTP handling
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 30000 // 30 seconds
                connection.readTimeout = 30000 // 30 seconds

                try {
                    val responseCode = connection.responseCode

                    if (responseCode in 200..299) {
                        // Success - download the file
                        connection.inputStream.use { input ->
                            FileOutputStream(outputFile).use { output ->
                                input.copyTo(output)
                            }
                        }

                        // If we get here, download was successful
                        if (attempt > 0) {
                            logger.i { "✅ Download succeeded after $attempt ${if (attempt == 1) "retry" else "retries"}" }
                        }
                        return true
                    } else {
                        // HTTP error
                        val errorMessage = "HTTP error: $responseCode ${connection.responseMessage}"
                        logger.w { "⚠️ $errorMessage (attempt ${attempt + 1}/$maxRetries)" }

                        // For certain status codes, retrying won't help
                        if (responseCode in listOf(400, 401, 403, 404)) {
                            logger.e { "❌ $errorMessage - permanent error, not retrying" }
                            return false
                        }

                        lastException = IOException(errorMessage)
                    }
                } finally {
                    connection.disconnect()
                }
            } catch (e: Exception) {
                lastException = e
                logger.w(e) { "⚠️ Download attempt ${attempt + 1}/$maxRetries failed: ${e.message}" }

                // Don't retry for certain exceptions where retry won't help
                if (e is SecurityException || e is IllegalArgumentException) {
                    logger.e(e) { "❌ Fatal error, not retrying: ${e.message}" }
                    return false
                }
            }

            attempt++
        }

        // If we get here, all retries failed
        val errorMessage = "Failed to download file after $maxRetries retries"
        logger.e(lastException) { "❌ $errorMessage" }
        return false
    }

}

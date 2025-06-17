package io.github.kdroidfilter.database.downloader

import kotlinx.coroutines.test.runTest
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandler
import java.net.URLStreamHandlerFactory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for the DatabaseDownloader class.
 * 
 * This test class uses a custom URL handler to intercept HTTP requests and
 * return predefined responses, allowing us to test the DatabaseDownloader
 * without making actual network requests.
 */
class DatabaseDownloaderTest {

    companion object {
        // Static reference to the mock URL handler
        val mockUrlHandler = MockURLStreamHandler()

        init {
            // Set up the mock URL handler once for all tests
            try {
                URL.setURLStreamHandlerFactory(mockUrlHandler)
            } catch (e: Error) {
                // If the factory is already set, we can ignore this error
                // This can happen when running multiple tests
                println("Note: URL handler factory already set: ${e.message}")
            }
        }
    }

    // Create a temporary directory for test files
    private lateinit var testDir: File

    @BeforeTest
    fun setup() {
        testDir = createTempDir("database-downloader-test")

        // Clear any previous mock responses
        mockUrlHandler.clearResponses()
    }

    @AfterTest
    fun cleanup() {
        testDir.deleteRecursively()
    }

    /**
     * Test downloading a store database for a specific language when the download is successful.
     */
    @Test
    fun `downloadLatestStoreDatabaseForLanguage should return true when download is successful`() = runTest {
        // Arrange
        val language = "en"
        val assetName = "store-database-$language.db"

        // Set up mock responses for the actual URL being requested
        mockUrlHandler.addResponse(
            "https://github.com/kdroidFilter/KDroidDatabase/releases/download/202506170506/$assetName",
            200,
            "test database content"
        )

        // Act
        val downloader = DatabaseDownloader()
        val result = downloader.downloadLatestStoreDatabaseForLanguage(testDir.absolutePath, language)

        // Assert
        assertTrue(result, "Download should be successful")
        val downloadedFile = File(testDir, assetName)
        assertTrue(downloadedFile.exists(), "Downloaded file should exist")
        assertTrue(downloadedFile.length() > 0, "Downloaded file should not be empty")
    }

    /**
     * Test downloading a store database for a specific language when no asset is found.
     */
    @Test
    fun `downloadLatestStoreDatabaseForLanguage should return false when no asset is found`() = runTest {
        // Arrange
        val language = "en"
        val releaseJson = """
            {
                "tag_name": "v1.0.0",
                "assets": [
                    {
                        "name": "some-other-file.db",
                        "browser_download_url": "https://example.com/some-other-file.db"
                    }
                ]
            }
        """.trimIndent()

        // Set up mock responses
        mockUrlHandler.addResponse(
            "https://api.github.com/repos/kdroidFilter/KDroidDatabase/releases/latest",
            200,
            releaseJson
        )

        // Act
        val downloader = DatabaseDownloader()
        val result = downloader.downloadLatestStoreDatabaseForLanguage(testDir.absolutePath, language)

        // Assert
        assertFalse(result, "Download should fail when no asset is found")
    }

    /**
     * Test downloading a store database for a specific language when the download fails.
     */
    @Test
    fun `downloadLatestStoreDatabaseForLanguage should return false when download fails`() = runTest {
        // Arrange
        val language = "en"
        val assetName = "store-database-$language.db"
        val releaseJson = """
            {
                "tag_name": "v1.0.0",
                "assets": [
                    {
                        "name": "$assetName",
                        "browser_download_url": "https://example.com/$assetName"
                    }
                ]
            }
        """.trimIndent()

        // Set up mock responses
        mockUrlHandler.addResponse(
            "https://api.github.com/repos/kdroidFilter/KDroidDatabase/releases/latest",
            200,
            releaseJson
        )
        mockUrlHandler.addResponse(
            "https://example.com/$assetName",
            404,
            "Not Found"
        )

        // Act
        val downloader = DatabaseDownloader()
        val result = downloader.downloadLatestStoreDatabaseForLanguage(testDir.absolutePath, language)

        // Assert
        assertFalse(result, "Download should fail when HTTP request fails")
    }

    /**
     * Test downloading a store database for a specific language when no release is found.
     */
    @Test
    fun `downloadLatestStoreDatabaseForLanguage should return false when no release is found`() = runTest {
        // Arrange
        val language = "en"

        // Set up mock responses
        mockUrlHandler.addResponse(
            "https://api.github.com/repos/kdroidFilter/KDroidDatabase/releases/latest",
            404,
            "Not Found"
        )

        // Act
        val downloader = DatabaseDownloader()
        val result = downloader.downloadLatestStoreDatabaseForLanguage(testDir.absolutePath, language)

        // Assert
        assertFalse(result, "Download should fail when no release is found")
    }

    /**
     * Test downloading store databases for all languages when all downloads are successful.
     */
    @Test
    fun `downloadLatestStoreDatabases should return all true when all downloads are successful`() = runTest {
        // Arrange
        val languages = listOf("en", "fr", "he")

        // Set up mock responses for the actual URLs being requested
        languages.forEach { lang ->
            mockUrlHandler.addResponse(
                "https://github.com/kdroidFilter/KDroidDatabase/releases/download/202506170506/store-database-$lang.db",
                200,
                "test database content for $lang"
            )
        }

        // Act
        val downloader = DatabaseDownloader()
        val results = downloader.downloadLatestStoreDatabases(testDir.absolutePath)

        // Assert
        assertEquals(3, results.size, "Should have results for all 3 languages")
        languages.forEach { lang ->
            assertTrue(results[lang] == true, "Download for $lang should be successful")
            val downloadedFile = File(testDir, "store-database-$lang.db")
            assertTrue(downloadedFile.exists(), "Downloaded file for $lang should exist")
            assertTrue(downloadedFile.length() > 0, "Downloaded file for $lang should not be empty")
        }
    }

    /**
     * Test downloading store databases for all languages when some downloads fail.
     */
    @Test
    fun `downloadLatestStoreDatabases should return mixed results when some downloads fail`() = runTest {
        // Arrange
        val languages = listOf("en", "fr", "he")

        // Set up mock responses for the actual URLs being requested
        // Add successful response for English
        mockUrlHandler.addResponse(
            "https://github.com/kdroidFilter/KDroidDatabase/releases/download/202506170506/store-database-en.db",
            200,
            "test database content for en"
        )

        // Add failed responses for other languages
        mockUrlHandler.addResponse(
            "https://github.com/kdroidFilter/KDroidDatabase/releases/download/202506170506/store-database-fr.db",
            404,
            "Not Found"
        )
        mockUrlHandler.addResponse(
            "https://github.com/kdroidFilter/KDroidDatabase/releases/download/202506170506/store-database-he.db",
            500,
            "Internal Server Error"
        )

        // Act
        val downloader = DatabaseDownloader()
        val results = downloader.downloadLatestStoreDatabases(testDir.absolutePath)

        // Assert
        assertEquals(3, results.size, "Should have results for all 3 languages")
        assertTrue(results["en"] == true, "Download for English should be successful")
        assertTrue(results["fr"] == false, "Download for French should fail")
        assertTrue(results["he"] == false, "Download for Hebrew should fail")

        // Verify files
        assertTrue(File(testDir, "store-database-en.db").exists(), "English file should exist")
        assertFalse(File(testDir, "store-database-fr.db").exists(), "French file should not exist")
        assertFalse(File(testDir, "store-database-he.db").exists(), "Hebrew file should not exist")
    }

    /**
     * Test downloading the policies database when the download is successful.
     */
    @Test
    fun `downloadLatestPoliciesDatabase should return true when download is successful`() = runTest {
        // Arrange
        val assetName = "policies-database.db"

        // Set up mock responses for the actual URL being requested
        mockUrlHandler.addResponse(
            "https://github.com/kdroidFilter/KDroidDatabase/releases/download/202506170506/$assetName",
            200,
            "test database content"
        )

        // Act
        val downloader = DatabaseDownloader()
        val result = downloader.downloadLatestPoliciesDatabase(testDir.absolutePath)

        // Assert
        assertTrue(result, "Download should be successful")
        val downloadedFile = File(testDir, assetName)
        assertTrue(downloadedFile.exists(), "Downloaded file should exist")
        assertTrue(downloadedFile.length() > 0, "Downloaded file should not be empty")
    }

    /**
     * Test downloading the policies database when no asset is found.
     */
    @Test
    fun `downloadLatestPoliciesDatabase should return false when no asset is found`() = runTest {
        // Arrange
        val releaseJson = """
            {
                "tag_name": "v1.0.0",
                "assets": [
                    {
                        "name": "some-other-file.db",
                        "browser_download_url": "https://example.com/some-other-file.db"
                    }
                ]
            }
        """.trimIndent()

        // Set up mock responses
        mockUrlHandler.addResponse(
            "https://api.github.com/repos/kdroidFilter/KDroidDatabase/releases/latest",
            200,
            releaseJson
        )

        // Act
        val downloader = DatabaseDownloader()
        val result = downloader.downloadLatestPoliciesDatabase(testDir.absolutePath)

        // Assert
        assertFalse(result, "Download should fail when no asset is found")
    }
}

/**
 * A custom URL stream handler factory and handler that allows us to intercept
 * HTTP requests and return predefined responses for testing.
 */
class MockURLStreamHandler : URLStreamHandlerFactory, URLStreamHandler() {
    private val responses = mutableMapOf<String, MockResponse>()

    override fun createURLStreamHandler(protocol: String): URLStreamHandler? {
        return if (protocol == "https" || protocol == "http") this else null
    }

    /**
     * Add a mock response for a specific URL.
     */
    fun addResponse(url: String, statusCode: Int, content: String) {
        responses[url] = MockResponse(statusCode, content)
    }

    /**
     * Clear all mock responses.
     */
    fun clearResponses() {
        responses.clear()
    }

    override fun openConnection(url: URL): URLConnection {
        println("[DEBUG_LOG] Requested URL: ${url.toString()}")
        return MockHttpURLConnection(url, responses[url.toString()])
    }

    /**
     * A mock HTTP URL connection that returns predefined responses.
     */
    private class MockHttpURLConnection(
        url: URL,
        private val response: MockResponse?
    ) : HttpURLConnection(url) {

        init {
            responseCode = response?.statusCode ?: 404
        }

        override fun connect() {
            connected = true
        }

        override fun disconnect() {
            connected = false
        }

        override fun getInputStream() = when {
            response == null -> throw IOException("No mock response for $url")
            responseCode >= 400 -> throw IOException("HTTP error: $responseCode")
            else -> ByteArrayInputStream(response.content.toByteArray())
        }

        override fun getResponseMessage(): String {
            return when (responseCode) {
                200 -> "OK"
                404 -> "Not Found"
                500 -> "Internal Server Error"
                else -> "Unknown"
            }
        }

        override fun usingProxy() = false
    }

    /**
     * A simple data class to hold a mock HTTP response.
     */
    private data class MockResponse(
        val statusCode: Int,
        val content: String
    )
}

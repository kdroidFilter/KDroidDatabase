import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import java.nio.file.Files
import java.nio.file.Path
import io.github.kdroidfilter.database.core.AppCategory
import io.github.kdroidfilter.database.core.NetworkMode
import io.github.kdroidfilter.database.core.NetworkPolicy
import io.github.kdroidfilter.database.core.ModeSpec
import io.github.kdroidfilter.database.core.policies.FixedPolicy

class JsonValidatorTest {

    @Test
    fun `validateAll should return valid result for valid JSON file`() {
        // Arrange
        val tempDir = Files.createTempDirectory("test-policies")
        createValidPolicyFile(tempDir)

        // Act
        val results = JsonValidator.validateAll(tempDir)

        // Assert
        assertEquals(1, results.size, "Should have one result")
        assertEquals(null, results[0].error, "Error should be null for valid file")

        // Cleanup
        deleteDirectory(tempDir)
    }

    @Test
    fun `validateAll should return error for invalid JSON file`() {
        // Arrange
        val tempDir = Files.createTempDirectory("test-policies")
        createInvalidPolicyFile(tempDir)

        // Act
        val results = JsonValidator.validateAll(tempDir)

        // Assert
        assertEquals(1, results.size, "Should have one result")
        assertTrue(results[0].error != null, "Error should not be null for invalid file")

        // Cleanup
        deleteDirectory(tempDir)
    }

    @Test
    fun `printResults should return true when all files are valid`() {
        // Arrange
        val validResult = JsonValidator.ValidationResult(
            Path.of("valid.json"), 
            null
        )
        val results = listOf(validResult)

        // Redirect System.out to capture output
        val originalOut = System.out
        val outContent = java.io.ByteArrayOutputStream()
        System.setOut(java.io.PrintStream(outContent))

        try {
            // Act
            val isValid = JsonValidator.printResults(results)

            // Assert
            assertTrue(isValid, "Should return true for valid files")
            assertTrue(outContent.toString().contains("1 valid, 0 invalid"), 
                "Output should indicate all files are valid")
        } finally {
            // Restore original System.out
            System.setOut(originalOut)
        }
    }

    @Test
    fun `printResults should return false when some files are invalid`() {
        // Arrange
        val invalidResult = JsonValidator.ValidationResult(
            Path.of("invalid.json"), 
            "Test error message"
        )
        val results = listOf(invalidResult)

        // Redirect System.out to capture output
        val originalOut = System.out
        val outContent = java.io.ByteArrayOutputStream()
        System.setOut(java.io.PrintStream(outContent))

        try {
            // Act
            val isValid = JsonValidator.printResults(results)

            // Assert
            assertFalse(isValid, "Should return false for invalid files")
            assertTrue(outContent.toString().contains("0 valid, 1 invalid"), 
                "Output should indicate some files are invalid")
            assertTrue(outContent.toString().contains("Test error message"), 
                "Output should include error message")
        } finally {
            // Restore original System.out
            System.setOut(originalOut)
        }
    }

    // Helper method to create a valid policy file
    private fun createValidPolicyFile(directory: Path) {
        // Create a valid FixedPolicy object
        val policy = FixedPolicy(
            packageName = "com.example.test",
            category = AppCategory.COMMUNICATION,
            networkPolicy = NetworkPolicy(
                mode = NetworkMode.WHITELIST,
                spec = ModeSpec.HostList(setOf("example.com"))
            ),
            minimumVersionCode = 1
        )

        // Serialize the policy using PolicyRepository's JSON configuration
        val policyContent = PolicyRepository.json.encodeToString(
            io.github.kdroidfilter.database.core.policies.AppPolicy.serializer(), 
            policy
        )

        Files.write(directory.resolve("valid.json"), policyContent.toByteArray())
    }

    // Helper method to create an invalid policy file
    private fun createInvalidPolicyFile(directory: Path) {
        val invalidContent = """
            {
                "type": "InvalidPolicyType",
                "packageName": "com.example.test",
                "category": "INVALID_CATEGORY",
                "networkPolicy": {
                    "mode": "INVALID_MODE"
                }
            }
        """.trimIndent()

        Files.write(directory.resolve("invalid.json"), invalidContent.toByteArray())
    }

    // Helper method to recursively delete a directory
    private fun deleteDirectory(directory: Path) {
        if (Files.exists(directory)) {
            Files.walk(directory)
                .sorted(Comparator.reverseOrder())
                .forEach { Files.deleteIfExists(it) }
        }
    }
}

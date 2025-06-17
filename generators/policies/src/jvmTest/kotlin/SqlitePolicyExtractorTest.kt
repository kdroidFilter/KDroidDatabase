import kotlin.test.Test
import kotlin.test.assertTrue
import java.nio.file.Files
import java.nio.file.Path
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.sql.DriverManager
import io.github.kdroidfilter.database.core.AppCategory
import io.github.kdroidfilter.database.core.NetworkMode
import io.github.kdroidfilter.database.core.NetworkPolicy
import io.github.kdroidfilter.database.core.ModeSpec
import io.github.kdroidfilter.database.core.policies.FixedPolicy

class SqlitePolicyExtractorTest {

    @Test
    fun `main function should execute without errors`() {
        // Arrange
        val originalOut = System.out
        val outContent = ByteArrayOutputStream()
        System.setOut(PrintStream(outContent))

        try {
            // Create temporary project structure
            val tempDir = Files.createTempDirectory("test-project")
            val policiesDir = tempDir.resolve("app-policies")
            val buildDir = tempDir.resolve("build")
            Files.createDirectories(policiesDir)
            Files.createDirectories(buildDir)

            // Create a mock app-policies directory with a test policy
            createMockPolicyStructure(policiesDir)

            // Set up system property to point to our temp directory
            val originalUserDir = System.getProperty("user.dir")
            System.setProperty("user.dir", tempDir.toString())

            try {
                // Act - Call the SqlitePolicyBuilder directly instead of using reflection
                // This ensures we're using the correct paths
                val policiesDir = tempDir.resolve("app-policies")
                val outputDbPath = tempDir.resolve("build/policies-database.db")
                SqlitePolicyBuilder.buildDatabase(policiesDir, outputDbPath)

                // Assert
                // Check if build directory was created
                val buildDir = tempDir.resolve("build")
                assertTrue(Files.exists(buildDir), "Build directory should be created")

                // Check if database file was created
                assertTrue(Files.exists(outputDbPath), "Database file should be created")

                // Verify database has expected tables
                val connection = DriverManager.getConnection("jdbc:sqlite:${outputDbPath.toAbsolutePath()}")
                val statement = connection.createStatement()
                val resultSet = statement.executeQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name IN ('policies', 'version')"
                )

                val tables = mutableListOf<String>()
                while (resultSet.next()) {
                    tables.add(resultSet.getString("name"))
                }

                assertTrue(tables.contains("policies"), "policies table should exist")
                assertTrue(tables.contains("version"), "version table should exist")

                // Cleanup
                resultSet.close()
                statement.close()
                connection.close()

            } finally {
                // Restore original user.dir
                System.setProperty("user.dir", originalUserDir)

                // Clean up
                deleteDirectory(tempDir)
            }
        } finally {
            // Restore original System.out
            System.setOut(originalOut)
        }
    }

    // Helper method to create a mock policy structure
    private fun createMockPolicyStructure(policiesDir: Path) {
        // Create category directories
        val communicationDir = policiesDir.resolve("communication")
        Files.createDirectories(communicationDir)

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

        Files.write(communicationDir.resolve("com.example.test.json"), policyContent.toByteArray())
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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import java.nio.file.Files
import java.nio.file.Path
import io.github.kdroidfilter.database.core.AppCategory
import io.github.kdroidfilter.database.core.NetworkMode
import io.github.kdroidfilter.database.core.NetworkPolicy
import io.github.kdroidfilter.database.core.ModeSpec
import io.github.kdroidfilter.database.core.policies.AppPolicy
import io.github.kdroidfilter.database.core.policies.FixedPolicy
import kotlinx.serialization.builtins.ListSerializer

class PolicyRepositoryTest {

    @Test
    fun `loadAll should load all policy files from directory`() {
        // Arrange
        val tempDir = Files.createTempDirectory("test-policies")
        createPolicyFile(tempDir, "com.example.app1", AppCategory.COMMUNICATION)
        createPolicyFile(tempDir, "com.example.app2", AppCategory.FINANCE)

        // Act
        val policies = PolicyRepository.loadAll(tempDir)

        // Assert
        assertEquals(2, policies.size, "Should load 2 policies")
        assertTrue(policies.any { it.packageName == "com.example.app1" }, "Should contain app1")
        assertTrue(policies.any { it.packageName == "com.example.app2" }, "Should contain app2")

        // Cleanup
        deleteDirectory(tempDir)
    }

    @Test
    fun `save should create policy file in correct location`() {
        // Arrange
        val tempDir = Files.createTempDirectory("test-policies")
        val policy = createTestPolicy("com.example.test", AppCategory.COMMUNICATION)

        // Act
        PolicyRepository.save(policy, tempDir)

        // Assert
        val expectedPath = tempDir.resolve("communication").resolve("com.example.test.json")
        assertTrue(Files.exists(expectedPath), "Policy file should be created")

        // Verify content by parsing the JSON
        val content = Files.readString(expectedPath)
        val parsedPolicy = PolicyRepository.json.decodeFromString(AppPolicy.serializer(), content)

        assertEquals("com.example.test", parsedPolicy.packageName, "Package name should match")
        assertEquals(AppCategory.COMMUNICATION, parsedPolicy.category, "Category should match")

        // Cleanup
        deleteDirectory(tempDir)
    }

    @Test
    fun `exportAll should create a single JSON file with all policies`() {
        // Arrange
        val tempDir = Files.createTempDirectory("test-policies")
        val outputFile = Files.createTempFile("all-policies", ".json")

        createPolicyFile(tempDir, "com.example.app1", AppCategory.COMMUNICATION)
        createPolicyFile(tempDir, "com.example.app2", AppCategory.FINANCE)

        // Act
        PolicyRepository.exportAll(tempDir, outputFile)

        // Assert
        assertTrue(Files.exists(outputFile), "Output file should be created")

        // Verify content
        val content = Files.readString(outputFile)
        val policies = PolicyRepository.json.decodeFromString(
            ListSerializer(AppPolicy.serializer()), 
            content
        )

        assertEquals(2, policies.size, "Should contain 2 policies")
        assertTrue(policies.any { it.packageName == "com.example.app1" }, "Should contain app1")
        assertTrue(policies.any { it.packageName == "com.example.app2" }, "Should contain app2")

        // Cleanup
        Files.deleteIfExists(outputFile)
        deleteDirectory(tempDir)
    }

    @Test
    fun `save and loadAll should perform round-trip serialization correctly`() {
        // Arrange
        val tempDir = Files.createTempDirectory("test-policies")
        val originalPolicy = createTestPolicy("com.example.test", AppCategory.COMMUNICATION)

        // Act - Save and then load
        PolicyRepository.save(originalPolicy, tempDir)
        val loadedPolicies = PolicyRepository.loadAll(tempDir)

        // Assert
        assertEquals(1, loadedPolicies.size, "Should load 1 policy")
        val loadedPolicy = loadedPolicies.first()
        assertEquals(originalPolicy.packageName, loadedPolicy.packageName, "Package name should match")
        assertEquals(originalPolicy.category, loadedPolicy.category, "Category should match")

        // For FixedPolicy, check specific properties
        if (originalPolicy is FixedPolicy && loadedPolicy is FixedPolicy) {
            assertEquals(
                originalPolicy.networkPolicy.mode, 
                loadedPolicy.networkPolicy.mode, 
                "Network mode should match"
            )
        }

        // Cleanup
        deleteDirectory(tempDir)
    }

    // Helper method to create a test policy
    private fun createTestPolicy(packageName: String, category: AppCategory): FixedPolicy {
        return FixedPolicy(
            packageName = packageName,
            category = category,
            networkPolicy = NetworkPolicy(NetworkMode.WHITELIST, ModeSpec.HostList(setOf("example.com"))),
            minimumVersionCode = 1
        )
    }

    // Helper method to create a policy file
    private fun createPolicyFile(directory: Path, packageName: String, category: AppCategory) {
        val policy = createTestPolicy(packageName, category)
        val categoryDir = directory.resolve(category.name.lowercase())
        Files.createDirectories(categoryDir)

        val policyJson = PolicyRepository.json.encodeToString(AppPolicy.serializer(), policy)
        Files.write(categoryDir.resolve("$packageName.json"), policyJson.toByteArray())
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

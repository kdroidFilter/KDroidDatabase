import io.github.kdroidfilter.database.core.AppCategory
import io.github.kdroidfilter.database.core.ModeSpec
import io.github.kdroidfilter.database.core.NetworkMode
import io.github.kdroidfilter.database.core.NetworkPolicy
import io.github.kdroidfilter.database.core.UserMode
import io.github.kdroidfilter.database.core.policies.AppPolicy
import io.github.kdroidfilter.database.core.policies.FixedPolicy
import io.github.kdroidfilter.database.core.policies.ModeBasedPolicy
import kotlinx.serialization.builtins.ListSerializer
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsonExtractorTest {

    @Test
    fun `exportAll should serialize all existing policies without error`() {
        // Arrange: point to the real app-policies folder in the project
        val projectDir = Path.of("").toAbsolutePath()
        val root = projectDir.parent.resolve("../app-policies")
        val output = Files.createTempFile("all-policies", ".json")

        // Act: generate the single JSON array
        PolicyRepository.exportAll(root, output)

        // Assert: JSON parses as List<AppPolicy> and n > 0
        val jsonStr = Files.readString(output)
        val policies: List<AppPolicy> =
            PolicyRepository.json
                .decodeFromString(ListSerializer(AppPolicy.serializer()), jsonStr)

        assertTrue(policies.isNotEmpty(), "Expected at least one policy in the JSON array")
    }

    @Test
    fun `roundtrip save and exportAll should preserve all policies`() {
        // Arrange: create temp folder and two in-memory policies
        val tempRoot = Files.createTempDirectory("policies-test")
        val fixed = FixedPolicy(
            packageName = "com.example.foo",
            category = AppCategory.COMMUNICATION,
            networkPolicy = NetworkPolicy(NetworkMode.WHITELIST, ModeSpec.HostList(setOf("foo.com"))),
            minimumVersionCode = 0
        )
        val modeBased = ModeBasedPolicy(
            packageName = "com.example.bar",
            category = AppCategory.MAIL,
            modePolicies = mapOf(
                UserMode.OFFLINE to NetworkPolicy(NetworkMode.OFFLINE),
                UserMode.MOST_OPEN to NetworkPolicy(NetworkMode.FULL_OPEN)
            ),
            minimumVersionCode = 1
        )

        // Save each policy as its own JSON file
        PolicyRepository.save(fixed, tempRoot)
        PolicyRepository.save(modeBased, tempRoot)

        // Act: export all into one JSON list
        val output = Files.createTempFile("all-policies", ".json")
        PolicyRepository.exportAll(tempRoot, output)

        // Assert: we get exactly 2 policies back
        val jsonStr = Files.readString(output)
        val policies: List<AppPolicy> =
            PolicyRepository.json
                .decodeFromString(ListSerializer(AppPolicy.serializer()), jsonStr)

        assertEquals(2, policies.size, "Should have serialized exactly 2 policies")
        // Order is the filesystem's iteration order; on JVM we can sort by packageName
        val names = policies.map { it.packageName }.toSet()
        assertTrue(names.containsAll(setOf("com.example.foo", "com.example.bar")), "Both package names must be present")
    }
}
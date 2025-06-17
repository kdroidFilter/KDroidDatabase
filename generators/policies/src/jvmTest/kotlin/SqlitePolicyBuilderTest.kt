import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import java.nio.file.Files
import java.nio.file.Path
import java.sql.DriverManager
import io.github.kdroidfilter.database.core.AppCategory
import io.github.kdroidfilter.database.core.NetworkMode
import io.github.kdroidfilter.database.core.NetworkPolicy
import io.github.kdroidfilter.database.core.ModeSpec
import io.github.kdroidfilter.database.core.policies.AppPolicy
import io.github.kdroidfilter.database.core.policies.FixedPolicy
import kotlinx.serialization.json.Json

class SqlitePolicyBuilderTest {
    
    @Test
    fun `buildDatabase should create a valid database file`() {
        // Arrange
        val tempDir = Files.createTempDirectory("test-policies")
        val outputDbPath = Files.createTempFile("test-db", ".db")
        Files.deleteIfExists(outputDbPath) // Delete so buildDatabase can create it
        
        // Create test policy files
        createPolicyFile(tempDir, "com.example.app1", AppCategory.COMMUNICATION)
        createPolicyFile(tempDir, "com.example.app2", AppCategory.FINANCE)
        
        // Act
        SqlitePolicyBuilder.buildDatabase(tempDir, outputDbPath)
        
        // Assert
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
        
        // Verify policies were inserted
        val policiesResultSet = statement.executeQuery("SELECT COUNT(*) as count FROM policies")
        assertTrue(policiesResultSet.next(), "Result set should have at least one row")
        assertEquals(2, policiesResultSet.getInt("count"), "Should have 2 policies")
        
        // Cleanup
        resultSet.close()
        policiesResultSet.close()
        statement.close()
        connection.close()
        Files.deleteIfExists(outputDbPath)
        deleteDirectory(tempDir)
    }
    
    @Test
    fun `createTables should create the expected tables`() {
        // Arrange
        val outputDbPath = Files.createTempFile("test-db", ".db")
        Files.deleteIfExists(outputDbPath) // Delete so we can create it fresh
        
        // Create a connection
        val url = "jdbc:sqlite:${outputDbPath.toAbsolutePath()}"
        val connection = DriverManager.getConnection(url)
        
        // Use reflection to access the private method
        val method = SqlitePolicyBuilder::class.java.getDeclaredMethod(
            "createTables", 
            java.sql.Connection::class.java
        )
        method.isAccessible = true
        
        // Act
        method.invoke(SqlitePolicyBuilder, connection)
        
        // Assert
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
        Files.deleteIfExists(outputDbPath)
    }
    
    @Test
    fun `insertVersion should add version information to database`() {
        // Arrange
        val outputDbPath = Files.createTempFile("test-db", ".db")
        Files.deleteIfExists(outputDbPath) // Delete so we can create it fresh
        
        // Create a connection and tables
        val url = "jdbc:sqlite:${outputDbPath.toAbsolutePath()}"
        val connection = DriverManager.getConnection(url)
        
        // Create tables first
        val createTablesMethod = SqlitePolicyBuilder::class.java.getDeclaredMethod(
            "createTables", 
            java.sql.Connection::class.java
        )
        createTablesMethod.isAccessible = true
        createTablesMethod.invoke(SqlitePolicyBuilder, connection)
        
        // Use reflection to access the private method
        val method = SqlitePolicyBuilder::class.java.getDeclaredMethod(
            "insertVersion", 
            java.sql.Connection::class.java,
            String::class.java
        )
        method.isAccessible = true
        
        val releaseName = "test-release-123"
        
        // Act
        method.invoke(SqlitePolicyBuilder, connection, releaseName)
        
        // Assert
        val statement = connection.createStatement()
        val resultSet = statement.executeQuery("SELECT release_name FROM version")
        
        assertTrue(resultSet.next(), "Result set should have at least one row")
        assertEquals(releaseName, resultSet.getString("release_name"), "Release name should match")
        
        // Cleanup
        resultSet.close()
        statement.close()
        connection.close()
        Files.deleteIfExists(outputDbPath)
    }
    
    @Test
    fun `insertPolicies should add policies to database`() {
        // Arrange
        val tempDir = Files.createTempDirectory("test-policies")
        val outputDbPath = Files.createTempFile("test-db", ".db")
        Files.deleteIfExists(outputDbPath) // Delete so we can create it fresh
        
        // Create test policy files
        createPolicyFile(tempDir, "com.example.app1", AppCategory.COMMUNICATION)
        createPolicyFile(tempDir, "com.example.app2", AppCategory.FINANCE)
        
        // Create a connection and tables
        val url = "jdbc:sqlite:${outputDbPath.toAbsolutePath()}"
        val connection = DriverManager.getConnection(url)
        
        // Create tables first
        val createTablesMethod = SqlitePolicyBuilder::class.java.getDeclaredMethod(
            "createTables", 
            java.sql.Connection::class.java
        )
        createTablesMethod.isAccessible = true
        createTablesMethod.invoke(SqlitePolicyBuilder, connection)
        
        // Use reflection to access the private method
        val method = SqlitePolicyBuilder::class.java.getDeclaredMethod(
            "insertPolicies", 
            java.sql.Connection::class.java,
            Path::class.java
        )
        method.isAccessible = true
        
        // Act
        method.invoke(SqlitePolicyBuilder, connection, tempDir)
        
        // Assert
        val statement = connection.createStatement()
        val resultSet = statement.executeQuery("SELECT package_name, data FROM policies")
        
        val packages = mutableListOf<String>()
        while (resultSet.next()) {
            packages.add(resultSet.getString("package_name"))
            // Verify data is valid JSON
            val data = resultSet.getString("data")
            assertTrue(data.contains("\"packageName\""), "Data should contain packageName")
        }
        
        assertEquals(2, packages.size, "Should have 2 policies")
        assertTrue(packages.contains("com.example.app1"), "Should contain app1")
        assertTrue(packages.contains("com.example.app2"), "Should contain app2")
        
        // Cleanup
        resultSet.close()
        statement.close()
        connection.close()
        Files.deleteIfExists(outputDbPath)
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
        
        val json = Json {
            classDiscriminator = "type"
            ignoreUnknownKeys = true
            encodeDefaults = false
            prettyPrint = true
            serializersModule = PolicyRepository.json.serializersModule
        }
        
        val policyJson = json.encodeToString(AppPolicy.serializer(), policy)
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
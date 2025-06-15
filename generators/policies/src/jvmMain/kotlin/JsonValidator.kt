import io.github.kdroidfilter.database.core.policies.AppPolicy
import kotlinx.serialization.SerializationException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

/**
 * Utility to validate JSON policy files
 */
object JsonValidator {
    /**
     * Validates all JSON files under the given root directory
     * @param root The root directory containing JSON files to validate
     * @return A list of validation results (file path and error message if any)
     */
    fun validateAll(root: Path): List<ValidationResult> {
        println("Validating JSON files in: $root")
        
        return Files.walk(root)
            .filter { it.isRegularFile() && it.extension == "json" }
            .map { path ->
                try {
                    val content = Files.newBufferedReader(path).use { it.readText() }
                    try {
                        // Try to parse the JSON using the PolicyRepository's JSON configuration
                        PolicyRepository.json.decodeFromString(AppPolicy.serializer(), content)
                        ValidationResult(path, null) // No error
                    } catch (e: SerializationException) {
                        ValidationResult(path, e.message ?: "Unknown serialization error")
                    }
                } catch (e: Exception) {
                    ValidationResult(path, "Error reading file: ${e.message}")
                }
            }
            .toList()
    }
    
    /**
     * Prints validation results to the console
     * @param results The validation results to print
     * @return true if all files are valid, false otherwise
     */
    fun printResults(results: List<ValidationResult>): Boolean {
        val validCount = results.count { it.error == null }
        val invalidCount = results.size - validCount
        
        println("Validation complete: $validCount valid, $invalidCount invalid")
        
        if (invalidCount > 0) {
            println("\nInvalid files:")
            results.filter { it.error != null }
                .forEach { result ->
                    println("- ${result.path.name}: ${result.error}")
                }
            return false
        }
        
        return true
    }
    
    /**
     * Data class to hold validation result for a single file
     */
    data class ValidationResult(val path: Path, val error: String?)
}

/**
 * Main function to run the JSON validator
 */
fun main() {
    val projectDir = java.nio.file.Paths.get("").toAbsolutePath()
    val root = projectDir.parent.resolve("../app-policies")
    
    val results = JsonValidator.validateAll(root)
    val isValid = JsonValidator.printResults(results)
    
    if (!isValid) {
        System.exit(1)
    }
}
import java.nio.file.Paths

fun main() {
    val projectDir = Paths.get("").toAbsolutePath()
    val root       = projectDir.parent.resolve("../app-policies")
    val output = Paths.get("build","all-policies.json")
    PolicyRepository.exportAll(root, output)
}
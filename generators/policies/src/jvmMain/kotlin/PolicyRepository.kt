import io.github.kdroidfilter.database.core.ModeSpec
import io.github.kdroidfilter.database.core.policies.AppPolicy
import io.github.kdroidfilter.database.core.policies.FixedPolicy
import io.github.kdroidfilter.database.core.policies.ModeBasedPolicy
import io.github.kdroidfilter.database.core.policies.MultiModePolicy
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile

object PolicyRepository {
    /**
     * JSON configuration: polymorphism, ignore unknown keys, pretty print
     */
    internal val json = Json {
        classDiscriminator = "type"
        ignoreUnknownKeys = true
        encodeDefaults = false
        prettyPrint = true
        serializersModule = SerializersModule {
            polymorphic(AppPolicy::class) {
                subclass(FixedPolicy::class, FixedPolicy.serializer())
                subclass(ModeBasedPolicy::class, ModeBasedPolicy.serializer())
                subclass(MultiModePolicy::class, MultiModePolicy.serializer())
            }
            polymorphic(ModeSpec::class) {
                subclass(ModeSpec.None::class, ModeSpec.None.serializer())
                subclass(ModeSpec.HostList::class, ModeSpec.HostList.serializer())
            }
        }
    }

    /**
     * Load recursively all *.json files under the given root
     */
    fun loadAll(root: Path): List<AppPolicy> =
        Files.walk(root)
            .filter { it.isRegularFile() && it.extension == "json" }
            .map { path ->
                Files.newBufferedReader(path).use { reader ->
                    json.decodeFromString(AppPolicy.serializer(), reader.readText())
                }
            }
            .toList()

    /**
     * Save (replace) a single policy to its corresponding file under root
     */
    fun save(policy: AppPolicy, root: Path) {
        val relPath = policy.category.name.lowercase()
        val file = root.resolve(relPath).resolve("${policy.packageName}.json")
        Files.createDirectories(file.parent)
        Files.newBufferedWriter(
            file,
            Charsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
        ).use { writer ->
            writer.write(json.encodeToString(AppPolicy.serializer(), policy))
        }
    }

    /**
     * Exports all loaded policies as a single JSON array to the specified output file
     */
    fun exportAll(root: Path, outputFile: Path) {
        val policies = loadAll(root)
        val jsonArray = json.encodeToString(ListSerializer(AppPolicy.serializer()), policies)
        // Ensure output directory exists if provided
        outputFile.parent?.let { Files.createDirectories(it) }
        Files.newBufferedWriter(
            outputFile,
            Charsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
        ).use { writer ->
            writer.write(jsonArray)
        }
    }
}


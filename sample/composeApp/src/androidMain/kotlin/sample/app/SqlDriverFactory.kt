package sample.app

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import io.github.kdroidfilter.database.store.Database
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

private lateinit var applicationContext: Context

fun initializeContext(context: Context) {
    applicationContext = context.applicationContext
}

actual fun createSqlDriver(): SqlDriver {
    val driver = AndroidSqliteDriver(
        schema = Database.Schema,
        context = applicationContext,
        name = "store-database.db"
    )
    return driver
}

@RequiresApi(Build.VERSION_CODES.O)
actual fun getDatabasePath(): Path {
    val databaseFile = applicationContext.getDatabasePath("store-database.db")
    return Paths.get(databaseFile.absolutePath)
}

actual fun getDeviceLanguage(): String {
    // Get the default locale's language
    val language = Locale.getDefault().language

    // Return the language code (en, fr, he, etc.)
    return when (language) {
        "fr" -> "fr"
        "he", "iw" -> "he" // iw is the old code for Hebrew
        else -> "en" // Default to English for any other language
    }
}
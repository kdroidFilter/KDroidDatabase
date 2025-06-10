package sample.app

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import io.github.kdroidfilter.database.sample.Database
import java.nio.file.Paths
import java.util.Locale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

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

actual fun getDatabasePath(): String {
    val databaseFile = applicationContext.getDatabasePath("store-database.db")
    return Paths.get(databaseFile.absolutePath).toString()
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
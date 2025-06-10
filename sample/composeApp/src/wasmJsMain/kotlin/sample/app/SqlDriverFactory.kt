package sample.app

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import org.w3c.dom.Worker

actual fun createSqlDriver(): SqlDriver {
    val worker = Worker(js("""new URL("sqljs.worker.js", import.meta.url)"""))
    return WebWorkerDriver(worker)
}

actual fun getDatabasePath(): String {
    error("File system is not available in WebAssembly")
}

actual fun getDeviceLanguage(): String {
    return (js("navigator.language") as? String)?.substring(0,2) ?: "en"
}

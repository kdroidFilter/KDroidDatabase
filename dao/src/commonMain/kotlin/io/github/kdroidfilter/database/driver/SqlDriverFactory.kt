package io.github.kdroidfilter.database.driver

import app.cash.sqldelight.db.SqlDriver

expect fun createSqlDriver(): SqlDriver
expect fun getDatabasePath(): String
expect fun getDeviceLanguage(): String

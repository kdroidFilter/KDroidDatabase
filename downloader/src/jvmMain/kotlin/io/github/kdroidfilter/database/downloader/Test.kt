package io.github.kdroidfilter.database.downloader

import kotlinx.coroutines.runBlocking

fun main() {
    runBlocking {
        DatabaseVersionChecker().isDatabaseVersionUpToDate("202506151126")
    }
}

package io.github.kdroidfilter.database.core

import kotlinx.serialization.Serializable

@Serializable
enum class UserMode(val level: Int) {
    OFFLINE (level = 0),
    LOCAL_ONLY(level = 1),
    NAVIGATION_ONLY(level = 2),
    NAVIGATION_AND_MAIL_ONLY(level = 3),
    REDUCED_RISK(level = 4),
    MOST_OPEN(level = 5)
}

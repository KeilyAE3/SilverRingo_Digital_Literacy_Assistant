package kr.silverringo.learning

import org.json.JSONObject

/** Only a successful guided/test result for the launched lesson advances progress. */
internal fun completedLesson(text: String, launchedLevel: Int): Int? = runCatching {
    val result = JSONObject(text)
    val id = result.getInt("levelId")
    id.takeIf {
        id == launchedLevel && result.getBoolean("passed") &&
            result.getString("mode") in setOf("learn", "test")
    }
}.getOrNull()

package kr.silverringo.learning

import org.junit.Assert.*
import org.junit.Test

class LearningResultTest {
    @Test fun completionIsBoundToLaunchedLessonAndMode() {
        assertEquals(1, completedLesson("""{"levelId":1,"passed":true,"mode":"learn"}""", 1))
        assertEquals(6, completedLesson("""{"levelId":6,"passed":true,"mode":"test"}""", 6))
        for (text in listOf(
            """{"levelId":2,"passed":true,"mode":"learn"}""",
            """{"levelId":1,"passed":false,"mode":"learn"}""",
            """{"levelId":1,"passed":true,"mode":"free"}""",
            """{"levelId":1,"passed":true}""", "not json"
        )) assertNull(completedLesson(text, 1))
    }
}

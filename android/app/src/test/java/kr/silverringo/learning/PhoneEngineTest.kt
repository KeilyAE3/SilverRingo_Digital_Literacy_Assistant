package kr.silverringo.learning

import kr.silverringo.learning.phone.*
import org.junit.Assert.*
import org.junit.Test

class PhoneEngineTest {
    private fun next(s: PhoneState) = reducePhone(s, PhoneAction.Continue)
    @Test fun brightnessAndSoundRequireBothTargets() {
        var s = next(PhoneState(PhoneLesson.SETTINGS))
        assertEquals(1, next(s).stage)
        s = next(reducePhone(s, PhoneAction.Brightness(70)))
        assertEquals(2, next(s).stage)
        s = next(reducePhone(s, PhoneAction.Volume(40)))
        assertTrue(s.complete)
        assertEquals(s, next(s))
    }
    @Test fun outgoingAndIncomingCallsMustBothFinish() {
        var s = next(PhoneState(PhoneLesson.CALLS))
        s = reducePhone(s, PhoneAction.Number("112"))
        assertEquals(1, next(s).stage)
        s = next(reducePhone(s, PhoneAction.Number("010-6344-2138")))
        assertFalse(s.complete)
        repeat(3) { s = next(s) }
        assertTrue(s.complete)
    }
    @Test fun contactMustBeSavedThenFound() {
        var s = next(next(PhoneState(PhoneLesson.CONTACTS)))
        assertEquals(2, next(s).stage)
        s = reducePhone(s, PhoneAction.Name("연습 친구"))
        s = next(reducePhone(s, PhoneAction.Number("01063442138")))
        assertEquals(3, next(s).stage)
        s = next(reducePhone(s, PhoneAction.Query("연습 친구")))
        assertFalse(s.complete)
        assertTrue(next(s).complete)
    }
    @Test fun messageChecksRecipientBodyAndReplyRead() {
        var s = PhoneState(PhoneLesson.MESSAGES)
        repeat(3) { s = next(s) }
        s = reducePhone(s, PhoneAction.Message("안녕하세요"))
        assertEquals(3, next(s).stage)
        s = next(reducePhone(s, PhoneAction.Number("01063442138")))
        assertFalse(s.complete)
        s = next(s)
        assertFalse(s.complete)
        assertTrue(next(s).complete)
    }
    @Test fun wrongInputsAllowCorrectionAndRecordFeedback() {
        val s = next(PhoneState(PhoneLesson.CALLS))
        val rejected = next(s)
        assertEquals(1, rejected.mistakes)
        assertTrue(rejected.feedback.isNotEmpty())
        assertEquals(2, next(reducePhone(rejected, PhoneAction.Number("01063442138"))).stage)
        assertEquals(0, s.mistakes)
    }
    @Test fun backHintAndRestartDoNotAwardCompletion() {
        val s = next(PhoneState(PhoneLesson.CALLS, guided = false))
        val hint = reducePhone(s, PhoneAction.Hint)
        assertEquals(1, hint.hints)
        assertEquals(s.stage, hint.stage)
        assertEquals(0, reducePhone(hint, PhoneAction.Back).stage)
        assertFalse(PhoneState(PhoneLesson.CALLS).complete)
    }
    @Test fun wrongScreenActionsAreIgnoredAndValuesAreBounded() {
        val s = PhoneState(PhoneLesson.SETTINGS)
        assertEquals(s, reducePhone(s, PhoneAction.Brightness(90)))
        assertEquals(100, reducePhone(next(s), PhoneAction.Brightness(1000)).brightness)
        assertEquals(0, reducePhone(next(s), PhoneAction.Brightness(-10)).brightness)
        assertEquals(s, reducePhone(s, PhoneAction.Number("01063442138")))
    }
    @Test fun curriculumHasSixWeeksAndFourDistinctWeekOneLessons() {
        assertEquals(6, phoneWeeks.size)
        assertEquals(4, phoneWeeks.first().lessons.size)
        assertEquals(4, PhoneLesson.entries.map { it.id }.toSet().size)
        PhoneLesson.entries.forEach { assertEquals(it, PhoneLesson.fromId(it.id)); assertFalse(PhoneState(it).complete) }
    }
}

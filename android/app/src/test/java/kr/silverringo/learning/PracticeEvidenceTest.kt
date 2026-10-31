package kr.silverringo.learning
import kr.silverringo.learning.phone.*
import org.junit.Assert.*
import org.junit.Test
class PracticeEvidenceTest {
    @Test fun unchangedVolumeIsNotAnAction() { assertEquals(false to false, volumeEvidence(4,4));assertEquals(true to false,volumeEvidence(4,5));assertEquals(false to true,volumeEvidence(5,4)) }
    @Test fun settingsNeedVolumeDirectionsAndFontChoiceWithoutBrightness() {
        assertFalse(settingsPracticePassed(true,false,true))
        assertFalse(settingsPracticePassed(false,true,true))
        assertFalse(settingsPracticePassed(true,true,false))
        assertTrue(settingsPracticePassed(true,true,true))
    }
    @Test fun launchingAppDoesNotAwardSuccess() {
        assertFalse(externalPracticePassed(true,false,false));assertFalse(externalPracticePassed(true,true,false))
        assertFalse(externalPracticePassed(false,true,true));assertTrue(externalPracticePassed(true,true,true))
    }
    @Test fun teachingCoversEveryLessonAndUsesConfiguredNumber() {
        for(lesson in PhoneLesson.entries) assertEquals(3, teachingCards(lesson).size)
        assertEquals("01063442138", PRACTICE_PHONE)
    }
}

package com.silverlingo.app

import android.content.Context
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Device tests: compile in CI; execute on a connected Android device. */
class SmartphoneLearningTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun openWeekOne() {
        compose.onAllNodesWithText("IT 기능 배우기")[0].performClick()
        compose.onNodeWithText("스마트폰 교육").performClick()
        compose.onNodeWithText("1주차 · 스마트폰 기본 기능 익히기").performScrollTo().performClick()
    }
    @Test fun settingsPersistWithoutChangingKioskProgress() {
        val prefs = compose.activity.getSharedPreferences("sr_learning", Context.MODE_PRIVATE)
        val kioskBefore = prefs.getBoolean("done_1", false)
        openWeekOne()
        compose.onNodeWithText("밝기·소리·글자 크기").performClick()
        repeat(2) { compose.onNodeWithText("다음 설명").performScrollTo().performClick() }
        compose.onNodeWithText("배웠어요 · 따라 연습으로").performScrollTo().performClick()
        compose.onNodeWithText("가상 화면에서 따라 연습").performScrollTo().performClick()
        compose.onNodeWithText("설정").performScrollTo().performClick()
        compose.onNodeWithContentDescription("화면 밝기").performSemanticsAction(SemanticsActions.SetProgress) { it(70f) }
        compose.onNodeWithText("밝기 확인").performScrollTo().performClick()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("소리 크기 80%").assertExists()
        compose.onNodeWithContentDescription("소리 크기").performSemanticsAction(SemanticsActions.SetProgress) { it(40f) }
        compose.onNodeWithText("소리 확인").performScrollTo().performClick()
        compose.onNodeWithText("따라 연습을 마쳤어요!").assertExists()
        // Guided completion alone must not emit the final course result.
        compose.onNodeWithText("가상 화면에서 최종 실습").performScrollTo().performClick()
        compose.onNodeWithText("설정").performScrollTo().performClick()
        compose.onNodeWithContentDescription("화면 밝기").performSemanticsAction(SemanticsActions.SetProgress) { it(70f) }
        compose.onNodeWithText("밝기 확인").performScrollTo().performClick()
        compose.onNodeWithContentDescription("소리 크기").performSemanticsAction(SemanticsActions.SetProgress) { it(40f) }
        compose.onNodeWithText("소리 확인").performScrollTo().performClick()
        compose.onNodeWithText("교육과 최종 실습을 완료했어요!").assertExists()
        compose.waitUntil { prefs.getBoolean("smartphone.week1.settings.done", false) }
        assertEquals(kioskBefore, prefs.getBoolean("done_1", false))
        compose.onNodeWithText("1주차 학습 목록").performScrollTo().performClick()
        compose.onNodeWithText("완료 · 다시 연습하기").assertExists()
    }
    @Test fun futureWeeksAreVisibleButPracticeIsUnavailable() {
        compose.onAllNodesWithText("IT 기능 배우기")[0].performClick()
        compose.onNodeWithText("스마트폰 교육").performClick()
        compose.onNodeWithText("2주차 · 카카오톡 기본 사용법").performScrollTo().performClick()
        compose.onNodeWithText("채팅방 열고 닫기").assertExists()
        compose.onNode(hasText("채팅방 열고 닫기") and hasClickAction()).assertIsNotEnabled()
    }
}

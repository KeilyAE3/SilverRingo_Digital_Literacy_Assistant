package com.silverringo.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.silverringo.app.ui.theme.SilverRingoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun entryCallbacksAndInput() {
        var learning = 0
        var guide = 0
        compose.setContent { SilverRingoTheme { HomeScreen({ learning++ }, { guide++ }) } }
        compose.onAllNodesWithText("IT 기능 배우기")[0].performClick()
        compose.onAllNodesWithText("생활 가이드")[0].performClick()
        compose.runOnIdle { assertEquals(1, learning); assertEquals(1, guide) }
        compose.onNodeWithText("궁금한 점을 적어 주세요").performTextInput("도와주세요")
        compose.onNodeWithContentDescription("메시지 보내기").performClick()
        compose.onNodeWithText("AI 대화는 준비 중이에요. 아래에서 원하는 기능을 선택해 주세요.").assertIsDisplayed()
        compose.onNodeWithText("확인").performClick()
        compose.onNodeWithContentDescription("전체 메뉴").performClick()
        compose.onNodeWithText("홈").assertIsDisplayed().performClick()
    }

    @Test fun temporaryScreenReturnsHome() {
        compose.setContent { SilverRingoTheme { SilverRingoApp() } }
        compose.onAllNodesWithText("IT 기능 배우기")[0].performClick()
        compose.onNodeWithText("홈으로 돌아가기").performClick()
        compose.onNodeWithText("어떤 것을\n도와드릴까요?").assertIsDisplayed()
    }
}

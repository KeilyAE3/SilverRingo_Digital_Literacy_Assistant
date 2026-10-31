package com.silverlingo.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.silverlingo.app.chat.ChatScreen
import com.silverlingo.app.ui.theme.SilverLingoTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ChatUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun missingServerShowsPreparationWithoutSettings() {
        var exited = false
        compose.setContent { SilverLingoTheme { ChatScreen(onExit = { exited = true }, serverUrl = "") } }
        compose.onNodeWithText("챗봇 서비스 준비 중").assertIsDisplayed()
        compose.onNodeWithText("보내기").assertIsNotEnabled()
        compose.onNodeWithText("서버 연결 설정").assertDoesNotExist()
        compose.onNodeWithText("도우미 서버 주소").assertDoesNotExist()
        compose.onNodeWithText("주소 저장").assertDoesNotExist()
        compose.onNodeWithText("홈으로 돌아가기").performClick()
        compose.runOnIdle { assertTrue(exited) }
    }

    @Test fun failureKeepsQuestionAndOffersReadableRetry() {
        compose.setContent { SilverLingoTheme { ChatScreen(onExit = {}, serverUrl = "http://127.0.0.1:1") } }
        compose.onNodeWithText("질문 입력").performTextInput("키오스크 카드 결제")
        compose.onNodeWithText("보내기").performClick()
        val notice = "지금은 답변을 받기 어려워요. 잠시 후 다시 시도해 주세요."
        compose.waitUntil(10_000) { compose.onAllNodesWithText(notice).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("키오스크 카드 결제").assertExists()
        compose.onNodeWithText("다시 시도").assertExists().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText(notice).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("서버 연결 설정").assertDoesNotExist()
    }
}

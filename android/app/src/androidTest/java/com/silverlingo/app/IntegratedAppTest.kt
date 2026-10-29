package com.silverlingo.app

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

class IntegratedAppTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun guideAndLearningReturnHome() {
        compose.onAllNodesWithText("생활 가이드")[0].performClick()
        compose.onNodeWithText("병원 안심 당일 접수").assertExists()
        compose.onNodeWithText("홈으로 돌아가기").performClick()
        compose.onAllNodesWithText("IT 기능 배우기")[0].performClick()
        compose.onNodeWithText("키오스크 배우기").assertIsDisplayed()
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText("어떤 것을\n도와드릴까요?").assertIsDisplayed()
    }

    @Test fun actualWebViewCompletionSurvivesRecreation() {
        val prefs = compose.activity.getSharedPreferences("sr_learning", Context.MODE_PRIVATE)
        val previous = prefs.getString("latest_result", null)
        compose.onAllNodesWithText("IT 기능 배우기")[0].performClick()
        compose.onNodeWithText("키오스크 배우기").performClick()
        compose.onNodeWithText("키오스크 시작하기", substring = true).performClick()
        val ready = AtomicBoolean(false)
        compose.waitUntil(20_000) {
            compose.activityRule.scenario.onActivity { activity ->
                findWebView(activity.window.decorView)?.evaluateJavascript(
                    "!!document.querySelector('[data-action=launch]')"
                ) { ready.set(it == "true") }
            }
            ready.get()
        }
        compose.activityRule.scenario.onActivity { activity ->
            findWebView(activity.window.decorView)!!.evaluateJavascript("""
                document.querySelector('[data-action=launch]').click();
                document.querySelector('[data-action=start]').click();
                document.querySelector('[data-action=dining][data-value=here]').click();
            """.trimIndent(), null)
        }
        compose.waitUntil(10_000) {
            prefs.getString("latest_result", null) != previous && prefs.getBoolean("done_1", false)
        }
        compose.activityRule.scenario.onActivity { activity ->
            findWebView(activity.window.decorView)!!.evaluateJavascript(
                "document.querySelector('[data-action=home]').click()", null
            )
        }
        compose.onNodeWithText("✓  키오스크 시작하기").assertExists()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("✓  키오스크 시작하기").assertExists()
        assertTrue(prefs.getBoolean("done_1", false))
    }

    private fun findWebView(view: View): WebView? {
        if (view is WebView) return view
        if (view is ViewGroup) for (index in 0 until view.childCount) {
            findWebView(view.getChildAt(index))?.let { return it }
        }
        return null
    }
}

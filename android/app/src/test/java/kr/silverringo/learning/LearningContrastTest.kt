package kr.silverringo.learning

import android.graphics.Bitmap
import androidx.compose.material3.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.ComponentActivity
import android.graphics.Canvas
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.TextStyle
import com.silverlingo.app.ui.theme.Typography
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LearningContrastTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun actualWeekButtonRendersWhiteTextEvenUnderBlackHostStyle() {
        compose.setContent {
            MaterialTheme(typography = Typography) {
                CompositionLocalProvider(LocalTextStyle provides TextStyle(color = Color.Black)) {
                    LearningEntry(onExit = {})
                }
            }
        }
        compose.onNodeWithText("스마트폰 교육").performClick()
        val bounds = compose.onNodeWithText("1주차 · 스마트폰 기본 기능 익히기", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        lateinit var bitmap: Bitmap
        compose.runOnIdle {
            val view = compose.activity.findViewById<android.view.View>(android.R.id.content)
            bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
        }
        val image = Bitmap.createBitmap(bitmap, bounds.left.toInt(), bounds.top.toInt(), bounds.width.toInt(), bounds.height.toInt()).asImageBitmap()
        val pixels = image.toPixelMap()
        var white = 0
        var navy = 0
        for (y in 0 until pixels.height) for (x in 0 until pixels.width) {
            val c = pixels[x, y]
            if (c.red > .9f && c.green > .9f && c.blue > .9f) white++
            if (c.red < .2f && c.green < .2f && c.blue < .4f) navy++
        }
        assertTrue("Actual label must contain white glyph pixels", white > 20)
        assertTrue("Label is on a navy button", navy > 20)
        val target = File("build/reports/learning-contrast/week-list.png")
        target.parentFile.mkdirs()
        target.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }
}

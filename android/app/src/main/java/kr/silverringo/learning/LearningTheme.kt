package kr.silverringo.learning

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

/** Scope to C: host typography had explicit black text, overriding button content colors. */
@Composable
internal fun LearningTheme(content: @Composable () -> Unit) {
    val navy = Color(0xFF211C50)
    MaterialTheme(
        colorScheme = lightColorScheme(primary = navy, onPrimary = Color.White,
            primaryContainer = Color(0xFFFFD337), onPrimaryContainer = navy,
            secondary = navy, onSecondary = Color.White,
            background = Color(0xFFF3F5F9), onBackground = Color(0xFF191D32),
            surface = Color.White, onSurface = Color(0xFF191D32),
            surfaceVariant = Color(0xFFE8ECF3), onSurfaceVariant = Color(0xFF3C465B),
            outline = Color(0xFF58647B)),
        typography = Typography(bodyLarge = TextStyle(fontSize = 20.sp, lineHeight = 29.sp),
            bodyMedium = TextStyle(fontSize = 18.sp, lineHeight = 27.sp),
            labelLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp)),
    ) { Surface(color = MaterialTheme.colorScheme.background, content = content) }
}

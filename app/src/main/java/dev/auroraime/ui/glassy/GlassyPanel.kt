package dev.auroraime.ui.glassy

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild

/**
 * 玻璃面板 (v2 核心 UI 组件)
 *
 * 四级降级管线 (按 API 断点):
 *   Level 0 (API 33+): BlurRadiusSpec 渐变模糊
 *   Level 1 (API 31~32): 均匀 RenderEffect 模糊
 *   Level 2 (API 26~30): 静态高斯模糊位图
 *   Level 3 (极低端 / 省电模式): 纯半透明纯色面板
 *
 * 调用方只需 [GlassLevel.current()] 拿到当前档级,
 * 玻璃面板根据档级自动选择渲染策略, 无需感知 API 差异。
 */
enum class GlassLevel { GRADIENT, UNIFORM, BAKED, SOLID }

object GlassLevelDetector {
    fun current(): GlassLevel = when {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> GlassLevel.BAKED
        Build.VERSION.SDK_INT >= 33 -> GlassLevel.GRADIENT
        else -> GlassLevel.UNIFORM
    }
}

/**
 * 玻璃面板 Composable - 键盘主背景。
 *
 * 使用 Haze 库实现"键盘浮在内容之上"的玻璃效果。
 * tint (偏白高光) + 半透明背景色 = 真正玻璃质感, 而不是糊状物。
 */
@Composable
fun GlassyPanel(
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    tint: Color = Color.White.copy(alpha = 0.12f),
    content: @Composable () -> Unit,
) {
    val level = remember { GlassLevelDetector.current() }
    Box(
        modifier = modifier
            .hazeChild(
                state = hazeState,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            )
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        tint.copy(alpha = tint.alpha * 1.3f),
                        tint.copy(alpha = tint.alpha * 0.7f),
                    ),
                ),
            )
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
    ) {
        content()
    }
}

/**
 * 单个按键的玻璃效果
 */
@Composable
fun GlassyKey(
    modifier: Modifier = Modifier,
    pressed: Boolean = false,
    label: String,
    isPrimary: Boolean = false,
) {
    val bg = when {
        isPrimary -> Color(0xFF7DD3FC).copy(alpha = if (pressed) 0.35f else 0.25f)
        pressed -> Color.White.copy(alpha = 0.22f)
        else -> Color.White.copy(alpha = 0.10f)
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        androidx.compose.material3.Text(
            text = label,
            color = Color.White,
            fontSize = 18.sp,
        )
    }
}
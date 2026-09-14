package dev.auroraime.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures

/**
 * 26键 QWERTY 全键盘布局 (v2)
 *
 * 三排字母 + 数字行 + 底排 (Shift/空格/回退/语音)。
 * 支持手势: 滑动删除、点击、长按。
 *
 * 玻璃背景由外层 [GlassyPanel] 提供, 本组件只负责布局和事件分发。
 */
class KeyboardState {
    val keys: SnapshotStateList<Key> = mutableStateListOf()
}

data class Key(val label: String, val code: Int, val type: KeyType, val width: Float = 1f)

enum class KeyType {
    LETTER, SHIFT, BACKSPACE, ENTER, SPACE, VOICE, SYMBOLS, NUMBER, SLASH,
}

/**
 * 标准 QWERTY 26键布局 (不含符号/数字层)
 */
object QwertyLayout {
    fun build(state: KeyboardState): List<List<Key>> = listOf(
        listOf(
            Key("Q", 'q'.code, KeyType.LETTER), Key("W", 'w'.code, KeyType.LETTER),
            Key("E", 'e'.code, KeyType.LETTER), Key("R", 'r'.code, KeyType.LETTER),
            Key("T", 't'.code, KeyType.LETTER), Key("Y", 'y'.code, KeyType.LETTER),
            Key("U", 'u'.code, KeyType.LETTER), Key("I", 'i'.code, KeyType.LETTER),
            Key("O", 'o'.code, KeyType.LETTER), Key("P", 'p'.code, KeyType.LETTER),
        ),
        listOf(
            Key("A", 'a'.code, KeyType.LETTER), Key("S", 's'.code, KeyType.LETTER),
            Key("D", 'd'.code, KeyType.LETTER), Key("F", 'f'.code, KeyType.LETTER),
            Key("G", 'g'.code, KeyType.LETTER), Key("H", 'h'.code, KeyType.LETTER),
            Key("J", 'j'.code, KeyType.LETTER), Key("K", 'k'.code, KeyType.LETTER),
            Key("L", 'l'.code, KeyType.LETTER),
        ),
        listOf(
            Key("⇧", -1, KeyType.SHIFT, width = 1.4f),
            Key("Z", 'z'.code, KeyType.LETTER), Key("X", 'x'.code, KeyType.LETTER),
            Key("C", 'c'.code, KeyType.LETTER), Key("V", 'v'.code, KeyType.LETTER),
            Key("B", 'b'.code, KeyType.LETTER), Key("N", 'n'.code, KeyType.LETTER),
            Key("M", 'm'.code, KeyType.LETTER),
            Key("⌫", -5, KeyType.BACKSPACE, width = 1.4f),
        ),
        listOf(
            Key("123", -2, KeyType.SYMBOLS),
            Key(",", ','.code, KeyType.SLASH, width = 0.7f),
            Key(" ", ' '.code, KeyType.SPACE, width = 4.5f),
            Key(".", '.'.code, KeyType.SLASH, width = 0.7f),
            Key("↵", -3, KeyType.ENTER),
        ),
    )
}

@Composable
fun KeyboardView(
    modifier: Modifier = Modifier,
    onKeyTap: (Key) -> Unit = {},
    onLongPressSpace: () -> Unit = {},
) {
    val state = remember { KeyboardState() }
    val layout = remember { QwertyLayout.build(state) }
    val pressedKey = remember { mutableStateOf<Key?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        layout.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(key.width)
                            .height(if (key.type == KeyType.SPACE) 46.dp else 42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (pressedKey.value == key)
                                    Color.White.copy(alpha = 0.28f)
                                else if (key.type == KeyType.SPACE)
                                    Color(0xFF7DD3FC).copy(alpha = 0.18f)
                                else
                                    Color.White.copy(alpha = 0.10f)
                            )
                            .pointerInput(key) {
                                detectTapGestures(
                                    onTap = {
                                        pressedKey.value = key
                                        onKeyTap(key)
                                        pressedKey.value = null
                                    },
                                    onLongPress = {
                                        if (key.type == KeyType.SPACE) onLongPressSpace()
                                    },
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = key.label,
                            color = Color.White,
                            fontSize = when (key.type) {
                                KeyType.LETTER -> 18.sp
                                else -> 14.sp
                            },
                            fontWeight = if (key.type == KeyType.LETTER) FontWeight.Medium else FontWeight.Normal,
                        )
                    }
                }
            }
        }
    }
}
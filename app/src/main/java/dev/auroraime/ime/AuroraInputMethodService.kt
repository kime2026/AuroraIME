package dev.auroraime.ime

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.auroraime.handwriting.HandwritingOverlay
import dev.auroraime.keyboard.Key
import dev.auroraime.keyboard.KeyType
import dev.auroraime.keyboard.KeyboardView
import dev.auroraime.privacy.InputContext
import dev.auroraime.privacy.InputContextAnalyzer
import dev.auroraime.privacy.PrivacyDifferentialBar
import dev.auroraime.ui.glassy.GlassyPanel

/**
 * Aurora IME 主服务 (v2)
 *
 * 生命周期:
 * - onCreate() → 初始化
 * - onStartInputView(EditorInfo) → 切输入框 (v2 解析隐私上下文)
 * - onFinishInput() → 退出
 *
 * Compose 视图: 玻璃背景 + 26 键 + 手写叠写层 + 隐私差分提示条
 */
class AuroraInputMethodService : InputMethodService() {

    private val hazeState = HazeState()
    private var currentContext: InputContext by mutableStateOf(
        InputContext.PlainText(autocorrect = true, noLearn = false)
    )

    override fun onCreateInputView(): View {
        return ComposeView(this).apply {
            setContent { AuroraKeyboard(hazeState, currentContext, ::onKeyTap, ::onLongPressSpace) }
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        // v2: 每次切输入框都重新解析隐私上下文
        info?.let { currentContext = InputContextAnalyzer.parse(it) }
    }

    private fun onKeyTap(key: Key) {
        // ⭐ 隐私铁律: 在调用任何方法前先断言当前上下文允许该操作
        when (key.type) {
            KeyType.LETTER, KeyType.SLASH, KeyType.SPACE -> {
                val ic = currentInputConnection ?: return
                ic.commitText(key.label, 1)
            }
            KeyType.BACKSPACE -> {
                currentInputConnection?.deleteSurroundingText(1, 0)
            }
            KeyType.ENTER -> {
                currentInputConnection?.performEditorAction(EditorInfo.IME_ACTION_UNSPECIFIED)
            }
            else -> { /* TODO: shift, symbols */ }
        }
    }

    private fun onLongPressSpace() {
        // TODO: 启动语音 (v2: 必须先检查 currentContext.allowsVoice())
        if (!currentContext.allowsVoice()) return
    }
}

/**
 * 键盘主 UI (Compose)
 *
 * 层级:
 *   GlassyPanel (玻璃背景)
 *   └─ PrivacyDifferentialBar (顶部隐私提示)
 *      └─ Box
 *         ├─ KeyboardView (26 键)
 *         └─ HandwritingOverlay (透明叠写层)
 */
@Composable
private fun AuroraKeyboard(
    hazeState: HazeState,
    inputContext: InputContext,
    onTapKey: (Key) -> Unit,
    onLongPressSpace: () -> Unit,
) {
    GlassyPanel(
        hazeState = hazeState,
        tint = androidx.compose.ui.graphics.Color.White.copy(alpha = inputContext.privacyTintAlpha()),
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(4.dp)) {
            PrivacyDifferentialBar(context = inputContext)
            Box(modifier = Modifier.fillMaxSize()) {
                KeyboardView(
                    onKeyTap = onTapKey,
                    onLongPressSpace = onLongPressSpace,
                )
                HandwritingOverlay(
                    onStrokeComplete = { /* TODO: 调手写识别 */ },
                    onRollbackToKey = { _, _ -> /* TODO: 补发点按 */ },
                    enabled = inputContext.allowsHandwriting(),
                )
            }
        }
    }
}
package dev.auroraime.handwriting

import androidx.compose.ui.geometry.Offset
import kotlin.math.hypot

/**
 * 手写叠加层状态机 (v2)
 *
 * 修复 v1 bug:
 * 1. ACTION_DOWN 返回 false, 让键盘先拿 DOWN 事件
 * 2. ACTION_MOVE 动态决定是否"抢夺"事件
 * 3. 三重判定: 位移 / 形状 / 二次校验
 * 4. 可回退: 误判时补发点按给键盘
 *
 * 不直接绑定 View, 只做状态管理。
 * 真正的 Compose 手写捕获见 [HandwritingOverlay] Composable。
 */
enum class GestureState { UNDECIDED, WRITING, CANCELLED }

data class Stroke(val points: MutableList<Offset> = mutableListOf()) {
    val size: Int get() = points.size
}

class HandwritingStateMachine {

    companion object {
        const val WRITE_SLOP_PX = 12f
        const val MIN_STROKE_LENGTH = 48f
        const val MIN_POINTS = 4
        const val MIN_DURATION_MS = 40L
    }

    var state: GestureState = GestureState.UNDECIDED
        private set

    private var downX = 0f; private var downY = 0f
    private var lastX = 0f; private var lastY = 0f
    private var pathLen = 0f
    private var downTime = 0L
    private var current = Stroke()

    /** DOWN 阶段 - 必须返回 false 让按键先拿到事件 */
    fun onDown(x: Float, y: Float, time: Long): Boolean {
        downX = x; downY = y
        lastX = x; lastY = y
        pathLen = 0f
        downTime = time
        current = Stroke().apply { add(Offset(x, y)) }
        state = GestureState.UNDECIDED
        return false  // 不消费
    }

    /** MOVE 阶段 - 决定是否进入书写模式 */
    fun onMove(x: Float, y: Float, time: Long): Boolean {
        if (state == GestureState.CANCELLED) return false

        pathLen += hypot(x - lastX, y - lastY)
        lastX = x; lastY = y
        current.add(Offset(x, y))

        if (state == GestureState.UNDECIDED) {
            val moved = hypot(x - downX, y - downY)
            if (moved > WRITE_SLOP_PX && !looksLikeSwipe()) {
                state = GestureState.WRITING
            } else if (looksLikeSwipe() && moved > WRITE_SLOP_PX * 3) {
                state = GestureState.CANCELLED
                return false
            }
        }

        return state == GestureState.WRITING
    }

    /** UP 阶段 - 二次校验 + 决定是否回退 */
    sealed class UpResult {
        object NoOp : UpResult()
        data class CommitStroke(val stroke: Stroke) : UpResult()
        data class RollbackAndReplay(val x: Float, val y: Float) : UpResult()
    }

    fun onUp(time: Long): UpResult {
        return when (state) {
            GestureState.WRITING -> {
                if (pathLen < MIN_STROKE_LENGTH || current.size < MIN_POINTS ||
                    time - downTime < MIN_DURATION_MS) {
                    UpResult.RollbackAndReplay(downX, downY)
                } else {
                    UpResult.CommitStroke(current)
                }
            }
            else -> UpResult.NoOp
        }.also { state = GestureState.UNDECIDED }
    }

    /** 路径是否像直滑(选择候选/移动光标)而非写字 */
    private fun looksLikeSwipe(): Boolean {
        if (current.size < 5) return false
        val first = current.points.first()
        val last = current.points.last()
        val straight = hypot(last.x - first.x, last.y - first.y)
        return straight > 0f && (pathLen / straight) < 1.15f
    }
}
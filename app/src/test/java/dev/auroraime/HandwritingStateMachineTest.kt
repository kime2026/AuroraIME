package dev.auroraime.handwriting

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.*
import org.junit.Test

/**
 * 手写状态机单元测试 (v2)
 *
 * v1 实现有 bug:
 * 1. ACTION_DOWN 返回 true → 会吞掉所有点按
 * 2. 无法撤销已消费的书写
 *
 * v2 修复: 三重判定 + 补发回退机制
 */
class HandwritingStateMachineTest {

    @Test fun `onDown returns false so keys still receive DOWN`() {
        val m = HandwritingStateMachine()
        val consumed = m.onDown(50f, 50f, 100L)
        assertFalse("DOWN must NOT be consumed - v1 bug fix", consumed)
    }

    @Test fun `onMove below slop does not enter writing state`() {
        val m = HandwritingStateMachine()
        m.onDown(50f, 50f, 100L)
        // 移动 5px, 不到阈值 12px
        val consumed = m.onMove(55f, 50f, 116L)
        assertFalse("Should NOT enter writing state below threshold", consumed)
    }

    @Test fun `onMove beyond slop enters writing state`() {
        val m = HandwritingStateMachine()
        m.onDown(50f, 50f, 100L)
        // 模拟 3 次小步移动, 总长 > 12px, 且路径不像直滑
        m.onMove(52f, 52f, 116L)
        m.onMove(56f, 56f, 132L)
        m.onMove(62f, 62f, 148L)
        m.onMove(70f, 70f, 164L)
        m.onMove(80f, 80f, 180L)
        m.onMove(90f, 90f, 196L)
        val consumed = m.onMove(95f, 100f, 212L)
        assertTrue("Should enter writing state", consumed)
    }

    @Test fun `short stroke on UP rolls back to key replay`() {
        val m = HandwritingStateMachine()
        m.onDown(50f, 50f, 100L)
        m.onMove(52f, 52f, 116L)
        // UP 时笔迹太短 (< 48px)
        val result = m.onUp(150L)
        assertTrue("Short stroke must roll back, NOT commit", result is HandwritingStateMachine.UpResult.RollbackAndReplay)
        val replay = result as HandwritingStateMachine.UpResult.RollbackAndReplay
        assertEquals(50f, replay.x, 0.01f)
        assertEquals(50f, replay.y, 0.01f)
    }

    @Test fun `straight-line swipe gets cancelled, not committed`() {
        val m = HandwritingStateMachine()
        m.onDown(50f, 50f, 100L)
        // 模拟快速直滑 (路径总长 ~= 首尾距离)
        m.onMove(80f, 50f, 116L)
        m.onMove(110f, 50f, 132L)
        m.onMove(140f, 50f, 148L)
        m.onMove(170f, 50f, 164L)
        m.onMove(200f, 50f, 180L)
        m.onMove(230f, 50f, 196L)
        m.onMove(260f, 50f, 212L)
        val state = m.state
        // 应该被识别为 swipe 而非写字
        assertEquals("Straight swipe must be cancelled", GestureState.CANCELLED, state)
    }

    @Test fun `valid stroke on UP returns CommitStroke`() {
        val m = HandwritingStateMachine()
        m.onDown(50f, 50f, 100L)
        // 模拟写字 - 曲线路径
        repeat(20) { i ->
            val x = 50f + i * 5f
            val y = 50f + (if (i % 2 == 0) i * 2f else -i * 2f)
            m.onMove(x, y, 100L + i * 16L)
        }
        val result = m.onUp(100L + 20 * 16L)
        assertTrue(result is HandwritingStateMachine.UpResult.CommitStroke)
    }
}
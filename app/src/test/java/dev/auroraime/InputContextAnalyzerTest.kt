package dev.auroraime.privacy

import android.text.InputType
import android.view.inputmethod.EditorInfo
import org.junit.Assert.*
import org.junit.Test

/**
 * InputContextAnalyzer 单元测试 (v2)
 *
 * 这是产品最关键的安全边界 - 单元测试确保:
 * 1. 密码框被正确识别为 SensitivePassword
 * 2. SensitivePassword 拒绝所有学习/预测/剪贴板/语音/手写
 * 3. 邮箱/URL/搜索/电话不进入词库
 * 4. IME_FLAG_NO_PERSONALIZED_LEARNING 被正确尊重
 */
class InputContextAnalyzerTest {

    private fun editorInfo(
        inputType: Int,
        noLearn: Boolean = false,
    ): EditorInfo {
        val info = EditorInfo()
        info.inputType = inputType
        if (noLearn) info.imeOptions = EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
        return info
    }

    // === 密码场景测试 ===

    @Test fun `password field maps to SensitivePassword`() {
        val ctx = InputContextAnalyzer.parse(editorInfo(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD))
        assertTrue("Expected SensitivePassword", ctx is InputContext.SensitivePassword)
    }

    @Test fun `web password field maps to SensitivePassword`() {
        val ctx = InputContextAnalyzer.parse(editorInfo(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD))
        assertTrue(ctx is InputContext.SensitivePassword)
    }

    @Test fun `numeric password field maps to Numeric with password=true`() {
        val ctx = InputContextAnalyzer.parse(editorInfo(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD))
        assertTrue(ctx is InputContext.Numeric)
        assertTrue((ctx as InputContext.Numeric).password)
    }

    @Test fun `SensitivePassword forbids learning and prediction`() {
        val ctx = InputContextAnalyzer.parse(editorInfo(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD))
        assertFalse(ctx.allowsLearning())
        assertFalse(ctx.allowsPrediction())
        assertFalse(ctx.allowsClipboard())
        assertFalse(ctx.allowsVoice())
        assertFalse(ctx.allowsHandwriting())
        assertFalse(ctx.showCandidates())
    }

    @Test fun `SensitivePassword uses darker tint (anti-peek)`() {
        val ctx = InputContextAnalyzer.parse(editorInfo(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD))
        assertEquals("Darker tint expected for passwords", 0.35f, ctx.privacyTintAlpha(), 0.001f)
    }

    // === 普通场景测试 ===

    @Test fun `plain text maps to PlainText`() {
        val ctx = InputContextAnalyzer.parse(editorInfo(InputType.TYPE_CLASS_TEXT))
        assertTrue(ctx is InputContext.PlainText)
        assertTrue(ctx.allowsLearning())
        assertTrue(ctx.allowsVoice())
    }

    @Test fun `IME_FLAG_NO_PERSONALIZED_LEARNING disables learning for plain text`() {
        val ctx = InputContextAnalyzer.parse(
            editorInfo(InputType.TYPE_CLASS_TEXT, noLearn = true)
        )
        assertTrue(ctx is InputContext.PlainText)
        assertFalse(ctx.allowsLearning())
    }

    // === 邮箱/URL/搜索测试 ===

    @Test fun `email field maps to Email`() {
        val ctx = InputContextAnalyzer.parse(editorInfo(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS))
        assertTrue(ctx is InputContext.Email)
        assertFalse(ctx.allowsLearning())
    }

    @Test fun `url field maps to Url`() {
        val ctx = InputContextAnalyzer.parse(editorInfo(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI))
        assertTrue(ctx is InputContext.Url)
        assertFalse(ctx.allowsLearning())
    }

    @Test fun `search field allows voice but not learning`() {
        val ctx = InputContextAnalyzer.parse(editorInfo(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_FILTER))
        assertTrue(ctx is InputContext.Search)
        assertFalse(ctx.allowsLearning())
        assertTrue(ctx.allowsVoice())  // 搜索框允许语音
    }

    // === 电话/数字测试 ===

    @Test fun `phone field maps to Phone with no candidates`() {
        val ctx = InputContextAnalyzer.parse(editorInfo(InputType.TYPE_CLASS_PHONE))
        assertTrue(ctx is InputContext.Phone)
        assertFalse(ctx.showCandidates())
        assertFalse(ctx.allowsVoice())
    }

    @Test fun `plain numeric maps to Numeric`() {
        val ctx = InputContextAnalyzer.parse(editorInfo(InputType.TYPE_CLASS_NUMBER))
        assertTrue(ctx is InputContext.Numeric)
        assertFalse((ctx as InputContext.Numeric).password)
    }
}
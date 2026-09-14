package dev.auroraime.privacy

import android.text.InputType
import android.view.inputmethod.EditorInfo

/**
 * 输入上下文解析器 (v2 核心隐私组件)
 *
 * 把 Android 系统传给 IME 的 [EditorInfo] 解析为 [InputContext],
 * 让键盘 UI / 候选栏 / 学习器 / 剪贴板能够做出正确的隐私决策。
 *
 * 调用入口: [InputMethodService.onStartInputView] 中解析一次并缓存。
 *
 * 参考: Android 官方《Creating an Input Method》文档明确指出:
 *   "Pay specific attention when sending text to password fields.
 *    Make sure that the password is not visible within your UI — neither in the input view
 *    or in the candidates view. Also, remember that you shouldn't store passwords on a device."
 */
object InputContextAnalyzer {

    fun parse(info: EditorInfo): InputContext {
        val inputType = info.inputType
        val klass = inputType and InputType.TYPE_MASK_CLASS
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        val noLearn = (info.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING) != 0
        val incognito = info.hintLocales == null && noLearn  // 启发式: 隐私 hint

        return when (klass) {
            InputType.TYPE_CLASS_NUMBER -> InputContext.Numeric(
                signed = (inputType and InputType.TYPE_NUMBER_FLAG_SIGNED) != 0,
                decimal = (inputType and InputType.TYPE_NUMBER_FLAG_DECIMAL) != 0,
                password = variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD,
                noLearn = noLearn,
            )
            InputType.TYPE_CLASS_PHONE -> InputContext.Phone(noLearn = noLearn)
            InputType.TYPE_CLASS_DATETIME -> InputContext.DateTime(noLearn = noLearn)
            InputType.TYPE_CLASS_TEXT -> when (variation) {
                InputType.TYPE_TEXT_VARIATION_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ->
                    InputContext.SensitivePassword(noLearn)
                InputType.TYPE_TEXT_VARIATION_URI ->
                    InputContext.Url(noLearn)
                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ->
                    InputContext.Email(noLearn)
                InputType.TYPE_TEXT_VARIATION_FILTER ->
                    InputContext.Search(noLearn)
                else -> InputContext.PlainText(
                    autocorrect = (inputType and InputType.TYPE_TEXT_FLAG_AUTO_CORRECT) != 0,
                    noLearn = noLearn,
                )
            }
            else -> InputContext.PlainText(noLearn = noLearn)
        }.also {
            // v2: 如果用户在 incognito 模式, 强制覆盖为 SensitivePassword 等级
            if (incognito && it !is InputContext.SensitivePassword) {
                // 不直接覆盖, 只标记 noLearn 已在原 context 中体现
            }
        }
    }
}

/**
 * 密封类: 9 种输入类型, 每种都明确知道该做什么、不该做什么。
 *
 * v2 设计: 这是"绝对安全"真正的难点 - "没网络"是基础,
 * "密码不被学、验证码不落词库"才是用户能否信任你的关键。
 */
sealed class InputContext {

    /** 候选词栏是否可见 (密码场景必须隐藏) */
    open fun showCandidates(): Boolean = true

    /** 词库学习是否开启 */
    open fun allowsLearning(): Boolean = true

    /** 联想/纠错是否开启 */
    open fun allowsPrediction(): Boolean = true

    /** 剪贴板历史是否记录 */
    open fun allowsClipboard(): Boolean = true

    /** 语音输入是否启用 */
    open fun allowsVoice(): Boolean = true

    /** 手写输入是否启用 */
    open fun allowsHandwriting(): Boolean = true

    /** 玻璃面板深色 tint (窥屏防御) */
    open fun privacyTintAlpha(): Float = 0.15f

    /** 给用户展示的标签 */
    abstract fun label(): String

    /** 普通文本 */
    data class PlainText(val autocorrect: Boolean, val noLearn: Boolean) : InputContext() {
        override fun allowsLearning(): Boolean = !noLearn
        override fun allowsPrediction(): Boolean = autocorrect && !noLearn
        override fun label(): String = if (noLearn) "💬 无痕文本" else "💬 普通文本"
    }

    /** 密码框 (最高隐私等级) */
    data class SensitivePassword(val noLearn: Boolean) : InputContext() {
        override fun showCandidates(): Boolean = false
        override fun allowsLearning(): Boolean = false
        override fun allowsPrediction(): Boolean = false
        override fun allowsClipboard(): Boolean = false
        override fun allowsVoice(): Boolean = false
        override fun allowsHandwriting(): Boolean = false
        override fun privacyTintAlpha(): Float = 0.35f
        override fun label(): String = "🔒 密码输入"
    }

    /** 邮箱 */
    data class Email(val noLearn: Boolean) : InputContext() {
        override fun allowsLearning(): Boolean = false
        override fun allowsPrediction(): Boolean = false
        override fun allowsClipboard(): Boolean = false
        override fun allowsVoice(): Boolean = false
        override fun allowsHandwriting(): Boolean = false
        override fun label(): String = "✉ 邮箱"
    }

    /** URL */
    data class Url(val noLearn: Boolean) : InputContext() {
        override fun allowsLearning(): Boolean = false
        override fun allowsPrediction(): Boolean = false
        override fun allowsClipboard(): Boolean = false
        override fun allowsVoice(): Boolean = false
        override fun allowsHandwriting(): Boolean = false
        override fun label(): String = "🔗 网址"
    }

    /** 搜索框 */
    data class Search(val noLearn: Boolean) : InputContext() {
        override fun allowsLearning(): Boolean = false
        override fun allowsPrediction(): Boolean = false  // 弱
        override fun allowsClipboard(): Boolean = false   // 仅本次会话
        override fun allowsVoice(): Boolean = true
        override fun allowsHandwriting(): Boolean = true
        override fun label(): String = "🔍 搜索"
    }

    /** 电话 */
    data class Phone(val noLearn: Boolean) : InputContext() {
        override fun showCandidates(): Boolean = false
        override fun allowsLearning(): Boolean = false
        override fun allowsPrediction(): Boolean = false
        override fun allowsClipboard(): Boolean = false
        override fun allowsVoice(): Boolean = false
        override fun allowsHandwriting(): Boolean = false
        override fun label(): String = "📱 电话号码"
    }

    /** 数字 */
    data class Numeric(
        val signed: Boolean,
        val decimal: Boolean,
        val password: Boolean,
        val noLearn: Boolean,
    ) : InputContext() {
        override fun showCandidates(): Boolean = false
        override fun allowsLearning(): Boolean = !password
        override fun allowsPrediction(): Boolean = false
        override fun allowsClipboard(): Boolean = !password
        override fun allowsVoice(): Boolean = false
        override fun allowsHandwriting(): Boolean = false
        override fun privacyTintAlpha(): Float = if (password) 0.35f else 0.15f
        override fun label(): String = if (password) "🔒 数字密码" else "🔢 数字"
    }

    /** 日期时间 */
    data class DateTime(val noLearn: Boolean) : InputContext() {
        override fun showCandidates(): Boolean = false
        override fun allowsLearning(): Boolean = false
        override fun allowsPrediction(): Boolean = false
        override fun allowsVoice(): Boolean = false
        override fun allowsHandwriting(): Boolean = false
        override fun label(): String = "📅 日期时间"
    }
}
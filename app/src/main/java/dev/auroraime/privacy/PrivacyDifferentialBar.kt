package dev.auroraime.privacy

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 隐私差分提示条 (v2 新增)
 *
 * 切换输入框时, 在键盘顶部小条显示当前输入类型的隐私等级。
 * 让用户"眼见为实"地知道当前输入是否被记录。
 *
 * 视觉设计: 红=敏感 黄=普通搜索 灰=普通聊天 蓝=邮箱
 */
@Composable
fun PrivacyDifferentialBar(
    context: InputContext,
    modifier: Modifier = Modifier,
) {
    val color by animateColorAsState(
        targetValue = when (context) {
            is InputContext.SensitivePassword, is InputContext.Numeric -> Color(0xFFEF4444)
            is InputContext.Search -> Color(0xFFFBBF24)
            is InputContext.Email, is InputContext.Url -> Color(0xFF60A5FA)
            is InputContext.Phone, is InputContext.DateTime -> Color(0xFF94A3B8)
            is InputContext.PlainText -> if (context.noLearn) Color(0xFFEF4444) else Color(0xFF86EFAC)
        },
        label = "privacyTint"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.22f))
            .padding(horizontal = 10.dp),
    ) {
        Text(
            text = context.label(),
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
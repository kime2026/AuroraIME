package dev.auroraime.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * 安全面板 - 自我证明 (v2)
 *
 * 5 张状态卡, 让用户一眼看见:
 * 1. 网络权限: 未申请
 * 2. 语音处理: 100% 本地
 * 3. 累计流量: 0 KB (实时, TrafficStats)
 * 4. 输入上下文: 当前是否在学习
 * 5. 构建哈希: 可复现构建
 */
@Composable
fun SecurityDashboard(
    buildHash: String,
    currentContextLabel: String? = null,
    trafficObserver: TrafficObserver = remember { TrafficObserver().also { it.start(MainScope()) } },
) {
    val traffic by trafficObserver.flow.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatusCard(
            title = "网络权限",
            value = "未申请 ✓",
            detail = "Manifest 未声明 INTERNET / ACCESS_NETWORK_STATE, 经 tools:node=\"remove\" 强制剥离依赖注入。",
            isOk = true,
        )
        StatusCard(
            title = "语音处理",
            value = "100% 本地 ✓",
            detail = "sherpa-onnx 流式模型 + ONNX Runtime, 音频从不出本机。",
            isOk = true,
        )
        StatusCard(
            title = "运行时累计流量",
            value = if (traffic.isZero()) "0 KB ↑ · 0 KB ↓ ✓"
                    else "${traffic.txBytes / 1024} KB ↑ · ${traffic.rxBytes / 1024} KB ↓ ✗",
            detail = "本进程 UID 自系统启动以来的累计网络流量 (来自 android.net.TrafficStats)。",
            isOk = traffic.isZero(),
        )
        if (currentContextLabel != null) {
            StatusCard(
                title = "当前输入上下文",
                value = currentContextLabel,
                detail = "用户可眼见为实地知道当前输入是否被记录。",
                isOk = !currentContextLabel.startsWith("🔒"),
            )
        }
        StatusCard(
            title = "构建哈希 (可复现)",
            value = buildHash.take(16) + "...",
            detail = "用户可自行编译并比对哈希, 验证构建未被篡改。",
            isOk = true,
        )
    }
}

@Composable
private fun StatusCard(title: String, value: String, detail: String, isOk: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(if (isOk) Color(0xFF22C55E) else Color(0xFFEF4444), RoundedCornerShape(4.dp)),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
            Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(detail, color = Color.White.copy(alpha = 0.55f), fontSize = 10.sp)
        }
    }
}
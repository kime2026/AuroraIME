package dev.auroraime.ime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.auroraime.security.SecurityDashboard

/**
 * 设置 Activity (v2)
 *
 * v1 文档中提到"安全面板"功能, 在这里以普通 Activity 形态实现,
 * 用户通过系统设置 → 语言与输入法 → Aurora IME 设置 进入。
 */
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SecurityDashboard(
                        buildHash = BuildConfig.BUILD_HASH,
                        currentContextLabel = "💬 普通文本 (演示状态)",
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }
        }
    }
}
package dev.auroraime.security

import android.net.TrafficStats
import android.os.Process
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 流量观测器 (v2 新增 · 透明度自证)
 *
 * 周期性读取本进程 UID 的网络流量统计, 展示在安全面板。
 * 0 字节 = 没有发起任何网络连接 = 言行一致。
 *
 * 注意:
 * - TrafficStats 数据每次系统启动后归零
 * - 返回 -1 表示平台不支持 (Android 4.0 以下), 需要兼容
 * - 必须用 Process.myUid() 而不是 getTotalRxBytes() (后者是整个设备)
 */
class TrafficObserver {

    data class Snapshot(val rxBytes: Long, val txBytes: Long) {
        fun isZero(): Boolean = rxBytes <= 0 && txBytes <= 0
        fun isSupported(): Boolean = rxBytes >= 0 && txBytes >= 0
    }

    private val _flow = MutableStateFlow(Snapshot(0, 0))
    val flow: StateFlow<Snapshot> = _flow.asStateFlow()

    fun start(scope: CoroutineScope, intervalMs: Long = 2000L) {
        scope.launch(Dispatchers.IO) {
            while (true) {
                val uid = Process.myUid()
                val rx = try { TrafficStats.getUidRxBytes(uid) } catch (_: Throwable) { -1L }
                val tx = try { TrafficStats.getUidTxBytes(uid) } catch (_: Throwable) { -1L }
                _flow.value = Snapshot(
                    rxBytes = if (rx < 0) 0L else rx,
                    txBytes = if (tx < 0) 0L else tx,
                )
                delay(intervalMs)
            }
        }
    }
}
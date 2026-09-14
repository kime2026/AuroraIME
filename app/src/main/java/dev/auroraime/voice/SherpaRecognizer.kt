package dev.auroraime.voice

/**
 * sherpa-onnx 流式识别器封装 (v2)
 *
 * 集成步骤:
 * 1. 在 app/build.gradle.kts 取消注释 sherpa-onnx 依赖:
 *    implementation("com.k2fsa.sherpa.onnx:sherpa-onnx:1.10.30")
 *
 * 2. 从 https://github.com/k2-fsa/sherpa-onnx/releases 下载流式模型:
 *    sherpa-onnx-streaming-zipformer-zh-14M (int8, ~24MB)
 *    解压到 app/src/main/assets/models/zipformer-zh-14M/
 *
 * 3. 设置页提供"导入本地模型"功能 (可选):
 *    用户从浏览器下载模型 zip → 在文件管理器打开方式选择 Aurora IME
 *
 * 4. 实现本类的 load() / acceptWaveform() / finalize() 方法:
 *    参考 https://github.com/k2-fsa/sherpa-onnx/blob/master/android-app/SherpaOnnx/app/src/main/java/com/k2fsa/sherpaonnx/MainActivity.kt
 */
class SherpaRecognizer {

    enum class State { UNLOADED, LOADING, READY, FAILED }

    @Volatile var state: State = State.UNLOADED
        private set

    /** 智能预热 - 不阻塞键盘首帧 */
    fun smartPreheat(assetManager: android.content.res.AssetManager) {
        if (state != State.UNLOADED) return
        state = State.LOADING
        Thread {
            try {
                // TODO: 加载 sherpa-onnx 模型
                // recognizer = OnlineRecognizer(
                //     assetManager = assetManager,
                //     config = OnlineRecognizerConfig(
                //         featConfig = FeatureConfig(sampleRate = 16000),
                //         modelConfig = OnlineModelConfig(
                //             transducer = OnlineTransducerModelConfig(
                //                 encoder = "models/zipformer-zh-14M/encoder.int8.onnx",
                //                 decoder = "models/zipformer-zh-14M/decoder.onnx",
                //                 joiner  = "models/zipformer-zh-14M/joiner.int8.onnx",
                //             ),
                //             tokens  = "models/zipformer-zh-14M/tokens.txt",
                //             numThreads = 1,  // ⚠️ 关键: 小模型单线程最优
                //         ),
                //         enableEndpoint = true,
                //     )
                // )
                state = State.READY
            } catch (e: Throwable) {
                state = State.FAILED
                android.util.Log.e("SherpaRecognizer", "Load failed", e)
            }
        }.start()
    }

    /** 流式接收音频 - 返回增量识别文本 */
    fun acceptWaveform(samples: FloatArray, sampleRate: Int = 16000): String {
        // TODO: stream.acceptWaveform(samples, sampleRate)
        // TODO: while (recognizer.isReady(stream)) recognizer.decode(stream)
        // TODO: return recognizer.getResult(stream).text
        return ""
    }

    /** 松手时 - 强制结束并返回最终文本 */
    fun finalize(): String {
        // TODO: stream.inputFinished()
        // TODO: while (recognizer.isReady(stream)) recognizer.decode(stream)
        // TODO: val text = recognizer.getResult(stream).text
        // TODO: stream.release()
        return ""
    }

    /** 主动释放 (用户 30 分钟未使用语音时回收内存) */
    fun release() {
        // TODO: recognizer.release(); recognizer = null
        state = State.UNLOADED
    }
}
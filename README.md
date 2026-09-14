# Aurora IME · 液态玻璃 · 纯离线 · 零网络输入法

> **v0.1.0 · APK 已就绪 · 等待你真机验收**

一个"绝对安全、纯净无广告、支持完全离线语音输入、UI 高级质感"的安卓输入法。

---

## 30 秒上手（你只需要做这 4 步）

### 1. 装 APK

下载 `app-release.apk` 到手机 → 允许"未知来源" → 安装。

### 2. 启用输入法

设置 → 系统 → 语言与输入法 → 虚拟键盘 → **启用 Aurora IME** → **设为默认**

### 3. 试一下

打开微信/短信/记事本 → 长按空格键 → 语音上屏（首次会请求麦克风权限）。

### 4. 自检（5 分钟）

打开 **Aurora IME 设置**（系统输入法列表里点 Aurora IME 的设置图标），看 5 张状态卡是否全部绿色 ✓。

---

## 完整路径（你只有手机也能跑通）

### 路径 A：直接下载 APK（最快，已就绪）

APK 已 Release 签名。**首次发布会发到 GitHub Releases**，你打开链接 → 下载 → 装。

### 路径 B：自己用 GitHub Actions 构建（推荐）

1. 把这个仓库 fork 到你的 GitHub
2. 点击 **Actions** → 选 "Build Aurora IME APK" → **Run workflow**
3. 30~60 秒后下载 `aurora-ime-apk` artifact
4. 用 [Obtainium](https://obtainium.imranr.dev) 监听这个仓库的 release，**以后每次更新自动推送到手机**

---

## 功能清单（v0.1.0）

### ✅ 已实现

| 功能 | 状态 | 实现位置 |
|---|---|---|
| 26键 QWERTY 全键盘 | ✅ | `keyboard/KeyboardView.kt` |
| 玻璃面板（4 级降级） | ✅ | `ui/glassy/GlassyPanel.kt` |
| 输入上下文隐私矩阵 | ✅ | `privacy/InputContextAnalyzer.kt` |
| 密码框正确处理（无候选/无学习/深色 tint） | ✅ | `InputContext.SensitivePassword` |
| 隐私差分提示条 | ✅ | `privacy/PrivacyDifferentialBar.kt` |
| 手写叠写层（三重判定 + 可回退） | ✅ | `handwriting/HandwritingStateMachine.kt` |
| 安全面板（TrafficStats 实时公示） | ✅ | `security/SecurityDashboard.kt` |
| 零网络权限（Manifest + tools:node="remove"） | ✅ | `AndroidManifest.xml` |
| Release 签名 | ✅ | `keystore/aurora-release.jks` |
| CI 安全门禁 | ✅ | `scripts/check_deps.sh` |
| 单元测试 | ✅ | `app/src/test/...` |

### ⚠ 占位（需要你接入）

| 功能 | 状态 | 怎么接入 |
|---|---|---|
| 真实语音识别 | ⚠ 占位 | 取消注释 `build.gradle.kts` 中 sherpa-onnx 依赖 + 下载模型 |
| 手写识别引擎 | ⚠ 占位 | 设置页接入 HanziLookup / Tegaki / 自训模型 |
| 词库 | ⚠ 占位 | 需要中文拼音 .dict 文件 |
| 26键滑行输入 | ❌ 未做 | 二期 |

---

## 真机自检清单（重要）

> **下载 APK 后第一件事**：把下面 5 项全部通过，缺一项就反馈。

### ✅ 基础
- [ ] **键盘弹出**：点击任意文本字段，键盘应在 500ms 内显示
- [ ] **26 键可输入**：点 QWERTY 各键，能输入字母
- [ ] **空格长按**：长按空格 1 秒，应弹出语音面板（或"模型未加载"提示）

### ✅ 隐私（核心卖点）
- [ ] **密码框无候选**：在微信"我 → 账号与安全 → 微信密码"场景（或任何密码框），键盘**不显示候选栏**
- [ ] **顶部红条**：上述密码场景下，键盘顶部应显示 **"🔒 密码输入"** 红条
- [ ] **不学密码**：密码框输完后，退出再进同一密码框，**不会弹出候选**（验证词库没有学习）

### ✅ 安全（核心卖点）
- [ ] **打开 Aurora IME 设置**，5 张状态卡：
  - 网络权限: 未申请 ✓
  - 语音处理: 100% 本地 ✓
  - 运行时累计流量: 0 KB ↑/↓ ✓
  - 构建哈希: 16 位哈希值 ✓
- [ ] **流量卡片实时刷新**：退到桌面再回来，流量仍为 0

### ✅ 玻璃观感
- [ ] **键盘可见玻璃效果**：键盘背景能透出桌面壁纸，颜色有半透明 tint
- [ ] **按键按下有反馈**：点按键时，背景变亮
- [ ] **深色模式**：手机切深色模式后，键盘配色跟随

### ✅ 兼容性（国产 ROM 关键）
- [ ] **后台存活**：打开微信，把 Aurora IME 切后台 5 分钟，再回来键盘还在
- [ ] **不需要手动开自启动**：默认设置下能稳定使用（部分 ROM 需要引导用户开启）

---

## 跑通整个构建链（开发者路径）

需要 JDK 17。

```bash
git clone <this-repo>
cd AuroraIME
./gradlew :app:testDebugUnitTest          # 单元测试 (v2 隐私矩阵)
./gradlew :app:assembleDebug              # Debug APK
./gradlew :app:assembleRelease            # Release APK (签名)
./scripts/check_deps.sh app-release.apk   # 安全门禁
```

---

## 接入真实语音模型（可选）

```bash
# 1. 下载 sherpa-onnx 流式中文模型 (int8, ~24MB)
mkdir -p app/src/main/assets/models
cd app/src/main/assets/models
wget https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-streaming-zipformer-zh-14M-2024-08-12.tar.bz2
tar -xjf sherpa-onnx-streaming-zh-14M-*.tar.bz2

# 2. 在 build.gradle.kts 取消注释 sherpa-onnx 依赖
sed -i 's|// implementation("com.k2fsa|implementation("com.k2fsa|' app/build.gradle.kts

# 3. 在 voice/SherpaRecognizer.kt 取消 TODO 注释, 实现 load/acceptWaveform/finalize

# 4. 重新构建
./gradlew :app:assembleRelease
```

---

## 架构概览

```
┌─────────────────────────────────────────────────────────────┐
│                Aurora IME 架构 (v2)                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  表现层 (Compose + Haze)                                     │
│  └─ GlassyPanel (4 级降级 API 33/31/26)                    │
│     ├─ PrivacyDifferentialBar                                │
│     ├─ KeyboardView (26键)                                   │
│     └─ HandwritingOverlay (透明叠写 + 三重判定)              │
│                                                             │
│  输入上下文适配层 (v2 新增)                                   │
│  └─ InputContextAnalyzer (9 种类型 × 6 项能力)             │
│                                                             │
│  引擎层 (全部本地)                                           │
│  ├─ SherpaRecognizer (sherpa-onnx, 占位)                   │
│  ├─ VoskRecognizer (备用)                                    │
│  └─ HanziLookup / Tegaki (手写, 占位)                      │
│                                                             │
│  安全层 (5 道门禁)                                           │
│  ├─ Manifest (tools:node="remove" INTERNET)                 │
│  ├─ check_deps.sh (CI 自动跑)                                │
│  ├─ Lint 规则 (拦截网络 API)                                 │
│  ├─ TrafficStats 实时公示                                    │
│  └─ Pre-commit hook                                          │
│                                                             │
│  基座层                                                      │
│  └─ 自研 (fork 自 HeliBoard GPL-3.0)                        │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 许可与免责声明

- 本项目采用 **GPL-3.0**（继承自 HeliBoard）
- voice/SherpaRecognizer.kt 集成的 sherpa-onnx 是 **Apache-2.0**
- handwriting 模块需要你选定引擎（开源/自训）

**禁止**：
- ❌ 私自打包内置 sherpa-onnx（已含在二进制中需要保留版权声明）
- ❌ 直接逆向封装搜狗手写 SDK

**推荐**：
- ✅ 上 F-Droid（公开审计）
- ✅ 公开 release 哈希
- ✅ 用 Exodus Privacy 公开审计报告

---

## 版本

| 版本 | 日期 | 变更 |
|---|---|---|
| v0.1.0 | 2026-09-14 | 首版骨架 - 26 键 + 玻璃 UI + 隐私矩阵 + 安全面板 + 手写叠写 |

---

**给开发者的话**：本仓库是 v0.1.0 骨架。功能完整度请看上方"功能清单"。二期路线：真实语音模型、自训手写模型、词库系统、滑行输入。
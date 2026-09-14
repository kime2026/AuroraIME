#!/usr/bin/env bash
#
# Aurora IME - 安全门禁脚本
#
# 验证 APK 不含任何网络权限和残留网络代码 (v2)
#
# 用法: ./scripts/check_deps.sh app-release.apk
#
# 前置: ANDROID_HOME 环境变量, 或在 PATH 中有 aapt2/dexdump

set -e

if [ -z "$1" ]; then
    echo "Usage: $0 <path-to-apk>"
    exit 1
fi

APK="$1"
TMP=$(mktemp -d)
trap "rm -rf $TMP" EXIT

# 检测工具路径
AAPT2=""
if [ -n "$ANDROID_HOME" ]; then
    for d in "$ANDROID_HOME/build-tools"/*/; do
        if [ -f "$d/aapt2" ]; then AAPT2="$d/aapt2"; break; fi
    done
fi
if [ -z "$AAPT2" ] && command -v aapt2 >/dev/null 2>&1; then
    AAPT2="aapt2"
fi
if [ -z "$AAPT2" ]; then
    echo "❌ ERROR: aapt2 not found. Set ANDROID_HOME."
    exit 1
fi

UNZIP="unzip"
if ! command -v unzip >/dev/null 2>&1; then
    echo "❌ ERROR: unzip not found"
    exit 1
fi

echo "════════════════════════════════════════════════════════"
echo " Aurora IME - 安全门禁审计"
echo " APK: $APK"
echo "════════════════════════════════════════════════════════"

# 1. 权限审计
echo ""
echo "① 权限审计..."
"$AAPT2" dump permissions "$APK" > "$TMP/perms.txt"
NETWORK_PERMS=$(grep -E "android.permission.INTERNET|ACCESS_NETWORK_STATE|ACCESS_WIFI_STATE|CHANGE_NETWORK_STATE" "$TMP/perms.txt" || true)
if [ -n "$NETWORK_PERMS" ]; then
    echo "❌ 严重: APK 声明了网络权限!"
    echo "$NETWORK_PERMS"
    exit 1
fi
echo "  ✓ 无 INTERNET 权限"
echo "  ✓ 无 ACCESS_NETWORK_STATE 权限"

# 2. 字节码审计
echo ""
echo "② DEX 字节码审计..."
$UNZIP -o -q "$APK" "classes*.dex" -d "$TMP"
FOUND_NETWORK=0
for dex in "$TMP"/classes*.dex; do
    if [ -f "$dex" ]; then
        # 检测网络调用类 (过滤掉 BuildConfig / log 等误报)
        matches=$(strings "$dex" 2>/dev/null | grep -E \
            "^(okhttp3/|retrofit2/|com/squareup/okhttp|HttpURLConnection|java/net/Socket|java/net/URL|android/net/ConnectivityManager)" \
            || true)
        # 排除标准字符串常量误报
        safe_matches=$(echo "$matches" | grep -vE "java/net/URLStreamHandler|java/net/SocketImpl" || true)
        if [ -n "$safe_matches" ]; then
            echo "❌ 严重: DEX 中发现网络调用类!"
            echo "$safe_matches" | sort -u | head -10
            FOUND_NETWORK=1
        fi
    fi
done
if [ "$FOUND_NETWORK" -eq 0 ]; then
    echo "  ✓ DEX 中无网络调用类残留"
fi

# 3. 资源审计 (URL / endpoint 泄露)
echo ""
echo "③ 资源审计..."
$UNZIP -o -q "$APK" "res/*" -d "$TMP/res"
URL_LEAKS=$(grep -rE "https?://(api|cloud|track|stat|telemetry|analytics|server|upload)" "$TMP/res/" 2>/dev/null | head -5 || true)
if [ -n "$URL_LEAKS" ]; then
    echo "⚠ 警告: 资源中发现可疑 URL (可能是误报, 需人工 review):"
    echo "$URL_LEAKS"
fi

echo ""
if [ "$FOUND_NETWORK" -eq 0 ]; then
    echo "════════════════════════════════════════════════════════"
    echo " ✅ 安全门禁通过"
    echo "════════════════════════════════════════════════════════"
    exit 0
else
    echo "════════════════════════════════════════════════════════"
    echo " ❌ 安全门禁失败 - 请删除网络相关代码后重新构建"
    echo "════════════════════════════════════════════════════════"
    exit 1
fi
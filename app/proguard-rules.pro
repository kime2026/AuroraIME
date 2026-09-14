# 保留行号用于崩溃堆栈反混淆
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Compose 不需要混淆
-dontwarn kotlinx.coroutines.**
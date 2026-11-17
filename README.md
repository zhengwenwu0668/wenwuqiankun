# 文武乾坤 · 玄空飞星排盘（Wenwu Qiankun / Xuan Kong Fei Xing）

一款纯原生 Android 玄空飞星排盘 App。输入元运、坐向、兼向、坐山度数，绘制三层同心圆罗盘（360° 刻度、二十四山、九宫飞星），并支持叠加流年、流月紫白飞星。

- 关键词：玄空、飞星、玄空飞星、紫白飞星、三元九运、风水、罗盘、排盘
- Keywords: xuankong, xuan kong, feixing, xuankongfeixing, flying star, feng shui, luopan, compass, zi bai, 三元九运

## 功能特性

- **玄空飞星排盘**：运盘（永远顺飞）、山星、向星，支持下卦与替卦（兼向）
- **二十四山罗盘**：三层同心圆，360° 刻度层、二十四山层、九宫格，坐山燕尾箭 + 向首实心箭头
- **流年 / 流月紫白飞星**：立春切年、逐月逆推，勾选后叠加显示红年星 / 绿月星
- **双 App 共存**：`文武乾坤`（com.example.wenwuqiankun）与 `张子易盘`（com.example.zhangziyipan）两个 flavor，仅包名不同

## 技术栈

- Kotlin + Jetpack Compose（BOM 2026.02.01 + Material3，Compose 编译器插件 2.2.10）
- Compose Canvas 自绘罗盘，零第三方依赖
- AGP 9.5.1 / Gradle 9.6.0 / compileSdk 37 / targetSdk 34 / minSdk 24
- 竖屏锁定，系统默认中文字体

## 界面预览

参数区 4 行（元运 / 坐向+兼向合并框 / 坐山度数+排盘按钮 / 流年+流月），下方为正方形罗盘绘图区。

## 构建

环境要求 JDK 17+（推荐 Android Studio 自带 JBR）：

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:assembleWenwuDebug   # 文武乾坤
.\gradlew.bat :app:assembleZzyDebug    # 张子易盘
```

产物：`app\build\outputs\apk\{flavor}\debug\`

## 安装

```powershell
adb install -r app/build/outputs/apk/wenwu/debug/app-wenwu-debug.apk
adb shell am start -n com.example.wenwuqiankun/com.example.wenwuqiankun.MainActivity
```

## 项目结构

```
app/src/main/java/com/example/wenwuqiankun/
  MainActivity.kt
  domain/           # Constants / LichunTable / FeixingCalculator / YearMonthCalculator
  ui/               # LuopanScreen（参数区）/ LuopanCanvas（罗盘绘制）
app/src/zzy/res/    # 张子易盘 flavor 资源
```

## 文档

- `Agent.md` —— 开发准则 + 完整可复刻规格（领域常量、算法、UI、绘图参数）
- `文武乾坤_Android_项目规格说明书.xlsx` —— 结构化规格 + 验收清单
- `归档说明.md` —— 项目归档与构建安装说明

## 许可证

[MIT](LICENSE)

## 致谢

本项目为个人独立开发的玄空飞星排盘工具，欢迎 star、issue 与 PR。

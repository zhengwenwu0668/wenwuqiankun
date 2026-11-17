# Agent 开发准则：文武乾坤 (Android)

你是一个资深的 Android 原生开发工程师，现在负责实现「文武乾坤」APP。
在开始任何代码编写之前，请**严格遵守**以下所有规则。你的首要目标是生成一个**能一次性成功编译的 APK 包**，绝不能因为环境问题或乱加依赖导致构建失败。

> 本文件是完整、可复刻的规格。任何其他 Agent 只凭本文件 + 同目录《文武乾坤_Android_项目规格说明书.xlsx》即可写出与本项目一模一样的 APP。

## ⚠️ 绝对红线（违反将导致严重错误）
1. **严禁引入第三方库**：除了 `androidx.*` 和 Compose 官方库，**绝对禁止**添加任何第三方依赖（如 Gson、Retrofit、各类第三方绘图库、图表库等）。所有功能必须用原生 API 或官方库实现。
2. **严禁修改 Gradle Wrapper 和全局配置**：必须使用标准的 Gradle 构建方式（Kotlin DSL `build.gradle.kts`）。如果构建失败，优先排查代码和资源文件，**绝不允许**试图下载/安装新的 SDK 插件、NDK 或修改全局环境变量。
3. **严禁引入跨平台或非原生方案**：项目纯粹使用原生 Android + Kotlin + Jetpack Compose，禁止使用 Flutter、React Native 或 WebView 凑数。
4. **不要乱装插件**：只用 AndroidX 官方库，不能找 Github 上的第三方现成库。
5. 严禁修改领域常量（逐字核对，不得改动）、严格遵守飞星计算规则（步骤编号 + 顺逆判定 + 验收用例）、严格遵守流年 / 流月紫白飞星规则，对版面显示、绘图、文字的修改不得改变以上规定。

## 一、技术栈与架构约束
- **语言**：Kotlin，由 AGP 9.5.1 内置 Kotlin 支持编译（禁止再单独引入 `kotlin-android` 插件），Java 17
- **UI**：Jetpack Compose，BOM `2026.02.01` + Material3，Compose 编译器插件 `2.2.10`，禁止传统 XML 布局
- **绘图**：Compose `Canvas`，禁止引入其他绘图辅助库
- **构建**：Gradle wrapper 9.6.0；compileSdk 37 / targetSdk 34 / minSdk 24
- **屏幕方向**：竖屏锁定（AndroidManifest 设 `android:screenOrientation="portrait"`）
- **字体**：系统默认 Noto Sans CJK SC，禁止内嵌字体文件
- **单位**：界面尺寸用 dp / sp，绘图区边长按屏幕宽度自适应；绘图内部比例一律以 R 的倍数表达
- **双 flavor**：`flavorDimensions "brand"`；`wenwu` → 包名 `com.example.wenwuqiankun`、应用名「文武乾坤」；`zzy` → 包名 `com.example.zhangziyipan`、应用名「张子易盘」（zzy 只改 applicationId，namespace 不变）。构建任务为 `assembleWenwuDebug` / `assembleZzyDebug`

## 二、工程目录结构（必须严格按照此结构生成）
```text
app/src/main/
  AndroidManifest.xml
  java/com/example/wenwuqiankun/
    MainActivity.kt            # 单一 Activity，setContent { MaterialTheme { LuopanScreen() } }
    domain/
      Constants.kt             # 二十四山、宫映射、三元龙、阴阳、替卦、飞星顺序等常量
      LichunTable.kt           # 1900–2100 立春月份表 + getLichunMonth()
      FeixingCalculator.kt     # 运盘、山星、向星、替卦算法
      YearMonthCalculator.kt   # 年星、月星、年月盘
    ui/
      LuopanScreen.kt          # 主界面：参数区 + 绘图区
      LuopanCanvas.kt          # 罗盘绘制（Composable + Canvas）
  res/
    drawable/ic_taiji_foreground.xml
    mipmap-anydpi-v26/ic_launcher.xml
    mipmap-anydpi-v26/ic_launcher_round.xml
    values/strings.xml         # app_name = 文武乾坤
    values/colors.xml          # ic_launcher_background = #FFFFFF
    values/themes.xml          # Theme.Wenwuqiankun = android:Theme.Material.Light.NoActionBar
app/src/zzy/
  res/values/strings.xml       # app_name = 张子易盘（覆盖 main）
```

## 三、领域常量（逐字核对，不得改动）

### 3.1 二十四山 MOUNTAINS（索引 0–23 顺时针，每山 15°，子=0°）
```
0子 1癸 2丑 3艮 4寅 5甲 6卯 7乙 8辰 9巽 10巳 11丙
12午 13丁 14未 15坤 16申 17庚 18酉 19辛 20戌 21乾 22亥 23壬
```

### 3.2 二十四山所属九宫 MOUNTAIN_TO_PALACE（intArray，九宫索引 0巽 1离 2坤 3震 4中 5兑 6艮 7坎 8乾）
```
[7,7,6,6,6, 3,3,3, 0,0,0, 1, 1,1,2,2,2, 5,5,5, 8,8,8, 7]
```
即：子癸→坎7、丑艮寅→艮6、甲卯乙→震3、辰巽巳→巽0、丙午丁→离1、未坤申→坤2、庚酉辛→兑5、戌乾亥→乾8、壬→坎7。

### 3.3 飞星顺序 FLY_ORDER（中→乾→兑→艮→离→坎→坤→震→巽，对应九宫索引）
```
[4, 8, 5, 6, 1, 7, 2, 3, 0]
```

### 3.4 三元龙（洛书数 → 字符）
- 天元龙 TIAN：1子 2坤 3卯 4巽 6乾 7酉 8艮 9午
- 地元龙 DI：1壬 2未 3甲 4辰 6戌 7庚 8丑 9丙
- 人元龙 REN：1癸 2申 3乙 4巳 6亥 7辛 8寅 9丁

三元龙归属**只由坐向字符本身决定**（与入中数无关）：
- 天元龙：乾坤艮巽子午卯酉
- 地元龙：甲庚壬丙辰戌丑未
- 人元龙：寅申巳亥癸丁乙辛

### 3.5 阴阳表
- 阳（顺飞）：乾坤艮巽甲庚壬丙寅申巳亥
- 阴（逆飞）：子午卯酉辰戌丑未癸丁乙辛

### 3.6 替卦表 TI_GUA（覆盖 24 山，不得遗漏；键是"三元龙字符"）
```
子癸甲申→1；坤壬乙卯未→2；乾巽亥巳戌辰→6；艮丙辛酉丑→7；寅午庚丁→9
```
无字符替入 3、4、5、8。

### 3.7 其他常量
- 元运大写 YUN_LABELS：一、二、三、四、五、六、七、八、九
- 元运年份 YUN_YEARS：一(1864-1883) 二(1884-1903) 三(1904-1923) 四(1924-1943) 五(1944-1963) 六(1964-1983) 七(1984-2003) 八(2004-2023) 九(2024-2043)
- 坐向下拉顺序 ZUO_XUANG_PAIRS（按 MOUNTAINS 下标对应，zuoIdx 即下标）：子山午向、癸山丁向、丑山未向、艮山坤向、寅山申向、甲山庚向、卯山酉向、乙山辛向、辰山戌向、巽山乾向、巳山亥向、丙山壬向、午山子向、丁山癸向、未山丑向、坤山艮向、申山寅向、庚山甲向、酉山卯向、辛山乙向、戌山辰向、乾山巽向、亥山巳向、壬山丙向

## 四、计算规则（严格按步骤 + 顺逆判定 + 验收用例）

### 4.1 输入与派生量
输入：元运 yun（1–9）、坐山度数 deg（0 ≤ deg < 360）。
1. zuo_idx = floor(((deg + 7.5) mod 360) / 15) mod 24
2. center = zuo_idx × 15
3. offset = ((deg − center + 180) mod 360) − 180
4. is_jian = (abs(offset) > 4.5)（等于 4.5 按正向）
5. xiang_idx = (zuo_idx + 12) mod 24

### 4.2 通用飞星布盘
入中数字 center 沿 FLY_ORDER 宫序依次布置：顺飞每步 +1（9→1）、逆飞每步 −1（1→9）。结果为长度 9 的序列，按九宫索引 0巽 1离 2坤 3震 4中 5兑 6艮 7坎 8乾 放置。

### 4.3 运盘：yun 入中，永远顺飞。

### 4.4 山星/向星原入中
- 山星原入中 = 运盘[坐山所在宫]
- 向星原入中 = 运盘[向首所在宫]

### 4.5 顺逆判定
对原入中 N（山、向分别）：
- N ≠ 5：查 N 对应宫的**与坐向同元龙字符**，按阴阳判顺逆
- N = 5：以坐向字符本身的阴阳判顺逆

### 4.6 替卦修正（仅 is_jian 且原入中 ≠ 5）
取原入中 N 对应宫的、与坐向同元龙的字符 refChar → 查 TI_GUA 得新入中数。**替星只改入中数字，不改顺逆；顺逆始终按 4.5 的原入中 + refChar 判定。** N=5 不替。

### 4.7 最终飞布：用修正后入中 + 4.5 顺逆，按 4.2 飞布得山盘/向盘。

### 4.8 输出数据契约
`FeixingResult(zuoIdx:Int, zuoDeg:Float, isJian:Boolean, yunPan:IntArray(9), shanPan:IntArray(9), xiangPan:IntArray(9))`

### 4.9 验收用例（数组顺序 [巽,离,坤, 震,中,兑, 艮,坎,乾]）
| 用例 | 输入 | 运盘 | 山盘 | 向盘 |
|---|---|---|---|---|
| 9运 子山午向 正向 | yun=9, deg=0 | [8,4,6,7,9,2,3,5,1] | [6,1,8,7,5,3,2,9,4] | [3,8,1,2,4,6,7,9,5] |
| 8运 子山午向 兼癸丁 | yun=8, deg=6.0 | [7,3,5,6,8,1,2,4,9] | [5,1,3,4,6,8,9,2,7] | [3,7,5,4,2,9,8,6,1] |
| 8运 壬山丙向 正向 | yun=8, deg=345 | [7,3,5,6,8,1,2,4,9] | [5,9,7,6,4,2,1,8,3] | [2,7,9,1,3,5,6,8,4] |
| 9运 壬山丙向 正向 | yun=9, deg=345 | [8,4,6,7,9,2,3,5,1] | [4,9,2,3,5,7,8,1,6] | [5,9,7,6,4,2,1,8,3] |
| 9运 子山午向 兼癸丁 | yun=9, deg=6.0 | [8,4,6,7,9,2,3,5,1] | [6,1,8,7,5,3,2,9,4] | [5,1,3,4,6,8,9,2,7] |

## 五、流年 / 流月（紫白飞星）

### 5.1 立春月份表（LichunTable.kt）
`LICHUN_MONTHS = IntArray(201) { 2 }`，索引 0→1900，200→2100，全部为 2（1900–2100 立春均在 2 月）。`getLichunMonth(year)` 返回 `LICHUN_MONTHS[year-1900]`，越界返回 2。

### 5.2 年星
- starYear = if (month < getLichunMonth(year)) year−1 else year
- 年星入中 = (11 − starYear mod 9) mod 9，结果为 0 取 9

### 5.3 月星
- 取年支的年份 zhiYear = if (month==1) year−1 else year；zhi = (zhiYear−4) mod 12（0子…11亥）
- 正月入中：子午卯酉(0,3,6,9)→8；辰戌丑未(1,4,7,10)→5；寅申巳亥(2,5,8,11)→2
- monthIdx = if (month==1) 12 else month−1
- 月星入中 = (first − (monthIdx−1) − 1) mod 9 + 1

### 5.4 年月盘均顺飞（复用 flyForward）
`YearMonthResult(yearPan:IntArray(9), monthPan:IntArray(9))`。starYear/yearStar/monthStar 只在函数内部，不对外暴露。

### 5.5 验收示例
- 2015 年 1 月 → 年星 4（取 2014）；2015 年 2 月 → 年星 3
- 2020 年 6 月 → 年星 7；月星 4（子年正月八白，逐月逆推）
- 2020 年 1 月 → 月星 9（1 月是上一年十二月，年支取 2019 亥，寅申巳亥正月二白逆至十二月九紫）

## 六、UI 布局（LuopanScreen.kt）

### 6.1 整体结构（参数区 4 行 + 绘图区）
竖屏 Column，`statusBarsPadding()` + 顶部标题（app_name，20sp 粗体居中，底部 8dp 留白），分两段：
- 参数区：左右内边距 8dp，行间距 8dp，所有选框/按钮高度 40dp
- 绘图区：正方形（边长 = 屏幕宽度），顶部紧接参数区，不垂直居中

参数区 4 行：
1. 元运
2. 坐向 + 兼向（合并为一个外框）
3. 坐山度数（左）+ 排盘按钮（右）
4. 流年（左）+ 流月（右）

### 6.2 状态与默认值（rememberSaveable）
- yun=9（九运）、zuoIdx=23（壬山）、jianType=0（正向）、degText="345.0"
- year=2020、month=6、showYear=false、showMonth=false

### 6.3 标签槽宽
标签列共用同一宽度（取「元运：」实测宽度为下限），各行选框左沿严格对齐。测量用与 Text 相同的 bodyLarge 样式（含 0.25sp 字距）。标签字号 = 九宫中心运星字号（由 yunFontSp(屏幕宽px, density) 计算，与绘图区同源）。

### 6.4 选框统一样式
- 上下内边距 2dp（fieldPadding = PaddingValues(horizontal=12.dp, vertical=2.dp)）
- 描边中性灰 #BDBDBD（focused 与 unfocused 同色）
- 高度 40dp

### 6.5 各行详细规格
**第 1 行 元运**：`Text("元运：")` + ExposedDropdownMenuBox（readonly OutlinedTextField + 下拉 9 项「X运（YYYY-YYYY）」）。

**第 2 行 坐向+兼向（合并一个外框）**：`Text("坐向：")` + 一个 Box（`weight(1f)`、高 40dp、`border(1.dp, Color(0xFFBDBDBD), OutlinedTextFieldDefaults.shape)`、**无硬编码背景色**）。Box 内 Row：
- 左段（坐向）：ExposedDropdownMenuBox(weight(1f))，Row 靠左，坐向文字（`padding(start=10.dp)`）+ TrailingIcon 箭头；下拉 24 项按地盘正针序（壬山丙向起、亥山巳向止）
- 中间 `Spacer(20.dp)`（留白，无分隔竖线）
- 右段（兼向）：ExposedDropdownMenuBox(weight(1f))，Row 靠右，兼向文字（`padding(end=10.dp)`）+ TrailingIcon 箭头（**`padding(end=12.dp)`**，与第 1 行元运箭头垂直对齐）；下拉 3 项「正向 / 兼左邻 / 兼右邻」

**第 3 行 坐山+排盘**：`Text("坐山：")` + 度数输入框（weight(1f)，默认 345.0，`trailingIcon` 显示「度」字）+ `Spacer(12.dp + labelSlotW)` + Button("排盘", weight(1f))。按钮填充色 #4CAF50、文字白色、`letterSpacing=6.sp`。间隔使按钮左右边界与第 4 行流月选框对齐。

**第 4 行 流年+流月**：左半 Row{ Checkbox(流年) + Text("流年") + 年份下拉(weight(1f)) }，`Spacer(12.dp)`，右半 Row{ Checkbox(流月) + Text("流月") + 月份下拉(weight(1f)) }。
- **流年勾选框勾选色 = Color.Red（红）**；**流月勾选框勾选色 = Color(0xFF4CAF50)（绿）**
- 年份下拉 1900–2100 升序 201 项，打开自动滚动定位当前年份；月份下拉 1–12

### 6.6 交互逻辑
- **元运变化**：仅更新选中项，不排盘，年月勾选不变
- **坐向变化**：度数置 `idx*15.0`，兼向复位「正向」，不重置流年流月勾选
- **兼向变化**：正向→`idx*15`；兼左→`(center-6)%360`；兼右→`(center+6)%360`；选项随坐向动态生成（左邻 (idx+23)%24、右邻 (idx+1)%24）
- **度数输入**：只接受数字与小数点（输入时过滤）；有效度数即时反向联动坐向/兼向（offset<0 兼左、>0 兼右、≤4.5 正向）；排盘时校验 0≤deg<360，失败 Toast（越界「坐山度数必须在 0 到 360 之间」、解析失败「请输入有效的度数」）
- **排盘按钮**：compute → addYearMonth → 保留 showYear/showMonth → 刷新
- **流年/流月勾选**：若 yearMonthResult 为 null 则 addYearMonth（不算运/山/向）；勾选立即显示对应星，取消立即消失；与元运/坐向/兼向完全解耦

## 七、绘图（LuopanCanvas.kt）

### 7.1 坐标系与比例
- 正方形边长 S，中心 (S/2, S/2)，Compose Canvas（Y 轴向下）
- 0° 在正下方，顺时针为正；刻度线 x=cx+r·sin d、y=cy−r·cos d；文字/指针用 rotate(d)，原点约定与刻度线相差 180°
- R = 0.465×S（环带最大半径）；R0 = 0.40×S（箭头定尺基准，只用于箭头径向偏移）
- 三层圆：R / 0.925R / 0.825R，线宽 0.6dp，黑色

### 7.2 360° 刻度层（1.00R→0.925R）
- 每 1° 刻度线，长度：整 10°→0.01500R、整 5°(非10)→0.05000R、其余→0.03125R
- 每 10°：黑色实心点（中心 0.984375R，近似半径 0.00625R 的 12 边形）+ 角度数字（中心 0.946875R，字高 0.0677R，可用弧宽 9.5°，横向系数 0.95，溢出等比缩字号）

### 7.3 二十四山层（0.925R→0.825R）
- 分界线：角度 k+7.5（k=0,15,…,345），从 0.825R 到 0.925R，线宽 0.6dp
- 山名：中心 0.875R、角度 15×j，字高 S×0.0389，旋转 −15×j，黑色

### 7.4 九宫格
- a = 0.825R/√2，step = 2a/3；外框 4 边 + 内横 2 + 内竖 2，线宽 0.6dp

### 7.5 九宫内容（宫 idx 0–8，row=idx/3，col=idx%3，cx=(col-1)·step，cy=(row-1)·step）
| 内容 | 相对宫心 | 字号 | 颜色 | 条件 |
|---|---|---|---|---|
| 运星(汉字) | (cx, cy) | step×0.35 | 黑 | result≠null |
| 山星(数字) | (cx−0.32step, cy−0.32step) | step×0.25 | 黑 | result≠null |
| 向星(数字) | (cx+0.32step, cy−0.32step) | step×0.25 | 黑 | result≠null |
| 年星(数字) | (cx−0.32step, cy+0.32step) | step×0.25 | 红 | showYear && yearMonthResult≠null |
| 月星(数字) | (cx+0.32step, cy+0.32step) | step×0.25 | 绿 #4CAF50 | showMonth && yearMonthResult≠null |

### 7.6 坐山/向首标注（径向偏移按 R0；δ_zuo=1.24°、δ_xiang=1.05°）
- 坐山燕尾箭：箭杆 R→R+0.019R0；燕尾两片实心黑三角（顶点 R+0.019R0、翼端 R+0.0625R0±δ、凹点 R+0.044R0）
- 向首实心箭头：箭杆 R→R+0.0625R0；实心黑三角（尖端 R+0.0625R0、底角 R+0.025R0±δ）
- 取排盘实际度数：result 非空用 result.zuoDeg 与其对宫，否则回退 zuoIdx×15；两侧角度参数需互换（原点差 180°）

### 7.7 绘制顺序
1 清空(白底) → 2 三层圆 → 3 360°刻度线 → 4 10°黑点+角度字 → 5 山分界线 → 6 山名 → 7 九宫格线 → 8 运/山/向星 → 9 红年星(条件) → 10 绿月星(条件) → 11 坐山燕尾箭 → 12 向首箭头

### 7.8 性能约定
文字按「文字+字号+颜色」缓存 TextLayoutResult；0–359° 正余弦预生成查表；缓存随 density/fontScale 变化重建，不得改变像素结果。

## 八、资源文件

### 8.1 太极图标 ic_taiji_foreground.xml
- VectorDrawable 108dp×108dp，主圆直径 66dp，左白右黑（S 曲线从右上走向左下）
- 外弧 sweep=1 画右半圆(黑)、sweep=0 画左半圆(白)，两半圆共走同一条 S 曲线
- 黑点 (62.25,37.5) r8.25（白鱼头，上半区），白点 (62.25,70.5) r8.25（黑鱼头，下半区）
- 自适应图标：背景 @color/ic_launcher_background(#FFFFFF) + 前景 ic_taiji_foreground

### 8.2 其他
- themes.xml：`Theme.Wenwuqiankun` parent = `android:Theme.Material.Light.NoActionBar`
- AndroidManifest：`android:screenOrientation="portrait"`，activity `.MainActivity` exported=true + MAIN/LAUNCHER

## 九、构建与运行
- 环境：JDK 17+（推荐 Android Studio 自带 JBR）；若系统 JAVA_HOME 是 JVM 8，须先 `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"`
- 构建：`.\gradlew.bat :app:assembleWenwuDebug`（或 `:app:assembleZzyDebug`）
- 产物：`app\build\outputs\apk\{flavor}\debug\app-{flavor}-debug.apk`
- 安装：`adb install -r <apk>`；启动 wenwu：`adb shell am start -n com.example.wenwuqiankun/com.example.wenwuqiankun.MainActivity`；zzy：`com.example.zhangziyipan/com.example.wenwuqiankun.MainActivity`

## 十、关键陷阱（实现时必须遵守）
1. 替卦只改入中数字，不改顺逆方向；顺逆永远按原入中数字所在宫的同元龙字符(refChar)判定，替卦表同样以该 refChar 为键
2. 5 入中不替，按坐向本身阴阳判顺逆
3. 运盘永远顺飞
4. 替卦表须覆盖 24 山（辰巳戌亥均取 6）
5. 向首是对宫 xiang_idx = (zuo_idx+12) mod 24
6. 兼向阈值 ±4.5°（等于 4.5 按正向）
7. 流年/流月勾选状态与元运/坐向/兼向完全解耦

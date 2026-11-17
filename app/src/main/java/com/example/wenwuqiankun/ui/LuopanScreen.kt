package com.example.wenwuqiankun.ui

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.*
import androidx.compose.foundation.text.input.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wenwuqiankun.R
import com.example.wenwuqiankun.domain.*
import com.example.wenwuqiankun.domain.Constants as C
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LuopanScreen() {
    var yun by rememberSaveable { mutableIntStateOf(9) }
    var zuoIdx by rememberSaveable { mutableIntStateOf(23) }
    var jianType by rememberSaveable { mutableIntStateOf(0) }
    var degText by rememberSaveable { mutableStateOf("345.0") }
    var year by rememberSaveable { mutableIntStateOf(2020) }
    var month by rememberSaveable { mutableIntStateOf(6) }
    var showYear by rememberSaveable { mutableStateOf(false) }
    var showMonth by rememberSaveable { mutableStateOf(false) }
    var result by remember { mutableStateOf<FeixingResult?>(null) }
    var yearMonthResult by remember { mutableStateOf<YearMonthResult?>(null) }

    var yunExpanded by remember { mutableStateOf(false) }
    var zuoExpanded by remember { mutableStateOf(false) }
    var jianExpanded by remember { mutableStateOf(false) }
    var yearExpanded by remember { mutableStateOf(false) }
    var monthExpanded by remember { mutableStateOf(false) }

    // 流年下拉框打开时自动滚动到当前年份项，避免从 1900 手动滑到 2020
    val yearBringIntoView = remember { BringIntoViewRequester() }
    LaunchedEffect(yearExpanded) {
        if (yearExpanded) {
            // 等待 ExposedDropdownMenu 内部 Column 完成布局再请求滚动
            delay(80)
            yearBringIntoView.bringIntoView()
        }
    }

    val context = LocalContext.current

    // 控制区标签字号 = 九宫格中心运星字号（排盘区满屏宽后 S≈屏幕宽）。
    val density = LocalDensity.current.density
    val screenWidthPx = LocalConfiguration.current.screenWidthDp * density
    val labelFontSp = yunFontSp(screenWidthPx, density)
    // 标签槽宽：四行标签都是「二字 + 全角冒号」，流年/流月的勾选框共用同一槽宽，选框左沿即严格对齐。
    // 测量必须用与 Text 相同的合成样式：Material3 的 bodyLarge 带 0.25sp 字距，按裸字号量会少算字距，
    // 把全角冒号挤到第二行变成竖排。槽宽只作下限，标签放不下时自行加宽、由选框让位。
    val textMeasurer = rememberTextMeasurer()
    val labelTextStyle = LocalTextStyle.current.copy(fontSize = labelFontSp.sp)
    val labelSlotWPx = remember(textMeasurer, labelTextStyle) {
        textMeasurer.measure("元运：", labelTextStyle).size.width
    }
    val labelSlotW = with(LocalDensity.current) { labelSlotWPx.toDp() }

    // 兼向选项按验收 #18：兼{左邻山}{左邻向} / 兼{右邻山}{右邻向}，左邻 = (idx+23)%24、右邻 = (idx+1)%24
    val jianOptions = remember(zuoIdx) {
        val xiangIdx = (zuoIdx + 12) % 24
        val zuoLeft = C.MOUNTAINS[(zuoIdx + 23) % 24]
        val xiangLeft = C.MOUNTAINS[(xiangIdx + 23) % 24]
        val zuoRight = C.MOUNTAINS[(zuoIdx + 1) % 24]
        val xiangRight = C.MOUNTAINS[(xiangIdx + 1) % 24]
        listOf("正向", "兼${zuoLeft}${xiangLeft}", "兼${zuoRight}${xiangRight}")
    }

    // 二十四山下拉按罗盘地盘正针顺序显示（壬山丙向起、亥山巳向止）；写回的仍是 MOUNTAINS 下标，计算不受影响。
    val zuoMenuOrder = remember { (0 until 24).map { (it + 23) % 24 } }

    // 矮选框只有 TextFieldState 版本暴露 contentPadding，能把 Material3 默认上下各 12dp 压到 2dp。
    val yunDisplay = "${C.YUN_LABELS[yun - 1]}运（${C.YUN_YEARS[yun - 1]}）"
    val zuoDisplay = C.ZUO_XUANG_PAIRS[zuoIdx]
    val jianDisplay = jianOptions[jianType]
    val yearDisplay = "${year}年"
    val monthDisplay = "${month}月"

    val yunState = rememberTextFieldState(yunDisplay)
    val yearState = rememberTextFieldState(yearDisplay)
    val monthState = rememberTextFieldState(monthDisplay)
    val degState = rememberTextFieldState(degText)

    yunState.syncTo(yunDisplay)
    yearState.syncTo(yearDisplay)
    monthState.syncTo(monthDisplay)
    // 坐山度数：下拉选择/兼向会程序化改写 degText，需要同步进输入框
    degState.syncTo(degText)
    // 用户在框内输入时过滤为数字与小数点，并回写 degText
    LaunchedEffect(degState) {
        snapshotFlow { degState.text.toString() }.collect { raw ->
            val filtered = raw.filter { it.isDigit() || it == '.' }
            if (filtered != raw) degState.edit { replace(0, length, filtered) }
            degText = filtered
        }
    }

    // 度数 → 坐向/兼向 反向联动：山向索引与是否兼向取 domain 的判定结果，不在 UI 重复规格 2.1 公式。
    // 度数框保留用户输入的原始值，不回写成中心度/兼向度，避免打断输入；左右兼向由偏移符号区分。
    LaunchedEffect(degText) {
        val deg = degText.toFloatOrNull() ?: return@LaunchedEffect
        if (deg < 0f || deg >= 360f) return@LaunchedEffect
        val probe = FeixingCalculator.compute(yun, deg)
        val offset = (((deg - probe.zuoIdx * 15f) + 540f) % 360f) - 180f
        zuoIdx = probe.zuoIdx
        jianType = when {
            !probe.isJian -> 0
            offset < 0f -> 1
            else -> 2
        }
    }

    // 矮选框：上下内边距压到 2dp，高度 40dp 仍可完整容纳 21sp 文字
    val fieldPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
    // 选框描边改用中性灰：Material3 默认 outline(#79747E) 太深，看上去接近黑线
    val fieldColors = OutlinedTextFieldDefaults.colors(
        unfocusedBorderColor = Color(0xFFBDBDBD),
        focusedBorderColor = Color(0xFFBDBDBD)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.app_name),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            textAlign = TextAlign.Center
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("元运：", fontSize = labelFontSp.sp, modifier = Modifier.widthIn(min = labelSlotW))
                ExposedDropdownMenuBox(
                    expanded = yunExpanded,
                    onExpandedChange = { yunExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        state = yunState,
                        colors = fieldColors,
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = yunExpanded) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth().height(40.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = labelFontSp.sp),
                        lineLimits = TextFieldLineLimits.SingleLine,
                        contentPadding = fieldPadding,
                    )
                    ExposedDropdownMenu(
                        expanded = yunExpanded,
                        onDismissRequest = { yunExpanded = false },
                        modifier = Modifier.heightIn(max = 500.dp)
                    ) {
                        for (i in 1..9) {
                            DropdownMenuItem(
                                modifier = Modifier.height(52.dp),
                                text = { Text("${C.YUN_LABELS[i - 1]}运（${C.YUN_YEARS[i - 1]}）", fontSize = labelFontSp.sp) },
                                onClick = {
                                    yun = i
                                    yunExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("坐向：", fontSize = labelFontSp.sp, modifier = Modifier.widthIn(min = labelSlotW))
                // 坐向 + 兼向合为一个框：统一外描边，内部左右两段文字各带下拉箭头，中间留白、无分隔竖线。
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .border(1.dp, Color(0xFFBDBDBD), OutlinedTextFieldDefaults.shape)
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        ExposedDropdownMenuBox(
                            expanded = zuoExpanded,
                            onExpandedChange = { zuoExpanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                    .fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Text(
                                    text = zuoDisplay,
                                    fontSize = labelFontSp.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(start = 10.dp)
                                )
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = zuoExpanded)
                            }
                            ExposedDropdownMenu(
                                expanded = zuoExpanded,
                                onDismissRequest = { zuoExpanded = false },
                                modifier = Modifier.heightIn(max = 500.dp)
                            ) {
                                for (idx in zuoMenuOrder) {
                                    DropdownMenuItem(
                                        modifier = Modifier.height(40.dp),
                                        text = { Text(C.ZUO_XUANG_PAIRS[idx], fontSize = labelFontSp.sp) },
                                        onClick = {
                                            zuoIdx = idx
                                            jianType = 0
                                            degText = "${idx * 15}.0"
                                            zuoExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.width(20.dp))
                        ExposedDropdownMenuBox(
                            expanded = jianExpanded,
                            onExpandedChange = { jianExpanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                    .fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = jianDisplay,
                                    fontSize = labelFontSp.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(end = 10.dp)
                                )
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = jianExpanded,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                            }
                            ExposedDropdownMenu(
                                expanded = jianExpanded,
                                onDismissRequest = { jianExpanded = false }
                            ) {
                                jianOptions.forEachIndexed { idx, label ->
                                    DropdownMenuItem(
                                        text = { Text(label, fontSize = labelFontSp.sp) },
                                        onClick = {
                                            jianType = idx
                                            val center = zuoIdx * 15
                                            degText = when (idx) {
                                                0 -> "${center}.0"
                                                1 -> "${((center - 6) % 360 + 360) % 360}.0"
                                                2 -> "${(center + 6) % 360}.0"
                                                else -> "${center}.0"
                                            }
                                            jianExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("坐山：", fontSize = labelFontSp.sp, modifier = Modifier.widthIn(min = labelSlotW))
                OutlinedTextField(
                    state = degState,
                    colors = fieldColors,
                    modifier = Modifier.weight(1f).height(40.dp),
                    // Material3 尾部图标槽固定占 48dp（不随内边距缩小），故数字侧只收字距与左内边距来腾出末位数字的宽度
                    textStyle = LocalTextStyle.current.copy(fontSize = labelFontSp.sp, letterSpacing = 0.sp),
                    lineLimits = TextFieldLineLimits.SingleLine,
                    contentPadding = PaddingValues(start = 9.dp, end = 12.dp, top = 2.dp, bottom = 2.dp),
                    trailingIcon = {
                        Text(
                            text = "度",
                            fontSize = labelFontSp.sp,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    },
                )
                // 流月框左沿 = 半行起点 + 勾选槽宽，而本行按钮与度数框等宽对分，
                // 故间隔取 12dp + 一个标签槽宽，按钮左右边界即与流月框重合。
                Spacer(Modifier.width(12.dp + labelSlotW))
                Button(
                    onClick = {
                        try {
                            val deg = degText.toFloat()
                            if (deg < 0 || deg >= 360) {
                                Toast.makeText(context, "坐山度数必须在 0 到 360 之间", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            result = FeixingCalculator.compute(yun, deg)
                            yearMonthResult = YearMonthCalculator.addYearMonth(year, month)
                        } catch (e: NumberFormatException) {
                            Toast.makeText(context, "请输入有效的度数", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f).height(40.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    shape = OutlinedTextFieldDefaults.shape
                ) {
                    Text("排盘", color = Color.White, fontSize = labelFontSp.sp, letterSpacing = 6.sp)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier.widthIn(min = labelSlotW),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = showYear,
                            onCheckedChange = { checked ->
                                showYear = checked
                                if (checked && yearMonthResult == null) {
                                    yearMonthResult = YearMonthCalculator.addYearMonth(year, month)
                                }
                            },
                            modifier = Modifier.size(24.dp),
                            colors = CheckboxDefaults.colors(checkedColor = Color.Red)
                        )
                        Text("流年", fontSize = 15.sp)
                    }
                    ExposedDropdownMenuBox(
                        expanded = yearExpanded,
                        onExpandedChange = { yearExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            state = yearState,
                            colors = fieldColors,
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearExpanded) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth().height(40.dp),
                            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                            lineLimits = TextFieldLineLimits.SingleLine,
                            contentPadding = fieldPadding,
                        )
                        ExposedDropdownMenu(
                            expanded = yearExpanded,
                            onDismissRequest = { yearExpanded = false },
                            modifier = Modifier.heightIn(max = 300.dp)
                        ) {
                            for (y in 1900..2100) {
                                DropdownMenuItem(
                                    modifier = if (y == year)
                                        Modifier.bringIntoViewRequester(yearBringIntoView)
                                    else Modifier,
                                    text = { Text("${y}年", fontSize = 14.sp) },
                                    onClick = {
                                        year = y
                                        yearExpanded = false
                                        if (showYear) {
                                            yearMonthResult = YearMonthCalculator.addYearMonth(year, month)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier.widthIn(min = labelSlotW),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = showMonth,
                            onCheckedChange = { checked ->
                                showMonth = checked
                                if (checked && yearMonthResult == null) {
                                    yearMonthResult = YearMonthCalculator.addYearMonth(year, month)
                                }
                            },
                            modifier = Modifier.size(24.dp),
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4CAF50))
                        )
                        Text("流月", fontSize = 15.sp)
                    }
                    ExposedDropdownMenuBox(
                        expanded = monthExpanded,
                        onExpandedChange = { monthExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            state = monthState,
                            colors = fieldColors,
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthExpanded) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth().height(40.dp),
                            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                            lineLimits = TextFieldLineLimits.SingleLine,
                            contentPadding = fieldPadding,
                        )
                        ExposedDropdownMenu(
                            expanded = monthExpanded,
                            onDismissRequest = { monthExpanded = false }
                        ) {
                            for (m in 1..12) {
                                DropdownMenuItem(
                                    text = { Text("${m}月", fontSize = 14.sp) },
                                    onClick = {
                                        month = m
                                        monthExpanded = false
                                        if (showMonth) {
                                            yearMonthResult = YearMonthCalculator.addYearMonth(year, month)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        LuopanCanvas(
            result = result,
            yearMonthResult = yearMonthResult,
            showYear = showYear,
            showMonth = showMonth,
            zuoIdx = zuoIdx,
            modifier = Modifier
                .fillMaxWidth()
        )
    }
}

// 程序化改写的显示值同步回输入框；只在文本确实不同时替换，避免打断用户正在输入的内容。
@Composable
private fun TextFieldState.syncTo(display: String) {
    LaunchedEffect(display) {
        if (text.toString() != display) edit { replace(0, length, display) }
    }
}

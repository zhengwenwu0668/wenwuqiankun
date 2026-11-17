package com.example.wenwuqiankun.domain

object Constants {
    val MOUNTAINS = arrayOf(
        "子","癸","丑","艮","寅","甲","卯","乙",
        "辰","巽","巳","丙","午","丁","未","坤",
        "申","庚","酉","辛","戌","乾","亥","壬"
    )

    val MOUNTAIN_TO_PALACE = intArrayOf(
        7,7,6,6,6, 3,3,3, 0,0,0, 1,
        1,1,2,2,2, 5,5,5, 8,8,8, 7
    )

    val FLY_ORDER = intArrayOf(4,8,5,6,1,7,2,3,0)

    val TIAN_YUAN_LONG = mapOf(
        1 to "子", 2 to "坤", 3 to "卯", 4 to "巽",
        6 to "乾", 7 to "酉", 8 to "艮", 9 to "午"
    )

    val DI_YUAN_LONG = mapOf(
        1 to "壬", 2 to "未", 3 to "甲", 4 to "辰",
        6 to "戌", 7 to "庚", 8 to "丑", 9 to "丙"
    )

    val REN_YUAN_LONG = mapOf(
        1 to "癸", 2 to "申", 3 to "乙", 4 to "巳",
        6 to "亥", 7 to "辛", 8 to "寅", 9 to "丁"
    )

    private val YIN_MOUNTAINS = setOf("子","午","卯","酉","辰","戌","丑","未","癸","丁","乙","辛")

    val TI_GUA = mapOf(
        "子" to 1, "癸" to 1, "甲" to 1, "申" to 1,
        "坤" to 2, "壬" to 2, "乙" to 2, "卯" to 2, "未" to 2,
        "乾" to 6, "巽" to 6, "亥" to 6, "巳" to 6, "戌" to 6, "辰" to 6,
        "艮" to 7, "丙" to 7, "辛" to 7, "酉" to 7, "丑" to 7,
        "寅" to 9, "午" to 9, "庚" to 9, "丁" to 9
    )

    val YUN_LABELS = arrayOf("一","二","三","四","五","六","七","八","九")

    val YUN_YEARS = arrayOf(
        "1864-1883","1884-1903","1904-1923","1924-1943","1944-1963",
        "1964-1983","1984-2003","2004-2023","2024-2043"
    )

    // ZUO_XUANG_PAIRS[i] 描述 MOUNTAINS[i] 的坐向，i 直接对应 24 山的顺时针索引（子=0，0°）。
    // 因此 zuoIdx 就是 MOUNTAINS 的下标：zuoIdx*15 = 该山的中心度数，(zuoIdx+23)%24 = 左邻山，(zuoIdx+1)%24 = 右邻山。
    val ZUO_XUANG_PAIRS = arrayOf(
        "子山午向","癸山丁向","丑山未向","艮山坤向","寅山申向","甲山庚向",
        "卯山酉向","乙山辛向","辰山戌向","巽山乾向","巳山亥向","丙山壬向",
        "午山子向","丁山癸向","未山丑向","坤山艮向","申山寅向","庚山甲向",
        "酉山卯向","辛山乙向","戌山辰向","乾山巽向","亥山巳向","壬山丙向"
    )

    fun isYin(mountainChar: String): Boolean = YIN_MOUNTAINS.contains(mountainChar)

    // 三元龙归属只由字符本身决定（规格 1.3），与入中数无关。
    fun getSanYuanLongType(mountainChar: String): String {
        if (TIAN_YUAN_LONG.containsValue(mountainChar)) return "tian"
        if (DI_YUAN_LONG.containsValue(mountainChar)) return "di"
        return "ren"
    }

    fun getDragonChar(luoshuNum: Int, sanYuanLongType: String): String {
        return when (sanYuanLongType) {
            "tian" -> TIAN_YUAN_LONG[luoshuNum] ?: ""
            "di" -> DI_YUAN_LONG[luoshuNum] ?: ""
            "ren" -> REN_YUAN_LONG[luoshuNum] ?: ""
            else -> ""
        }
    }
}

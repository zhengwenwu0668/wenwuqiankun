package com.example.wenwuqiankun.domain

data class YearMonthResult(
    val yearPan: IntArray,
    val monthPan: IntArray
)

object YearMonthCalculator {

    fun addYearMonth(year: Int, month: Int): YearMonthResult {
        val lichunMonth = LichunTable.getLichunMonth(year)
        val starYear = if (month < lichunMonth) year - 1 else year

        var yearStar = (11 - (starYear % 9)) % 9
        if (yearStar == 0) yearStar = 9

        // 规格 3.3：1 月是上一年十二月，年支取 year−1
        val zhiYear = if (month == 1) year - 1 else year
        val zhi = ((zhiYear - 4) % 12 + 12) % 12
        val firstMonthStar = when (zhi % 3) {
            0 -> 8
            1 -> 5
            else -> 2
        }

        val monthIdx = if (month == 1) 12 else month - 1
        val monthStar = ((firstMonthStar - (monthIdx - 1) - 1) % 9 + 9) % 9 + 1

        return YearMonthResult(
            yearPan = flyForward(yearStar),
            monthPan = flyForward(monthStar)
        )
    }
}

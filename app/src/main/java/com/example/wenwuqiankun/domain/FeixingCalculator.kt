package com.example.wenwuqiankun.domain

import com.example.wenwuqiankun.domain.Constants as C

data class FeixingResult(
    val zuoIdx: Int,
    val zuoDeg: Float,
    val isJian: Boolean,
    val yunPan: IntArray,
    val shanPan: IntArray,
    val xiangPan: IntArray
)

// 洛书顺飞：按 FLY_ORDER 宫序从入中数起，9 后回到 1。运盘、山/向盘与流年流月盘共用。
internal fun flyForward(center: Int): IntArray {
    val pan = IntArray(9)
    var num = center
    for (i in 0 until 9) {
        pan[C.FLY_ORDER[i]] = num
        num = if (num == 9) 1 else num + 1
    }
    return pan
}

object FeixingCalculator {

    private fun flyBackward(center: Int): IntArray {
        val pan = IntArray(9)
        var num = center
        for (i in 0 until 9) {
            pan[C.FLY_ORDER[i]] = num
            num = if (num == 1) 9 else num - 1
        }
        return pan
    }

    fun compute(yun: Int, deg: Float): FeixingResult {
        val normalizedDeg = ((deg % 360) + 360) % 360
        val zuoIdx = (Math.floor(((normalizedDeg + 7.5) % 360) / 15.0).toInt()) % 24
        val centerDeg = zuoIdx * 15
        val offset = ((normalizedDeg - centerDeg + 180) % 360 + 360) % 360 - 180
        val isJian = Math.abs(offset) > 4.5
        val xiangIdx = (zuoIdx + 12) % 24

        val yunPan = flyForward(yun)

        val zuoPalace = C.MOUNTAIN_TO_PALACE[zuoIdx]
        val xiangPalace = C.MOUNTAIN_TO_PALACE[xiangIdx]

        val shanOriginCenter = yunPan[zuoPalace]
        val xiangOriginCenter = yunPan[xiangPalace]

        val shanPan = flyPan(shanOriginCenter, C.MOUNTAINS[zuoIdx], isJian)
        val xiangPan = flyPan(xiangOriginCenter, C.MOUNTAINS[xiangIdx], isJian)

        return FeixingResult(
            zuoIdx = zuoIdx,
            zuoDeg = normalizedDeg,
            isJian = isJian,
            yunPan = yunPan,
            shanPan = shanPan,
            xiangPan = xiangPan
        )
    }

    // 规格 2.5→2.7：顺逆按「原入中数所在宫的同元龙字符」判定；兼向只换入中数（替卦表同样以该同元龙字符为键），
    // 原入中为 5 时不替，且改按坐向字符本身的阴阳定顺逆。
    private fun flyPan(originCenter: Int, mountainChar: String, isJian: Boolean): IntArray {
        val refChar = if (originCenter == 5) mountainChar
            else C.getDragonChar(originCenter, C.getSanYuanLongType(mountainChar))
        val isShun = !C.isYin(refChar)
        val center = if (isJian && originCenter != 5) C.TI_GUA[refChar] ?: originCenter else originCenter
        return if (isShun) flyForward(center) else flyBackward(center)
    }
}

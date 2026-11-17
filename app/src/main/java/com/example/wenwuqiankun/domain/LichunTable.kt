package com.example.wenwuqiankun.domain

object LichunTable {
    val LICHUN_MONTHS: IntArray = IntArray(201) { 2 }

    fun getLichunMonth(year: Int): Int {
        val idx = year - 1900
        return if (idx in 0..200) LICHUN_MONTHS[idx] else 2
    }
}

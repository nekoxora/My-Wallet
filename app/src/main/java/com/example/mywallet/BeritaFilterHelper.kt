package com.example.mywallet

import com.example.mywallet.data.BeritaSaham

object BeritaFilterHelper {
    private val ALWAYS = setOf(
        "IHSG",
        "NASDAQ",
        "DJIA",
        "S&P500",
        "USD/IDR",
        "WTI OIL",
        "COAL",
        "BI RATE",
        "GOLD",
        "BIG CAPS",
        "IPO"
    )
    private val GLOBAL = setOf(
        "COAL",
        "GOLD",
        "WTI OIL",
        "OIL",
        "IHSG",
        "NASDAQ",
        "DJIA",
        "S&P500",
        "USD/IDR",
        "BI RATE"
    )

    fun isBeritaRelevant(b: BeritaSaham, uE: Set<String>): Boolean {
        val e = b.emiten.uppercase().trim()
        if (e.isEmpty() && b.judul.contains("IPO", true)) return true
        return ALWAYS.contains(e) || uE.contains(e)
    }

    fun isGlobal(e: String): Boolean = GLOBAL.contains(e.uppercase().trim())
}

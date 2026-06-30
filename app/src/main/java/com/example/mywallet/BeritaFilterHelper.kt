package com.example.mywallet

import com.example.mywallet.data.BeritaSaham

object BeritaFilterHelper {

    private val ALWAYS_SHOW_EMITENS = setOf(
        "IHSG", "NASDAQ", "DJIA", "S&P500", "USD/IDR",
        "WTI OIL", "COAL", "BI RATE", "GOLD", "BIG CAPS"
    )

    fun isBeritaRelevant(berita: BeritaSaham, userEmitens: Set<String>): Boolean {
        val emitenBerita = berita.emiten.uppercase().trim()
        if (ALWAYS_SHOW_EMITENS.contains(emitenBerita)) return true
        if (userEmitens.contains(emitenBerita)) return true
        return false
    }
}
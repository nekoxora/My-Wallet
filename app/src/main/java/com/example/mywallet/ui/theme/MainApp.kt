package com.example.mywallet.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

enum class Layar { DASHBOARD, FORM, RINCIAN, NOTIFIKASI }

@Composable
fun MainApp() {
    var layarAktif by remember { mutableStateOf(Layar.DASHBOARD) }
    var layarSebelumnya by remember { mutableStateOf(Layar.DASHBOARD) }

    when (layarAktif) {
        Layar.DASHBOARD -> DashboardScreen(
            onNavigateToForm = {
                layarSebelumnya = Layar.DASHBOARD
                layarAktif = Layar.FORM
            },
            onNavigateToRincian = { layarAktif = Layar.RINCIAN },
            onNavigateToNotifikasi = { layarAktif = Layar.NOTIFIKASI }
        )

        Layar.FORM -> FormInvestasi(
            onBack = { layarAktif = layarSebelumnya }
        )

        Layar.RINCIAN -> RincianScreen(
            onNavigateToHome = { layarAktif = Layar.DASHBOARD },
            onNavigateToForm = {
                layarSebelumnya = Layar.RINCIAN
                layarAktif = Layar.FORM
            }
        )

        Layar.NOTIFIKASI -> NotifikasiScreen(onBack = { layarAktif = Layar.DASHBOARD })
    }
}
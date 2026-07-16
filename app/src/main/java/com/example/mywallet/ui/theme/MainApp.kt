package com.example.mywallet.ui.theme

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

enum class Layar { DASHBOARD, FORM, RINCIAN, NOTIFIKASI, CHATBOT }

@Composable
fun MainApp() {
    var act by remember { mutableStateOf(Layar.DASHBOARD) };
    var prev by remember { mutableStateOf(Layar.DASHBOARD) };
    var nKey by remember { mutableIntStateOf(0) }
    when (act) {
        Layar.DASHBOARD -> DashboardScreen(
            { prev = Layar.DASHBOARD; act = Layar.FORM },
            { act = Layar.RINCIAN },
            { nKey++; act = Layar.NOTIFIKASI },
            { prev = Layar.DASHBOARD; act = Layar.CHATBOT })

        Layar.FORM -> {
            BackHandler { act = prev }; FormInvestasi { act = prev }
        }

        Layar.RINCIAN -> {
            BackHandler { act = Layar.DASHBOARD }; RincianScreen(
                { act = Layar.DASHBOARD },
                { prev = Layar.RINCIAN; act = Layar.FORM },
                { prev = Layar.RINCIAN; act = Layar.CHATBOT })
        }

        Layar.NOTIFIKASI -> key(nKey) {
            BackHandler {
                act = Layar.DASHBOARD
            }; NotifikasiScreen { act = Layar.DASHBOARD }
        }

        Layar.CHATBOT -> {
            BackHandler { act = prev }; ChatBotScreen { act = prev }
        }
    }
}

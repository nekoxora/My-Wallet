package com.example.mywallet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.mywallet.ui.theme.BgDark
import com.example.mywallet.ui.theme.MainApp
import com.example.mywallet.ui.theme.MyWalletTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyWalletTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BgDark,
                    contentColor = Color.White
                ) {
                    MainApp()
                }
            }
        }
    }
}
package com.example.mywallet.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mywallet.R

data class PesanChat(val teks: String, val dariUser: Boolean)

@Composable
fun ChatBotScreen(onBack: () -> Unit) {
    var inputTeks by remember { mutableStateOf("") }
    val daftarPesan = remember {
        mutableStateListOf(
            PesanChat("Halo! Saya asisten finansial Anda. Ada yang bisa saya bantu?", false)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(id = R.drawable.arrow_back),
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Smart AI Analyst",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(daftarPesan) { pesan ->
                BubbleChat(pesan)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputTeks,
                onValueChange = { inputTeks = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Analisis saham...", color = TextGray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = RingColor,
                    unfocusedBorderColor = TextGray,
                    cursorColor = Color.White
                ),
                shape = RoundedCornerShape(25.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (inputTeks.isNotBlank()) {
                        daftarPesan.add(PesanChat(inputTeks, true))
                        val userMsg = inputTeks
                        inputTeks = ""

                        daftarPesan.add(
                            PesanChat(
                                "Saya sedang memproses pertanyaan Anda tentang '$userMsg'...",
                                false
                            )
                        )
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(RingColor)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.arrow_up),
                    contentDescription = "Send",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun BubbleChat(pesan: PesanChat) {
    val alignment = if (pesan.dariUser) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (pesan.dariUser) RingColor else CardDark
    val textColor = Color.White

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (pesan.dariUser) 16.dp else 0.dp,
                        bottomEnd = if (pesan.dariUser) 0.dp else 16.dp
                    )
                )
                .background(bgColor)
                .padding(12.dp)
        ) {
            Text(text = pesan.teks, color = textColor, fontSize = 14.sp)
        }
    }
}
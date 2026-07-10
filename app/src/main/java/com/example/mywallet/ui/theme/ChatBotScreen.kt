package com.example.mywallet.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mywallet.DeviceIdHelper
import com.example.mywallet.R
import com.example.mywallet.data.ChatBotService
import com.example.mywallet.data.RetrofitClient
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

data class PesanChat(val teks: String, val dariUser: Boolean)

@Composable
fun ChatBotScreen(onBack: () -> Unit) {
    var inputTeks by remember { mutableStateOf("") };
    val context = LocalContext.current;
    val scrollState = rememberLazyListState();
    val scope = rememberCoroutineScope()
    val daftarPesan = remember {
        mutableStateListOf(
            PesanChat(
                "Halo! Saya Elysia, asisten finansial cerdas Anda. Ada yang bisa saya bantu hari ini?",
                false
            )
        )
    }
    var isBotTyping by remember { mutableStateOf(false) };
    var portofolioEmitenList by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(Unit) {
        try {
            val id = DeviceIdHelper.getDeviceId(context); portofolioEmitenList =
                RetrofitClient.instance.getHistori(id).map { it.emiten }.distinct()
        } catch (e: Exception) {
        }
    }
    val templates = listOf(
        "Analisa portofolio saya",
        "Analisa saham hari ini",
        "Tips investasi pemula",
        "Apa itu IHSG?"
    )
    LaunchedEffect(daftarPesan.size) {
        if (daftarPesan.isNotEmpty()) scrollState.animateScrollToItem(
            daftarPesan.size - 1
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .windowInsetsPadding(WindowInsets.statusBars)
            .navigationBarsPadding()
            .imePadding()
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
            Spacer(modifier = Modifier.width(8.dp)); Text(
            text = "Smart AI Analyst",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        }
        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(daftarPesan) { pesan -> BubbleChat(pesan) }
            if (isBotTyping) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = RingColor,
                            strokeWidth = 2.dp
                        ); Spacer(modifier = Modifier.width(8.dp)); Text(
                        "Bot sedang berpikir...",
                        color = TextGray,
                        fontSize = 12.sp
                    )
                    }
                }
            }
        }
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(templates) { tmp ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(CardDark)
                        .border(1.dp, RingColor, RoundedCornerShape(20.dp))
                        .clickable {
                            if (!isBotTyping) {
                                daftarPesan.add(PesanChat(tmp, true)); kirimPesanKeBot(
                                    tmp,
                                    context,
                                    daftarPesan,
                                    portofolioEmitenList,
                                    scope
                                ) { isBotTyping = it }
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) { Text(text = tmp, color = Color.White, fontSize = 13.sp) }
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
                placeholder = { Text("Tanya analisis saham...", color = TextGray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = RingColor,
                    unfocusedBorderColor = TextGray,
                    cursorColor = Color.White
                ),
                shape = RoundedCornerShape(25.dp)
            )
            Spacer(modifier = Modifier.width(8.dp)); IconButton(
            onClick = {
                if (inputTeks.isNotBlank() && !isBotTyping) {
                    val msg = inputTeks; daftarPesan.add(PesanChat(msg, true)); inputTeks =
                        ""; kirimPesanKeBot(
                        msg,
                        context,
                        daftarPesan,
                        portofolioEmitenList,
                        scope
                    ) { isBotTyping = it }
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
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        }
    }
}

private fun kirimPesanKeBot(
    msg: String,
    ctx: android.content.Context,
    list: MutableList<PesanChat>,
    emts: List<String>,
    scp: CoroutineScope,
    setL: (Boolean) -> Unit
) {
    setL(true); scp.launch {
        try {
            val id = DeviceIdHelper.getDeviceId(ctx); list.add(
                PesanChat(
                    ChatBotService.generateResponse(
                        msg,
                        id,
                        emts
                    ), false
                )
            )
        } catch (e: Exception) {
            list.add(PesanChat("Gagal: ${e.localizedMessage}", false))
        } finally {
            setL(false)
        }
    }
}

@Composable
fun BubbleChat(pesan: PesanChat) {
    val align = if (pesan.dariUser) Alignment.CenterEnd else Alignment.CenterStart
    val bg = if (pesan.dariUser) RingColor else CardDark
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = align) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (pesan.dariUser) 16.dp else 0.dp,
                        bottomEnd = if (pesan.dariUser) 0.dp else 16.dp
                    )
                )
                .background(bg)
                .padding(12.dp)
        ) {
            if (pesan.dariUser) Text(text = pesan.teks, color = Color.White, fontSize = 14.sp)
            else MarkdownText(markdown = pesan.teks, color = Color.White, fontSize = 14.sp)
        }
    }
}

package com.example.mywallet.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mywallet.data.Transaksi

@Composable
fun InvestmentCard(kodeEmiten: String, tanggal: String, jumlahLot: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CardDark)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = kodeEmiten,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = tanggal, color = TextGray, fontSize = 12.sp)
        }
        Box(
            modifier = Modifier
                .border(1.dp, RingColor, RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$jumlahLot LOT",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableInvestmentCard(transaksi: Transaksi, onDelete: () -> Unit) {
    var sudahHapus by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart && !sudahHapus) {
                sudahHapus = true
                onDelete()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFEF4444))
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    "Hapus",
                    fontSize = 15.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        content = {
            InvestmentCard(
                kodeEmiten = transaksi.emiten,
                tanggal = transaksi.tgl,
                jumlahLot = transaksi.lot
            )
        }
    )
}

class NumberDotTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) return TransformedText(text, OffsetMapping.Identity)

        val reversed = originalText.reversed()
        var formattedReversed = ""
        for (i in reversed.indices) {
            formattedReversed += reversed[i]
            if ((i + 1) % 3 == 0 && i != reversed.lastIndex) {
                formattedReversed += "."
            }
        }
        val formattedText = formattedReversed.reversed()

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                if (offset >= originalText.length) return formattedText.length
                val totalDots = (originalText.length - 1) / 3
                val dotsToRight = (originalText.length - offset - 1) / 3
                return offset + (totalDots - dotsToRight)
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                if (offset >= formattedText.length) return originalText.length
                var originalOffset = 0
                var transformedCount = 0
                while (transformedCount < offset && originalOffset < originalText.length) {
                    if (formattedText[transformedCount] != '.') originalOffset++
                    transformedCount++
                }
                return originalOffset
            }
        }

        return TransformedText(AnnotatedString(formattedText), offsetMapping)
    }
}
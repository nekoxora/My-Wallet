package com.example.mywallet.ui.theme

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mywallet.ui.theme.NumberDotTransformation
import com.example.mywallet.data.InvestasiData
import com.example.mywallet.data.RetrofitClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormInvestasi(onBack: () -> Unit) {
    var kodeEmiten by remember { mutableStateOf("") }
    var jumlahLot by remember { mutableStateOf("") }
    var hargaBeli by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Tambah Portofolio",
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = kodeEmiten,
            onValueChange = { kodeEmiten = it },
            label = { Text("Kode Emiten", color = TextGray) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(15.dp),
            textStyle = TextStyle(
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 16.sp
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = RingColor,
                unfocusedBorderColor = TextGray,
                focusedLabelColor = RingColor,
                unfocusedLabelColor = TextGray,
                cursorColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = jumlahLot,
            onValueChange = { input ->
                if (input.all { char -> char.isDigit() }) jumlahLot = input
            },
            label = { Text("Jumlah Lot", color = TextGray) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(15.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            visualTransformation = NumberDotTransformation(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = RingColor,
                unfocusedBorderColor = TextGray,
                focusedLabelColor = RingColor,
                unfocusedLabelColor = TextGray,
                cursorColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = hargaBeli,
            onValueChange = { input ->
                if (input.matches(Regex("^\\d*\\.?\\d*$"))) {
                    hargaBeli = input
                }
            },
            label = { Text("Harga per Lembar", color = TextGray) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(15.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                focusedBorderColor = RingColor, unfocusedBorderColor = TextGray,
                focusedLabelColor = RingColor, unfocusedLabelColor = TextGray,
                cursorColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (kodeEmiten.isNotEmpty() && jumlahLot.isNotEmpty() && hargaBeli.isNotEmpty()) {
                    val dataKirim = InvestasiData(
                        kode_emiten = kodeEmiten.uppercase(),
                        jumlah_lot = jumlahLot.toIntOrNull() ?: 0,
                        harga_beli = hargaBeli.replace(',', '.').toDoubleOrNull() ?: 0.0
                    )
                    coroutineScope.launch {
                        try {
                            val response =
                                RetrofitClient.instance.simpanInvestasi(dataKirim)
                            Toast.makeText(
                                context,
                                response.message,
                                Toast.LENGTH_SHORT
                            ).show()
                            if (response.status == "success") {
                                onBack()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "Gagal: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "Isi semua data!", Toast.LENGTH_SHORT)
                        .show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(25.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RingColor)
        ) {
            Text("Simpan Investasi", color = Color.White, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(15.dp))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(25.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = RingColor
            ),
            border = BorderStroke(1.dp, RingColor)
        ) {
            Text("Back", fontSize = 16.sp)
        }
    }
}
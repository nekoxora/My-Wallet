package com.example.mywallet.data

data class InvestasiData(
    val device_id: String,
    val kode_emiten: String,
    val jumlah_lot: Int,
    val harga_beli: Double
)

data class DeleteData(
    val device_id: String,
    val emiten: String
)

data class ApiResponse(val status: String, val message: String)

data class Transaksi(
    val id: Int,
    val emiten: String,
    val tgl: String,
    val lot: Int,
    val harga: Double
)

data class BeritaSaham(
    val id: String,
    val judul: String,
    val isi: String,
    val emiten: String,
    val tgl: String,
    val harga: Int = 0,
    val persentase: String = "-",
    val url: String
)

data class BeritaResponse(
    val status: String,
    val total: Int,
    val data: List<BeritaSaham>
)
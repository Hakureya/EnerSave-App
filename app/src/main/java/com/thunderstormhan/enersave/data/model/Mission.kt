package com.thunderstormhan.enersave.data.model

data class Mission(
    val id: String = "",           // ID Dokumen dari Firestore
    val title: String = "",        // Judul Misi (ex: "Matikan Lampu")
    val description: String = "",  // Deskripsi detail
    val rewardPoints: Int = 0,     // Jumlah koin yang didapat
    val category: String = "",     // "Hemat Banget", "Normal", "Butuh Perhatian"
    val minBillThreshold: Long = 0,// Batas minimal tagihan agar misi ini muncul
    val type: String = "Daily",    // "Daily" atau "Special"
    val isTaken: Boolean = false   // Status apakah user sedang mengambil misi ini
)

/**
 * Objek pembantu untuk pengujian UI tanpa harus konek ke Firebase (Mock Data)
 */
object DummyMissions {
    val list = listOf(
        Mission(
            id = "m1",
            title = "Bagikan Tips",
            description = "Tagihanmu rendah! Share rahasianya ke komunitas.",
            rewardPoints = 500,
            category = "Hemat Banget"
        ),
        Mission(
            id = "m2",
            title = "Kurangi AC 1 Jam",
            description = "Matikan AC 1 jam lebih awal dari biasanya.",
            rewardPoints = 1000,
            category = "Normal",
            minBillThreshold = 100000
        ),
        Mission(
            id = "m3",
            title = "Audit Energi",
            description = "Lakukan simulasi alat elektronik di menu Audit.",
            rewardPoints = 2000,
            category = "Butuh Perhatian",
            minBillThreshold = 500000
        )
    )
}
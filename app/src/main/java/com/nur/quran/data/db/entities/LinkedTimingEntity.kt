package com.nur.quran.data.db.entities

import androidx.room.Entity

@Entity(tableName = "linked_timings", primaryKeys = ["reciterId", "sura", "ayah"])
data class LinkedTimingEntity(
    val reciterId: Int,
    val sura: Int,
    val ayah: Int,
    val startMs: Long
)

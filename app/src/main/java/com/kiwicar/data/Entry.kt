package com.kiwicar.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class EntryType { BENZINA, OLIO }

@Entity(tableName = "entries")
data class Entry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: EntryType,
    val dateEpochDay: Long,
    val odometerKm: Int,
    val liters: Double,
)

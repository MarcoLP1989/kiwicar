package com.kiwicar.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface KiwiDao {
    @Query("SELECT * FROM entries ORDER BY dateEpochDay DESC, id DESC")
    fun observeAll(): Flow<List<Entry>>

    @Query("SELECT * FROM entries ORDER BY dateEpochDay DESC, id DESC")
    suspend fun getAll(): List<Entry>

    @Upsert
    suspend fun upsert(e: Entry): Long

    @Delete
    suspend fun delete(e: Entry)
}

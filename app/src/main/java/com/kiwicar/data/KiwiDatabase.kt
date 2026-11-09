package com.kiwicar.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter
    fun fromType(type: EntryType): String = type.name

    @TypeConverter
    fun toType(value: String): EntryType = EntryType.valueOf(value)
}

// exportSchema = false: schema JSON export not configured yet; add a schema dir before the first v2 migration.
@Database(entities = [Entry::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class KiwiDatabase : RoomDatabase() {
    abstract fun kiwiDao(): KiwiDao

    companion object {
        @Volatile
        private var instance: KiwiDatabase? = null

        fun get(context: Context): KiwiDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                KiwiDatabase::class.java,
                "kiwicar.db",
            ).build().also { instance = it }
        }
    }
}

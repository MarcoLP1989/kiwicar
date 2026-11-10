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

// Schema JSON is exported to app/schemas (see ksp arg in build.gradle.kts); commit it with every version bump.
@Database(entities = [Entry::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class KiwiCarDatabase : RoomDatabase() {
    abstract fun kiwiDao(): KiwiCarDao

    companion object {
        @Volatile
        private var instance: KiwiCarDatabase? = null

        fun get(context: Context): KiwiCarDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                KiwiCarDatabase::class.java,
                "kiwicar.db",
            ).build().also { instance = it }
        }
    }
}

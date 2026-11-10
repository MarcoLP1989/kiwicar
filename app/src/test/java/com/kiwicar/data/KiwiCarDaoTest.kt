package com.kiwicar.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class KiwiCarDaoTest {
    private lateinit var db: KiwiCarDatabase
    private lateinit var dao: KiwiCarDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, KiwiCarDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.kiwiDao()
    }

    @After
    fun tearDown() = db.close()

    private val fuel = Entry(type = EntryType.BENZINA, dateEpochDay = 20000, odometerKm = 1000, liters = 35.5)
    private val oil = Entry(type = EntryType.OLIO, dateEpochDay = 20010, odometerKm = 1200, liters = 1.5)

    @Test
    fun insertedEntriesAreObserved() = runBlocking {
        dao.upsert(fuel)
        dao.upsert(oil)
        val all = dao.observeAll().first()
        assertEquals(2, all.size)
        assertEquals(setOf(fuel.copy(id = all.first { it.type == EntryType.BENZINA }.id), oil.copy(id = all.first { it.type == EntryType.OLIO }.id)), all.toSet())
    }

    @Test
    fun deleteRemovesOnlyThatEntry() = runBlocking {
        dao.upsert(fuel)
        dao.upsert(oil)
        val toDelete = dao.getAll().first { it.type == EntryType.BENZINA }
        dao.delete(toDelete)
        val left = dao.getAll()
        assertEquals(1, left.size)
        assertEquals(EntryType.OLIO, left.single().type)
    }

    @Test
    fun entryTypeRoundTripsThroughStorage() = runBlocking {
        val idF = dao.upsert(fuel)
        val idO = dao.upsert(oil)
        val byId = dao.getAll().associateBy { it.id }
        assertEquals(EntryType.BENZINA, byId[idF]!!.type)
        assertEquals(EntryType.OLIO, byId[idO]!!.type)
        assertEquals(35.5, byId[idF]!!.liters, 0.0)
    }

    @Test
    fun upsertWithExistingIdUpdates() = runBlocking {
        val id = dao.upsert(fuel)
        dao.upsert(fuel.copy(id = id, liters = 40.0))
        val all = dao.getAll()
        assertEquals(1, all.size)
        assertEquals(40.0, all.single().liters, 0.0)
    }
}

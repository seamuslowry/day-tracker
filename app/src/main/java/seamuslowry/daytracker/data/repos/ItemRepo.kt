package seamuslowry.daytracker.data.repos

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import seamuslowry.daytracker.data.room.daos.ItemDao
import seamuslowry.daytracker.models.Item
import seamuslowry.daytracker.models.ItemWithConfiguration
import java.time.LocalDate
import javax.inject.Inject

interface ItemRepo {
    fun getAll(): Flow<List<Item>>
    fun get(date: LocalDate): Flow<List<Item>>
    fun getEarliest(): Flow<LocalDate>
    fun getFull(min: LocalDate, max: LocalDate): Flow<List<ItemWithConfiguration>>
    fun getFull(date: LocalDate): Flow<List<ItemWithConfiguration>>
    suspend fun save(vararg item: Item)
}

class RoomItemRepo @Inject constructor(private val itemDao: ItemDao) : ItemRepo {
    override fun getAll(): Flow<List<Item>> = itemDao.getAll()
    override fun get(date: LocalDate): Flow<List<Item>> = itemDao.get(date)
    override fun getEarliest(): Flow<LocalDate> = itemDao.getEarliestDate().filterNotNull()
    override fun getFull(min: LocalDate, max: LocalDate): Flow<List<ItemWithConfiguration>> = itemDao.getFull(min, max)
    override fun getFull(date: LocalDate): Flow<List<ItemWithConfiguration>> = itemDao.getFull(date)
    override suspend fun save(vararg item: Item) = itemDao.upsert(*item)
}

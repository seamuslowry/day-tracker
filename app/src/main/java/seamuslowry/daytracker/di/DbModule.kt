package seamuslowry.daytracker.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import seamuslowry.daytracker.data.room.DayTrackerDatabase
import seamuslowry.daytracker.data.room.MIGRATION_6_7
import seamuslowry.daytracker.data.room.MIGRATION_7_8
import seamuslowry.daytracker.data.room.daos.ItemConfigurationDao
import seamuslowry.daytracker.data.room.daos.ItemDao
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object DbModule {
    @Provides
    @Singleton
    fun provideDayTrackerDb(
        @ApplicationContext context: Context,
    ): DayTrackerDatabase = Room
        .databaseBuilder(context, DayTrackerDatabase::class.java, "day_tracker_database")
        .addMigrations(MIGRATION_6_7)
        .addMigrations(MIGRATION_7_8)
        .build()

    @Provides
    @Singleton
    fun provideItemDao(
        db: DayTrackerDatabase,
    ): ItemDao = db.itemDao()

    @Provides
    @Singleton
    fun provideItemConfigurationDao(
        db: DayTrackerDatabase,
    ): ItemConfigurationDao = db.itemConfigurationDao()
}

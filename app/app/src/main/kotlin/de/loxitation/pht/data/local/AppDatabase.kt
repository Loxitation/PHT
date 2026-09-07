package de.loxitation.pht.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class SyncStatusConverters {
    @TypeConverter
    fun fromSyncStatus(value: SyncStatus): String = value.name

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus = SyncStatus.valueOf(value)
}

@Database(
    entities = [BloodPressureMeasurementEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(SyncStatusConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bloodPressureMeasurementDao(): BloodPressureMeasurementDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pht.db",
                ).build().also { instance = it }
            }
    }
}

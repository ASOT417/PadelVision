package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        MatchEntity::class,
        PlayerEntity::class,
        ShotEntity::class,
        CalibrationPresetEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class PadelDatabase : RoomDatabase() {

    abstract fun padelDao(): PadelDao

    companion object {
        @Volatile
        private var INSTANCE: PadelDatabase? = null

        fun getDatabase(context: Context): PadelDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PadelDatabase::class.java,
                    "padel_vision.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

package com.iris.gallery.data.search

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PhotoIndexEntity::class, PhotoIndexFts::class],
    version = 2,
    exportSchema = false,
)
abstract class PhotoSearchDatabase : RoomDatabase() {
    abstract fun photoSearchDao(): PhotoSearchDao

    companion object {
        @Volatile
        private var INSTANCE: PhotoSearchDatabase? = null

        fun getInstance(context: Context): PhotoSearchDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PhotoSearchDatabase::class.java,
                    "photo_search.db",
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

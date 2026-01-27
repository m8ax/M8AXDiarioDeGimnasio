package com.mviiiax.m8ax_diariogimnasio

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Glucosa::class], version = 1)
abstract class AppDatabase2 : RoomDatabase() {
    abstract fun glucosaDao(): GlucosaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase2? = null
        fun getDatabase(context: Context): AppDatabase2 {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext, AppDatabase2::class.java, "M8AX-Glucosa_DB"
                ).allowMainThreadQueries().build()
                INSTANCE = instance
                instance
            }
        }

        fun closeInstance() {
            AppDatabase2.Companion.INSTANCE?.close()
            AppDatabase2.Companion.INSTANCE = null
        }
    }
}
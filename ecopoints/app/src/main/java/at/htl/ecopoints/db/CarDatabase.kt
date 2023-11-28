package at.htl.ecopoints.db

import androidx.room.Database
import androidx.room.RoomDatabase
import android.content.Context
import androidx.room.Room

@Database(entities = [CarData::class], version = 1)
abstract class CarDatabase : RoomDatabase() {
    abstract fun carDataDao(): CarDataDao

    companion object {
        private var INSTANCE: CarDatabase? = null

        fun getDatabase(context: Context): CarDatabase{
            if (INSTANCE == null){
                synchronized(this){
                    INSTANCE = Room.databaseBuilder(
                        context.applicationContext,
                        CarDatabase::class.java,
                        "SQLLiteCarDatabase"
                    ).build()
                }
            }
            return INSTANCE!!
        }
    }
}
package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StudioDao {
    @Query("SELECT * FROM webapp_projects ORDER BY updatedAt DESC")
    fun observeProjects(): Flow<List<WebAppProjectEntity>>

    @Query("SELECT * FROM webapp_projects ORDER BY updatedAt DESC")
    suspend fun getAllProjectsOnce(): List<WebAppProjectEntity>

    @Query("SELECT * FROM webapp_projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: Long): WebAppProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: WebAppProjectEntity): Long

    @Update
    suspend fun updateProject(project: WebAppProjectEntity)

    @Query("DELETE FROM webapp_projects WHERE id = :id")
    suspend fun deleteProjectById(id: Long)

    @Query("SELECT * FROM build_records ORDER BY createdAt DESC")
    fun observeBuildRecords(): Flow<List<BuildRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBuildRecord(record: BuildRecordEntity): Long

    @Query("DELETE FROM build_records WHERE id = :id")
    suspend fun deleteBuildRecordById(id: Long)
}

@Database(
    entities = [WebAppProjectEntity::class, BuildRecordEntity::class],
    version = 1,
    exportSchema = false
)
abstract class StudioDatabase : RoomDatabase() {
    abstract fun studioDao(): StudioDao

    companion object {
        @Volatile
        private var INSTANCE: StudioDatabase? = null

        fun getInstance(context: Context): StudioDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StudioDatabase::class.java,
                    "web2apk_studio.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

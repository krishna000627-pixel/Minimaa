package com.minimaa

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import javax.inject.Singleton

@Entity
data class Journal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val mood: Int = 2,
    val branches: String = "",
    val created: Long = System.currentTimeMillis(),
    val deleted: Long = 0
)

@Entity
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val type: String = "TODO",
    val category: String = "Personal",
    val priority: String = "Low",
    val done: Boolean = false,
    val scheduledAt: Long = 0,
    val sortOrder: Int = 0,
    val created: Long = System.currentTimeMillis(),
    val deleted: Long = 0
)

@Dao
interface LoopDao {
    @Query("SELECT * FROM Journal WHERE deleted = 0 ORDER BY created DESC") fun journals(): Flow<List<Journal>>
    @Query("SELECT * FROM Journal WHERE deleted > 0 ORDER BY deleted DESC") fun trashedJournals(): Flow<List<Journal>>
    @Query("SELECT * FROM Task WHERE deleted = 0 ORDER BY sortOrder ASC, created DESC") fun tasks(): Flow<List<Task>>
    @Query("SELECT * FROM Task WHERE deleted > 0 ORDER BY deleted DESC") fun trashedTasks(): Flow<List<Task>>
    @Upsert suspend fun upsertJ(j: Journal)
    @Upsert suspend fun upsertT(t: Task)
    @Delete suspend fun deleteJ(j: Journal)
    @Delete suspend fun deleteT(t: Task)
    @Query("DELETE FROM Journal WHERE deleted > 0") suspend fun purgeJ()
    @Query("DELETE FROM Task WHERE deleted > 0") suspend fun purgeT()
}

@Database(entities = [Journal::class, Task::class], version = 2, exportSchema = false)
abstract class AppDb : RoomDatabase() { abstract fun dao(): LoopDao }

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE Journal ADD COLUMN branches TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE Task ADD COLUMN scheduledAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE Task ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DbModule {
    @Provides @Singleton
    fun db(@ApplicationContext c: Context): AppDb =
        Room.databaseBuilder(c, AppDb::class.java, "minimaa.db")
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides fun dao(db: AppDb): LoopDao = db.dao()
}

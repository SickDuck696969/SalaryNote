package com.example.tinhluong

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "work_months")
data class WorkMonth(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var monthName: String
)

@Entity(
    tableName = "work_days",
    foreignKeys = [ForeignKey(
        entity = WorkMonth::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("monthId"),
        onDelete = ForeignKey.CASCADE
    )]
)
data class WorkDay(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val monthId: Int,
    var dateString: String,
    var shiftType: String,
    var hours: Double,
    var overtimeHours: Double = 0.0,
    var totalWage: Int
)

@Dao
interface SalaryDao {
    @Query("SELECT * FROM work_months")
    fun getAllMonths(): Flow<List<WorkMonth>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMonth(month: WorkMonth): Long

    @Update
    suspend fun updateMonth(month: WorkMonth)

    @Delete
    suspend fun deleteMonth(month: WorkMonth)

    @Query("SELECT * FROM work_days WHERE monthId = :monthId ORDER BY id ASC")
    fun getDaysForMonth(monthId: Int): Flow<List<WorkDay>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDay(day: WorkDay)

    // HÀM MỚI: Lưu 1 lúc nhiều ngày để tránh đơ app
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDays(days: List<WorkDay>)

    @Update
    suspend fun updateDay(day: WorkDay)

    @Delete
    suspend fun deleteDay(day: WorkDay)
}

@Database(entities = [WorkMonth::class, WorkDay::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun salaryDao(): SalaryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "salary_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
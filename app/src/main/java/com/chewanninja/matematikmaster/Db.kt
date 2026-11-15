package com.chewanninja.matematikmaster

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update

@Entity(tableName = "profiles", indices = [Index(value = ["username"], unique = true)])
data class Profile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val displayName: String,
    val passwordHash: String,
    val tahun: Int,
    val avatar: String,
    val photoPath: String? = null,
    val isAdmin: Boolean = false
)

@Entity(tableName = "results", indices = [Index("profileId")])
data class QuizResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val tahun: Int,
    val mode: String,
    val total: Int,
    val correct: Int,
    val points: Int,
    val seconds: Int,
    val timestamp: Long
)

@Entity(tableName = "answers", indices = [Index("profileId")])
data class AnswerRec(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val tahun: Int,
    val topic: String,
    val correct: Boolean,
    val timestamp: Long
)

@Entity(tableName = "badges", primaryKeys = ["profileId", "code"])
data class BadgeRec(
    val profileId: Long,
    val code: String,
    val timestamp: Long
)

data class TopicRow(val tahun: Int, val topic: String, val total: Int, val correct: Int)

data class Totals(val profileId: Long, val sessions: Int, val questions: Int, val correct: Int, val points: Int)

@Dao
interface MmDao {
    @Insert
    suspend fun insertProfile(p: Profile): Long

    @Update
    suspend fun updateProfile(p: Profile)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun deleteProfile(id: Long)

    @Query("DELETE FROM results WHERE profileId = :id")
    suspend fun deleteResults(id: Long)

    @Query("DELETE FROM answers WHERE profileId = :id")
    suspend fun deleteAnswers(id: Long)

    @Query("DELETE FROM badges WHERE profileId = :id")
    suspend fun deleteBadges(id: Long)

    @Query("SELECT * FROM profiles ORDER BY isAdmin DESC, displayName COLLATE NOCASE")
    suspend fun profiles(): List<Profile>

    @Query("SELECT * FROM profiles WHERE username = :u COLLATE NOCASE LIMIT 1")
    suspend fun byUsername(u: String): Profile?

    @Insert
    suspend fun insertResult(r: QuizResult)

    @Insert
    suspend fun insertAnswers(a: List<AnswerRec>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBadge(b: BadgeRec): Long

    @Query("SELECT * FROM badges WHERE profileId = :id")
    suspend fun badges(id: Long): List<BadgeRec>

    @Query("SELECT * FROM results WHERE profileId = :id ORDER BY timestamp DESC")
    suspend fun allResults(id: Long): List<QuizResult>

    @Query("SELECT tahun AS tahun, topic AS topic, COUNT(*) AS total, SUM(correct) AS correct FROM answers WHERE profileId = :id GROUP BY tahun, topic ORDER BY tahun, topic")
    suspend fun topicStats(id: Long): List<TopicRow>

    @Query("SELECT profileId AS profileId, COUNT(*) AS sessions, SUM(total) AS questions, SUM(correct) AS correct, SUM(points) AS points FROM results GROUP BY profileId")
    suspend fun totals(): List<Totals>
}

@Database(
    entities = [Profile::class, QuizResult::class, AnswerRec::class, BadgeRec::class],
    version = 1,
    exportSchema = false
)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): MmDao

    companion object {
        @Volatile
        private var inst: AppDb? = null

        fun get(c: Context): AppDb = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(c.applicationContext, AppDb::class.java, "matematik_master.db")
                .fallbackToDestructiveMigration()
                .build()
                .also { inst = it }
        }
    }
}

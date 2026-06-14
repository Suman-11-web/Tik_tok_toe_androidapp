package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM match_records ORDER BY timestamp DESC")
    fun getAllMatches(): Flow<List<MatchRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchRecord)

    @Query("SELECT * FROM profile_stats WHERE id = 'primary_user_profile' LIMIT 1")
    fun getProfileStats(): Flow<ProfileStats?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateProfileStats(stats: ProfileStats)

    @Query("DELETE FROM match_records")
    suspend fun clearMatchHistory()
}

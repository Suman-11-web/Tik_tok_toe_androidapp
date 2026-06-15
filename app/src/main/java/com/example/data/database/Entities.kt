package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "match_records")
data class MatchRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val mode: String, // "ONLINE", "VS_AI", "LOCAL"
    val roomCode: String?,
    val opponentName: String,
    val playerSymbol: String, // "X" or "O"
    val outcome: String, // "WIN", "LOSS", "DRAW"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "profile_stats")
data class ProfileStats(
    @PrimaryKey val id: String = "primary_user_profile",
    val username: String = "NeonPlayer",
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0,
    val currentStreak: Int = 0
)

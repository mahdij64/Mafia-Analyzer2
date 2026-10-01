package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    // --- GAMES ---
    @Query("SELECT * FROM games ORDER BY updatedAt DESC")
    fun getAllGames(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games ORDER BY updatedAt DESC")
    suspend fun getAllGamesDirect(): List<GameEntity>

    @Query("SELECT * FROM games WHERE id = :gameId LIMIT 1")
    fun getGameById(gameId: Long): Flow<GameEntity?>

    @Query("SELECT * FROM games WHERE id = :gameId LIMIT 1")
    suspend fun getGameByIdDirect(gameId: Long): GameEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: GameEntity): Long

    @Update
    suspend fun updateGame(game: GameEntity)

    @Query("UPDATE games SET currentStageIndex = :stageIndex, updatedAt = :timestamp WHERE id = :gameId")
    suspend fun updateGameStage(gameId: Long, stageIndex: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE games SET currentStageIndex = :stageIndex, maxUnlockedStageIndex = :maxUnlocked, updatedAt = :timestamp WHERE id = :gameId")
    suspend fun updateGameStageAndUnlocked(gameId: Long, stageIndex: Int, maxUnlocked: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE games SET name = :name, updatedAt = :timestamp WHERE id = :gameId")
    suspend fun renameGame(gameId: Long, name: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE games SET citizenCount = :citizenCount, mafiaCount = :mafiaCount, independentCount = :independentCount, ownerPlayerId = :ownerPlayerId, ownerRole = :ownerRole, updatedAt = :timestamp WHERE id = :gameId")
    suspend fun updateGameScenarioAndOwner(
        gameId: Long,
        citizenCount: Int,
        mafiaCount: Int,
        independentCount: Int,
        ownerPlayerId: Long?,
        ownerRole: String,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM players WHERE gameId = :gameId")
    suspend fun deletePlayersByGameId(gameId: Long)

    @Query("DELETE FROM targets WHERE gameId = :gameId")
    suspend fun deleteTargetsByGameId(gameId: Long)

    @Query("DELETE FROM player_notes WHERE gameId = :gameId")
    suspend fun deleteNotesByGameId(gameId: Long)

    @Query("DELETE FROM votes WHERE gameId = :gameId")
    suspend fun deleteVotesByGameId(gameId: Long)

    @Query("DELETE FROM manual_suspicions WHERE gameId = :gameId")
    suspend fun deleteManualSuspicionsByGameId(gameId: Long)

    @Query("DELETE FROM secret_notes WHERE gameId = :gameId")
    suspend fun deleteSecretNotesByGameId(gameId: Long)

    @Query("DELETE FROM algorithm_weights WHERE gameId = :gameId")
    suspend fun deleteWeightsByGameId(gameId: Long)

    @Query("DELETE FROM games WHERE id = :gameId")
    suspend fun deleteGameById(gameId: Long)

    @Transaction
    suspend fun completelyDeleteGame(gameId: Long) {
        deletePlayersByGameId(gameId)
        deleteTargetsByGameId(gameId)
        deleteNotesByGameId(gameId)
        deleteVotesByGameId(gameId)
        deleteManualSuspicionsByGameId(gameId)
        deleteSecretNotesByGameId(gameId)
        deleteWeightsByGameId(gameId)
        deleteGameById(gameId)
        deleteOrphanedData()
    }

    @Query("DELETE FROM players WHERE gameId NOT IN (SELECT id FROM games)")
    suspend fun deleteOrphanedPlayers()

    @Query("DELETE FROM targets WHERE gameId NOT IN (SELECT id FROM games)")
    suspend fun deleteOrphanedTargets()

    @Query("DELETE FROM player_notes WHERE gameId NOT IN (SELECT id FROM games)")
    suspend fun deleteOrphanedNotes()

    @Query("DELETE FROM votes WHERE gameId NOT IN (SELECT id FROM games)")
    suspend fun deleteOrphanedVotes()

    @Query("DELETE FROM manual_suspicions WHERE gameId NOT IN (SELECT id FROM games)")
    suspend fun deleteOrphanedManualSuspicions()

    @Query("DELETE FROM secret_notes WHERE gameId NOT IN (SELECT id FROM games)")
    suspend fun deleteOrphanedSecretNotes()

    @Query("DELETE FROM algorithm_weights WHERE gameId NOT IN (SELECT id FROM games)")
    suspend fun deleteOrphanedWeights()

    @Transaction
    suspend fun deleteOrphanedData() {
        deleteOrphanedPlayers()
        deleteOrphanedTargets()
        deleteOrphanedNotes()
        deleteOrphanedVotes()
        deleteOrphanedManualSuspicions()
        deleteOrphanedSecretNotes()
        deleteOrphanedWeights()
    }

    @Transaction
    suspend fun clearAllData() {
        deleteAllTargets()
        deleteAllNotes()
        deleteAllVotes()
        deleteAllManualSuspicions()
        deleteAllSecretNotes()
        deleteAllWeights()
        deleteAllPlayers()
        deleteAllGames()
    }

    @Query("DELETE FROM games")
    suspend fun deleteAllGames()

    @Query("DELETE FROM players")
    suspend fun deleteAllPlayers()

    @Query("DELETE FROM targets")
    suspend fun deleteAllTargets()

    @Query("DELETE FROM player_notes")
    suspend fun deleteAllNotes()

    @Query("DELETE FROM votes")
    suspend fun deleteAllVotes()

    @Query("DELETE FROM manual_suspicions")
    suspend fun deleteAllManualSuspicions()

    @Query("DELETE FROM secret_notes")
    suspend fun deleteAllSecretNotes()

    @Query("DELETE FROM algorithm_weights")
    suspend fun deleteAllWeights()

    // --- PLAYERS ---
    @Query("SELECT * FROM players WHERE gameId = :gameId ORDER BY displayOrder ASC, id ASC")
    fun getPlayersForGame(gameId: Long): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players WHERE gameId = :gameId ORDER BY displayOrder ASC, id ASC")
    suspend fun getPlayersForGameDirect(gameId: Long): List<PlayerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayer(player: PlayerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayers(players: List<PlayerEntity>)

    @Update
    suspend fun updatePlayer(player: PlayerEntity)

    @Delete
    suspend fun deletePlayer(player: PlayerEntity)

    @Query("UPDATE players SET isEliminated = :isEliminated, eliminatedStageIndex = :stageIndex WHERE id = :playerId")
    suspend fun setPlayerEliminated(playerId: Long, isEliminated: Boolean, stageIndex: Int?)

    @Query("UPDATE players SET name = :name WHERE id = :playerId")
    suspend fun renamePlayer(playerId: Long, name: String)

    @Query("UPDATE players SET knownRole = :role WHERE id = :playerId")
    suspend fun setPlayerKnownRole(playerId: Long, role: String?)

    @Query("UPDATE players SET knownRole = NULL WHERE gameId = :gameId AND isOwner = 0")
    suspend fun clearNonOwnerRoles(gameId: Long)

    @Query("UPDATE players SET isOwner = (id == :ownerPlayerId), knownRole = CASE WHEN id == :ownerPlayerId THEN :ownerRole ELSE knownRole END WHERE gameId = :gameId")
    suspend fun updateOwnerPlayer(gameId: Long, ownerPlayerId: Long?, ownerRole: String)

    @Update
    suspend fun updatePlayers(players: List<PlayerEntity>)

    @Query("UPDATE players SET displayOrder = :order WHERE id = :playerId")
    suspend fun updatePlayerOrder(playerId: Long, order: Int)

    // --- TARGETS ---
    @Query("SELECT * FROM targets WHERE gameId = :gameId")
    fun getAllTargetsForGame(gameId: Long): Flow<List<TargetEntity>>

    @Query("SELECT * FROM targets WHERE gameId = :gameId")
    suspend fun getAllTargetsForGameDirect(gameId: Long): List<TargetEntity>

    @Query("SELECT * FROM targets WHERE gameId = :gameId AND stageIndex = :stageIndex")
    fun getTargetsForStage(gameId: Long, stageIndex: Int): Flow<List<TargetEntity>>

    @Query("DELETE FROM targets WHERE gameId = :gameId AND stageIndex = :stageIndex AND sourcePlayerId = :sourcePlayerId")
    suspend fun clearTargetsForPlayerInStage(gameId: Long, stageIndex: Int, sourcePlayerId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTargets(targets: List<TargetEntity>)

    @Transaction
    suspend fun replaceTargetsForPlayerInStage(
        gameId: Long,
        stageIndex: Int,
        sourcePlayerId: Long,
        targetPlayerIds: List<Long>
    ) {
        clearTargetsForPlayerInStage(gameId, stageIndex, sourcePlayerId)
        if (targetPlayerIds.isNotEmpty()) {
            val entities = targetPlayerIds.map { targetId ->
                TargetEntity(
                    gameId = gameId,
                    stageIndex = stageIndex,
                    sourcePlayerId = sourcePlayerId,
                    targetPlayerId = targetId
                )
            }
            insertTargets(entities)
        }
    }

    // --- NOTES ---
    @Query("SELECT * FROM player_notes WHERE gameId = :gameId ORDER BY timestamp DESC")
    fun getAllNotesForGame(gameId: Long): Flow<List<PlayerNoteEntity>>

    @Query("SELECT * FROM player_notes WHERE gameId = :gameId ORDER BY timestamp DESC")
    suspend fun getAllNotesForGameDirect(gameId: Long): List<PlayerNoteEntity>

    @Query("SELECT * FROM player_notes WHERE gameId = :gameId AND playerId = :playerId ORDER BY timestamp DESC")
    fun getNotesForPlayer(gameId: Long, playerId: Long): Flow<List<PlayerNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: PlayerNoteEntity): Long

    @Query("DELETE FROM player_notes WHERE id = :noteId")
    suspend fun deleteNoteById(noteId: Long)

    // --- SECRET NOTES (MAFIA STEALTH MODE ONLY) ---
    @Query("SELECT * FROM secret_notes WHERE gameId = :gameId ORDER BY timestamp DESC")
    fun getSecretNotesForGame(gameId: Long): Flow<List<SecretNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSecretNote(note: SecretNoteEntity): Long

    @Query("DELETE FROM secret_notes WHERE id = :noteId")
    suspend fun deleteSecretNoteById(noteId: Long)

    // --- VOTES ---
    @Query("SELECT * FROM votes WHERE gameId = :gameId")
    fun getAllVotesForGame(gameId: Long): Flow<List<VoteEntity>>

    @Query("SELECT * FROM votes WHERE gameId = :gameId")
    suspend fun getAllVotesForGameDirect(gameId: Long): List<VoteEntity>

    @Query("SELECT * FROM votes WHERE gameId = :gameId AND stageIndex = :stageIndex")
    fun getVotesForStage(gameId: Long, stageIndex: Int): Flow<List<VoteEntity>>

    @Query("DELETE FROM votes WHERE gameId = :gameId AND stageIndex = :stageIndex AND voterId = :voterId")
    suspend fun deleteVoteForVoter(gameId: Long, stageIndex: Int, voterId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVote(vote: VoteEntity): Long

    @Transaction
    suspend fun setVote(gameId: Long, stageIndex: Int, voterId: Long, targetId: Long) {
        deleteVoteForVoter(gameId, stageIndex, voterId)
        if (targetId >= 0) {
            insertVote(VoteEntity(gameId = gameId, stageIndex = stageIndex, voterId = voterId, targetId = targetId))
        }
    }

    @Query("DELETE FROM votes WHERE gameId = :gameId AND stageIndex = :stageIndex")
    suspend fun clearVotesForStage(gameId: Long, stageIndex: Int)

    // --- MANUAL SUSPICIONS ---
    @Query("SELECT * FROM manual_suspicions WHERE gameId = :gameId")
    fun getAllManualSuspicionsForGame(gameId: Long): Flow<List<ManualSuspicionEntity>>

    @Query("SELECT * FROM manual_suspicions WHERE gameId = :gameId")
    suspend fun getAllManualSuspicionsForGameDirect(gameId: Long): List<ManualSuspicionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertManualSuspicion(entity: ManualSuspicionEntity)

    // --- ALGORITHM WEIGHTS ---
    @Query("SELECT * FROM algorithm_weights WHERE gameId = :gameId LIMIT 1")
    fun getWeightsForGame(gameId: Long): Flow<AlgorithmWeightEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveWeights(weights: AlgorithmWeightEntity)

    // --- AI SETTINGS (app-level singleton row, id = 0) ---
    @Query("SELECT * FROM ai_settings WHERE id = 0 LIMIT 1")
    fun getAiSettings(): Flow<AiSettingsEntity?>

    @Query("SELECT * FROM ai_settings WHERE id = 0 LIMIT 1")
    suspend fun getAiSettingsDirect(): AiSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAiSettings(settings: AiSettingsEntity)
}

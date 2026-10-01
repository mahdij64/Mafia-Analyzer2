package com.example.data.repository

import com.example.data.local.AlgorithmWeightEntity
import com.example.data.local.AppDao
import com.example.data.local.GameEntity
import com.example.data.local.ManualSuspicionEntity
import com.example.data.local.PlayerEntity
import com.example.data.local.PlayerNoteEntity
import com.example.data.local.TargetEntity
import com.example.data.local.VoteEntity
import kotlinx.coroutines.flow.Flow

class MafiaRepository(private val dao: AppDao) {

    val allGames: Flow<List<GameEntity>> = dao.getAllGames()

    fun getGame(gameId: Long): Flow<GameEntity?> = dao.getGameById(gameId)

    suspend fun createGame(
        name: String,
        playerNames: List<String>,
        citizenCount: Int = 0,
        mafiaCount: Int = 0,
        independentCount: Int = 0,
        ownerPlayerIndex: Int? = null,
        ownerRole: String = "CITIZEN",
        mafiaTeammateIndices: Set<Int> = emptySet()
    ): Long {
        val validPlayers = playerNames.filter { it.isNotBlank() }
        val total = validPlayers.size.coerceAtLeast(1)
        val calculatedMafia = if (mafiaCount > 0) mafiaCount else (total / 3).coerceAtLeast(1)
        val calculatedCitizen = if (citizenCount > 0) citizenCount else (total - calculatedMafia - independentCount).coerceAtLeast(1)

        val gameId = dao.insertGame(
            GameEntity(
                name = name.ifBlank { "بازی جدید مافیا" },
                createdAt = System.currentTimeMillis(),
                currentStageIndex = 0,
                maxUnlockedStageIndex = 0,
                updatedAt = System.currentTimeMillis(),
                citizenCount = calculatedCitizen,
                mafiaCount = calculatedMafia,
                independentCount = independentCount,
                ownerRole = ownerRole
            )
        )
        var ownerPId: Long? = null
        for ((index, pName) in validPlayers.withIndex()) {
            val isOwner = ownerPlayerIndex != null && index == ownerPlayerIndex
            val isTeammate = ownerRole == "MAFIA" && mafiaTeammateIndices.contains(index)
            val assignedRole = when {
                isOwner -> ownerRole
                isTeammate -> "MAFIA"
                else -> null
            }
            val pId = dao.insertPlayer(
                PlayerEntity(
                    gameId = gameId,
                    name = pName.trim(),
                    isEliminated = false,
                    displayOrder = index,
                    isOwner = isOwner,
                    knownRole = assignedRole
                )
            )
            if (isOwner) {
                ownerPId = pId
            }
        }
        if (ownerPId != null) {
            dao.updateGameScenarioAndOwner(
                gameId = gameId,
                citizenCount = calculatedCitizen,
                mafiaCount = calculatedMafia,
                independentCount = independentCount,
                ownerPlayerId = ownerPId,
                ownerRole = ownerRole
            )
        }
        dao.saveWeights(AlgorithmWeightEntity(gameId = gameId))
        return gameId
    }

    suspend fun setPlayerKnownRole(playerId: Long, role: String?) {
        dao.setPlayerKnownRole(playerId, role)
    }

    suspend fun updateMafiaTeammates(gameId: Long, mafiaPlayerIds: Set<Long>) {
        dao.clearNonOwnerRoles(gameId)
        for (id in mafiaPlayerIds) {
            dao.setPlayerKnownRole(id, "MAFIA")
        }
    }

    suspend fun updateGameScenarioAndOwner(
        gameId: Long,
        citizenCount: Int,
        mafiaCount: Int,
        independentCount: Int,
        ownerPlayerId: Long?,
        ownerRole: String
    ) {
        dao.updateGameScenarioAndOwner(
            gameId = gameId,
            citizenCount = citizenCount,
            mafiaCount = mafiaCount,
            independentCount = independentCount,
            ownerPlayerId = ownerPlayerId,
            ownerRole = ownerRole
        )
        dao.updateOwnerPlayer(gameId, ownerPlayerId, ownerRole)
    }

    suspend fun duplicateGame(gameId: Long): Long {
        val originalGame = dao.getGameByIdDirect(gameId) ?: return 0L
        val originalPlayers = dao.getPlayersForGameDirect(gameId)
        val originalTargets = dao.getAllTargetsForGameDirect(gameId)
        val originalNotes = dao.getAllNotesForGameDirect(gameId)
        val originalVotes = dao.getAllVotesForGameDirect(gameId)
        val originalSuspicions = dao.getAllManualSuspicionsForGameDirect(gameId)

        val newGameId = dao.insertGame(
            GameEntity(
                name = "${originalGame.name} (کپی)",
                createdAt = System.currentTimeMillis(),
                currentStageIndex = originalGame.currentStageIndex,
                maxUnlockedStageIndex = originalGame.maxUnlockedStageIndex,
                updatedAt = System.currentTimeMillis()
            )
        )

        // Mapping old playerId -> new playerId
        val playerMap = mutableMapOf<Long, Long>()
        for (p in originalPlayers) {
            val newPId = dao.insertPlayer(
                PlayerEntity(
                    gameId = newGameId,
                    name = p.name,
                    isEliminated = p.isEliminated,
                    eliminatedStageIndex = p.eliminatedStageIndex,
                    displayOrder = p.displayOrder
                )
            )
            playerMap[p.id] = newPId
        }

        // Duplicate targets
        val newTargets = originalTargets.mapNotNull { t ->
            val newSource = playerMap[t.sourcePlayerId]
            val newTarget = playerMap[t.targetPlayerId]
            if (newSource != null && newTarget != null) {
                TargetEntity(
                    gameId = newGameId,
                    stageIndex = t.stageIndex,
                    sourcePlayerId = newSource,
                    targetPlayerId = newTarget
                )
            } else null
        }
        if (newTargets.isNotEmpty()) {
            dao.insertTargets(newTargets)
        }

        // Duplicate notes
        for (n in originalNotes) {
            val newPId = playerMap[n.playerId] ?: continue
            dao.insertNote(
                PlayerNoteEntity(
                    gameId = newGameId,
                    playerId = newPId,
                    stageIndex = n.stageIndex,
                    category = n.category,
                    text = n.text,
                    timestamp = n.timestamp
                )
            )
        }

        // Duplicate votes
        for (v in originalVotes) {
            val newVoter = playerMap[v.voterId] ?: continue
            val newTarget = if (v.targetId >= 0) playerMap[v.targetId] ?: -1L else -1L
            dao.insertVote(
                VoteEntity(
                    gameId = newGameId,
                    stageIndex = v.stageIndex,
                    voterId = newVoter,
                    targetId = newTarget
                )
            )
        }

        // Duplicate manual suspicions
        for (s in originalSuspicions) {
            val newPId = playerMap[s.playerId] ?: continue
            dao.insertManualSuspicion(
                ManualSuspicionEntity(
                    gameId = newGameId,
                    playerId = newPId,
                    stageIndex = s.stageIndex,
                    score = s.score
                )
            )
        }

        dao.saveWeights(AlgorithmWeightEntity(gameId = newGameId))
        return newGameId
    }

    suspend fun renameGame(gameId: Long, name: String) = dao.renameGame(gameId, name)

    suspend fun deleteGame(gameId: Long) = dao.completelyDeleteGame(gameId)

    suspend fun clearAllData() = dao.clearAllData()

    suspend fun getAllGamesDirect(): List<GameEntity> = dao.getAllGamesDirect()

    suspend fun updateStage(gameId: Long, stageIndex: Int) {
        val game = dao.getGameByIdDirect(gameId)
        val maxUnlocked = maxOf(game?.maxUnlockedStageIndex ?: 0, stageIndex)
        dao.updateGameStageAndUnlocked(gameId, stageIndex, maxUnlocked)
    }

    suspend fun advanceStage(gameId: Long, nextStageIndex: Int) {
        val game = dao.getGameByIdDirect(gameId)
        val maxUnlocked = maxOf(game?.maxUnlockedStageIndex ?: 0, nextStageIndex)
        dao.updateGameStageAndUnlocked(gameId, nextStageIndex, maxUnlocked)
    }

    fun getPlayers(gameId: Long): Flow<List<PlayerEntity>> = dao.getPlayersForGame(gameId)

    suspend fun addPlayer(gameId: Long, name: String): Long {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return -1L
        return dao.insertPlayer(
            PlayerEntity(
                gameId = gameId,
                name = trimmed,
                isEliminated = false
            )
        )
    }

    suspend fun updatePlayer(player: PlayerEntity) = dao.updatePlayer(player)

    suspend fun renamePlayer(playerId: Long, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isNotBlank()) {
            dao.renamePlayer(playerId, trimmed)
        }
    }

    suspend fun reorderPlayers(reorderedPlayers: List<PlayerEntity>) {
        val updated = reorderedPlayers.mapIndexed { idx, p -> p.copy(displayOrder = idx) }
        dao.updatePlayers(updated)
    }

    suspend fun deletePlayer(player: PlayerEntity) = dao.deletePlayer(player)

    suspend fun setPlayerEliminated(playerId: Long, isEliminated: Boolean, stageIndex: Int?) =
        dao.setPlayerEliminated(playerId, isEliminated, stageIndex)

    fun getAllTargets(gameId: Long): Flow<List<TargetEntity>> = dao.getAllTargetsForGame(gameId)

    suspend fun replaceTargets(gameId: Long, stageIndex: Int, sourcePlayerId: Long, targetPlayerIds: List<Long>) =
        dao.replaceTargetsForPlayerInStage(gameId, stageIndex, sourcePlayerId, targetPlayerIds)

    fun getAllNotes(gameId: Long): Flow<List<PlayerNoteEntity>> = dao.getAllNotesForGame(gameId)

    suspend fun addNote(gameId: Long, playerId: Long, stageIndex: Int, category: String, text: String): Long =
        dao.insertNote(
            PlayerNoteEntity(
                gameId = gameId,
                playerId = playerId,
                stageIndex = stageIndex,
                category = category,
                text = text.trim()
            )
        )

    suspend fun deleteNote(noteId: Long) = dao.deleteNoteById(noteId)

    // --- SECRET NOTES (MAFIA STEALTH ONLY) ---
    fun getSecretNotes(gameId: Long): Flow<List<com.example.data.local.SecretNoteEntity>> =
        dao.getSecretNotesForGame(gameId)

    suspend fun addSecretNote(gameId: Long, playerId: Long, stageIndex: Int, content: String, suggestion: String = ""): Long =
        dao.insertSecretNote(
            com.example.data.local.SecretNoteEntity(
                gameId = gameId,
                playerId = playerId,
                stageIndex = stageIndex,
                content = content.trim(),
                misdirectionSuggestion = suggestion
            )
        )

    suspend fun deleteSecretNote(noteId: Long) = dao.deleteSecretNoteById(noteId)

    fun getAllVotes(gameId: Long): Flow<List<VoteEntity>> = dao.getAllVotesForGame(gameId)

    suspend fun recordVote(gameId: Long, stageIndex: Int, voterId: Long, targetId: Long) =
        dao.setVote(gameId, stageIndex, voterId, targetId)

    suspend fun clearVotesForStage(gameId: Long, stageIndex: Int) =
        dao.clearVotesForStage(gameId, stageIndex)

    fun getAllManualSuspicions(gameId: Long): Flow<List<ManualSuspicionEntity>> =
        dao.getAllManualSuspicionsForGame(gameId)

    suspend fun setManualSuspicion(gameId: Long, playerId: Long, stageIndex: Int, score: Int) =
        dao.insertManualSuspicion(
            ManualSuspicionEntity(
                gameId = gameId,
                playerId = playerId,
                stageIndex = stageIndex,
                score = score
            )
        )

    fun getWeights(gameId: Long): Flow<AlgorithmWeightEntity?> = dao.getWeightsForGame(gameId)

    suspend fun saveWeights(weights: AlgorithmWeightEntity) = dao.saveWeights(weights)
}

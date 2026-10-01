package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val currentStageIndex: Int = 0,
    val maxUnlockedStageIndex: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
    val citizenCount: Int = 0,
    val mafiaCount: Int = 0,
    val independentCount: Int = 0,
    val ownerPlayerId: Long? = null,
    val ownerRole: String = "CITIZEN" // "CITIZEN", "MAFIA", "INDEPENDENT"
)

@Entity(
    tableName = "players",
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["gameId"])]
)
data class PlayerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val gameId: Long,
    val name: String,
    val isEliminated: Boolean = false,
    val eliminatedStageIndex: Int? = null,
    val displayOrder: Int = 0,
    val isOwner: Boolean = false,
    val knownRole: String? = null // "CITIZEN", "MAFIA", "INDEPENDENT"
)

@Entity(
    tableName = "targets",
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["gameId", "stageIndex"]),
        Index(value = ["sourcePlayerId"]),
        Index(value = ["targetPlayerId"])
    ]
)
data class TargetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val gameId: Long,
    val stageIndex: Int,
    val sourcePlayerId: Long,
    val targetPlayerId: Long
)

@Entity(
    tableName = "player_notes",
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["gameId", "playerId"]),
        Index(value = ["stageIndex"])
    ]
)
data class PlayerNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val gameId: Long,
    val playerId: Long,
    val stageIndex: Int,
    val category: String, // TALK, SUSPICIOUS, OPINION, FREE
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "votes",
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["gameId", "stageIndex"]),
        Index(value = ["voterId"]),
        Index(value = ["targetId"])
    ]
)
data class VoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val gameId: Long,
    val stageIndex: Int,
    val voterId: Long,
    val targetId: Long // -1 if skipped or no vote
)

@Entity(
    tableName = "manual_suspicions",
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["gameId", "playerId", "stageIndex"], unique = true)
    ]
)
data class ManualSuspicionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val gameId: Long,
    val playerId: Long,
    val stageIndex: Int,
    val score: Int // 0 to 100
)

@Entity(tableName = "algorithm_weights")
data class AlgorithmWeightEntity(
    @PrimaryKey
    val gameId: Long,
    val manualWeight: Float = 0.35f,
    val suspiciousNoteWeight: Float = 0.25f,
    val targetSpreadWeight: Float = 0.15f,
    val targetFlipWeight: Float = 0.15f,
    val votesReceivedWeight: Float = 0.10f
)

@Entity(
    tableName = "secret_notes",
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["gameId", "playerId"]),
        Index(value = ["stageIndex"])
    ]
)
data class SecretNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val gameId: Long,
    val playerId: Long,
    val stageIndex: Int,
    val content: String,
    val misdirectionSuggestion: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Application-level AI provider configuration (single row, id = 0).
 *
 * The app talks to any OpenAI-compatible endpoint (e.g. a 9router deployment):
 * - GET  {baseUrl}/models            → list of available models (shown in the combo box)
 * - POST {baseUrl}/chat/completions  → chat/analysis requests
 *
 * When not configured, the analyzer falls back to the Gemini REST API key from
 * BuildConfig (injected via the secrets plugin) and finally to the local
 * on-device heuristic analysis.
 */
@Entity(tableName = "ai_settings")
data class AiSettingsEntity(
    @PrimaryKey
    val id: Int = SINGLETON_ID,
    val baseUrl: String = DEFAULT_BASE_URL,
    val apiKey: String = "",
    val model: String = "",
    val temperature: Float = 0.4f,
    val updatedAt: Long = System.currentTimeMillis()
) {
    /** True when the user has provided everything needed to call the router. */
    @get:Ignore
    val isRouterConfigured: Boolean
        get() = baseUrl.isNotBlank() && apiKey.isNotBlank() && model.isNotBlank()

    companion object {
        const val SINGLETON_ID = 0

        /** Default OpenAI-compatible endpoint (9router). The user only needs to paste an API key. */
        const val DEFAULT_BASE_URL = "https://9router-production-e6c9.up.railway.app/v1"
    }
}

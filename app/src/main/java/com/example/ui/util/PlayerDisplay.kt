package com.example.ui.util

import com.example.data.local.GameEntity
import com.example.data.local.PlayerEntity

/**
 * Helpers for displaying player information safely depending on
 * whether the local user is a citizen, mafia, or independent role.
 *
 * If the owner (local user) plays a non-citizen role (Mafia or
 * Independent), their own identity is hidden under a stable code
 * so the screen can be shown to neighbours without revealing their
 * role or real name.
 */
data class SafePlayerDisplay(
    val displayName: String,   // what to show in the UI
    val isOwner: Boolean,      // whether this player is the local user
    val isHidden: Boolean,     // whether the real name is being concealed
    val roleCode: String       // "CITIZEN", "MAFIA", "INDEPENDENT"
)

object PlayerDisplay {
    /**
     * Returns the SafePlayerDisplay for [player] given the current
     * [game]. If the local user owns the game and has a non-citizen
     * role, the owner's own display name is replaced with a code.
     */
    fun forPlayer(player: PlayerEntity, game: GameEntity?): SafePlayerDisplay {
        val isOwner = game?.ownerPlayerId == player.id
        val role = if (isOwner) game.ownerRole else "CITIZEN"
        val isHidden = isOwner && role != "CITIZEN"
        val display = if (isHidden) {
            // Code format: ROLE + short suffix (e.g. "MAFIA-01")
            val short = (player.id % 100).toString().padStart(2, '0')
            "${role}-${short}"
        } else {
            player.name
        }
        return SafePlayerDisplay(
            displayName = display,
            isOwner = isOwner,
            isHidden = isHidden,
            roleCode = role
        )
    }

    /** Short tag/badge for non-citizen owners in the reports. */
    fun ownerTag(game: GameEntity?): String {
        val role = game?.ownerRole ?: "CITIZEN"
        return when (role) {
            "MAFIA" -> "🔪 ${role}"
            "INDEPENDENT" -> "🎭 ${role}"
            else -> "👑 شهروند"
        }
    }
}

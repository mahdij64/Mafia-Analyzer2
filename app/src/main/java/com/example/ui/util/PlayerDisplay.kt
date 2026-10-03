package com.example.ui.util

import com.example.data.local.GameEntity
import com.example.data.local.PlayerEntity

/**
 * Helpers for displaying player information safely.
 *
 * For the local user (game owner) playing a non-citizen role, the
 * real name is replaced with a stable numeric code so the screen
 * can be shown to neighbours without revealing identity.
 *
 * The code never mentions any role name (no MAFIA / INDEPENDENT
 * wording leaks into the UI). It is just a number like "کد ۰۱" or
 * "کد ۷۷".
 */
data class SafePlayerDisplay(
    val displayName: String,   // what to show in the UI
    val isOwner: Boolean,      // whether this player is the local user
    val isHidden: Boolean      // whether the real name is being concealed
)

object PlayerDisplay {
    /**
     * Returns the SafePlayerDisplay for [player] given the current
     * [game]. If the local user owns the game and has a non-citizen
     * role, the owner's own display name is replaced with a code.
     * The code is purely numeric and never contains role words.
     */
    fun forPlayer(player: PlayerEntity, game: GameEntity?): SafePlayerDisplay {
        val isOwner = game?.ownerPlayerId == player.id
        val isHidden = isOwner && (game?.ownerRole != "CITIZEN")
        val display = if (isHidden) {
            // Pure numeric code, padded to 2 digits, with a Persian prefix
            // so the user knows it's a code, not a name.
            // Use id directly (positive) so the code is stable per player.
            val num = (player.id.coerceAtLeast(0L) % 100L).toInt()
            "کد ${num.toString().padStart(2, '0')}"
        } else {
            player.name
        }
        return SafePlayerDisplay(
            displayName = display,
            isOwner = isOwner,
            isHidden = isHidden
        )
    }
}

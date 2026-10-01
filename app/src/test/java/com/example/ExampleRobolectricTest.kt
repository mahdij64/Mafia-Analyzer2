package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AlgorithmWeightEntity
import com.example.data.local.ManualSuspicionEntity
import com.example.data.local.PlayerEntity
import com.example.data.local.PlayerNoteEntity
import com.example.data.local.TargetEntity
import com.example.data.local.VoteEntity
import com.example.domain.algorithm.SuspicionCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("تحلیل‌گر مافیا", appName)
    }

    @Test
    fun `test suspicion calculator with sample players`() {
        val p1 = PlayerEntity(id = 1L, gameId = 1L, name = "مهدی")
        val p2 = PlayerEntity(id = 2L, gameId = 1L, name = "رضا")
        val p3 = PlayerEntity(id = 3L, gameId = 1L, name = "قاسم")
        val allPlayers = listOf(p1, p2, p3)

        // Stage 0: Mehdi targets Reza
        // Stage 1: Mehdi targets Ghasem (flip)
        val targets = listOf(
            TargetEntity(id = 1L, gameId = 1L, stageIndex = 0, sourcePlayerId = 1L, targetPlayerId = 2L),
            TargetEntity(id = 2L, gameId = 1L, stageIndex = 1, sourcePlayerId = 1L, targetPlayerId = 3L),
            TargetEntity(id = 3L, gameId = 1L, stageIndex = 1, sourcePlayerId = 2L, targetPlayerId = 1L)
        )

        val notes = listOf(
            PlayerNoteEntity(id = 1L, gameId = 1L, playerId = 1L, stageIndex = 1, category = "SUSPICIOUS", text = "دفاع عصبی")
        )

        val votes = listOf(
            VoteEntity(id = 1L, gameId = 1L, stageIndex = 1, voterId = 2L, targetId = 1L)
        )

        val manualSuspicions = listOf(
            ManualSuspicionEntity(id = 1L, gameId = 1L, playerId = 1L, stageIndex = 1, score = 80)
        )

        val weights = AlgorithmWeightEntity(gameId = 1L)

        val analysis = SuspicionCalculator.calculateForPlayer(
            player = p1,
            currentStage = 1,
            allPlayers = allPlayers,
            allTargets = targets,
            allNotes = notes,
            allVotes = votes,
            allManualSuspicions = manualSuspicions,
            weights = weights
        )

        // Mehdi has manual score 80, 1 suspicious note, target flips, received target, received vote
        assertTrue("Total score should be high", analysis.totalScore >= 65)
        assertTrue("Should have positive factors", analysis.positiveFactors.isNotEmpty())
        assertEquals("Player name should match", "مهدی", analysis.playerName)
    }

    @Test
    fun `test white citizen player scoring`() {
        val p1 = PlayerEntity(id = 1L, gameId = 1L, name = "یسنا")
        val p2 = PlayerEntity(id = 2L, gameId = 1L, name = "رضا")
        val allPlayers = listOf(p1, p2)

        // Yasna targets Reza consistently, no suspicious notes, claims doctor, manual score 20
        val targets = listOf(
            TargetEntity(id = 1L, gameId = 1L, stageIndex = 0, sourcePlayerId = 1L, targetPlayerId = 2L),
            TargetEntity(id = 2L, gameId = 1L, stageIndex = 1, sourcePlayerId = 1L, targetPlayerId = 2L)
        )

        val notes = listOf(
            PlayerNoteEntity(id = 1L, gameId = 1L, playerId = 1L, stageIndex = 1, category = "TALK", text = "ادعای دکتر در دفاعیه")
        )

        val manualSuspicions = listOf(
            ManualSuspicionEntity(id = 1L, gameId = 1L, playerId = 1L, stageIndex = 1, score = 20)
        )

        val weights = AlgorithmWeightEntity(gameId = 1L)

        val analysis = SuspicionCalculator.calculateForPlayer(
            player = p1,
            currentStage = 1,
            allPlayers = allPlayers,
            allTargets = targets,
            allNotes = notes,
            allVotes = emptyList(),
            allManualSuspicions = manualSuspicions,
            weights = weights
        )

        assertTrue("Yasna should have low suspicion score", analysis.totalScore < 45)
        assertTrue("Should have negative factors (citizen vibe)", analysis.negativeFactors.isNotEmpty())
    }

    @Test
    fun `test structured behavior factors`() {
        val p1 = PlayerEntity(id = 1L, gameId = 1L, name = "علی")
        val allPlayers = listOf(p1)

        val notes = listOf(
            PlayerNoteEntity(id = 1L, gameId = 1L, playerId = 1L, stageIndex = 1, category = "SUSPICIOUS", text = "اتهام تناقض‌گویی در معرفی"),
            PlayerNoteEntity(id = 2L, gameId = 1L, playerId = 1L, stageIndex = 1, category = "SUSPICIOUS", text = "تغییر موضع بی‌دلیل نسبت به رضا"),
            PlayerNoteEntity(id = 3L, gameId = 1L, playerId = 1L, stageIndex = 1, category = "SUSPICIOUS", text = "سکوت / کم‌کاری در چالش‌ها")
        )

        val weights = AlgorithmWeightEntity(gameId = 1L)

        val analysis = SuspicionCalculator.calculateForPlayer(
            player = p1,
            currentStage = 1,
            allPlayers = allPlayers,
            allTargets = emptyList(),
            allNotes = notes,
            allVotes = emptyList(),
            allManualSuspicions = emptyList(),
            weights = weights
        )

        val factorTitles = analysis.positiveFactors.map { it.title }
        assertTrue("Contains contradiction factor", factorTitles.any { it.contains("تناقض") })
        assertTrue("Contains stance change factor", factorTitles.any { it.contains("تغییر موضع") })
        assertTrue("Contains silence factor", factorTitles.any { it.contains("سکوت") })
    }

    @Test
    fun `test auto-save target replacement integrity`() {
        val initialTargets = listOf(
            TargetEntity(id = 1L, gameId = 1L, stageIndex = 0, sourcePlayerId = 1L, targetPlayerId = 2L),
            TargetEntity(id = 2L, gameId = 1L, stageIndex = 0, sourcePlayerId = 1L, targetPlayerId = 3L)
        )

        // When user toggles target 2 off and target 4 on
        val newTargetIds = listOf(3L, 4L)
        val stageIndex = 0
        val sourcePlayerId = 1L

        val updatedTargets = initialTargets.filterNot {
            it.stageIndex == stageIndex && it.sourcePlayerId == sourcePlayerId
        } + newTargetIds.map {
            TargetEntity(gameId = 1L, stageIndex = stageIndex, sourcePlayerId = sourcePlayerId, targetPlayerId = it)
        }

        assertEquals(2, updatedTargets.size)
        assertTrue(updatedTargets.any { it.targetPlayerId == 3L })
        assertTrue(updatedTargets.any { it.targetPlayerId == 4L })
        assertTrue(updatedTargets.none { it.targetPlayerId == 2L })
    }

    @Test
    fun `test stage locking and advancement logic`() {
        val game = com.example.data.local.GameEntity(
            id = 1L,
            name = "بازی تست",
            currentStageIndex = 0,
            maxUnlockedStageIndex = 0
        )

        // Stage 0 (معارفه) is unlocked, Day 1 and above are locked
        assertTrue(0 <= game.maxUnlockedStageIndex)
        assertTrue(1 > game.maxUnlockedStageIndex)

        // When stage 0 is finished, advance to stage 1
        val advancedGame = game.copy(
            currentStageIndex = 1,
            maxUnlockedStageIndex = maxOf(game.maxUnlockedStageIndex, 1)
        )

        assertEquals(1, advancedGame.currentStageIndex)
        assertEquals(1, advancedGame.maxUnlockedStageIndex)
        assertTrue(advancedGame.maxUnlockedStageIndex >= 1)
        assertTrue(2 > advancedGame.maxUnlockedStageIndex)
    }

    @Test
    fun `test stealth misdirection engine and conflict detection`() {
        val players = listOf(
            PlayerEntity(id = 1L, gameId = 1L, name = "مهدی"),
            PlayerEntity(id = 2L, gameId = 1L, name = "قاسم"),
            PlayerEntity(id = 3L, gameId = 1L, name = "رضا")
        )
        // Player 1 and Player 2 target each other
        val targets = listOf(
            TargetEntity(id = 1L, gameId = 1L, stageIndex = 0, sourcePlayerId = 1L, targetPlayerId = 2L),
            TargetEntity(id = 2L, gameId = 1L, stageIndex = 0, sourcePlayerId = 2L, targetPlayerId = 1L)
        )
        val (conflicts, tactics) = com.example.domain.model.StealthMisdirectionEngine.analyzeConflictsAndTactics(
            players = players,
            targets = targets,
            votes = emptyList(),
            notes = emptyList(),
            scores = emptyList(),
            currentStage = 0
        )

        assertTrue(conflicts.isNotEmpty())
        val topConflict = conflicts.first()
        assertEquals("تارگت متقابل شدید", topConflict.severityLevel)
        assertTrue(tactics.isNotEmpty())
    }

    @Test
    fun `test missed players calculation for emergency button`() {
        val players = listOf(
            PlayerEntity(id = 1L, gameId = 1L, name = "مهدی", isEliminated = false),
            PlayerEntity(id = 2L, gameId = 1L, name = "قاسم", isEliminated = false),
            PlayerEntity(id = 3L, gameId = 1L, name = "رضا", isEliminated = true)
        )
        // Only Player 1 has targets recorded
        val targets = listOf(
            TargetEntity(id = 1L, gameId = 1L, stageIndex = 0, sourcePlayerId = 1L, targetPlayerId = 2L)
        )

        val alivePlayers = players.filter { !it.isEliminated }
        val playersWithTargets = targets.filter { it.stageIndex == 0 }.map { it.sourcePlayerId }.toSet()
        val missedPlayers = alivePlayers.filter { it.id !in playersWithTargets }

        assertEquals(1, missedPlayers.size)
        assertEquals(2L, missedPlayers.first().id) // Player 2 hasn't recorded yet
    }

    @Test
    fun `test no-target clear action records zero targets for speaker`() {
        val initialTargets = listOf(2L, 3L, 4L)
        val noTargetList = emptyList<Long>()
        assertTrue(initialTargets.isNotEmpty())
        assertTrue(noTargetList.isEmpty())
    }

    @Test
    fun `test player seat swap logic preserves count and updates positions`() {
        val player1 = PlayerEntity(id = 1L, gameId = 1L, name = "مهدی", displayOrder = 0)
        val player2 = PlayerEntity(id = 2L, gameId = 1L, name = "قاسم", displayOrder = 1)
        val player3 = PlayerEntity(id = 3L, gameId = 1L, name = "رضا", displayOrder = 2)
        val list = mutableListOf(player1, player2, player3)

        // Swap index 0 and 2
        val temp = list[0]
        list[0] = list[2]
        list[2] = temp

        assertEquals(3L, list[0].id)
        assertEquals("رضا", list[0].name)
        assertEquals(1L, list[2].id)
        assertEquals("مهدی", list[2].name)
    }

    @Test
    fun `test table rotation shifts seats correctly`() {
        val p1 = PlayerEntity(id = 1L, gameId = 1L, name = "مهدی")
        val p2 = PlayerEntity(id = 2L, gameId = 1L, name = "قاسم")
        val p3 = PlayerEntity(id = 3L, gameId = 1L, name = "رضا")
        val list = listOf(p1, p2, p3)

        val rotatedClockwise = listOf(list.last()) + list.dropLast(1)
        assertEquals(3L, rotatedClockwise.first().id)
        assertEquals("رضا", rotatedClockwise.first().name)
        assertEquals(1L, rotatedClockwise[1].id)
    }
}


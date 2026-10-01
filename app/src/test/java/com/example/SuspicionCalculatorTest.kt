package com.example

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

/**
 * Pure-JVM unit tests for the transparent suspicion scoring algorithm.
 *
 * Default weights used by [AlgorithmWeightEntity]:
 *  manualWeight = 0.35, suspiciousNoteWeight = 0.25, targetSpreadWeight = 0.15,
 *  targetFlipWeight = 0.15, votesReceivedWeight = 0.10
 */
class SuspicionCalculatorTest {

    private val weights = AlgorithmWeightEntity(gameId = 1L)

    private fun player(id: Long, name: String, isOwner: Boolean = false, knownRole: String? = null, isEliminated: Boolean = false) =
        PlayerEntity(id = id, gameId = 1L, name = name, isOwner = isOwner, knownRole = knownRole, isEliminated = isEliminated)

    private fun analyze(
        target: PlayerEntity,
        stage: Int,
        players: List<PlayerEntity>,
        targets: List<TargetEntity> = emptyList(),
        notes: List<PlayerNoteEntity> = emptyList(),
        votes: List<VoteEntity> = emptyList(),
        manuals: List<ManualSuspicionEntity> = emptyList()
    ) = SuspicionCalculator.calculateForPlayer(
        player = target,
        currentStage = stage,
        allPlayers = players,
        allTargets = targets,
        allNotes = notes,
        allVotes = votes,
        allManualSuspicions = manuals,
        weights = weights
    )

    @Test
    fun `neutral player with no data scores 45 at stage 0`() {
        val p1 = player(1L, "مهدی")
        val result = analyze(p1, stage = 0, players = listOf(p1, player(2L, "رضا")))

        // base 50, minus 5 because no suspicious notes are recorded
        assertEquals(45, result.totalScore)
        assertEquals("کم‌مشکوک / نسبتاً سفید", result.statusLabel)
        assertEquals(50, result.manualScore)
        assertEquals(1, result.stageTrendScores.size)
    }

    @Test
    fun `high manual suspicion raises score`() {
        val p1 = player(1L, "مهدی")
        val manuals = listOf(ManualSuspicionEntity(gameId = 1L, playerId = 1L, stageIndex = 0, score = 80))

        val result = analyze(p1, stage = 0, players = listOf(p1), manuals = manuals)

        // ((80-50) * 0.35 * 1.5).roundToInt() = 16 ; 50 + 16 - 5(no notes) = 61
        assertEquals(80, result.manualScore)
        assertEquals(61, result.totalScore)
        assertEquals("متوسط / نیازمند بررسی بیشتر", result.statusLabel)
        assertTrue(result.positiveFactors.any { it.title == "نظر شهودی کاربر" && it.points == 16 })
    }

    @Test
    fun `low manual suspicion reduces score`() {
        val p1 = player(1L, "مهدی")
        val manuals = listOf(ManualSuspicionEntity(gameId = 1L, playerId = 1L, stageIndex = 0, score = 20))

        val result = analyze(p1, stage = 0, players = listOf(p1), manuals = manuals)

        // ((20-50) * 0.525).roundToInt() = -16 ; 50 - 16 - 5 = 29
        assertEquals(29, result.totalScore)
        assertTrue(result.negativeFactors.any { it.title == "اعتماد شخصی کاربر" && it.points == -16 })
    }

    @Test
    fun `suspicious note with contradiction keyword adds two factors`() {
        val p1 = player(1L, "مهدی")
        val notes = listOf(
            PlayerNoteEntity(gameId = 1L, playerId = 1L, stageIndex = 0, category = "SUSPICIOUS", text = "تناقض در حرف‌های روز قبل")
        )

        val result = analyze(p1, stage = 0, players = listOf(p1), notes = notes)

        // notePts = 1*10*(0.25/0.25) = 10 ; keyword "تناقض" = +10 ; 50 + 20 = 70
        assertEquals(1, result.suspiciousNotesCount)
        assertEquals(70, result.totalScore)
        assertEquals("مشکوک / پرریسک", result.statusLabel)
        assertTrue(result.positiveFactors.any { it.title == "تناقض‌گویی در بیانات" })
    }

    @Test
    fun `target flip between stages is detected and scored`() {
        val p1 = player(1L, "مهدی")
        val p2 = player(2L, "رضا")
        val p3 = player(3L, "قاسم")
        val targets = listOf(
            TargetEntity(gameId = 1L, stageIndex = 0, sourcePlayerId = 1L, targetPlayerId = 2L),
            TargetEntity(gameId = 1L, stageIndex = 1, sourcePlayerId = 1L, targetPlayerId = 3L)
        )

        val result = analyze(p1, stage = 1, players = listOf(p1, p2, p3), targets = targets)

        // flips = 1 → +8 ; no notes → -5 ; no received targets at stage>0 → -6 ; 50+8-5-6 = 47
        assertEquals(1, result.targetFlipsCount)
        assertEquals(47, result.totalScore)
        assertEquals(2, result.stageTrendScores.size)
        assertTrue(result.positiveFactors.any { it.title == "نوسان و تغییر مسیر تارگت‌ها" })
    }

    @Test
    fun `votes received in current stage increase suspicion`() {
        val p1 = player(1L, "مهدی")
        val p2 = player(2L, "رضا")
        val p3 = player(3L, "قاسم")
        val votes = listOf(
            VoteEntity(gameId = 1L, stageIndex = 1, voterId = 2L, targetId = 1L),
            VoteEntity(gameId = 1L, stageIndex = 1, voterId = 3L, targetId = 1L)
        )

        val result = analyze(p1, stage = 1, players = listOf(p1, p2, p3), votes = votes)

        // votePts = 2*10*(0.10/0.10) = 20 ; -5 no notes ; -6 no received targets ; 50+20-11 = 59
        assertEquals(2, result.votesReceivedCount)
        assertEquals(59, result.totalScore)
    }

    @Test
    fun `spread targets add points and focused targets reduce them`() {
        val p1 = player(1L, "مهدی")
        val others = (2L..5L).map { player(it, "بازیکن$it") }
        val allPlayers = listOf(p1) + others

        // Spread: 4 targets on 4 distinct players at stage 0
        val spreadTargets = others.map {
            TargetEntity(gameId = 1L, stageIndex = 0, sourcePlayerId = 1L, targetPlayerId = it.id)
        }
        val spreadResult = analyze(p1, stage = 0, players = allPlayers, targets = spreadTargets)
        // +10 spread, -5 no notes → 55
        assertEquals(55, spreadResult.totalScore)
        assertTrue(spreadResult.positiveFactors.any { it.title == "تارگت‌های پراکنده و پرتعداد" })

        // Focus: same target (p2) on 3 stages up to stage 2
        val focusTargets = (0..2).map {
            TargetEntity(gameId = 1L, stageIndex = it, sourcePlayerId = 1L, targetPlayerId = 2L)
        }
        val focusResult = analyze(p1, stage = 2, players = allPlayers, targets = focusTargets)
        // -8 focus, -5 no notes, -6 no received at stage>0 → 31
        assertEquals(31, focusResult.totalScore)
        assertTrue(focusResult.negativeFactors.any { it.title == "ثبات در تارگت‌ها" })
    }

    @Test
    fun `owner eliminated and mafia teammate labels are produced`() {
        val owner = player(1L, "من", isOwner = true, knownRole = "MAFIA")
        val mate = player(2L, "یار", knownRole = "MAFIA")
        val dead = player(3L, "مرده", isEliminated = true)
        val players = listOf(owner, mate, dead)

        val ownerResult = analyze(owner, stage = 0, players = players)
        assertEquals("👑 شما (مافیا 🗡️)", ownerResult.statusLabel)

        val mateResult = analyze(mate, stage = 0, players = players)
        assertEquals("🗡️ یار مافیا (تیم شما)", mateResult.statusLabel)
        assertTrue(mateResult.positiveFactors.any { it.title == "یار مافیا (تأیید کاربر)" && it.points == 35 })

        val deadResult = analyze(dead, stage = 0, players = players)
        assertEquals("💀 حذف‌شده از بازی", deadResult.statusLabel)
    }

    @Test
    fun `score is always clamped between 0 and 100`() {
        val p1 = player(1L, "مهدی")
        val others = (2L..6L).map { player(it, "بازیکن$it") }
        val allPlayers = listOf(p1) + others

        val notes = (1..6).map {
            PlayerNoteEntity(gameId = 1L, playerId = 1L, stageIndex = 1, category = "SUSPICIOUS", text = "تناقض و تغییر موضع و واکنش تدافعی و سکوت و استرس $it")
        }
        val received = others.map {
            TargetEntity(gameId = 1L, stageIndex = 1, sourcePlayerId = it.id, targetPlayerId = 1L)
        }
        val votes = others.map {
            VoteEntity(gameId = 1L, stageIndex = 1, voterId = it.id, targetId = 1L)
        }
        val manuals = listOf(ManualSuspicionEntity(gameId = 1L, playerId = 1L, stageIndex = 1, score = 100))

        val result = analyze(p1, stage = 1, players = allPlayers, targets = received, notes = notes, votes = votes, manuals = manuals)

        assertTrue("score must not exceed 100 but was ${result.totalScore}", result.totalScore <= 100)
        assertTrue(result.totalScore >= 0)
        assertEquals("بسیار مشکوک / کانون سوءظن", result.statusLabel)
    }

    @Test
    fun `historical trend contains one entry per stage`() {
        val p1 = player(1L, "مهدی")
        val p2 = player(2L, "رضا")
        val targets = listOf(
            TargetEntity(gameId = 1L, stageIndex = 0, sourcePlayerId = 2L, targetPlayerId = 1L),
            TargetEntity(gameId = 1L, stageIndex = 2, sourcePlayerId = 2L, targetPlayerId = 1L)
        )

        val result = analyze(p1, stage = 3, players = listOf(p1, p2), targets = targets)

        assertEquals(4, result.stageTrendScores.size)
        assertEquals(listOf(0, 1, 2, 3), result.stageTrendScores.map { it.first })
        result.stageTrendScores.forEach { (_, score) -> assertTrue(score in 0..100) }
    }
}

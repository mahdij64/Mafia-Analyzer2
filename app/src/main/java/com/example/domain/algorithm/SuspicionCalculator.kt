package com.example.domain.algorithm

import com.example.data.local.AlgorithmWeightEntity
import com.example.data.local.ManualSuspicionEntity
import com.example.data.local.PlayerEntity
import com.example.data.local.PlayerNoteEntity
import com.example.data.local.TargetEntity
import com.example.data.local.VoteEntity
import kotlin.math.roundToInt

data class FactorItem(
    val title: String,
    val points: Int, // e.g. +15 or -10
    val description: String,
    val isSuspicious: Boolean // true = increases suspicion, false = reduces suspicion / citizen vibe
)

data class PlayerScoreAnalysis(
    val playerId: Long,
    val playerName: String,
    val stageIndex: Int,
    val totalScore: Int, // 0..100
    val statusLabel: String,
    val positiveFactors: List<FactorItem>,
    val negativeFactors: List<FactorItem>,
    val stageTrendScores: List<Pair<Int, Int>>, // List of (stageIndex, score)
    val totalTargetsMade: Int,
    val uniqueTargetsCount: Int,
    val targetsReceivedCount: Int,
    val targetFlipsCount: Int,
    val votesCastCount: Int,
    val votesReceivedCount: Int,
    val suspiciousNotesCount: Int,
    val talkNotesCount: Int,
    val opinionNotesCount: Int,
    val manualScore: Int,
    val isEliminated: Boolean = false,
    val isOwner: Boolean = false,
    val knownRole: String? = null
) {
    /**
     * Name safe for display in reports when the screen might be seen
     * by neighbours. Returns the real [playerName] but UI should use
     * [ownerRoleColor] to indicate the owner's role visually.
     */
    val displayName: String
        get() = playerName
    
    /**
     * Color to use for displaying the owner's name based on their role.
     * Returns null for non-owners or citizens.
     */
    val ownerRoleColor: String?
        get() = if (isOwner && knownRole != null) {
            when (knownRole) {
                "MAFIA" -> "PURPLE"
                "CITIZEN" -> "GREEN"
                "INDEPENDENT" -> "BLUE"
                else -> null
            }
        } else null
}

object SuspicionCalculator {

    fun calculateForPlayer(
        player: PlayerEntity,
        currentStage: Int,
        allPlayers: List<PlayerEntity>,
        allTargets: List<TargetEntity>,
        allNotes: List<PlayerNoteEntity>,
        allVotes: List<VoteEntity>,
        allManualSuspicions: List<ManualSuspicionEntity>,
        weights: AlgorithmWeightEntity
    ): PlayerScoreAnalysis {
        val playerMap = allPlayers.associateBy { it.id }

        // Filter data up to current stage
        val targetsUpToNow = allTargets.filter { it.stageIndex <= currentStage }
        val notesUpToNow = allNotes.filter { it.stageIndex <= currentStage }
        val votesUpToNow = allVotes.filter { it.stageIndex <= currentStage }
        val manualUpToNow = allManualSuspicions.filter { it.stageIndex <= currentStage }

        // Player specific data
        val myTargets = targetsUpToNow.filter { it.sourcePlayerId == player.id }
        val targetsReceived = targetsUpToNow.filter { it.targetPlayerId == player.id }
        val myNotes = notesUpToNow.filter { it.playerId == player.id }
        val myVotes = votesUpToNow.filter { it.voterId == player.id }
        val votesReceived = votesUpToNow.filter { it.targetId == player.id }

        val suspiciousNotes = myNotes.filter { it.category == "SUSPICIOUS" }
        val talkNotes = myNotes.filter { it.category == "TALK" }
        val opinionNotes = myNotes.filter { it.category == "OPINION" }

        // Manual score for current stage (or latest available, default 50)
        val currentManual = manualUpToNow
            .filter { it.playerId == player.id && it.stageIndex == currentStage }
            .firstOrNull()?.score
            ?: manualUpToNow.filter { it.playerId == player.id }.maxByOrNull { it.stageIndex }?.score
            ?: 50

        val positiveFactors = mutableListOf<FactorItem>()
        val negativeFactors = mutableListOf<FactorItem>()

        // 1. Manual user suspicion score
        val manualContribution = ((currentManual - 50) * weights.manualWeight * 1.5f).roundToInt()
        if (currentManual > 50) {
            positiveFactors.add(
                FactorItem(
                    title = "نظر شهودی کاربر",
                    points = manualContribution,
                    description = "امتیاز دستی ثبت‌شده: $currentManual از ۱۰۰ (سوءظن شخصی کاربر)",
                    isSuspicious = true
                )
            )
        } else if (currentManual < 50) {
            negativeFactors.add(
                FactorItem(
                    title = "اعتماد شخصی کاربر",
                    points = manualContribution, // negative number
                    description = "امتیاز دستی ثبت‌شده: $currentManual از ۱۰۰ (دید مثبت یا شهروندی کاربر)",
                    isSuspicious = false
                )
            )
        }

        // 2. Suspicious behavior notes & structured observations
        if (suspiciousNotes.isNotEmpty()) {
            val notePts = (suspiciousNotes.size * 10 * weights.suspiciousNoteWeight / 0.25f).roundToInt().coerceAtMost(30)
            positiveFactors.add(
                FactorItem(
                    title = "رفتارهای مشکوک ثبت‌شده",
                    points = notePts,
                    description = "${suspiciousNotes.size} مورد یادداشت رفتار مشکوک در سوابق",
                    isSuspicious = true
                )
            )

            // Specific structured flags inside notes
            val noteTexts = suspiciousNotes.map { it.text }
            if (noteTexts.any { it.contains("تناقض") }) {
                positiveFactors.add(
                    FactorItem(
                        title = "تناقض‌گویی در بیانات",
                        points = 10,
                        description = "ثبت تناقض در صحبت‌ها یا استدلال‌های بازیکن",
                        isSuspicious = true
                    )
                )
            }
            if (noteTexts.any { it.contains("تغییر موضع") }) {
                positiveFactors.add(
                    FactorItem(
                        title = "تغییر موضع ناگهانی / بی‌دلیل",
                        points = 8,
                        description = "چرخش نظر بدون ارائه فکت یا استدلال مشخص",
                        isSuspicious = true
                    )
                )
            }
            if (noteTexts.any { it.contains("واکنش تدافعی") || it.contains("دفاع بسیار عصبی") }) {
                positiveFactors.add(
                    FactorItem(
                        title = "واکنش تدافعی افراطی",
                        points = 8,
                        description = "پرخاشگری یا دفاع غیرعادی در برابر اتهام کوچک",
                        isSuspicious = true
                    )
                )
            }
            if (noteTexts.any { it.contains("سکوت") || it.contains("کم‌کاری") }) {
                positiveFactors.add(
                    FactorItem(
                        title = "سکوت و عدم مشارکت فعال",
                        points = 7,
                        description = "انفعال در جریان بازی و پرهیز از اثرگذاری",
                        isSuspicious = true
                    )
                )
            }
            if (noteTexts.any { it.contains("استرس") || it.contains("تنش") }) {
                positiveFactors.add(
                    FactorItem(
                        title = "تنش و اضطراب محسوس",
                        points = 6,
                        description = "نشانه‌های رفتاری استرس و فشار روانی بالا",
                        isSuspicious = true
                    )
                )
            }
        } else {
            negativeFactors.add(
                FactorItem(
                    title = "عدم ثبت رفتار مشکوک",
                    points = -5,
                    description = "هیچ رفتار مشکوکی برای این بازیکن ثبت نشده است",
                    isSuspicious = false
                )
            )
        }

        // Claims / Talk notes
        if (talkNotes.any { it.text.contains("ادعای کارآگاه") || it.text.contains("ادعای دکتر") }) {
            negativeFactors.add(
                FactorItem(
                    title = "ادعای نقش کلیدی شهروندی",
                    points = -6,
                    description = "ادعای نقش‌داری (کارآگاه یا دکتر) در صحبت‌ها",
                    isSuspicious = false
                )
            )
        }

        // 3. Target analysis: Targets made, unique targets, and changes (flips) between stages
        val uniqueTargets = myTargets.map { it.targetPlayerId }.distinct()
        val targetsByStage = (0..currentStage).associateWith { stg ->
            myTargets.filter { it.stageIndex == stg }.map { it.targetPlayerId }.toSet()
        }

        // Target flips: targets dropped or radically altered between consecutive stages
        var targetFlips = 0
        for (stg in 1..currentStage) {
            val prev = targetsByStage[stg - 1] ?: emptySet()
            val curr = targetsByStage[stg] ?: emptySet()
            if (prev.isNotEmpty() && curr.isNotEmpty()) {
                val dropped = prev - curr
                val added = curr - prev
                if (dropped.isNotEmpty() && added.isNotEmpty()) {
                    targetFlips += dropped.size
                }
            }
        }

        if (targetFlips > 0) {
            val flipPts = (targetFlips * 8 * weights.targetFlipWeight / 0.15f).roundToInt().coerceAtMost(25)
            positiveFactors.add(
                FactorItem(
                    title = "نوسان و تغییر مسیر تارگت‌ها",
                    points = flipPts,
                    description = "$targetFlips مورد تغییر و چرخش ناگهانی در اهداف تارگت‌ها بین روزها",
                    isSuspicious = true
                )
            )
        }

        // Target spreading vs focus
        if (myTargets.size >= 4 && uniqueTargets.size >= 4) {
            val spreadPts = (10 * weights.targetSpreadWeight / 0.15f).roundToInt()
            positiveFactors.add(
                FactorItem(
                    title = "تارگت‌های پراکنده و پرتعداد",
                    points = spreadPts,
                    description = "${myTargets.size} تارگت به ${uniqueTargets.size} بازیکن مختلف (پخش کردن اتهامات)",
                    isSuspicious = true
                )
            )
        } else if (myTargets.size >= 3 && uniqueTargets.size <= 2) {
            // Consistent focus on specific suspects
            negativeFactors.add(
                FactorItem(
                    title = "ثبات در تارگت‌ها",
                    points = -8,
                    description = "تارگت‌های متمرکز و مداوم روی افراد مشخص (${uniqueTargets.size} نفر)",
                    isSuspicious = false
                )
            )
        }

        // 4. Being targeted by others (Pressure / Table consensus)
        val targetsReceivedInCurrentStage = targetsReceived.filter { it.stageIndex == currentStage }
        if (targetsReceivedInCurrentStage.size >= 3) {
            positiveFactors.add(
                FactorItem(
                    title = "کانون توجه و اتهام میز",
                    points = 14,
                    description = "${targetsReceivedInCurrentStage.size} بازیکن در این مرحله به او تارگت زده‌اند",
                    isSuspicious = true
                )
            )
        } else if (targetsReceivedInCurrentStage.size == 1 || targetsReceivedInCurrentStage.size == 2) {
            positiveFactors.add(
                FactorItem(
                    title = "دریافت تارگت در مرحله جاری",
                    points = 6 * targetsReceivedInCurrentStage.size,
                    description = "${targetsReceivedInCurrentStage.size} تارگت دریافتی در این مرحله",
                    isSuspicious = true
                )
            )
        } else if (targetsReceivedInCurrentStage.isEmpty() && currentStage > 0) {
            negativeFactors.add(
                FactorItem(
                    title = "عدم دریافت تارگت در مرحله",
                    points = -6,
                    description = "هیچ بازیکنی در این مرحله به او تارگت نزده است",
                    isSuspicious = false
                )
            )
        }

        // 5. Direct clash (Mutual targeting)
        val currentStageMyTargets = targetsByStage[currentStage] ?: emptySet()
        val opponentsWhoTargetedMe = targetsReceivedInCurrentStage.map { it.sourcePlayerId }.toSet()
        val mutualClashes = currentStageMyTargets.intersect(opponentsWhoTargetedMe)
        if (mutualClashes.isNotEmpty()) {
            val clashNames = mutualClashes.mapNotNull { playerMap[it]?.name }.joinToString("، ")
            positiveFactors.add(
                FactorItem(
                    title = "تقابل مستقیم و دوطرفه",
                    points = 8,
                    description = "تارگت متقابل با: $clashNames",
                    isSuspicious = true
                )
            )
        }

        // 6. Voting patterns
        val votesReceivedInCurrentStage = votesReceived.filter { it.stageIndex == currentStage }
        if (votesReceivedInCurrentStage.isNotEmpty()) {
            val votePts = (votesReceivedInCurrentStage.size * 10 * weights.votesReceivedWeight / 0.10f).roundToInt().coerceAtMost(30)
            positiveFactors.add(
                FactorItem(
                    title = "رأی خروج دریافتی",
                    points = votePts,
                    description = "${votesReceivedInCurrentStage.size} رأی برای خروج از بازی در این مرحله دریافت کرده",
                    isSuspicious = true
                )
            )
        }

        // Vote changes between days
        val myVotesByStage = (1..currentStage).mapNotNull { stg ->
            myVotes.firstOrNull { it.stageIndex == stg }?.let { stg to it.targetId }
        }
        if (myVotesByStage.size >= 2) {
            val uniqueVotes = myVotesByStage.map { it.second }.filter { it >= 0 }.distinct()
            if (uniqueVotes.size >= 2) {
                positiveFactors.add(
                    FactorItem(
                        title = "تغییر رأی بین روزها",
                        points = 6,
                        description = "رأی خروج را بین روزهای مختلف تغییر داده است",
                        isSuspicious = true
                    )
                )
            }
        }

        // Factor for targeting players who got eliminated
        val eliminatedPlayers = allPlayers.filter { it.isEliminated && it.id != player.id }
        if (eliminatedPlayers.isNotEmpty()) {
            val targetedEliminated = myTargets.filter { t -> eliminatedPlayers.any { it.id == t.targetPlayerId } }
            if (targetedEliminated.isNotEmpty()) {
                positiveFactors.add(
                    FactorItem(
                        title = "فشار روی بازیکنان حذف‌شده",
                        points = 7,
                        description = "تارگت زدن به ${targetedEliminated.size} بازیکنی که از بازی حذف شدند (احتمال موج‌سواری)",
                        isSuspicious = true
                    )
                )
            }
        }

        // Known role or teammate factor
        if (player.knownRole == "MAFIA" && !player.isOwner) {
            positiveFactors.add(
                FactorItem(
                    title = "یار مافیا (تأیید کاربر)",
                    points = 35,
                    description = "این بازیکن به عنوان هم‌تیمی مافیای شما تعیین شده است",
                    isSuspicious = true
                )
            )
        }

        if (player.isOwner) {
            val roleFa = when (player.knownRole) {
                "MAFIA" -> "مافیا 🗡️"
                "INDEPENDENT" -> "مستقل 🎭"
                else -> "شهروند 🛡️"
            }
            negativeFactors.add(
                FactorItem(
                    title = "مالک بازی (شما)",
                    points = 0,
                    description = "نقش شما: $roleFa",
                    isSuspicious = false
                )
            )
        }

        if (player.isEliminated) {
            negativeFactors.add(
                FactorItem(
                    title = "کشته / حذف‌شده از بازی",
                    points = 0,
                    description = "این بازیکن از بازی خارج شده و در فازهای بعدی صامت است",
                    isSuspicious = false
                )
            )
        }

        // Baseline score
        val baseScore = 50
        val totalPos = positiveFactors.sumOf { it.points }
        val totalNeg = negativeFactors.sumOf { it.points } // this is <= 0
        val rawCalculatedScore = (baseScore + totalPos + totalNeg).coerceIn(0, 100)

        // Status Label
        val statusLabel = when {
            player.isEliminated -> "💀 حذف‌شده از بازی"
            player.isOwner -> {
                val roleFa = when (player.knownRole) {
                    "MAFIA" -> "مافیا 🗡️"
                    "INDEPENDENT" -> "مستقل 🎭"
                    else -> "شهروند 🛡️"
                }
                "👑 شما ($roleFa)"
            }
            player.knownRole == "MAFIA" -> "🗡️ یار مافیا (تیم شما)"
            rawCalculatedScore in 0..25 -> "کاملاً کم‌خطر / شهروند محتمل"
            rawCalculatedScore in 26..45 -> "کم‌مشکوک / نسبتاً سفید"
            rawCalculatedScore in 46..65 -> "متوسط / نیازمند بررسی بیشتر"
            rawCalculatedScore in 66..80 -> "مشکوک / پرریسک"
            else -> "بسیار مشکوک / کانون سوءظن"
        }

        // Historical scores across all completed stages up to currentStage
        val stageTrends = (0..currentStage).map { stg ->
            stg to calculateScoreForHistoricalStage(
                player = player,
                targetStage = stg,
                allPlayers = allPlayers,
                allTargets = allTargets,
                allNotes = allNotes,
                allVotes = allVotes,
                allManualSuspicions = allManualSuspicions,
                weights = weights
            )
        }

        return PlayerScoreAnalysis(
            playerId = player.id,
            playerName = player.name,
            stageIndex = currentStage,
            totalScore = rawCalculatedScore,
            statusLabel = statusLabel,
            positiveFactors = positiveFactors,
            negativeFactors = negativeFactors,
            stageTrendScores = stageTrends,
            totalTargetsMade = myTargets.size,
            uniqueTargetsCount = uniqueTargets.size,
            targetsReceivedCount = targetsReceived.size,
            targetFlipsCount = targetFlips,
            votesCastCount = myVotes.size,
            votesReceivedCount = votesReceived.size,
            suspiciousNotesCount = suspiciousNotes.size,
            talkNotesCount = talkNotes.size,
            opinionNotesCount = opinionNotes.size,
            manualScore = currentManual,
            isEliminated = player.isEliminated,
            isOwner = player.isOwner,
            knownRole = player.knownRole ?: if (player.isOwner) "CITIZEN" else null
        )
    }

    private fun calculateScoreForHistoricalStage(
        player: PlayerEntity,
        targetStage: Int,
        allPlayers: List<PlayerEntity>,
        allTargets: List<TargetEntity>,
        allNotes: List<PlayerNoteEntity>,
        allVotes: List<VoteEntity>,
        allManualSuspicions: List<ManualSuspicionEntity>,
        weights: AlgorithmWeightEntity
    ): Int {
        val targetsUpTo = allTargets.filter { it.stageIndex <= targetStage && it.sourcePlayerId == player.id }
        val targetsReceivedUpTo = allTargets.filter { it.stageIndex == targetStage && it.targetPlayerId == player.id }
        val notesUpTo = allNotes.filter { it.stageIndex <= targetStage && it.playerId == player.id }
        val votesReceivedUpTo = allVotes.filter { it.stageIndex == targetStage && it.targetId == player.id }
        val manual = allManualSuspicions
            .firstOrNull { it.stageIndex == targetStage && it.playerId == player.id }?.score ?: 50

        var score = 50
        score += ((manual - 50) * weights.manualWeight * 1.5f).roundToInt()
        val suspiciousNotes = notesUpTo.filter { it.category == "SUSPICIOUS" }
        if (suspiciousNotes.isNotEmpty()) {
            score += (suspiciousNotes.size * 12 * weights.suspiciousNoteWeight / 0.25f).roundToInt().coerceAtMost(30)
        } else {
            score -= 5
        }
        if (targetsReceivedUpTo.size >= 2) {
            score += 10
        }
        if (votesReceivedUpTo.isNotEmpty()) {
            score += votesReceivedUpTo.size * 10
        }
        return score.coerceIn(0, 100)
    }
}

package com.example.domain.model

import com.example.data.local.PlayerEntity
import com.example.data.local.PlayerNoteEntity
import com.example.data.local.TargetEntity
import com.example.data.local.VoteEntity
import com.example.domain.algorithm.PlayerScoreAnalysis

data class CitizenConflict(
    val player1Id: Long,
    val player1Name: String,
    val player1Seat: Int,
    val player2Id: Long,
    val player2Name: String,
    val player2Seat: Int,
    val severityLevel: String, // "اصطکاک شدید", "تارگت متقابل", "اختلاف رأی"
    val description: String,
    val exploitTip: String
)

data class MisdirectionTactic(
    val targetPlayerId: Long,
    val targetPlayerName: String,
    val targetSeat: Int,
    val tacticTitle: String,
    val tacticDescription: String,
    val safetyLevel: String // "ایمن", "کم‌ریسک", "تهاجمی"
)

object StealthMisdirectionEngine {

    fun analyzeConflictsAndTactics(
        players: List<PlayerEntity>,
        targets: List<TargetEntity>,
        votes: List<VoteEntity>,
        notes: List<PlayerNoteEntity>,
        scores: List<PlayerScoreAnalysis>,
        currentStage: Int
    ): Pair<List<CitizenConflict>, List<MisdirectionTactic>> {
        val alivePlayers = players.filter { !it.isEliminated }
        val playerMap = players.associateBy { it.id }
        val playerSeatMap = players.mapIndexed { index, p -> p.id to (index + 1) }.toMap()
        val scoreMap = scores.associateBy { it.playerId }

        val conflicts = mutableListOf<CitizenConflict>()

        // 1. Find mutual targets and fierce cross-accusations
        val targetsInStage = targets.filter { it.stageIndex <= currentStage }
        for (i in alivePlayers.indices) {
            for (j in (i + 1) until alivePlayers.size) {
                val p1 = alivePlayers[i]
                val p2 = alivePlayers[j]

                val p1TargetsP2 = targetsInStage.any { it.sourcePlayerId == p1.id && it.targetPlayerId == p2.id }
                val p2TargetsP1 = targetsInStage.any { it.sourcePlayerId == p2.id && it.targetPlayerId == p1.id }

                val s1 = playerSeatMap[p1.id] ?: 1
                val s2 = playerSeatMap[p2.id] ?: 2

                if (p1TargetsP2 && p2TargetsP1) {
                    conflicts.add(
                        CitizenConflict(
                            player1Id = p1.id,
                            player1Name = p1.name,
                            player1Seat = s1,
                            player2Id = p2.id,
                            player2Name = p2.name,
                            player2Seat = s2,
                            severityLevel = "تارگت متقابل شدید",
                            description = "صندلی $s1 (${p1.name}) و صندلی $s2 (${p2.name}) پیوسته به یکدیگر تارگت می‌زنند.",
                            exploitTip = "یکی از این دو را تأیید کنید و روی دیگری فشار ملایم بگذارید تا توجه از تیم مافیا دور بماند."
                        )
                    )
                } else if (p1TargetsP2 || p2TargetsP1) {
                    val attacker = if (p1TargetsP2) p1 else p2
                    val victim = if (p1TargetsP2) p2 else p1
                    val sa = playerSeatMap[attacker.id] ?: 1
                    val sv = playerSeatMap[victim.id] ?: 2
                    conflicts.add(
                        CitizenConflict(
                            player1Id = attacker.id,
                            player1Name = attacker.name,
                            player1Seat = sa,
                            player2Id = victim.id,
                            player2Name = victim.name,
                            player2Seat = sv,
                            severityLevel = "فشار یک‌طرفه",
                            description = "صندلی $sa (${attacker.name}) روی صندلی $sv (${victim.name}) اتهام زده است.",
                            exploitTip = "در دفاع از صندلی $sv سخن بگویید یا موج صندلی $sa را تشدید کنید تا شکاف شهروندی عمیق‌تر شود."
                        )
                    )
                }
            }
        }

        // 2. Compute subtle misdirection targets
        val tactics = mutableListOf<MisdirectionTactic>()

        // Find citizens with moderate to high suspicion who are NOT the actual player
        val highSuspicionCitizens = alivePlayers
            .mapNotNull { p ->
                val sc = scoreMap[p.id]?.totalScore ?: 50
                p to sc
            }
            .sortedByDescending { it.second }

        for ((citizen, score) in highSuspicionCitizens.take(4)) {
            val seat = playerSeatMap[citizen.id] ?: 1
            val receivedTargetsCount = targetsInStage.count { it.targetPlayerId == citizen.id }

            if (score >= 60 || receivedTargetsCount >= 2) {
                tactics.add(
                    MisdirectionTactic(
                        targetPlayerId = citizen.id,
                        targetPlayerName = citizen.name,
                        targetSeat = seat,
                        tacticTitle = "سوار شدن روی موج منفی صندلی $seat",
                        tacticDescription = "«${citizen.name}» هم‌اکنون $score٪ مشکوکی و $receivedTargetsCount تارگت دارد. تارگت زدن به او بسیار طبیعی به نظر می‌رسد و شک کسی را برنمی‌انگیزد.",
                        safetyLevel = "ایمن"
                    )
                )
            } else {
                tactics.add(
                    MisdirectionTactic(
                        targetPlayerId = citizen.id,
                        targetPlayerName = citizen.name,
                        targetSeat = seat,
                        tacticTitle = "ایجاد شک بدون درگیری مستقیم",
                        tacticDescription = "با اشاره به سکوت یا موضع خنثی «${citizen.name}»، بحث میز را به سمت او منحرف کنید.",
                        safetyLevel = "کم‌ریسک"
                    )
                )
            }
        }

        return Pair(conflicts.take(5), tactics.take(4))
    }
}

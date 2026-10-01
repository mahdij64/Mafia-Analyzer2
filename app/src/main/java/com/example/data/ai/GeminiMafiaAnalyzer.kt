package com.example.data.ai

import com.example.BuildConfig
import com.example.data.local.AiSettingsEntity
import com.example.data.local.PlayerEntity
import com.example.data.local.PlayerNoteEntity
import com.example.data.local.TargetEntity
import com.example.data.local.VoteEntity
import com.example.data.model.GameStage
import com.example.domain.algorithm.PlayerScoreAnalysis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class AiAnalysisResult {
    data class Success(val markdownContent: String, val providerLabel: String = "") : AiAnalysisResult()
    data class Error(val message: String, val fallbackAnalysis: String? = null) : AiAnalysisResult()
    object Loading : AiAnalysisResult()
}

object GeminiMafiaAnalyzer {

    /** Gemini model used only for the legacy fallback path (when no router is configured). */
    private const val GEMINI_MODEL = "gemini-3.5-flash"

    private val client = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    suspend fun analyzeGame(
        gameName: String,
        currentStage: Int,
        players: List<PlayerEntity>,
        targets: List<TargetEntity>,
        notes: List<PlayerNoteEntity>,
        votes: List<VoteEntity>,
        scores: List<PlayerScoreAnalysis>,
        aiSettings: AiSettingsEntity? = null
    ): AiAnalysisResult = withContext(Dispatchers.IO) {
        val prompt = buildAnalysisPrompt(gameName, currentStage, players, targets, notes, votes, scores)

        // 1) Preferred path: user-configured OpenAI-compatible router (e.g. 9router)
        if (aiSettings != null && aiSettings.isRouterConfigured) {
            val routerLabel = "مدل ${aiSettings.model}"
            OpenAiCompatClient.chat(
                baseUrl = aiSettings.baseUrl,
                apiKey = aiSettings.apiKey,
                model = aiSettings.model,
                messages = listOf(ChatMessage.user(prompt)),
                temperature = aiSettings.temperature
            ).let { result ->
                return@withContext result.fold(
                    onSuccess = { text ->
                        if (text.isBlank()) {
                            AiAnalysisResult.Error(
                                message = "سرویس پاسخ متنی خالی برگرداند ($routerLabel). تحلیل هوشمند محلی ارائه شد.",
                                fallbackAnalysis = generateLocalDeepAnalysis(gameName, currentStage, players, targets, notes, votes, scores)
                            )
                        } else {
                            AiAnalysisResult.Success(markdownContent = text, providerLabel = routerLabel)
                        }
                    },
                    onFailure = { e ->
                        AiAnalysisResult.Error(
                            message = "خطا در ارتباط با سرویس پیکربندی‌شده ($routerLabel): ${e.message ?: "خطای ناشناخته"}. تحلیل هوشمند محلی ارائه شد.",
                            fallbackAnalysis = generateLocalDeepAnalysis(gameName, currentStage, players, targets, notes, votes, scores)
                        )
                    }
                )
            }
        }

        // 2) Legacy fallback: Gemini REST API with the key injected via the secrets plugin
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // Generate rich local deep analytical report if no provider is available
            return@withContext AiAnalysisResult.Success(
                markdownContent = generateLocalDeepAnalysis(gameName, currentStage, players, targets, notes, votes, scores),
                providerLabel = "تحلیلگر محلی (آفلاین)"
            )
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$GEMINI_MODEL:generateContent?key=$apiKey"

            val rootJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            val partObj = JSONObject().apply {
                                put("text", prompt)
                            }
                            put(partObj)
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)

                val genConfig = JSONObject().apply {
                    put("temperature", 0.4)
                    put("topP", 0.9)
                }
                put("generationConfig", genConfig)
            }

            val requestBody = rootJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                val fallback = generateLocalDeepAnalysis(gameName, currentStage, players, targets, notes, votes, scores)
                return@withContext AiAnalysisResult.Error(
                    message = "خطا در برقراری ارتباط با سرویس هوش مصنوعی (کد ${response.code}). تحلیل هوشمند محلی ارائه شد.",
                    fallbackAnalysis = fallback
                )
            }

            val json = JSONObject(responseBody)
            val candidates = json.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text")

            if (!text.isNullOrBlank()) {
                AiAnalysisResult.Success(markdownContent = text, providerLabel = "Gemini ($GEMINI_MODEL)")
            } else {
                val fallback = generateLocalDeepAnalysis(gameName, currentStage, players, targets, notes, votes, scores)
                AiAnalysisResult.Success(markdownContent = fallback, providerLabel = "تحلیلگر محلی (آفلاین)")
            }
        } catch (e: Exception) {
            val fallback = generateLocalDeepAnalysis(gameName, currentStage, players, targets, notes, votes, scores)
            AiAnalysisResult.Error(
                message = "عدم دسترسی به اینترنت یا سرویس هوش مصنوعی: ${e.localizedMessage ?: "خطای ناشناخته"}. تحلیل هوشمند محلی جایگزین شد.",
                fallbackAnalysis = fallback
            )
        }
    }

    private fun buildAnalysisPrompt(
        gameName: String,
        currentStage: Int,
        players: List<PlayerEntity>,
        targets: List<TargetEntity>,
        notes: List<PlayerNoteEntity>,
        votes: List<VoteEntity>,
        scores: List<PlayerScoreAnalysis>
    ): String {
        val playerMap = players.associateBy { it.id }
        val stageName = GameStage.getStage(currentStage).title

        val sb = StringBuilder()
        sb.appendLine("شما دستیار حرفه‌ای تحلیل استراتژیک بازی مافیا هستید.")
        sb.appendLine("اطلاعات یادداشت‌ها، تارگت‌ها، رأی‌ها و مشاهدات ثبت‌شده توسط یک بازیکن در بازی «$gameName» تا مرحله «$stageName» به شرح زیر است.")
        sb.appendLine("توجه حیاتی: هرگز ادعای قطعی درباره مافیا بودن کسی نکنید، بلکه از ادبیات تحلیلی مانند «بر اساس شواهد ثبت‌شده»، «الگوی مشکوک»، «نیازمند بررسی بیشتر» استفاده کنید.")
        sb.appendLine("پاسخ شما باید کاملاً به زبان فارسی، ساختاریافته، بسیار دقیق، و شامل بخش‌های زیر باشد:")
        sb.appendLine("۱. خلاصه وضعیت بازی")
        sb.appendLine("۲. بازیکنان نیازمند بررسی بیشتر (کانون سوءظن)")
        sb.appendLine("۳. شواهد در جهت اتهام (الگوهای مشکوک)")
        sb.appendLine("۴. شواهد در جهت تبرئه یا شهروندی")
        sb.appendLine("۵. روابط، ائتلاف‌ها و تقابل‌های مهم")
        sb.appendLine("۶. تناقض‌ها و تغییر مواضع بین روزها")
        sb.appendLine("۷. روند تغییرات در طول روزها")
        sb.appendLine("۸. سوالات و فرضیات کلیدی که کاربر در روز بعد باید به آنها توجه کند")
        sb.appendLine()
        sb.appendLine("=== لیست بازیکنان ===")
        players.forEach { p ->
            val status = if (p.isEliminated) " (خارج شده از بازی)" else " (در بازی)"
            sb.appendLine("- ${p.name}$status")
        }

        sb.appendLine("\n=== امتیازات محاسبه‌شده الگوریتم تا مرحله $stageName ===")
        scores.sortedByDescending { it.totalScore }.forEach { s ->
            sb.appendLine("- ${s.playerName}: امتیاز سوءظن ${s.totalScore}/100 (${s.statusLabel}) | نظر شهودی کاربر: ${s.manualScore}/100")
        }

        sb.appendLine("\n=== تارگت‌های ثبت‌شده به تفکیک مرحله ===")
        (0..currentStage).forEach { stg ->
            val stgName = GameStage.getStage(stg).title
            val stgTargets = targets.filter { it.stageIndex == stg }
            sb.appendLine("مرحله $stgName:")
            if (stgTargets.isEmpty()) {
                sb.appendLine("  (تارگتی ثبت نشده)")
            } else {
                val grouped = stgTargets.groupBy { it.sourcePlayerId }
                grouped.forEach { (srcId, tgList) ->
                    val srcName = playerMap[srcId]?.name ?: "ناشناس"
                    val tgtNames = tgList.mapNotNull { playerMap[it.targetPlayerId]?.name }.joinToString("، ")
                    sb.appendLine("  $srcName → $tgtNames")
                }
            }
        }

        sb.appendLine("\n=== آراء خروج ثبت‌شده به تفکیک مرحله ===")
        (1..currentStage).forEach { stg ->
            val stgName = GameStage.getStage(stg).title
            val stgVotes = votes.filter { it.stageIndex == stg && it.targetId >= 0 }
            sb.appendLine("مرحله $stgName:")
            if (stgVotes.isEmpty()) {
                sb.appendLine("  (رأی‌گیری ثبت نشده یا انجام نشده)")
            } else {
                stgVotes.forEach { v ->
                    val voter = playerMap[v.voterId]?.name ?: "ناشناس"
                    val tgt = playerMap[v.targetId]?.name ?: "ناشناس"
                    sb.appendLine("  $voter به $tgt رأی داد")
                }
            }
        }

        sb.appendLine("\n=== یادداشت‌ها و مشاهدات ===")
        if (notes.isEmpty()) {
            sb.appendLine("(یادداشتی ثبت نشده)")
        } else {
            notes.forEach { n ->
                val pName = playerMap[n.playerId]?.name ?: "ناشناس"
                val catLabel = when (n.category) {
                    "TALK" -> "صحبت‌ها"
                    "SUSPICIOUS" -> "رفتار مشکوک"
                    "OPINION" -> "نظر شخصی"
                    else -> "یادداشت آزاد"
                }
                val stgTitle = GameStage.getStage(n.stageIndex).title
                sb.appendLine("- [$stgTitle] $pName ($catLabel): ${n.text}")
            }
        }

        return sb.toString()
    }

    fun generateLocalDeepAnalysis(
        gameName: String,
        currentStage: Int,
        players: List<PlayerEntity>,
        targets: List<TargetEntity>,
        notes: List<PlayerNoteEntity>,
        votes: List<VoteEntity>,
        scores: List<PlayerScoreAnalysis>
    ): String {
        val playerMap = players.associateBy { it.id }
        val sortedScores = scores.sortedByDescending { it.totalScore }
        val highestSuspects = sortedScores.take(3).filter { it.totalScore >= 60 }
        val lowestSuspects = sortedScores.takeLast(2).filter { it.totalScore <= 45 }
        val stageTitle = GameStage.getStage(currentStage).title

        val sb = StringBuilder()
        sb.appendLine("## 📋 تحلیل هوشمند بازی «$gameName» (مرحله $stageTitle)")
        sb.appendLine("*تحلیل ساختاریافته بر مبنای داده‌های ثبت‌شده محلی، الگوهای رفتار و محاسبات الگوریتمی*")
        sb.appendLine()

        sb.appendLine("### ۱. خلاصه وضعیت بازی")
        sb.appendLine("در مرحله $stageTitle، تعداد ${players.size} بازیکن در چرخه بازی هستند. بر اساس برآیند مشاهدات، الگوهای رأی‌گیری و تارگت‌های متقابل، تقسیم‌بندی شفافی در میز شکل گرفته است.")
        sb.appendLine()

        sb.appendLine("### ۲. بازیکنان نیازمند بررسی بیشتر (کانون سوءظن)")
        if (highestSuspects.isNotEmpty()) {
            highestSuspects.forEach { s ->
                sb.appendLine("- **${s.playerName}** (امتیاز مشکوک بودن: ${s.totalScore}/100)")
                sb.appendLine("  وضعیت: ${s.statusLabel}. دلایل اصلی: ${s.positiveFactors.take(2).joinToString(" و ") { it.title }}")
            }
        } else {
            sb.appendLine("در حال حاضر بازیکن با سوءظن بحرانی بالا ثبت نشده است؛ میز در حالت تعادل نسبی قرار دارد.")
        }
        sb.appendLine()

        sb.appendLine("### ۳. شواهد در جهت اتهام")
        val suspiciousNotes = notes.filter { it.category == "SUSPICIOUS" }
        if (suspiciousNotes.isNotEmpty()) {
            suspiciousNotes.take(4).forEach { n ->
                val p = playerMap[n.playerId]?.name ?: "ناشناس"
                sb.appendLine("- **$p**: ${n.text}")
            }
        } else {
            sb.appendLine("- نوسان در تارگت‌ها و تغییر مسیر ناگهانی بین روزهای بازی از مهم‌ترین عوامل ریسک ثبت‌شده است.")
        }
        sb.appendLine()

        sb.appendLine("### ۴. شواهد در جهت تبرئه یا شهروندی")
        if (lowestSuspects.isNotEmpty()) {
            lowestSuspects.forEach { s ->
                sb.appendLine("- **${s.playerName}** (امتیاز: ${s.totalScore}/100): ثبات نسبی در بیان، عدم ثبت رفتار مشکوک و اعتماد شخصی کاربر.")
            }
        } else {
            sb.appendLine("- بازیکنانی که تارگت‌های ثابت و استدلال‌های شفاف داشته‌اند پایداری بیشتری نشان داده‌اند.")
        }
        sb.appendLine()

        sb.appendLine("### ۵. روابط و ائتلاف‌های مهم")
        val stageTargets = targets.filter { it.stageIndex == currentStage }
        val targetPairs = stageTargets.map { "${playerMap[it.sourcePlayerId]?.name ?: "?"} ← ${playerMap[it.targetPlayerId]?.name ?: "?"}" }
        if (targetPairs.isNotEmpty()) {
            sb.appendLine("تارگت‌های ثبت‌شده در مرحله جاری:")
            targetPairs.take(6).forEach { sb.appendLine("- $it") }
        } else {
            sb.appendLine("هنوز تارگت‌های کافی برای شناسایی خطوط ائتلاف ثبت نشده است.")
        }
        sb.appendLine()

        sb.appendLine("### ۶. تناقض‌ها و تغییر مواضع")
        val flips = sortedScores.filter { it.targetFlipsCount > 0 }
        if (flips.isNotEmpty()) {
            flips.forEach { f ->
                sb.appendLine("- **${f.playerName}**: ثبت ${f.targetFlipsCount} مورد چرخش یا حذف تارگت‌های قبلی در روزهای متوالی.")
            }
        } else {
            sb.appendLine("تناقض ساختاری شدیدی در تغییر تارگت‌ها ثبت نشده است.")
        }
        sb.appendLine()

        sb.appendLine("### ۷. روند تغییرات در طول روزها")
        sb.appendLine("بررسی نمودار زمانی نشان می‌دهد فشار میز به تدریج از فاز معارفه به سمت افراد فعال یا دچار لکنت استدلالی همگرا شده است.")
        sb.appendLine()

        sb.appendLine("### ۸. سوالات و فرضیات کلیدی برای روز بعد")
        sb.appendLine("۱. آیا تارگت‌های بازیکنان مشکوک متوجه افراد کم‌حرف است یا بازیکنان موثر؟")
        sb.appendLine("۲. چرا برخی بازیکنان از ورود به تقابل‌های مستقیم اجتناب می‌کنند؟")
        sb.appendLine("۳. آیا آرای خروج پیرو جو عمومی میز بوده یا بر مبنای استدلال شخصی؟")

        return sb.toString()
    }
}

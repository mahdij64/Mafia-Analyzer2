package com.example.domain.speech

import com.example.data.local.GameEntity
import com.example.data.local.PlayerEntity
import com.example.data.local.PlayerNoteEntity
import com.example.data.local.TargetEntity
import com.example.data.model.GameStage
import com.example.domain.algorithm.PlayerScoreAnalysis

data class SpeechGuide(
    val ownerName: String,
    val ownerRole: String, // CITIZEN, MAFIA, INDEPENDENT
    val currentStageTitle: String,
    val isDayOne: Boolean,
    val openingHook: String, // اول چی بگم؟ (شروع عامیانه و جذاب)
    val keyTalkingPoints: List<String>, // نکات مهم به زبان عامیانه و بازاری دور میز
    val targetPatternAnalysis: String, // تحلیل الگوی تارگت‌های زده شده
    val topSuspectsAdvice: List<SuspectTalkingTip>, // بازیکن‌های مشکوک و نحوه صحبت درباره آن‌ها
    val userNotesInsights: List<String>, // تحلیل یادداشت‌های ثبت‌شده شما
    val roleSpecificStrategy: List<String>, // توصیه‌های ویژه متناسب با نقش
    val mafiaTeammatesInfo: MafiaTeammatesGuide?, // راهنمایی ویژه یاران مافیا در صورت مافیا بودن
    val fullColloquialSpeechText: String // متن نطق کامل و منسجم به زبان عامیانه برای اجرا دور میز
)

data class SuspectTalkingTip(
    val playerId: Long,
    val playerName: String,
    val suspicionScore: Int,
    val isTeammate: Boolean,
    val colloquialArgument: String
)

data class MafiaTeammatesGuide(
    val teammatesNames: List<String>,
    val tacticalCoveringAdvice: String,
    val safeTargetsToPush: List<String>
)

object SpeechCoachEngine {

    fun generateGuide(
        currentStageIndex: Int,
        activeGame: GameEntity?,
        players: List<PlayerEntity>,
        scores: List<PlayerScoreAnalysis>,
        allTargets: List<TargetEntity>,
        allNotes: List<PlayerNoteEntity>
    ): SpeechGuide {
        val stage = GameStage.getStage(currentStageIndex)
        val isDayOne = currentStageIndex <= 1

        val ownerPlayer = players.firstOrNull { it.isOwner || (activeGame?.ownerPlayerId != null && it.id == activeGame.ownerPlayerId) }
        val ownerName = ownerPlayer?.name ?: "من"
        val ownerRole = activeGame?.ownerRole ?: ownerPlayer?.knownRole ?: "CITIZEN"

        val livingPlayers = players.filter { !it.isEliminated }
        val eliminatedPlayers = players.filter { it.isEliminated }

        // Targets for this stage and previous stages
        val currentStageTargets = allTargets.filter { it.stageIndex == currentStageIndex }
        val previousTargets = allTargets.filter { it.stageIndex < currentStageIndex }

        // Identify mafia teammates (if owner is Mafia)
        val mafiaTeammates = players.filter {
            it.knownRole == "MAFIA" && it.id != ownerPlayer?.id
        }

        // Notes recorded by user or about other players
        val notesAboutLiving = allNotes.filter { note -> livingPlayers.any { it.id == note.playerId } }

        // Top suspects among living players (excluding owner)
        val livingScores = scores
            .filter { s -> livingPlayers.any { it.id == s.playerId } && s.playerId != ownerPlayer?.id }
            .sortedByDescending { it.totalScore }

        val topSuspects = livingScores.take(3)

        // 1. OPENING HOOK (اول چی بگم؟ به زبان عامیانه و میخکوب‌کننده)
        val openingHook = when (ownerRole) {
            "MAFIA" -> {
                if (isDayOne) {
                    "«سلام به همگی، من نمی‌خوام الکی وقت بازی رو بگیرم. فقط دقت کنید به بازی؛ الان روز اوله و تارگت‌های سفت زدن یعنی خودزنی. بذارید ببینیم کی با تارگت‌های چرت داره جو رو متشنج می‌کنه...»"
                } else {
                    val deadName = eliminatedPlayers.lastOrNull()?.name ?: "کشته شب"
                    "«سلام رفقا؛ از صبح که $deadName از بازی رفت، خیلیا دستپاچه شدن. من از دیشب تارگت‌ها رو رصد کردم، یه عده دارن عمداً جهت بازی رو کج می‌کنن تا ما رو از خط اصلی منحرف کنن. الان وقتشه حواس‌مون جمع باشه و بازیچه نشیم!»"
                }
            }
            "INDEPENDENT" -> {
                "«سلام رفقا؛ من نه با چپم نه با راست، فقط دارم از بیرون بازی رو تماشا می‌کنم و تناقض‌ها داره جیغ می‌زنه! بازی دست دو قطبی‌های الکی افتاده، بذارید دودوتا چهارتا کنیم ببینیم کی این وسط داره سود می‌بره...»"
            }
            else -> { // CITIZEN
                if (isDayOne) {
                    "«سلام رفقا؛ من به عنوان یک شهروند آگاه دارم با گوش باز بازی رو می‌شنوم. از همین اول کار بگم: تارگت‌های بدون دلیل و بازی‌خونی‌های تخیلی پذیرفته نیست! هر کی ادعا داره باید فکت بیاره وسط میز.»"
                } else {
                    val deadName = eliminatedPlayers.lastOrNull()?.name ?: "کشته دیشب"
                    "«سلام دوستان؛ دیشب $deadName رفت بیرون و این یعنی زنگ خطره. نگاه کنید کی دیروز بیشترین فشار رو روی $deadName گذاشت و امروز داره با خیال راحت سوت می‌زنه و تارگت عوض می‌کنه؟ فکت‌ها دارن باهامون حرف می‌زنن!»"
                }
            }
        }

        // 2. TARGET PATTERN ANALYSIS (الگوی تارگت زدن‌ها با زبان عامیانه)
        val targetFlips = livingScores.filter { it.targetFlipsCount > 0 }
        val targetPatternText = buildString {
            if (targetFlips.isNotEmpty()) {
                val fNames = targetFlips.take(2).joinToString(" و ") { "«${it.playerName}»" }
                append("🔄 چرخش و نوسان تارگت: $fNames موضع خودشون رو عوض کردن. این نوسان بی‌منطق، از بارزترین نشانه‌های موج‌سواریه؛ چون شهروند معمولاً تارگتش پیوستگی منطقی داره نه اینکه مدام بچرخه.")
            } else if (currentStageTargets.isNotEmpty()) {
                val mostTargeted = livingPlayers.maxByOrNull { p -> currentStageTargets.count { it.targetPlayerId == p.id } }
                val targetCount = currentStageTargets.count { it.targetPlayerId == mostTargeted?.id }
                if (mostTargeted != null && targetCount >= 2) {
                    append("🎯 تمرکز تارگت‌ها روی «${mostTargeted.name}» ($targetCount تارگت همزمان). باید دقت کنی آیا این یک پوش سازماندهی‌شده است یا اتهام واقعی؟")
                } else {
                    append("🎯 تارگت‌ها پراکنده است و بازیکن‌ها دارن دست به عصا حرکت می‌کنن تا واکنش بقیه رو بسنجن.")
                }
            } else {
                append("🎯 هنوز الگوی تارگت سنگینی شکل نگرفته؛ زمان طلاییه که تو با استدلال‌های خودت فضا رو دست بگیری.")
            }
        }

        // 3. USER NOTES ANALYSIS (تحلیل یادداشت‌های ثبت‌شده شما - بخش بسیار مهم)
        val userNotesInsights = mutableListOf<String>()
        if (notesAboutLiving.isNotEmpty()) {
            val suspiciousNotes = notesAboutLiving.filter { it.category == "SUSPICIOUS" }
            val talkNotes = notesAboutLiving.filter { it.category == "TALK" }
            val otherNotes = notesAboutLiving.filter { it.category !in listOf("SUSPICIOUS", "TALK") }

            if (suspiciousNotes.isNotEmpty()) {
                val sample = suspiciousNotes.take(3)
                sample.forEach { n ->
                    val pName = players.firstOrNull { it.id == n.playerId }?.name ?: "بازیکن"
                    userNotesInsights.add("📌 یادداشت سوءظن تو درباره «$pName»: نوشتی «${n.text}». تو نطق خودمانی بگو: «من رفتار $pName رو دقیق زیر نظر داشتم؛ این حرکتش اصلاً توجیه شهروندی نداشت!»")
                }
            }

            if (talkNotes.isNotEmpty()) {
                val sampleTalk = talkNotes.take(2)
                sampleTalk.forEach { n ->
                    val pName = players.firstOrNull { it.id == n.playerId }?.name ?: "بازیکن"
                    userNotesInsights.add("🗣️ از صحبت‌های ثبت‌شده «$pName»: ثبت کردی «${n.text}». می‌تونی تناقض این حرفش با ادعای امروزش رو توی صورتش بکوبی.")
                }
            }

            if (userNotesInsights.isEmpty() && otherNotes.isNotEmpty()) {
                val n = otherNotes.first()
                val pName = players.firstOrNull { it.id == n.playerId }?.name ?: "بازیکن"
                userNotesInsights.add("📝 یادداشت تو درباره «$pName»: «${n.text}» - به عنوان شاهد زنده ازش استفاده کن.")
            }
        } else {
            userNotesInsights.add("💡 شما هنوز یادداشت متنی خاصی برای بازیکن‌های زنده ثبت نکرده‌اید. با دکمه «یادداشت» سوتی‌ها، تناقض‌ها و جملات کلیدی رو بنویسید تا مستقیماً به نطق شما تزریق شوند.")
        }

        // 4. SUSPECTS ADVICE (بازیکن‌های مشکوک بر اساس آمار و نقش)
        val suspectsAdvice = topSuspects.map { s ->
            val isTeammate = mafiaTeammates.any { it.id == s.playerId }
            val tip = when {
                ownerRole == "MAFIA" && isTeammate -> {
                    "«این یار مافیای توئه! حواست باشه خرابش نکنی. اگر خواستی سوءظن بقیه رو از روش برداری، بگو: فلانی رو الکی دارن بزرگ می‌کنن، اون داره گیج بازی درمیاره ولی مافیا نیست، چشم‌تون رو روی مظنون اصلی نبندید!»"
                }
                ownerRole == "MAFIA" -> {
                    "«این شهروند امتیاز سوءظنش ${s.totalScore}٪ شده! بهترین طعمه برای پوش کردنه. بگو: فلانی تمام تارگتاش چرخشی و بدون فکته، چرا هیچ‌کس به این تناقض‌ها توجه نمی‌کنه؟»"
                }
                ownerRole == "INDEPENDENT" -> {
                    "«امتیاز سوءظن ${s.totalScore}٪. این بازیکن زیر تیغه. آروم آتش جنگ رو بین اون و طرف مقابل تندتر کن بدون اینکه مسئولیت بیرون رفتنش بیفته گردن تو.»"
                }
                else -> { // CITIZEN
                    "«امتیاز سوءظن ${s.totalScore}٪ (بالاترین سطح خطر). به شهر بگو: ببینید «${s.playerName}» هم تارگت‌هاش متناقضه هم رفتار دفاعیش غیرعادی بود. من رسماً به عنوان تارگت اول شهروندی روم رو به ${s.playerName} می‌کنم!»"
                }
            }
            SuspectTalkingTip(
                playerId = s.playerId,
                playerName = s.playerName,
                suspicionScore = s.totalScore,
                isTeammate = isTeammate,
                colloquialArgument = tip
            )
        }

        // 5. MAFIA TEAMMATES GUIDE (اگر مافیا باشد)
        val mafiaGuide = if (ownerRole == "MAFIA") {
            val names = mafiaTeammates.map { it.name }
            val safeTargets = livingPlayers
                .filter { it.id != ownerPlayer?.id && !mafiaTeammates.any { m -> m.id == it.id } }
                .take(2)
                .map { it.name }

            val coverAdvice = if (names.isNotEmpty()) {
                "هم‌تیمی‌های شما: ${names.joinToString("، ")}. به هیچ عنوان تارگت هماهنگ و تابلودار نزنید. یک نفر از شما باید کاملاً شهروند موجه بازی کند. به یارهایتان تارگت‌های سبک و بدون تعقیب بزنید تا تارگت فیک (زرگری) حساب شود ولی از بازی بیرون نروند!"
            } else {
                "⚠️ شما نقش مافیا دارید اما هنوز یاران مافیای خود را مشخص نکرده‌اید! لطفاً از بخش بالای همین صفحه یاران مافیای خود را تعیین کنید تا سناریوی دفاع و تخریب دقیق‌تر شود."
            }
            MafiaTeammatesGuide(
                teammatesNames = names,
                tacticalCoveringAdvice = coverAdvice,
                safeTargetsToPush = safeTargets
            )
        } else null

        // 6. ROLE SPECIFIC STRATEGY TIPS (نکات کلیدی متناسب با نقش)
        val roleStrategy = when (ownerRole) {
            "MAFIA" -> listOf(
                "🗡️ حفظ خونسردی: نگذار صدات بلرزه یا عصبی بشی. شهروندها دنبال کسی می‌گردن که تدافعی واکنش نشون بده.",
                "🗡️ سوار شدن روی موج اتهامات: هر شهروندی که سوتی داده یا شک برانگیزه رو آروم با فکت‌های منطقی زیر ضرب ببر.",
                "🗡️ کاور یاران مافیا: اگر به یارت فشار اومد، به جای دفاع کورکورانه بگو «موضوع فلانی مبهمه، اول به پرونده شفاف‌تر برسیم».",
                "🗡️ پرهیز از رأی تابلوی اول: صبر کن شهروندها رأی‌ها رو بشکنن، بعد با موج همراه شو."
            )
            "INDEPENDENT" -> listOf(
                "🎭 نقش دلسوز بازی: خودت رو به عنوان متفکری که فقط دنبال حقیقته نشون بده.",
                "🎭 انداختن دو قطبی: شهروندها و مافیاها رو روبروی هم بچین تا همدیگه رو پاره کنند و تو تا روز آخر امن بمونی.",
                "🎭 رأی‌های سرنوشت‌ساز: نگذار یک تیم خیلی زود برنده بشه؛ بازی رو در تعادل نگه دار."
            )
            else -> listOf( // CITIZEN
                "🛡️ مطالبه فکت و منطق: از کسایی که روی هوا تارگت می‌زنن بپرس «دقیقاً رو چه فکتی این حرفو می‌زنی؟».",
                "🛡️ رصد چرخش‌ها: کسانی که بعد از کشته شدن شهروندها تارگت‌شون رو ۱۸۰ درجه چرخوندن زیر ذره‌بین ببر.",
                "🛡️ اتحاد شهروندی: شهروندهای سفید رو به هم وصل کن و نگذار مافیا بین‌تون تفرقه بندازه.",
                "🛡️ شجاعت در دفاع: اگر بهت تارگت خورد، با خنده و فکت استدلال کن، عصبانیت فقط به نفع مافیاست."
            )
        }

        // 7. KEY TALKING POINTS (نکات مهم با زبان عامیانه)
        val keyPoints = mutableListOf<String>()
        keyPoints.add(openingHook)

        if (topSuspects.isNotEmpty()) {
            val mainTarget = topSuspects.first()
            if (ownerRole == "MAFIA" && mafiaTeammates.any { it.id == mainTarget.playerId }) {
                val altTarget = topSuspects.getOrNull(1) ?: topSuspects.first()
                keyPoints.add("🎯 نوک پیکان رو بچرخون سمت «${altTarget.playerName}»: بگو این همه شواهد و رفتارهای مشکوک رو ول کردید چسبیدید به حواشی؟")
            } else {
                keyPoints.add("🎯 تارگت اصلی من «${mainTarget.playerName}» هست (سوءظن ${mainTarget.totalScore}٪): به خاطر تغییر موضع و عدم ارائه استدلال منطقی.")
            }
        }

        if (userNotesInsights.isNotEmpty()) {
            keyPoints.add("📋 استناد محکم به یادداشت‌ها: فکت‌های ثبت‌شده‌ات رو شمرده شمرده بگو تا حس کنند تسلط کامل به تمام حرف‌ها داری.")
        }

        if (eliminatedPlayers.isNotEmpty()) {
            val deadList = eliminatedPlayers.joinToString("، ") { it.name }
            keyPoints.add("💀 استناد به کشته‌های بازی ($deadList): یادآوری کن که چطور بازی به این نقطه رسید و چه کسانی اونارو قربانی کردن.")
        }

        // 8. FULL COLLOQUIAL SPEECH SCRIPT (متن کامل سخنرانی خلاصه و جذاب)
        val fullSpeech = buildColloquialSpeech(
            ownerName = ownerName,
            ownerRole = ownerRole,
            isDayOne = isDayOne,
            stage = stage,
            topSuspects = topSuspects,
            mafiaTeammates = mafiaTeammates,
            userNotes = notesAboutLiving,
            eliminated = eliminatedPlayers,
            living = livingPlayers
        )

        return SpeechGuide(
            ownerName = ownerName,
            ownerRole = ownerRole,
            currentStageTitle = stage.title,
            isDayOne = isDayOne,
            openingHook = openingHook,
            keyTalkingPoints = keyPoints,
            targetPatternAnalysis = targetPatternText,
            topSuspectsAdvice = suspectsAdvice,
            userNotesInsights = userNotesInsights,
            roleSpecificStrategy = roleStrategy,
            mafiaTeammatesInfo = mafiaGuide,
            fullColloquialSpeechText = fullSpeech
        )
    }

    private fun buildColloquialSpeech(
        ownerName: String,
        ownerRole: String,
        isDayOne: Boolean,
        stage: GameStage,
        topSuspects: List<PlayerScoreAnalysis>,
        mafiaTeammates: List<PlayerEntity>,
        userNotes: List<PlayerNoteEntity>,
        eliminated: List<PlayerEntity>,
        living: List<PlayerEntity>
    ): String {
        return buildString {
            // سلام و افتتاحیه
            appendLine("سلام به همگی رفقا، وقت صحبت من کمه پس رک و پوست‌کنده می‌گم:")
            appendLine()

            // شروع متناسب با نقش
            when (ownerRole) {
                "MAFIA" -> {
                    if (isDayOne) {
                        appendLine("ببینید بچه‌ها، الان روز اوله و هیچ فکت قطعی روی میز نیست. ولی من دارم می‌بینم یه سری از دوستان جوری با هیجان و تند تارگت می‌زنن انگار شب از نقشه همه باخبر بودن! این رفتار اصلاً شهروندی نیست. شهروند واقعی صبر می‌کنه، گوش میده، بعد استدلال میاره.")
                    } else {
                        val lastDead = eliminated.lastOrNull()?.name ?: "کشته شب"
                        appendLine("بچه‌ها خوب گوش کنید؛ از وقتی «$lastDead» از بازی رفت، بازی داره از مسیرش منحرف می‌شه. یه عده نشستن و دارن ما رو هدایت می‌کنن سمت دعواهای زرگری و بی‌فایده، در حالی که مافیای اصلی خیلی شیک و ساکت نشسته تماشا می‌کنه.")
                    }

                    // هدایت به سمت مظنونین غیر یار
                    val targetPlayer = topSuspects.firstOrNull { s -> !mafiaTeammates.any { m -> m.id == s.playerId } }
                    if (targetPlayer != null) {
                        appendLine()
                        appendLine("من مشخصاً روی «${targetPlayer.playerName}» حرف دارم. چرا؟ چون صحبت‌هاش ثبات نداره. دیروز یه چیز می‌گفت، امروز کاملاً جهتش عوض شده! وقتی بهش تارگت می‌خوره دفاع عصبی می‌کنه، ولی خودش بدون فکت به بقیه انگ می‌زنه. به نظرم تارگت اول و آخر امروز باید ایشون باشه.")
                    }

                    // استفاده از یادداشت‌های کاربر
                    val relevantNote = userNotes.firstOrNull { it.playerId == targetPlayer?.playerId }
                    if (relevantNote != null) {
                        appendLine()
                        appendLine("من حتی توی صحبت‌های قبلیش یادداشت کردم: «${relevantNote.text}». آخه مگه می‌شه یه شهروند این‌طوری با منطق خودش دربیفته؟")
                    }

                    // پوشش هوشمند یاران مافیا
                    if (mafiaTeammates.isNotEmpty()) {
                        val mateNames = mafiaTeammates.joinToString(" و ") { it.name }
                        appendLine()
                        appendLine("در مورد «$mateNames» هم به نظرم الکی بحث رو کش ندید. بازی اونا مشخصه و تارگت زدن بهشون فقط آب به آسیاب مافیای واقعی ریختنه!")
                    }

                    appendLine()
                    appendLine("خلاصه اینکه رفقا، موج‌سواری نکنید، پشت دست من بازی کنید تا امروز دست مافیا از بازی کوتاه بشه!")
                }
                "INDEPENDENT" -> {
                    appendLine("من بازی رو دارم بدون تعصب نگاه می‌کنم. الان دو قطبی شکل گرفته که هر دو طرف دارن اشتباه می‌زنن. اگر شهروندید چرا انقدر راحت بازیچه می‌شید؟")
                    if (topSuspects.isNotEmpty()) {
                        val p1 = topSuspects.first().playerName
                        appendLine("نگاه کنید به حرکات «$p1». داد می‌زنه که این بازی طبیعی نیست. نه فکت داره نه منطق، فقط داره صداشو می‌بره بالا که ضعف استدلالش رو بپوشونه.")
                    }
                    appendLine("پیشنهاد من اینه که آرامش‌تون رو حفظ کنید و اجازه ندید کسی با بازی احساسی هدایت‌تون کنه.")
                }
                else -> { // CITIZEN
                    if (isDayOne) {
                        appendLine("من به عنوان یک شهروند پیگیر و با دقت، دارم خط و ربط صحبت‌ها رو ثبت می‌کنم. بچه‌ها، بازی معارفه تموم شد، الان وقت حساب‌کشیه. هر کسی که بدون فکت بخواد به دیگران اتهام بزنه، از نظر من اولین مظنون مافیاست!")
                    } else {
                        val lastDead = eliminated.lastOrNull()?.name ?: "کشته شب"
                        appendLine("شهدا و کشته‌های ما چرا رفتن بیرون؟ چون «$lastDead» شهروند موجه بود. حالا نگاه کنید کی بود که مدام به $lastDead حمله می‌کرد و امروز انگار نه انگار؟")
                    }

                    // تارگت اصلی
                    if (topSuspects.isNotEmpty()) {
                        val s1 = topSuspects.first()
                        appendLine()
                        appendLine("تارگت من شفاف روی «${s1.playerName}» هست. دلایلش هم روشنه: اولاً نوسان شدید در تارگت‌ها؛ ثانیاً فرافکنی وقتی که ازش توضیح خواستیم.")

                        val note = userNotes.firstOrNull { it.playerId == s1.playerId }
                        if (note != null) {
                            appendLine("من یادداشت کردم که در فاز قبلی دقیقاً گفت: «${note.text}». این تناقض آشکار چطور قابل توجیهه؟")
                        }
                    }

                    if (topSuspects.size >= 2) {
                        val s2 = topSuspects[1]
                        appendLine("نفر دوم هم «${s2.playerName}» است که داره با احتیاط بیش از حد بازی می‌کنه تا توی هیچ چالشی نیفته. شهروند واقعی ترسی از شفاف بودن نداره.")
                    }

                    appendLine()
                    appendLine("شهروندهای عزیز، متحد بمونید، تارگت‌های بدون دلیل رو هوا نکنید و بیاید امروز روی تارگت‌های منطقی متمرکز بشیم. ممنون از وقتتون.")
                }
            }
        }
    }
}

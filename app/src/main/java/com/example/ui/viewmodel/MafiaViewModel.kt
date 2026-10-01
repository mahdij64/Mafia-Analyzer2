package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiAnalysisResult
import com.example.data.ai.AiModelInfo
import com.example.data.ai.ChatMessage
import com.example.data.ai.GeminiMafiaAnalyzer
import com.example.data.ai.OpenAiCompatClient
import com.example.data.local.AiSettingsEntity
import com.example.data.local.AlgorithmWeightEntity
import com.example.data.local.AppDatabase
import com.example.data.local.GameEntity
import com.example.data.local.ManualSuspicionEntity
import com.example.data.local.PlayerEntity
import com.example.data.local.PlayerNoteEntity
import com.example.data.local.SecretNoteEntity
import com.example.data.local.TargetEntity
import com.example.data.local.VoteEntity
import com.example.data.model.GameStage
import com.example.data.repository.MafiaRepository
import com.example.domain.algorithm.PlayerScoreAnalysis
import com.example.domain.algorithm.SuspicionCalculator
import com.example.domain.speech.SpeechCoachEngine
import com.example.domain.speech.SpeechGuide
import com.example.domain.model.CitizenConflict
import com.example.domain.model.MisdirectionTactic
import com.example.domain.model.StealthMisdirectionEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MafiaUiState(
    val activeGame: GameEntity? = null,
    val allGames: List<GameEntity> = emptyList(),
    val currentStageIndex: Int = 0,
    val players: List<PlayerEntity> = emptyList(),
    val targets: List<TargetEntity> = emptyList(),
    val notes: List<PlayerNoteEntity> = emptyList(),
    val votes: List<VoteEntity> = emptyList(),
    val manualSuspicions: List<ManualSuspicionEntity> = emptyList(),
    val weights: AlgorithmWeightEntity = AlgorithmWeightEntity(0L),
    val scores: List<PlayerScoreAnalysis> = emptyList(),
    val searchQuery: String = "",
    val aiResult: AiAnalysisResult? = null,
    val snackbarMessage: String? = null
)

/** State of the "fetch models" action in the AI settings screen. */
sealed class AiModelsFetchState {
    object Idle : AiModelsFetchState()
    object Loading : AiModelsFetchState()
    data class Success(val count: Int) : AiModelsFetchState()
    data class Error(val message: String) : AiModelsFetchState()
}

/** One bubble in the AI settings test-chat conversation. */
data class TestChatMessage(
    val isFromUser: Boolean,
    val content: String,
    val isError: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

class MafiaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MafiaRepository

    private val _activeGameId = MutableStateFlow<Long?>(null)
    private val _currentStageIndex = MutableStateFlow(0)
    private val _searchQuery = MutableStateFlow("")
    private val _aiResult = MutableStateFlow<AiAnalysisResult?>(null)
    private val _snackbarMessage = MutableStateFlow<String?>(null)

    val activeGameId: StateFlow<Long?> = _activeGameId.asStateFlow()
    val currentStageIndex: StateFlow<Int> = _currentStageIndex.asStateFlow()
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    val aiResult: StateFlow<AiAnalysisResult?> = _aiResult.asStateFlow()
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = MafiaRepository(db.appDao())

        // Automatically load latest game or reset active game if empty
        viewModelScope.launch {
            repository.allGames.collect { games ->
                if (_activeGameId.value == null && games.isNotEmpty()) {
                    val latest = games.first()
                    _activeGameId.value = latest.id
                    _currentStageIndex.value = latest.currentStageIndex
                } else if (games.isEmpty()) {
                    _activeGameId.value = null
                    _currentStageIndex.value = 0
                }
            }
        }
    }

    val allGames: StateFlow<List<GameEntity>> = repository.allGames
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeGame: StateFlow<GameEntity?> = _activeGameId.flatMapLatest { id ->
        if (id == null) flowOf(null) else repository.getGame(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val players: StateFlow<List<PlayerEntity>> = _activeGameId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.getPlayers(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val targets: StateFlow<List<TargetEntity>> = _activeGameId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.getAllTargets(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val notes: StateFlow<List<PlayerNoteEntity>> = _activeGameId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.getAllNotes(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val votes: StateFlow<List<VoteEntity>> = _activeGameId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.getAllVotes(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val manualSuspicions: StateFlow<List<ManualSuspicionEntity>> = _activeGameId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.getAllManualSuspicions(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val algorithmWeights: StateFlow<AlgorithmWeightEntity> = _activeGameId.flatMapLatest { id ->
        if (id == null) flowOf(AlgorithmWeightEntity(0L))
        else repository.getWeights(id)
    }.combine(_activeGameId) { w, id ->
        w ?: AlgorithmWeightEntity(gameId = id ?: 0L)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AlgorithmWeightEntity(0L))

    // Reactive calculated scores
    private val _gameData = combine(players, targets, notes) { p, t, n ->
        Triple(p, t, n)
    }

    private val _actionData = combine(votes, manualSuspicions, algorithmWeights) { v, m, w ->
        Triple(v, m, w)
    }

    val calculatedScores: StateFlow<List<PlayerScoreAnalysis>> = combine(
        _gameData,
        _actionData,
        currentStageIndex
    ) { (pList, tList, nList), (vList, mList, weights), stage ->
        pList.map { p ->
            SuspicionCalculator.calculateForPlayer(
                player = p,
                currentStage = stage,
                allPlayers = pList,
                allTargets = tList,
                allNotes = nList,
                allVotes = vList,
                allManualSuspicions = mList,
                weights = weights
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val speechGuide: StateFlow<SpeechGuide?> = combine(
        _gameData,
        calculatedScores,
        activeGame,
        currentStageIndex
    ) { (pList, tList, nList), sList, game, stage ->
        if (game == null || pList.isEmpty()) null
        else SpeechCoachEngine.generateGuide(
            currentStageIndex = stage,
            activeGame = game,
            players = pList,
            scores = sList,
            allTargets = tList,
            allNotes = nList
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val maxUnlockedStageIndex: StateFlow<Int> = combine(
        activeGame,
        targets,
        votes,
        notes
    ) { game, tList, vList, nList ->
        val fromGame = game?.maxUnlockedStageIndex ?: 0
        val maxDataStage = maxOf(
            tList.maxOfOrNull { it.stageIndex } ?: 0,
            vList.maxOfOrNull { it.stageIndex } ?: 0,
            nList.maxOfOrNull { it.stageIndex } ?: 0,
            0
        )
        maxOf(fromGame, maxDataStage, game?.currentStageIndex ?: 0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // --- STEALTH MAFIA MISDIRECTION MODE (COMPLETELY SECRET) ---
    private val _isStealthMafiaMode = MutableStateFlow(false)
    val isStealthMafiaMode: StateFlow<Boolean> = _isStealthMafiaMode.asStateFlow()

    fun toggleStealthMafiaMode(): Boolean {
        _isStealthMafiaMode.value = !_isStealthMafiaMode.value
        return _isStealthMafiaMode.value
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val secretNotes: StateFlow<List<SecretNoteEntity>> = combine(
        _activeGameId,
        _isStealthMafiaMode
    ) { gId, isStealth ->
        if (gId != null && isStealth) {
            repository.getSecretNotes(gId)
        } else {
            flowOf(emptyList())
        }
    }.flatMapLatest { it }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stealthAnalysis: StateFlow<Pair<List<CitizenConflict>, List<MisdirectionTactic>>> = combine(
        combine(players, targets, votes) { p, t, v -> Triple(p, t, v) },
        combine(notes, calculatedScores, currentStageIndex) { n, s, stg -> Triple(n, s, stg) }
    ) { (pList, tList, vList), (nList, scores, stg) ->
        StealthMisdirectionEngine.analyzeConflictsAndTactics(
            players = pList,
            targets = tList,
            votes = vList,
            notes = nList,
            scores = scores,
            currentStage = stg
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Pair(emptyList(), emptyList()))

    fun addSecretNote(playerId: Long, content: String, suggestion: String = "") {
        val gId = _activeGameId.value ?: return
        val stg = _currentStageIndex.value
        viewModelScope.launch {
            repository.addSecretNote(gId, playerId, stg, content, suggestion)
        }
    }

    fun deleteSecretNote(noteId: Long) {
        viewModelScope.launch {
            repository.deleteSecretNote(noteId)
        }
    }

    fun addQuickTagNote(playerId: Long, tag: String, freeText: String = "") {
        val gId = _activeGameId.value ?: return
        val stg = _currentStageIndex.value
        val fullText = if (freeText.isBlank()) tag else "$tag: $freeText"
        viewModelScope.launch {
            repository.addNote(gId, playerId, stg, category = "SUSPICIOUS", text = fullText)
        }
    }

    fun selectGame(gameId: Long) {
        viewModelScope.launch {
            _activeGameId.value = gameId
            _aiResult.value = null
            // Also update current stage from game
            val g = allGames.value.firstOrNull { it.id == gameId }
            if (g != null) {
                _currentStageIndex.value = g.currentStageIndex
            }
        }
    }

    fun setStage(stageIndex: Int) {
        val maxUnlocked = maxUnlockedStageIndex.value
        if (stageIndex > maxUnlocked) {
            val lockedStageTitle = GameStage.getStage(stageIndex).title
            val currentStageTitle = GameStage.getStage(_currentStageIndex.value).title
            showSnackbar("مرحله «$lockedStageTitle» قفل است. لطفاً ابتدا «$currentStageTitle» را به پایان برسانید.")
            return
        }
        _currentStageIndex.value = stageIndex
        val gId = _activeGameId.value ?: return
        viewModelScope.launch {
            repository.updateStage(gId, stageIndex)
        }
    }

    fun finishStageAndAdvance(stageIndex: Int) {
        val nextStageIndex = stageIndex + 1
        val currentTitle = GameStage.getStage(stageIndex).title
        val nextTitle = GameStage.getStage(nextStageIndex).title
        _currentStageIndex.value = nextStageIndex
        val gId = _activeGameId.value ?: return
        viewModelScope.launch {
            repository.advanceStage(gId, nextStageIndex)
            showSnackbar("پرونده «$currentTitle» بسته شد و «$nextTitle» آغاز گردید 🌅")
        }
    }

    fun createNewGame(
        name: String,
        playerNames: List<String>,
        citizenCount: Int = 0,
        mafiaCount: Int = 0,
        independentCount: Int = 0,
        ownerPlayerIndex: Int? = null,
        ownerRole: String = "CITIZEN",
        mafiaTeammateIndices: Set<Int> = emptySet()
    ) {
        viewModelScope.launch {
            val id = repository.createGame(
                name = name,
                playerNames = playerNames,
                citizenCount = citizenCount,
                mafiaCount = mafiaCount,
                independentCount = independentCount,
                ownerPlayerIndex = ownerPlayerIndex,
                ownerRole = ownerRole,
                mafiaTeammateIndices = mafiaTeammateIndices
            )
            _activeGameId.value = id
            _currentStageIndex.value = 0
            _aiResult.value = null
            showSnackbar("بازی «$name» با موفقیت ایجاد شد")
        }
    }

    fun updateMafiaTeammates(teammatePlayerIds: Set<Long>) {
        val gId = _activeGameId.value ?: return
        viewModelScope.launch {
            repository.updateMafiaTeammates(gId, teammatePlayerIds)
            showSnackbar("لیست یاران مافیا به‌روزرسانی شد 🗡️")
        }
    }

    fun setPlayerKnownRole(playerId: Long, role: String?) {
        viewModelScope.launch {
            repository.setPlayerKnownRole(playerId, role)
            showSnackbar("نقش بازیکن ذخیره شد")
        }
    }

    fun updateGameScenarioAndOwner(
        citizenCount: Int,
        mafiaCount: Int,
        independentCount: Int,
        ownerPlayerId: Long?,
        ownerRole: String
    ) {
        val gId = _activeGameId.value ?: return
        viewModelScope.launch {
            repository.updateGameScenarioAndOwner(
                gameId = gId,
                citizenCount = citizenCount,
                mafiaCount = mafiaCount,
                independentCount = independentCount,
                ownerPlayerId = ownerPlayerId,
                ownerRole = ownerRole
            )
            showSnackbar("سناریو و هویت مالک به‌روزرسانی شد")
        }
    }

    fun createSampleGame() {
        val samplePlayers = listOf(
            "قاسم", "رضا", "مهدی", "مهدیار", "یسنا", "زهرا", "فاطی", "علی"
        )
        createNewGame("بازی نمونه مافیا", samplePlayers)
    }

    fun renameGame(gameId: Long, newName: String) {
        viewModelScope.launch {
            repository.renameGame(gameId, newName)
            showSnackbar("نام بازی تغییر یافت")
        }
    }

    fun duplicateGame(gameId: Long) {
        viewModelScope.launch {
            val newId = repository.duplicateGame(gameId)
            if (newId > 0) {
                _activeGameId.value = newId
                showSnackbar("بازی کپی شد و فعال شد")
            }
        }
    }

    fun deleteGame(gameId: Long) {
        viewModelScope.launch {
            repository.deleteGame(gameId)
            val remaining = repository.getAllGamesDirect().filter { it.id != gameId }
            if (_activeGameId.value == gameId) {
                if (remaining.isNotEmpty()) {
                    selectGame(remaining.first().id)
                } else {
                    _activeGameId.value = null
                    _currentStageIndex.value = 0
                    _aiResult.value = null
                }
            }
            showSnackbar("بازی و تمام داده‌های آن به طور کامل پاک شدند 🗑️")
        }
    }

    fun clearAllDataAndReset() {
        viewModelScope.launch {
            repository.clearAllData()
            _activeGameId.value = null
            _currentStageIndex.value = 0
            _aiResult.value = null
            showSnackbar("تمام بازی‌ها و اطلاعات سیستم به طور کامل پاک شدند 🗑️")
        }
    }

    fun addPlayer(name: String) {
        val gId = _activeGameId.value ?: return
        viewModelScope.launch {
            val result = repository.addPlayer(gId, name)
            if (result > 0) {
                showSnackbar("بازیکن «$name» اضافه شد")
            }
        }
    }

    fun updatePlayer(player: PlayerEntity) {
        viewModelScope.launch {
            repository.updatePlayer(player)
            showSnackbar("اطلاعات بازیکن به‌روزرسانی شد")
        }
    }

    fun renamePlayer(playerId: Long, newName: String) {
        viewModelScope.launch {
            repository.renamePlayer(playerId, newName)
            showSnackbar("نام بازیکن به «$newName» تغییر یافت")
        }
    }

    fun swapPlayers(player1Id: Long, player2Id: Long) {
        val currentList = players.value.toMutableList()
        val idx1 = currentList.indexOfFirst { it.id == player1Id }
        val idx2 = currentList.indexOfFirst { it.id == player2Id }
        if (idx1 >= 0 && idx2 >= 0 && idx1 != idx2) {
            val p1 = currentList[idx1]
            val p2 = currentList[idx2]
            currentList[idx1] = p2
            currentList[idx2] = p1
            viewModelScope.launch {
                repository.reorderPlayers(currentList)
                showSnackbar("صندلی «${p1.name}» و «${p2.name}» با موفقیت جابجا شد")
            }
        }
    }

    fun reorderPlayers(newOrder: List<PlayerEntity>) {
        viewModelScope.launch {
            repository.reorderPlayers(newOrder)
            showSnackbar("چیدمان صندلی‌های میز ذخیره شد")
        }
    }

    fun movePlayer(player: PlayerEntity, directionUp: Boolean) {
        val currentList = players.value.toMutableList()
        val currentIndex = currentList.indexOfFirst { it.id == player.id }
        if (currentIndex < 0) return

        val targetIndex = if (directionUp) currentIndex - 1 else currentIndex + 1
        if (targetIndex in 0 until currentList.size) {
            val item = currentList.removeAt(currentIndex)
            currentList.add(targetIndex, item)
            viewModelScope.launch {
                repository.reorderPlayers(currentList)
            }
        }
    }

    fun deletePlayer(player: PlayerEntity) {
        viewModelScope.launch {
            repository.deletePlayer(player)
            showSnackbar("بازیکن «${player.name}» حذف شد")
        }
    }

    fun togglePlayerEliminated(player: PlayerEntity) {
        viewModelScope.launch {
            val nextState = !player.isEliminated
            val stg = if (nextState) _currentStageIndex.value else null
            repository.setPlayerEliminated(player.id, nextState, stg)
            val msg = if (nextState) "«${player.name}» از بازی خارج شد 💀" else "«${player.name}» به بازی برگشت 🔄"
            showSnackbar(msg)
        }
    }

    fun setPlayerEliminated(playerId: Long, isEliminated: Boolean) {
        val player = players.value.firstOrNull { it.id == playerId } ?: return
        if (player.isEliminated != isEliminated) {
            togglePlayerEliminated(player)
        }
    }

    fun saveTargets(sourcePlayerId: Long, targetPlayerIds: List<Long>, showMessage: Boolean = false) {
        val gId = _activeGameId.value ?: return
        val stg = _currentStageIndex.value
        viewModelScope.launch {
            repository.replaceTargets(gId, stg, sourcePlayerId, targetPlayerIds)
            if (showMessage) {
                showSnackbar("تارگت‌ها با موفقیت ذخیره شدند")
            }
        }
    }

    fun addNote(playerId: Long, category: String, text: String) {
        val gId = _activeGameId.value ?: return
        val stg = _currentStageIndex.value
        viewModelScope.launch {
            repository.addNote(gId, playerId, stg, category, text)
            showSnackbar("یادداشت با موفقیت ثبت شد")
        }
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            repository.deleteNote(noteId)
            showSnackbar("یادداشت حذف شد")
        }
    }

    fun setManualSuspicion(playerId: Long, score: Int) {
        val gId = _activeGameId.value ?: return
        val stg = _currentStageIndex.value
        viewModelScope.launch {
            repository.setManualSuspicion(gId, playerId, stg, score)
        }
    }

    fun recordVote(voterId: Long, targetId: Long) {
        val gId = _activeGameId.value ?: return
        val stg = _currentStageIndex.value
        viewModelScope.launch {
            repository.recordVote(gId, stg, voterId, targetId)
            showSnackbar("رأی با موفقیت ثبت شد")
        }
    }

    fun clearVotesForCurrentStage() {
        val gId = _activeGameId.value ?: return
        val stg = _currentStageIndex.value
        viewModelScope.launch {
            repository.clearVotesForStage(gId, stg)
            showSnackbar("تمام آرای مرحله جاری پاک شدند")
        }
    }

    fun updateWeights(weights: AlgorithmWeightEntity) {
        viewModelScope.launch {
            repository.saveWeights(weights)
            showSnackbar("ضرایب الگوریتم ذخیره شدند")
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun runAiAnalysis() {
        val game = activeGame.value ?: return
        val pList = players.value
        val tList = targets.value
        val nList = notes.value
        val vList = votes.value
        val sList = calculatedScores.value
        val currentStage = _currentStageIndex.value
        val settings = aiSettings.value

        _aiResult.value = AiAnalysisResult.Loading
        viewModelScope.launch {
            val result = GeminiMafiaAnalyzer.analyzeGame(
                gameName = game.name,
                currentStage = currentStage,
                players = pList,
                targets = tList,
                notes = nList,
                votes = vList,
                scores = sList,
                aiSettings = settings
            )
            _aiResult.value = result
        }
    }

    fun clearAiAnalysis() {
        _aiResult.value = null
    }

    // ------------------------------------------------------------------
    // AI PROVIDER SETTINGS (OpenAI-compatible router, e.g. 9router)
    // ------------------------------------------------------------------

    val aiSettings: StateFlow<AiSettingsEntity> = repository.aiSettings
        .map { it ?: AiSettingsEntity() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AiSettingsEntity())

    fun saveAiSettings(settings: AiSettingsEntity) {
        viewModelScope.launch {
            repository.saveAiSettings(settings)
            showSnackbar("تنظیمات هوش مصنوعی ذخیره شد ✅")
        }
    }

    private val _aiModels = MutableStateFlow<List<AiModelInfo>>(emptyList())
    val aiModels: StateFlow<List<AiModelInfo>> = _aiModels.asStateFlow()

    private val _aiModelsFetchState = MutableStateFlow<AiModelsFetchState>(AiModelsFetchState.Idle)
    val aiModelsFetchState: StateFlow<AiModelsFetchState> = _aiModelsFetchState.asStateFlow()

    /**
     * Fetches the model list from the router. Uses the values passed from the screen
     * (the drafts the user is currently editing) so it works even before saving.
     */
    fun fetchAiModels(baseUrl: String, apiKey: String) {
        if (baseUrl.isBlank() || apiKey.isBlank()) {
            _aiModelsFetchState.value = AiModelsFetchState.Error("ابتدا آدرس سرویس و کلید API را وارد کنید")
            return
        }
        _aiModelsFetchState.value = AiModelsFetchState.Loading
        viewModelScope.launch {
            OpenAiCompatClient.fetchModels(baseUrl, apiKey)
                .onSuccess { list ->
                    _aiModels.value = list
                    _aiModelsFetchState.value =
                        if (list.isEmpty()) AiModelsFetchState.Error("سرویس لیست مدل خالی برگرداند")
                        else AiModelsFetchState.Success(list.size)
                }
                .onFailure { e ->
                    _aiModelsFetchState.value = AiModelsFetchState.Error(e.message ?: "خطای ناشناخته")
                }
        }
    }

    // --- Test chat ---
    private val _testChatMessages = MutableStateFlow<List<TestChatMessage>>(emptyList())
    val testChatMessages: StateFlow<List<TestChatMessage>> = _testChatMessages.asStateFlow()

    private val _testChatLoading = MutableStateFlow(false)
    val testChatLoading: StateFlow<Boolean> = _testChatLoading.asStateFlow()

    fun sendTestChatMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank() || _testChatLoading.value) return

        val s = aiSettings.value
        if (!s.isRouterConfigured) {
            showSnackbar("برای تست چت، ابتدا آدرس سرویس، کلید API و مدل را وارد و ذخیره کنید")
            return
        }

        _testChatMessages.value = _testChatMessages.value + TestChatMessage(isFromUser = true, content = trimmed)
        _testChatLoading.value = true
        viewModelScope.launch {
            // Send the whole (non-error) conversation so the model has context
            val history = _testChatMessages.value
                .filterNot { it.isError }
                .map { ChatMessage(if (it.isFromUser) "user" else "assistant", it.content) }
            OpenAiCompatClient.chat(s.baseUrl, s.apiKey, s.model, history, s.temperature)
                .onSuccess { reply ->
                    _testChatMessages.value = _testChatMessages.value + TestChatMessage(isFromUser = false, content = reply)
                }
                .onFailure { e ->
                    _testChatMessages.value = _testChatMessages.value + TestChatMessage(
                        isFromUser = false,
                        content = e.message ?: "خطای ناشناخته",
                        isError = true
                    )
                }
            _testChatLoading.value = false
        }
    }

    fun clearTestChat() {
        _testChatMessages.value = emptyList()
    }

    fun showSnackbar(message: String) {
        _snackbarMessage.value = message
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun generateShareableReport(): String {
        val game = activeGame.value ?: return "هیچ بازی فعالی وجود ندارد"
        val stg = GameStage.getStage(_currentStageIndex.value).title
        val pScores = calculatedScores.value.sortedByDescending { it.totalScore }

        val sb = StringBuilder()
        sb.appendLine("🕵️ گزارش تحلیل‌گر مافیا")
        sb.appendLine("بازی: ${game.name}")
        sb.appendLine("مرحله: $stg")
        sb.appendLine("────────────────────")
        sb.appendLine("📊 رده‌بندی سوءظن بازیکنان:")
        pScores.forEachIndexed { i, s ->
            sb.appendLine("${i + 1}. ${s.playerName} ── ${s.totalScore}/100 (${s.statusLabel})")
            if (s.positiveFactors.isNotEmpty()) {
                sb.appendLine("   ⚠️ دلایل اصلی: " + s.positiveFactors.take(2).joinToString(" | ") { it.title })
            }
        }
        sb.appendLine("────────────────────")
        sb.appendLine("🎯 خلاصه تارگت‌های مرحله جاری:")
        val stgTargets = targets.value.filter { it.stageIndex == _currentStageIndex.value }
        val playerMap = players.value.associateBy { it.id }
        if (stgTargets.isEmpty()) {
            sb.appendLine("تارگتی ثبت نشده است.")
        } else {
            stgTargets.groupBy { it.sourcePlayerId }.forEach { (src, tgList) ->
                val srcName = playerMap[src]?.name ?: "ناشناس"
                val tgtNames = tgList.mapNotNull { playerMap[it.targetPlayerId]?.name }.joinToString("، ")
                sb.appendLine("$srcName → $tgtNames")
            }
        }
        sb.appendLine("────────────────────")
        sb.appendLine("تولید شده توسط اپلیکیشن تحلیل‌گر مافیا")
        return sb.toString()
    }
}

package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.PlayerEntity
import com.example.ui.theme.*

/**
 * Screen for selecting a single speaker's targets.
 *
 * When a player is tapped, they are REMOVED from the grid and added
 * to a numbered text list below ("تارگت‌های امروز"). Previous days'
 * targets are shown in a history section for reference.
 *
 * Flow:
 *  1. Speaker header card at the top.
 *  2. History of previous days' targets (if any).
 *  3. Today's selected targets as a numbered list with remove buttons.
 *  4. Grid of remaining (non-targeted) players to tap and add.
 *  5. Optional notes (quick tags + free text).
 *  6. Buttons: save + next / back.
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SpeakerTargetScreen(
    speaker: PlayerEntity,
    allPlayers: List<PlayerEntity>,
    alreadyTargetedIds: Set<Long> = emptySet(),
    initialSelectedTargetIds: Set<Long> = emptySet(),
    initialNoteText: String = "",
    initialNoteTags: Set<String> = emptySet(),
    isLastPlayer: Boolean = false,
    previousStageTargets: Map<Int, List<PlayerEntity>> = emptyMap(),
    previousStageNotes: Map<Int, String> = emptyMap(),
    currentStageIndex: Int = 0,
    onBack: () -> Unit,
    onSaveAndNext: (targetIds: List<Long>, noteText: String, noteTags: List<String>) -> Unit,
    onSaveAndFinish: (targetIds: List<Long>, noteText: String, noteTags: List<String>) -> Unit =
        { ids, t, tg -> onSaveAndNext(ids, t, tg) },
    onSelectionChange: (Set<Long>) -> Unit = {}
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        // Local state - use speaker.id as key to ensure state resets when
        // speaker changes, even if initialSelectedTargetIds values are equal
        var selectedTargetIds by remember(speaker.id) {
            mutableStateOf(initialSelectedTargetIds)
        }
        var noteText by remember(speaker.id) { mutableStateOf(initialNoteText) }
        var selectedTags by remember(speaker.id) { mutableStateOf(initialNoteTags) }
        var showNoteDialog by remember { mutableStateOf(false) }

        val playerMap = remember(allPlayers) { allPlayers.associateBy { it.id } }

        // Players available to target (not self, not eliminated, not already selected)
        val availablePlayers = remember(allPlayers, speaker.id, selectedTargetIds) {
            allPlayers.filter { p ->
                p.id != speaker.id && !p.isEliminated && p.id !in selectedTargetIds
            }
        }

        // Selected targets as player entities (ordered by selection)
        val selectedTargets = remember(selectedTargetIds, playerMap) {
            selectedTargetIds.mapNotNull { playerMap[it] }
        }

        // Note & Tags Dialog
        if (showNoteDialog) {
            NoteAndTagsDialog(
                noteText = noteText,
                selectedTags = selectedTags,
                onNoteChange = { noteText = it },
                onToggleTag = { tag ->
                    selectedTags = if (selectedTags.contains(tag)) {
                        selectedTags - tag
                    } else {
                        selectedTags + tag
                    }
                },
                onDismiss = { showNoteDialog = false }
            )
        }

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🎤 سخنران: ${speaker.name}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextPrimaryDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            // Show tag/note count badge
                            if (selectedTags.isNotEmpty() || noteText.isNotBlank()) {
                                Spacer(Modifier.width(6.dp))
                                Surface(
                                    color = MafiaCrimson.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "📝${selectedTags.size + if (noteText.isNotBlank()) 1 else 0}",
                                        color = MafiaCrimsonLight,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "بازگشت",
                                tint = TextPrimaryDark
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showNoteDialog = true }) {
                            Icon(
                                Icons.Default.NoteAdd,
                                contentDescription = "یادداشت و برچسب",
                                tint = if (selectedTags.isNotEmpty() || noteText.isNotBlank()) MafiaCrimsonLight else TextPrimaryDark,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MafiaDarkBg,
                        titleContentColor = TextPrimaryDark
                    )
                )
            },
            bottomBar = {
                BottomAppBar(
                    containerColor = MafiaCardBg,
                    contentColor = TextPrimaryDark
                ) {
                    // "No target" button — save with empty targets
                    OutlinedButton(
                        onClick = {
                            onSaveAndNext(
                                emptyList(),
                                noteText,
                                selectedTags.toList()
                            )
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = TextSecondaryDark
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MafiaBorder),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("تارگت نزد", fontSize = 11.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text("بازگشت", color = TextPrimaryDark, fontSize = 11.sp)
                    }
                    Button(
                        onClick = {
                            onSaveAndNext(
                                selectedTargetIds.toList(),
                                noteText,
                                selectedTags.toList()
                            )
                        },
                        enabled = selectedTargetIds.isNotEmpty() || noteText.isNotBlank() || selectedTags.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MafiaCrimson,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (isLastPlayer) "ثبت و پایان" else "ثبت و نفر بعد", fontSize = 11.sp)
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MafiaDarkBg)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // ── Compact speaker header ──
                CompactSpeakerHeader(speaker = speaker)

                // ── History: Previous stages' targets ──
                if (previousStageTargets.isNotEmpty() || previousStageNotes.isNotEmpty()) {
                    PreviousTargetsHistory(
                        previousStageTargets = previousStageTargets,
                        previousStageNotes = previousStageNotes,
                        currentStageIndex = currentStageIndex
                    )
                }

                // ── "No target" banner when no targets selected ──
                if (selectedTargetIds.isEmpty()) {
                    Surface(
                        color = MafiaCardBg,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MafiaBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🎯 هنوز تارگتی انتخاب نشده — یا از پایین «تارگت نزد» بزنید",
                            color = TextMutedDark,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // ── Today's targets list ──
                TodayTargetsList(
                    selectedTargets = selectedTargets,
                    onRemove = { id ->
                        val newSel = selectedTargetIds - id
                        selectedTargetIds = newSel
                        onSelectionChange(newSel)
                    }
                )

                // ── Notes & Tags summary (shown below targets) ──
                if (selectedTags.isNotEmpty() || noteText.isNotBlank()) {
                    Surface(
                        color = MafiaCardBg,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MafiaBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showNoteDialog = true }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            // Tags
                            if (selectedTags.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    selectedTags.forEach { tag ->
                                        Surface(
                                            color = MafiaCrimson.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = tag,
                                                color = MafiaCrimsonLight,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            // Note text
                            if (noteText.isNotBlank()) {
                                if (selectedTags.isNotEmpty()) Spacer(Modifier.height(6.dp))
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "یادداشت: ",
                                        color = TextMutedDark,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = noteText,
                                        color = TextPrimaryDark,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Available players grid ──
                Text(
                    text = "افزودن تارگت (${availablePlayers.size} نفر باقی‌مانده)",
                    color = TextPrimaryDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )

                if (availablePlayers.isEmpty()) {
                    Surface(
                        color = MafiaCardBg,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (selectedTargetIds.isNotEmpty())
                                "✅ همه بازیکنان تارگت شده‌اند"
                            else
                                "بازیکن دیگری برای انتخاب وجود ندارد.",
                            color = TextMutedDark,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    PlayerCircleGrid(
                        players = availablePlayers,
                        selectedIds = emptySet(),
                        onToggle = { id ->
                            val newSel = selectedTargetIds + id
                            selectedTargetIds = newSel
                            onSelectionChange(newSel)
                        }
                    )
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ================================================================
// Today's targets — numbered list with remove buttons
// ================================================================

@Composable
private fun TodayTargetsList(
    selectedTargets: List<PlayerEntity>,
    onRemove: (Long) -> Unit
) {
    if (selectedTargets.isEmpty()) return

    Surface(
        color = MafiaCrimson.copy(alpha = 0.08f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MafiaCrimson.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.MyLocation,
                    contentDescription = null,
                    tint = MafiaCrimsonLight,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "تارگت‌های امروز (${selectedTargets.size} نفر)",
                    color = MafiaCrimsonLight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Spacer(Modifier.height(8.dp))

            selectedTargets.forEachIndexed { index, player ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Number badge
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MafiaCrimson),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = player.name,
                        color = TextPrimaryDark,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    // Remove button
                    IconButton(
                        onClick = { onRemove(player.id) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "حذف تارگت",
                            tint = MafiaCrimsonLight,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// ================================================================
// History of previous stages' targets
// ================================================================

@Composable
private fun PreviousTargetsHistory(
    previousStageTargets: Map<Int, List<PlayerEntity>>,
    previousStageNotes: Map<Int, String>,
    currentStageIndex: Int
) {
    Surface(
        color = MafiaCardBg,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MafiaBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    tint = MafiaGold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "📋 تاریخچه روزهای قبل",
                    color = MafiaGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Spacer(Modifier.height(6.dp))

            // Merge all previous stage indices from both maps
            val allStages = (previousStageTargets.keys + previousStageNotes.keys).sorted()

            allStages.forEach { stageIndex ->
                val targets = previousStageTargets[stageIndex] ?: emptyList()
                val note = previousStageNotes[stageIndex]
                val stageTitle = com.example.data.model.GameStage.getStage(stageIndex).title

                Surface(
                    color = MafiaSurfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        // Stage title + targets
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Surface(
                                color = MafiaGold.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.padding(end = 6.dp)
                            ) {
                                Text(
                                    text = stageTitle,
                                    color = MafiaGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            if (targets.isEmpty()) {
                                Text(
                                    text = "تارگتی نزد",
                                    color = TextMutedDark,
                                    fontSize = 12.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                val names = targets.joinToString("، ") { it.name }
                                Text(
                                    text = names,
                                    color = TextSecondaryDark,
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Note for this stage (if any)
                        if (!note.isNullOrBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "📝 ",
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = note,
                                    color = MafiaCrimsonLight.copy(alpha = 0.9f),
                                    fontSize = 11.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ================================================================
// Compact speaker header — single row, minimal space
// ================================================================

@Composable
private fun CompactSpeakerHeader(speaker: PlayerEntity) {
    Surface(
        color = MafiaCrimson.copy(alpha = 0.1f),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MafiaCrimson.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MafiaCrimson),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = speaker.name.take(2),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "🎤 سخنران",
                    color = TextMutedDark,
                    fontSize = 11.sp
                )
                Text(
                    text = speaker.name,
                    color = TextPrimaryDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

// ================================================================
// 4-column grid of player circles (tappable to ADD as target)
// ================================================================

@Composable
private fun PlayerCircleGrid(
    players: List<PlayerEntity>,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        players.chunked(4).forEach { rowPlayers ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowPlayers.forEach { player ->
                    val isSelected = selectedIds.contains(player.id)
                    PlayerCircle(
                        player = player,
                        isSelected = isSelected,
                        onClick = { onToggle(player.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(4 - rowPlayers.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PlayerCircle(
    player: PlayerEntity,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isSelected) MafiaCrimson else MafiaCardBg
    val borderColor = if (isSelected) MafiaCrimsonLight else MafiaBorder
    val textColor = if (isSelected) Color.White else TextPrimaryDark

    Column(
        modifier = modifier.padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .scale(if (isSelected) 1.05f else 1f)
                .clip(CircleShape)
                .background(bgColor)
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = borderColor,
                    shape = CircleShape
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = player.name.take(2),
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = player.name,
            color = textColor,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ================================================================
// Combined notes block: dropdown tags + free text
// ================================================================

private val QUICK_NOTE_TAGS = listOf(
    "تناقض",
    "سکوت مشکوک",
    "دفاع عصبی",
    "اتهام فله‌ای",
    "تغییر موضع",
    "خیلی محکم حرف زد",
    "ضعیف و مردد",
    "مشکوک به مافیا"
)

@Composable
private fun NoteAndTagsDialog(
    noteText: String,
    selectedTags: Set<String>,
    onNoteChange: (String) -> Unit,
    onToggleTag: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var tagsExpanded by remember { mutableStateOf(selectedTags.isEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MafiaCardBg,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.NoteAdd, contentDescription = null, tint = MafiaCrimsonLight, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("یادداشت و برچسب", color = TextPrimaryDark, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // ── Tags dropdown ──
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { tagsExpanded = !tagsExpanded }
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "برچسب‌های سریع",
                        color = TextPrimaryDark,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (selectedTags.isNotEmpty()) {
                        Surface(color = MafiaCrimson.copy(alpha = 0.2f), shape = RoundedCornerShape(10.dp)) {
                            Text(
                                text = "${selectedTags.size} انتخاب",
                                color = MafiaCrimsonLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                    }
                    Icon(
                        if (tagsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = TextMutedDark,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Selected tags as removable chips
                if (selectedTags.isNotEmpty()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        selectedTags.forEach { tag ->
                            Surface(
                                color = MafiaCrimson.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.clickable { onToggleTag(tag) }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Text(text = tag, color = MafiaCrimsonLight, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    Spacer(Modifier.width(4.dp))
                                    Icon(Icons.Default.Close, contentDescription = "حذف", tint = MafiaCrimsonLight, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                }

                // Dropdown list
                if (tagsExpanded) {
                    HorizontalDivider(color = MafiaBorder, thickness = 0.5.dp)
                    QUICK_NOTE_TAGS.forEach { tag ->
                        val isSel = selectedTags.contains(tag)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleTag(tag) }
                                .padding(vertical = 5.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSel,
                                onCheckedChange = { onToggleTag(tag) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MafiaCrimson,
                                    uncheckedColor = TextMutedDark,
                                    checkmarkColor = Color.White
                                ),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = tag,
                                color = if (isSel) MafiaCrimsonLight else TextPrimaryDark,
                                fontSize = 13.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                HorizontalDivider(color = MafiaBorder, thickness = 0.5.dp)

                // ── Free text note ──
                Text("یادداشت آزاد", color = TextPrimaryDark, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                OutlinedTextField(
                    value = noteText,
                    onValueChange = onNoteChange,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                    placeholder = { Text("نکته‌ای درباره سخنران...", color = TextMutedDark) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark,
                        focusedContainerColor = MafiaSurfaceVariant,
                        unfocusedContainerColor = MafiaSurfaceVariant,
                        focusedBorderColor = MafiaCrimson,
                        unfocusedBorderColor = MafiaBorder,
                        cursorColor = MafiaCrimsonLight,
                        focusedPlaceholderColor = TextMutedDark,
                        unfocusedPlaceholderColor = TextMutedDark
                    ),
                    minLines = 3,
                    maxLines = 6
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MafiaCrimson, contentColor = Color.White)
            ) {
                Text("بستن", fontSize = 13.sp)
            }
        }
    )
}

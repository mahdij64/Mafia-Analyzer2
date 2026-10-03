package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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
import com.example.data.local.PlayerNoteEntity
import com.example.ui.theme.*

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

/**
 * Screen for selecting a single speaker's targets.
 *
 * Flow:
 *  1. Speaker is shown at the top (large circle).
 *  2. The other players are shown in 4-per-row circular grid.
 *  3. The user taps players to toggle them as targets (multi-select).
 *  4. Optional notes (quick tags + free text) at the bottom.
 *  5. Buttons: "ثبت و بعدی" (save + advance to next player) and "بازگشت" (back).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeakerTargetScreen(
    speaker: PlayerEntity,
    allPlayers: List<PlayerEntity>,
    alreadyTargetedIds: Set<Long>, // who already targets the speaker (read-only info, optional)
    initialSelectedTargetIds: Set<Long> = emptySet(),
    initialNoteText: String = "",
    initialNoteTags: Set<String> = emptySet(),
    isLastPlayer: Boolean = false,
    onBack: () -> Unit,
    onSaveAndNext: (targetIds: List<Long>, noteText: String, noteTags: List<String>) -> Unit,
    onSaveAndFinish: (targetIds: List<Long>, noteText: String, noteTags: List<String>) -> Unit =
        { ids, t, tg -> onSaveAndNext(ids, t, tg) }
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        // Local state
        val candidatePlayers = remember(allPlayers, speaker) {
            allPlayers.filter { it.id != speaker.id }
        }
        var selectedTargetIds by remember(initialSelectedTargetIds) {
            mutableStateOf(initialSelectedTargetIds)
        }
        var noteText by remember(initialNoteText) { mutableStateOf(initialNoteText) }
        var selectedTags by remember(initialNoteTags) { mutableStateOf(initialNoteTags) }

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🎤 سخنران: ${speaker.name}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = TextPrimaryDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
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
                    Spacer(Modifier.weight(1f))
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("بازگشت", color = TextPrimaryDark)
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
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(if (isLastPlayer) "ثبت و پایان" else "ثبت و نفر بعد")
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
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Speaker big card
                SpeakerHeaderCard(speaker = speaker)

                // Target grid header
                Text(
                    text = "انتخاب تارگت (${selectedTargetIds.size} انتخاب شده)",
                    color = TextPrimaryDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )

                // 4-column grid of player circles
                if (candidatePlayers.isEmpty()) {
                    Text(
                        text = "بازیکن دیگری برای انتخاب وجود ندارد.",
                        color = TextMutedDark,
                        modifier = Modifier.padding(8.dp)
                    )
                } else {
                    PlayerCircleGrid(
                        players = candidatePlayers,
                        selectedIds = selectedTargetIds,
                        onToggle = { id ->
                            selectedTargetIds = if (selectedTargetIds.contains(id)) {
                                selectedTargetIds - id
                            } else {
                                selectedTargetIds + id
                            }
                        }
                    )
                }

                Divider(color = MafiaBorder, thickness = 0.5.dp)

                // Quick tags
                NotesTagsBlock(
                    selectedTags = selectedTags,
                    onToggleTag = { tag ->
                        selectedTags = if (selectedTags.contains(tag)) {
                            selectedTags - tag
                        } else {
                            selectedTags + tag
                        }
                    }
                )

                // Free note
                NotesFreeTextBlock(
                    noteText = noteText,
                    onChange = { noteText = it }
                )

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SpeakerHeaderCard(speaker: PlayerEntity) {
    Surface(
        color = MafiaCardBg,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MafiaCrimson.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MafiaCrimson),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = speaker.name.take(2),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "در حال صحبت:",
                    color = TextMutedDark,
                    fontSize = 12.sp
                )
                Text(
                    text = speaker.name,
                    color = TextPrimaryDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
private fun PlayerCircleGrid(
    players: List<PlayerEntity>,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit
) {
    // 4 per row
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
                // fill empty slots to keep alignment (only if last row is short)
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
        modifier = modifier
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .scale(if (isSelected) 1.05f else 1f)
                .clip(CircleShape)
                .background(bgColor)
                .border(width = if (isSelected) 2.dp else 1.dp, color = borderColor, shape = CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "انتخاب شده",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = player.name.take(2),
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
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

@Composable
private fun NotesTagsBlock(
    selectedTags: Set<String>,
    onToggleTag: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.NoteAdd,
                contentDescription = null,
                tint = MafiaCrimsonLight,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "برچسب‌های سریع",
                color = TextPrimaryDark,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
        // FlowRow of tags
        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            QUICK_NOTE_TAGS.forEach { tag ->
                val isSel = selectedTags.contains(tag)
                FilterChip(
                    selected = isSel,
                    onClick = { onToggleTag(tag) },
                    label = { Text(tag, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MafiaCrimson,
                        selectedLabelColor = Color.White,
                        containerColor = MafiaCardBg,
                        labelColor = TextPrimaryDark
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSel,
                        borderColor = MafiaBorder,
                        selectedBorderColor = MafiaCrimson
                    )
                )
            }
        }
    }
}

@Composable
private fun NotesFreeTextBlock(
    noteText: String,
    onChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "یادداشت آزاد",
            color = TextPrimaryDark,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
        OutlinedTextField(
            value = noteText,
            onValueChange = onChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 96.dp),
            placeholder = { Text("نکته‌ای درباره سخنران...", color = TextMutedDark) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimaryDark,
                unfocusedTextColor = TextPrimaryDark,
                focusedContainerColor = MafiaCardBg,
                unfocusedContainerColor = MafiaCardBg,
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
}

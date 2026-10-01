package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ManualSuspicionEntity
import com.example.data.local.PlayerEntity
import com.example.data.local.PlayerNoteEntity
import com.example.data.model.GameStage
import com.example.ui.components.StageSelectorTabs
import com.example.ui.components.SuspicionScoreBadge
import com.example.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun PlayerNotesScreen(
    currentStageIndex: Int,
    unlockedStageIndex: Int = 4,
    players: List<PlayerEntity>,
    notes: List<PlayerNoteEntity>,
    manualSuspicions: List<ManualSuspicionEntity>,
    onStageSelected: (Int) -> Unit,
    onAddNote: (playerId: Long, category: String, text: String) -> Unit,
    onDeleteNote: (noteId: Long) -> Unit,
    onSetManualSuspicion: (playerId: Long, score: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPlayerId by remember(players) {
        mutableStateOf(players.firstOrNull()?.id ?: 0L)
    }

    LaunchedEffect(players) {
        if (players.none { it.id == selectedPlayerId }) {
            selectedPlayerId = players.firstOrNull()?.id ?: 0L
        }
    }

    val selectedPlayer = players.firstOrNull { it.id == selectedPlayerId }

    // Manual score for selected player in this stage
    val currentManualScore = remember(manualSuspicions, selectedPlayerId, currentStageIndex) {
        manualSuspicions.firstOrNull {
            it.playerId == selectedPlayerId && it.stageIndex == currentStageIndex
        }?.score ?: 50
    }

    var sliderValue by remember(currentManualScore) {
        mutableStateOf(currentManualScore.toFloat())
    }

    var selectedCategory by remember { mutableStateOf("TALK") }
    var noteText by remember { mutableStateOf("") }
    var noteSearchQuery by remember { mutableStateOf("") }
    var noteCategoryFilter by remember { mutableStateOf("ALL") }
    var showAllPlayersNotes by remember { mutableStateOf(false) }

    val categories = listOf(
        Triple("TALK", "🗣️ صحبت‌ها", Color(0xFF64B5F6)),
        Triple("SUSPICIOUS", "⚠️ رفتار مشکوک", MafiaCrimson),
        Triple("OPINION", "💡 نظر شخصی", MafiaGold),
        Triple("FREE", "📝 یادداشت آزاد", Color(0xFF81C784))
    )

    // Specific structured Mafia behaviors required for personal analysis
    val structuredMafiaBehaviors = listOf(
        "ادعای شهروندی" to "TALK",
        "ادعای کارآگاه" to "TALK",
        "ادعای دکتر" to "TALK",
        "دفاع محکم از بازیکن" to "TALK",
        "تارگت شدید به بازیکن" to "SUSPICIOUS",
        "اتهام تناقض‌گویی" to "SUSPICIOUS",
        "تغییر موضع بی‌دلیل" to "SUSPICIOUS",
        "سکوت / کم‌کاری" to "SUSPICIOUS",
        "تنش و استرس بالا" to "SUSPICIOUS",
        "واکنش تدافعی به تارگت کوچک" to "SUSPICIOUS"
    )

    // Quick tag suggestions based on category
    val quickSuggestions = remember(selectedCategory) {
        when (selectedCategory) {
            "TALK" -> listOf("ادعای شهروندی", "ادعای کارآگاه", "ادعای دکتر", "دفاع محکم از بازیکن", "کاور کردن بی‌دلیل")
            "SUSPICIOUS" -> listOf("اتهام تناقض‌گویی", "تغییر موضع بی‌دلیل", "سکوت / کم‌کاری", "تنش و استرس بالا", "واکنش تدافعی به تارگت کوچک")
            "OPINION" -> listOf("حس می‌کنم شهرونده", "مشکوک به مافیای ساده", "شاید نقش‌دار باشه", "خیلی آرام و خونسرد")
            else -> listOf("چالش با رضا", "دست بلند نکردن", "اشاره غیرکلامی")
        }
    }

    val playerMap = remember(players) { players.associateBy { it.id } }
    val filteredNotes = remember(notes, selectedPlayerId, showAllPlayersNotes, noteSearchQuery, noteCategoryFilter) {
        notes
            .filter { if (showAllPlayersNotes) true else it.playerId == selectedPlayerId }
            .filter {
                if (noteSearchQuery.isBlank()) true
                else {
                    val pName = playerMap[it.playerId]?.name ?: ""
                    it.text.contains(noteSearchQuery.trim(), ignoreCase = true) ||
                        pName.contains(noteSearchQuery.trim(), ignoreCase = true)
                }
            }
            .filter {
                if (noteCategoryFilter == "ALL") true
                else it.category == noteCategoryFilter
            }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MafiaDarkBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 14.dp)
    ) {
        // Stage Tabs
        item {
            StageSelectorTabs(
                currentStageIndex = currentStageIndex,
                unlockedStageIndex = unlockedStageIndex,
                onStageSelected = onStageSelected
            )
        }

        // Player Selection Row
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MafiaBorder)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "انتخاب بازیکن برای بررسی و یادداشت:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(players, key = { it.id }) { player ->
                            val isSelected = player.id == selectedPlayerId
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedPlayerId = player.id
                                },
                                label = { Text(text = player.name) },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MafiaCrimsonDark,
                                    selectedLabelColor = TextPrimaryDark,
                                    containerColor = MafiaSurfaceVariant,
                                    labelColor = TextSecondaryDark
                                ),
                                modifier = Modifier.testTag("notes_player_chip_${player.id}")
                            )
                        }
                    }
                }
            }
        }

        if (selectedPlayer != null) {
            // Manual Suspicion Slider Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MafiaCardElevated),
                    shape = RoundedCornerShape(14.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MafiaBorder)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_suspicion_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "امتیاز مشکوک بودن شخصی (شهودی):",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                                Text(
                                    text = "برای مرحله ${GameStage.getStage(currentStageIndex).title}",
                                    fontSize = 12.sp,
                                    color = TextSecondaryDark
                                )
                            }
                            SuspicionScoreBadge(score = sliderValue.roundToInt())
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Slider(
                            value = sliderValue,
                            onValueChange = { sliderValue = it },
                            onValueChangeFinished = {
                                onSetManualSuspicion(selectedPlayer.id, sliderValue.roundToInt())
                            },
                            valueRange = 0f..100f,
                            steps = 19,
                            colors = SliderDefaults.colors(
                                thumbColor = when (sliderValue.roundToInt()) {
                                    in 0..35 -> SuspicionGreen
                                    in 36..65 -> SuspicionYellow
                                    else -> SuspicionRed
                                },
                                activeTrackColor = MafiaCrimson,
                                inactiveTrackColor = MafiaSurfaceVariant
                            ),
                            modifier = Modifier.testTag("manual_suspicion_slider")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "۰ = کاملاً غیرمشکوک (سفید)",
                                fontSize = 11.sp,
                                color = SuspicionGreen
                            )
                            Text(
                                text = "۵۰ = متوسط",
                                fontSize = 11.sp,
                                color = SuspicionYellow
                            )
                            Text(
                                text = "۱۰۰ = بسیار مشکوک (سیاه)",
                                fontSize = 11.sp,
                                color = SuspicionRed
                            )
                        }
                    }
                }
            }

            // Add Note Form
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
                    shape = RoundedCornerShape(14.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MafiaBorder)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "ثبت مشاهده / یادداشت برای «${selectedPlayer.name}»:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Category Tabs
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(categories) { (catKey, catLabel, catColor) ->
                                val isCatSelected = selectedCategory == catKey
                                Surface(
                                    onClick = { selectedCategory = catKey },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isCatSelected) catColor.copy(alpha = 0.2f) else MafiaSurfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = 1.dp,
                                        color = if (isCatSelected) catColor else Color.Transparent
                                    ),
                                    modifier = Modifier.testTag("category_chip_$catKey")
                                ) {
                                    Text(
                                        text = catLabel,
                                        fontSize = 13.sp,
                                        fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isCatSelected) catColor else TextSecondaryDark,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "🏷️ رفتارهای ساختاریافته مافیا (انتخاب سریع):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MafiaGold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Structured mafia behavior pills
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(structuredMafiaBehaviors) { (behavior, cat) ->
                                Surface(
                                    onClick = {
                                        selectedCategory = cat
                                        noteText = if (noteText.isBlank()) behavior else "$noteText - $behavior"
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (cat == "SUSPICIOUS") MafiaCrimsonDark.copy(alpha = 0.5f) else MafiaSurfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = 1.dp,
                                        color = if (cat == "SUSPICIOUS") MafiaCrimson.copy(alpha = 0.6f) else MafiaBorder
                                    ),
                                    modifier = Modifier.testTag("structured_behavior_$behavior")
                                ) {
                                    Text(
                                        text = "+ $behavior",
                                        fontSize = 11.sp,
                                        color = if (cat == "SUSPICIOUS") MafiaCrimsonLight else TextPrimaryDark,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = noteText,
                            onValueChange = { noteText = it },
                            placeholder = {
                                Text(
                                    text = "متن مشاهده یا رفتار بازیکن را بنویسید...",
                                    color = TextMutedDark,
                                    fontSize = 13.sp
                                )
                            },
                            minLines = 2,
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MafiaDarkBg,
                                unfocusedContainerColor = MafiaDarkBg,
                                focusedBorderColor = MafiaCrimson,
                                unfocusedBorderColor = MafiaBorder,
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("note_input_field")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (noteText.isNotBlank()) {
                                    onAddNote(selectedPlayer.id, selectedCategory, noteText)
                                    noteText = ""
                                }
                            },
                            enabled = noteText.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = MafiaCrimson),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("btn_submit_note")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "ثبت این یادداشت", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Notes Search & Filter Header
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MafiaBorder)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (showAllPlayersNotes) "🔍 جستجو در تمام یادداشت‌های بازی:"
                                else "🔍 سوابق «${selectedPlayer.name}» (${filteredNotes.size} مورد):",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )

                            TextButton(onClick = { showAllPlayersNotes = !showAllPlayersNotes }) {
                                Text(
                                    text = if (showAllPlayersNotes) "فقط این بازیکن" else "نمایش همه بازیکنان",
                                    fontSize = 11.sp,
                                    color = MafiaGold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Search Bar
                        OutlinedTextField(
                            value = noteSearchQuery,
                            onValueChange = { noteSearchQuery = it },
                            placeholder = { Text("جستجو در متن یا نام بازیکن...", color = TextMutedDark, fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextMutedDark, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                if (noteSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { noteSearchQuery = "" }) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "پاک کردن", tint = TextMutedDark, modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MafiaDarkBg,
                                unfocusedContainerColor = MafiaDarkBg,
                                focusedBorderColor = MafiaCrimson,
                                unfocusedBorderColor = MafiaBorder,
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("notes_search_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Category filter chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                FilterChip(
                                    selected = noteCategoryFilter == "ALL",
                                    onClick = { noteCategoryFilter = "ALL" },
                                    label = { Text("همه", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MafiaCrimsonDark,
                                        selectedLabelColor = TextPrimaryDark,
                                        containerColor = MafiaSurfaceVariant,
                                        labelColor = TextSecondaryDark
                                    )
                                )
                            }
                            items(categories) { (catKey, catLabel, _) ->
                                FilterChip(
                                    selected = noteCategoryFilter == catKey,
                                    onClick = { noteCategoryFilter = catKey },
                                    label = { Text(catLabel, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MafiaCrimsonDark,
                                        selectedLabelColor = TextPrimaryDark,
                                        containerColor = MafiaSurfaceVariant,
                                        labelColor = TextSecondaryDark
                                    )
                                )
                            }
                        }
                    }
                }
            }

            if (filteredNotes.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (noteSearchQuery.isNotBlank() || noteCategoryFilter != "ALL") "یادداشتی با این فیلتر یا عبارت یافت نشد."
                            else "هنوز یادداشتی برای این بازیکن ثبت نشده است.",
                            fontSize = 13.sp,
                            color = TextMutedDark,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(filteredNotes, key = { it.id }) { note ->
                    val (catKey, catLabel, catColor) = categories.firstOrNull { it.first == note.category }
                        ?: Triple("FREE", "📝 یادداشت", Color.Gray)
                    val stgTitle = GameStage.getStage(note.stageIndex).title
                    val noteOwnerName = playerMap[note.playerId]?.name ?: ""

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
                        shape = RoundedCornerShape(12.dp),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MafiaBorder)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("note_item_${note.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = catColor.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = catLabel,
                                            fontSize = 11.sp,
                                            color = catColor,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stgTitle,
                                        fontSize = 11.sp,
                                        color = MafiaGold
                                    )
                                    if (showAllPlayersNotes && noteOwnerName.isNotBlank()) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "• $noteOwnerName",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimaryDark
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = note.text,
                                    fontSize = 14.sp,
                                    color = TextPrimaryDark,
                                    lineHeight = 22.sp
                                )
                            }

                            IconButton(
                                onClick = { onDeleteNote(note.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "حذف یادداشت",
                                    tint = TextMutedDark,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

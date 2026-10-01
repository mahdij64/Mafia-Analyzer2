package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.GameEntity
import com.example.data.local.PlayerEntity
import com.example.ui.components.PersianConfirmDialog
import com.example.ui.theme.*

@Composable
fun GameManagementScreen(
    activeGameId: Long?,
    allGames: List<GameEntity>,
    players: List<PlayerEntity>,
    onSelectGame: (Long) -> Unit,
    onCreateGame: (name: String, players: List<String>, citizenCount: Int, mafiaCount: Int, independentCount: Int, ownerPlayerIndex: Int?, ownerRole: String, mafiaTeammateIndices: Set<Int>) -> Unit,
    onRenameGame: (gameId: Long, newName: String) -> Unit,
    onDuplicateGame: (gameId: Long) -> Unit,
    onDeleteGame: (gameId: Long) -> Unit,
    onClearAllData: () -> Unit = {},
    onAddPlayer: (String) -> Unit,
    onTogglePlayerEliminated: (PlayerEntity) -> Unit,
    onDeletePlayer: (PlayerEntity) -> Unit,
    onRenamePlayer: (playerId: Long, newName: String) -> Unit = { _, _ -> },
    onMovePlayer: (player: PlayerEntity, directionUp: Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var showNewGameDialog by remember { mutableStateOf(false) }
    var gameToDelete by remember { mutableStateOf<GameEntity?>(null) }
    var gameToRename by remember { mutableStateOf<GameEntity?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    var playerToEditInActiveGame by remember { mutableStateOf<PlayerEntity?>(null) }
    var playerEditInput by remember { mutableStateOf("") }
    var newGameNameInput by remember { mutableStateOf("") }
    var renameGameInput by remember { mutableStateOf("") }
    var newPlayerNameInput by remember { mutableStateOf("") }

    // Dialog: Create New Game
    if (showNewGameDialog) {
        var gameTitle by remember { mutableStateOf("بازی جدید مافیا") }
        val currentPlayers = remember {
            mutableStateListOf("قاسم", "رضا", "مهدی", "مهدیار", "یسنا", "زهرا", "فاطی", "علی")
        }
        var newPlayerField by remember { mutableStateOf("") }
        var showBatchAddDialog by remember { mutableStateOf(false) }
        var batchAddText by remember { mutableStateOf("") }
        var editingPlayerIdx by remember { mutableStateOf<Int?>(null) }
        var editingPlayerName by remember { mutableStateOf("") }
        var errorMsg by remember { mutableStateOf<String?>(null) }
        var mafiaCount by remember { mutableIntStateOf(3) }
        var independentCount by remember { mutableIntStateOf(1) }
        var citizenCount by remember { mutableIntStateOf(4) }
        var ownerPlayerIndex by remember { mutableStateOf<Int?>(0) }
        var ownerRole by remember { mutableStateOf("CITIZEN") }
        var mafiaTeammateIndices by remember { mutableStateOf(setOf<Int>()) }

        val syncCounts: () -> Unit = {
            val total = currentPlayers.size
            if (total > 0) {
                if (mafiaCount >= total) mafiaCount = (total / 3).coerceAtLeast(1)
                citizenCount = (total - mafiaCount - independentCount).coerceAtLeast(1)
            }
        }

        val addSinglePlayer: () -> Unit = {
            val trimmed = newPlayerField.trim()
            if (trimmed.isBlank()) {
                errorMsg = "نام بازیکن را بنویسید"
            } else if (currentPlayers.contains(trimmed)) {
                errorMsg = "این نام قبلاً اضافه شده است"
            } else {
                currentPlayers.add(trimmed)
                newPlayerField = ""
                errorMsg = null
                syncCounts()
            }
        }

        val dialogScrollState = rememberScrollState()

        // Batch Paste Dialog
        if (showBatchAddDialog) {
            AlertDialog(
                onDismissRequest = { showBatchAddDialog = false },
                title = { Text("ورود دسته‌جمعی اسامی", color = MafiaGold, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            text = "اسامی بازیکنان را با کاما (،) یا با رفتن به خط جدید بنویسید یا پیست کنید:",
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = batchAddText,
                            onValueChange = { batchAddText = it },
                            placeholder = { Text("مثال:\nمهدی\nرضا\nنازنین\nعلی\nسارا") },
                            modifier = Modifier.fillMaxWidth().height(140.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val items = batchAddText
                                .split("\n", "،", ",")
                                .map { it.trim() }
                                .filter { it.isNotBlank() }
                            for (name in items) {
                                if (!currentPlayers.contains(name)) {
                                    currentPlayers.add(name)
                                }
                            }
                            syncCounts()
                            batchAddText = ""
                            showBatchAddDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MafiaCrimson)
                    ) {
                        Text("افزودن به بازی")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBatchAddDialog = false }) {
                        Text("انصراف")
                    }
                },
                containerColor = MafiaCardBg
            )
        }

        // Edit Player Name Dialog
        if (editingPlayerIdx != null) {
            AlertDialog(
                onDismissRequest = { editingPlayerIdx = null },
                title = { Text("ویرایش نام بازیکن", color = MafiaGold, fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = editingPlayerName,
                        onValueChange = { editingPlayerName = it },
                        label = { Text("نام بازیکن") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val idx = editingPlayerIdx
                            if (idx != null && editingPlayerName.isNotBlank()) {
                                currentPlayers[idx] = editingPlayerName.trim()
                            }
                            editingPlayerIdx = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MafiaCrimson)
                    ) {
                        Text("ذخیره")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingPlayerIdx = null }) {
                        Text("انصراف")
                    }
                },
                containerColor = MafiaCardBg
            )
        }

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            AlertDialog(
                onDismissRequest = { showNewGameDialog = false },
                containerColor = MafiaCardBg,
                title = {
                    Text(
                        text = "ایجاد بازی جدید و سناریو",
                        fontWeight = FontWeight.Bold,
                        color = MafiaGold,
                        fontSize = 16.sp
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 520.dp)
                            .verticalScroll(dialogScrollState)
                    ) {
                        // 1. GAME TITLE
                        OutlinedTextField(
                            value = gameTitle,
                            onValueChange = { gameTitle = it },
                            label = { Text("نام بازی") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark,
                                focusedContainerColor = MafiaDarkBg,
                                unfocusedContainerColor = MafiaDarkBg,
                                focusedBorderColor = MafiaGold,
                                unfocusedBorderColor = MafiaBorder,
                                cursorColor = MafiaGold
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("new_game_name_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 2. PLAYER NAMES SECTION (FIRST & PROMINENT)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MafiaSurfaceVariant,
                            border = BorderStroke(1.dp, MafiaBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "👥 بازیکنان بازی (${currentPlayers.size} نفر):",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MafiaGold
                                    )
                                    Row {
                                        TextButton(
                                            onClick = { showBatchAddDialog = true },
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                        ) {
                                            Text("📋 پیست دسته‌جمعی", fontSize = 10.sp, color = MafiaGold)
                                        }
                                        TextButton(
                                            onClick = {
                                                currentPlayers.clear()
                                                syncCounts()
                                            },
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                        ) {
                                            Text("پاک کردن", fontSize = 10.sp, color = MafiaCrimsonLight)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Input row to type player name
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = newPlayerField,
                                        onValueChange = {
                                            newPlayerField = it
                                            errorMsg = null
                                        },
                                        placeholder = { Text("نام بازیکن را بنویسید...", fontSize = 12.sp, color = TextSecondaryDark) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                        keyboardActions = KeyboardActions(onDone = { addSinglePlayer() }),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = TextPrimaryDark,
                                            unfocusedTextColor = TextPrimaryDark,
                                            focusedContainerColor = MafiaDarkBg,
                                            unfocusedContainerColor = MafiaDarkBg,
                                            focusedBorderColor = MafiaGold,
                                            unfocusedBorderColor = MafiaBorder,
                                            cursorColor = MafiaGold
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("dialog_new_player_field")
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = { addSinglePlayer() },
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AddCircle,
                                            contentDescription = "افزودن بازیکن",
                                            tint = MafiaGold,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }

                                if (errorMsg != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = errorMsg ?: "",
                                        color = MafiaCrimson,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (currentPlayers.isEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "هیچ بازیکنی ثبت نشده. نام بنویسید یا دکمه «نمونه ۸ نفره» زیر را بزنید:",
                                        fontSize = 11.sp,
                                        color = TextSecondaryDark
                                    )
                                    TextButton(
                                        onClick = {
                                            currentPlayers.clear()
                                            currentPlayers.addAll(listOf("قاسم", "رضا", "مهدی", "مهدیار", "یسنا", "زهرا", "فاطی", "علی"))
                                            syncCounts()
                                        }
                                    ) {
                                        Text("➕ بارگذاری ۸ بازیکن نمونه", fontSize = 11.sp, color = MafiaGold)
                                    }
                                } else {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        currentPlayers.forEachIndexed { idx, pName ->
                                            val isMe = ownerPlayerIndex == idx
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(MafiaDarkBg.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "${idx + 1}. $pName" + if (isMe) " (👑 من)" else "",
                                                    fontSize = 12.sp,
                                                    color = if (isMe) MafiaGold else TextPrimaryDark,
                                                    fontWeight = if (isMe) FontWeight.Bold else FontWeight.Normal,
                                                    modifier = Modifier.weight(1f)
                                                )

                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    // Edit button
                                                    IconButton(
                                                        onClick = {
                                                            editingPlayerIdx = idx
                                                            editingPlayerName = pName
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Edit,
                                                            contentDescription = "ویرایش نام",
                                                            tint = MafiaGold,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                    // Delete button
                                                    IconButton(
                                                        onClick = {
                                                            currentPlayers.removeAt(idx)
                                                            syncCounts()
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Close,
                                                            contentDescription = "حذف",
                                                            tint = MafiaCrimsonLight,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 3. SCENARIO COUNTS SECTION
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MafiaSurfaceVariant,
                            border = BorderStroke(1.dp, MafiaBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "⚙️ ترکیب سناریوی بازی:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MafiaGold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Citizen counter
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("🛡️ شهروند", fontSize = 10.sp, color = TextSecondaryDark)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { if (citizenCount > 1) citizenCount-- },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Text("-", fontSize = 16.sp, color = MafiaGold, fontWeight = FontWeight.Bold)
                                            }
                                            Text("$citizenCount", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            IconButton(
                                                onClick = { citizenCount++ },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Text("+", fontSize = 16.sp, color = MafiaGold, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    // Mafia counter
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("🗡️ مافیا", fontSize = 10.sp, color = TextSecondaryDark)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { if (mafiaCount > 1) mafiaCount-- },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Text("-", fontSize = 16.sp, color = MafiaCrimson, fontWeight = FontWeight.Bold)
                                            }
                                            Text("$mafiaCount", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            IconButton(
                                                onClick = { mafiaCount++ },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Text("+", fontSize = 16.sp, color = MafiaCrimson, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    // Independent counter
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("🎭 مستقل", fontSize = 10.sp, color = TextSecondaryDark)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { if (independentCount > 0) independentCount-- },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Text("-", fontSize = 16.sp, color = MafiaGold, fontWeight = FontWeight.Bold)
                                            }
                                            Text("$independentCount", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            IconButton(
                                                onClick = { independentCount++ },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Text("+", fontSize = 16.sp, color = MafiaGold, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 4. OWNER IDENTITY SECTION
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF231626),
                            border = BorderStroke(1.dp, MafiaGold.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "👑 تعیین هویت شما (مالک):",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MafiaGold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "نقش شما چیست؟",
                                    fontSize = 10.sp,
                                    color = TextSecondaryDark
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val roles = listOf(
                                        Triple("CITIZEN", "🛡️ شهروند", Color(0xFF1976D2)),
                                        Triple("MAFIA", "🗡️ مافیا", MafiaCrimson),
                                        Triple("INDEPENDENT", "🎭 مستقل", Color(0xFFFFA000))
                                    )
                                    roles.forEach { (roleKey, roleLabel, color) ->
                                        val isSelected = ownerRole == roleKey
                                        Surface(
                                            onClick = { ownerRole = roleKey },
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isSelected) color else MafiaSurfaceVariant,
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(30.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = roleLabel,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) Color.White else TextSecondaryDark
                                                )
                                            }
                                        }
                                    }
                                }

                                if (currentPlayers.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "کدام بازیکن شما هستید؟",
                                        fontSize = 10.sp,
                                        color = TextSecondaryDark
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    androidx.compose.foundation.lazy.LazyRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        itemsIndexed(currentPlayers) { idx, pName ->
                                            val isMe = ownerPlayerIndex == idx
                                            Surface(
                                                onClick = { ownerPlayerIndex = idx },
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (isMe) MafiaGold else MafiaSurfaceVariant,
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 6.dp)
                                                ) {
                                                    Text(
                                                        text = "${idx + 1}. $pName" + if (isMe) " (👑 من)" else "",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isMe) Color.Black else TextPrimaryDark
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                if (ownerRole == "MAFIA" && currentPlayers.size > 1) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "🗡️ همکاران و یاران مافیای شما کدامند؟ (برای راهنمایی بهتر در نطق):",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFF8A80)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    androidx.compose.foundation.lazy.LazyRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        itemsIndexed(currentPlayers) { idx, pName ->
                                            if (ownerPlayerIndex != idx) {
                                                val isTeammate = mafiaTeammateIndices.contains(idx)
                                                Surface(
                                                    onClick = {
                                                        val newSet = mafiaTeammateIndices.toMutableSet()
                                                        if (isTeammate) newSet.remove(idx) else newSet.add(idx)
                                                        mafiaTeammateIndices = newSet
                                                    },
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (isTeammate) MafiaCrimson else MafiaSurfaceVariant,
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(horizontal = 6.dp)
                                                    ) {
                                                        Text(
                                                            text = if (isTeammate) "🗡️ ${idx + 1}. $pName" else "${idx + 1}. $pName",
                                                            fontSize = 10.sp,
                                                            fontWeight = if (isTeammate) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (isTeammate) Color.White else TextPrimaryDark
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (currentPlayers.isEmpty()) {
                                errorMsg = "حداقل باید یک بازیکن اضافه کنید"
                            } else {
                                onCreateGame(
                                    gameTitle,
                                    currentPlayers.toList(),
                                    citizenCount,
                                    mafiaCount,
                                    independentCount,
                                    ownerPlayerIndex,
                                    ownerRole,
                                    mafiaTeammateIndices
                                )
                                showNewGameDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MafiaCrimson),
                        modifier = Modifier.testTag("btn_confirm_create_game")
                    ) {
                        Text("شروع بازی")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNewGameDialog = false }) {
                        Text("انصراف", color = TextMutedDark)
                    }
                },
                containerColor = MafiaCardBg
            )
        }
    }

    // Dialog: Delete Game
    if (gameToDelete != null) {
        PersianConfirmDialog(
            title = "حذف بازی",
            message = "آیا مطمئن هستید که می‌خواهید بازی «${gameToDelete?.name}» و تمام اطلاعات آن را برای همیشه به طور کامل حذف کنید؟",
            confirmText = "حذف کامل با تمام دیتا",
            isDestructive = true,
            onConfirm = {
                gameToDelete?.let { onDeleteGame(it.id) }
                gameToDelete = null
            },
            onDismiss = { gameToDelete = null }
        )
    }

    // Dialog: Clear All System Data
    if (showClearAllDialog) {
        PersianConfirmDialog(
            title = "پاک‌سازی کامل سیستم",
            message = "آیا مطمئن هستید که می‌خواهید بازی پیش‌فرض و تمام بازی‌ها و کلیه داده‌های ثبت‌شده را به طور کامل پاک کنید؟ این عملیات غیرقابل بازگشت است.",
            confirmText = "پاک‌سازی کامل همه چیز 🗑️",
            isDestructive = true,
            onConfirm = {
                onClearAllData()
                showClearAllDialog = false
            },
            onDismiss = { showClearAllDialog = false }
        )
    }

    // Dialog: Rename Game
    if (gameToRename != null) {
        AlertDialog(
            onDismissRequest = { gameToRename = null },
            title = { Text("تغییر نام بازی", fontWeight = FontWeight.Bold, color = TextPrimaryDark) },
            text = {
                OutlinedTextField(
                    value = renameGameInput,
                    onValueChange = { renameGameInput = it },
                    label = { Text("نام جدید") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        gameToRename?.let { onRenameGame(it.id, renameGameInput) }
                        gameToRename = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MafiaCrimson)
                ) {
                    Text("ذخیره")
                }
            },
            dismissButton = {
                TextButton(onClick = { gameToRename = null }) {
                    Text("انصراف", color = TextMutedDark)
                }
            },
            containerColor = MafiaCardBg
        )
    }

    // Dialog: Edit Player in Active Game
    if (playerToEditInActiveGame != null) {
        AlertDialog(
            onDismissRequest = { playerToEditInActiveGame = null },
            title = { Text("ویرایش نام بازیکن", fontWeight = FontWeight.Bold, color = TextPrimaryDark) },
            text = {
                OutlinedTextField(
                    value = playerEditInput,
                    onValueChange = { playerEditInput = it },
                    label = { Text("نام بازیکن") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val p = playerToEditInActiveGame
                        if (p != null && playerEditInput.isNotBlank()) {
                            onRenamePlayer(p.id, playerEditInput.trim())
                        }
                        playerToEditInActiveGame = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MafiaCrimson)
                ) {
                    Text("ذخیره")
                }
            },
            dismissButton = {
                TextButton(onClick = { playerToEditInActiveGame = null }) {
                    Text("انصراف", color = TextMutedDark)
                }
            },
            containerColor = MafiaCardBg
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MafiaDarkBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Create New Game Action Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MafiaBorder)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "بازی جدید مافیا",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "تعریف بازی با تعداد دلخواه بازیکن بدون نیاز به نقش",
                            fontSize = 12.sp,
                            color = TextSecondaryDark
                        )
                    }

                    Button(
                        onClick = { showNewGameDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MafiaCrimson),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_open_new_game_dialog")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("بازی جدید")
                    }
                }
            }
        }

        // Active Game Players Management
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MafiaBorder)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "مدیریت بازیکنان بازی جاری (${players.size} نفر):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MafiaGold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Add player to active game
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newPlayerNameInput,
                            onValueChange = { newPlayerNameInput = it },
                            placeholder = { Text("افزودن بازیکن جدید به این بازی...", fontSize = 12.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MafiaDarkBg,
                                unfocusedContainerColor = MafiaDarkBg,
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("active_game_add_player_input")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newPlayerNameInput.isNotBlank()) {
                                    onAddPlayer(newPlayerNameInput)
                                    newPlayerNameInput = ""
                                }
                            },
                            enabled = newPlayerNameInput.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = MafiaCrimson),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_add_player_to_active")
                        ) {
                            Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "افزودن")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    players.forEachIndexed { idx, p ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(MafiaSurfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                IconButton(
                                    onClick = { onTogglePlayerEliminated(p) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Text(text = if (p.isEliminated) "💀" else "🟢", fontSize = 16.sp)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${idx + 1}. " + p.name + if (p.isEliminated) " (خارج شده)" else "",
                                    fontSize = 14.sp,
                                    fontWeight = if (p.isEliminated) FontWeight.Normal else FontWeight.Bold,
                                    color = if (p.isEliminated) TextMutedDark else TextPrimaryDark
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Move up
                                IconButton(
                                    onClick = { onMovePlayer(p, true) },
                                    enabled = idx > 0,
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = "انتقال به بالا",
                                        tint = if (idx > 0) TextSecondaryDark else TextMutedDark.copy(alpha = 0.3f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Move down
                                IconButton(
                                    onClick = { onMovePlayer(p, false) },
                                    enabled = idx < players.size - 1,
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "انتقال به پایین",
                                        tint = if (idx < players.size - 1) TextSecondaryDark else TextMutedDark.copy(alpha = 0.3f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Edit name
                                IconButton(
                                    onClick = {
                                        playerToEditInActiveGame = p
                                        playerEditInput = p.name
                                    },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "ویرایش نام بازیکن",
                                        tint = MafiaGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Delete
                                IconButton(
                                    onClick = { onDeletePlayer(p) },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "حذف بازیکن",
                                        tint = TextMutedDark,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Saved Games List
        item {
            Text(
                text = "بازی‌های ذخیره‌شده روی دستگاه (${allGames.size}):",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextPrimaryDark
            )
        }

        items(allGames, key = { it.id }) { game ->
            val isActive = game.id == activeGameId

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isActive) MafiaSurfaceVariant else MafiaCardBg
                ),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        if (isActive) MafiaCrimson else MafiaBorder
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("saved_game_${game.id}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectGame(game.id) }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = game.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextPrimaryDark
                            )
                            if (isActive) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = MafiaCrimson.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "در حال اجرا",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MafiaCrimsonLight,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = "مرحله فعال: روز ${game.currentStageIndex}",
                            fontSize = 12.sp,
                            color = TextSecondaryDark
                        )
                    }

                    Row {
                        IconButton(
                            onClick = {
                                gameToRename = game
                                renameGameInput = game.name
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "تغییر نام", tint = TextSecondaryDark)
                        }

                        IconButton(onClick = { onDuplicateGame(game.id) }) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "کپی بازی", tint = TextSecondaryDark)
                        }

                        IconButton(onClick = { gameToDelete = game }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = MafiaCrimsonLight)
                        }
                    }
                }
            }
        }

        // Action to completely delete default game & reset system
        item {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = { showClearAllDialog = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MafiaCrimson),
                border = BorderStroke(1.dp, MafiaCrimson.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_clear_all_data")
            ) {
                Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, tint = MafiaCrimson)
                Spacer(modifier = Modifier.width(8.dp))
                Text("🗑️ پاک‌سازی کامل بازی پیش‌فرض و تمام داده‌های سیستم", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

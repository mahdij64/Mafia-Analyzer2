package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.AiChatService
import com.example.data.ai.AiModel
import com.example.data.ai.AiSettingsManager
import com.example.data.ai.ChatMessage
import com.example.data.ai.ChatResult
import com.example.ui.theme.*
import kotlinx.coroutines.launch

data class ChatBubble(
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    gameContextProvider: () -> String = { "" },
    activeGameName: String? = null
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        // Settings from SharedPreferences
        var apiUrl by remember { mutableStateOf(AiSettingsManager.apiUrl) }
        var apiKey by remember { mutableStateOf(AiSettingsManager.apiKey) }
        var showApiKey by remember { mutableStateOf(false) }
        var selectedModel by remember { mutableStateOf(AiSettingsManager.selectedModel) }
        var systemPrompt by remember { mutableStateOf(AiSettingsManager.systemPrompt) }

        // Persist on change
        LaunchedEffect(apiUrl) { AiSettingsManager.apiUrl = apiUrl }
        LaunchedEffect(apiKey) { AiSettingsManager.apiKey = apiKey }
        LaunchedEffect(selectedModel) { AiSettingsManager.selectedModel = selectedModel }
        LaunchedEffect(systemPrompt) { AiSettingsManager.systemPrompt = systemPrompt }

        // Models
        var models by remember { mutableStateOf<List<AiModel>>(emptyList()) }
        var isLoadingModels by remember { mutableStateOf(false) }
        var modelsError by remember { mutableStateOf<String?>(null) }
        var showModelDropdown by remember { mutableStateOf(false) }

        // Chat state
        val chatMessages = remember { mutableStateListOf<ChatBubble>() }
        var inputText by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }
        var chatError by remember { mutableStateOf<String?>(null) }

        // Settings panel
        var showSettings by remember { mutableStateOf(apiKey.isBlank()) }

        val scope = rememberCoroutineScope()
        val listState = rememberLazyListState()
        val clipboardManager = LocalClipboardManager.current

        // Auto-scroll
        LaunchedEffect(chatMessages.size) {
            if (chatMessages.isNotEmpty()) {
                listState.animateScrollToItem(chatMessages.size - 1)
            }
        }

        // Auto-load models on first open if key exists
        LaunchedEffect(apiKey) {
            if (apiKey.isNotBlank() && models.isEmpty()) {
                isLoadingModels = true
                val result = AiChatService.fetchModels(apiUrl, apiKey)
                result.onSuccess { fetched ->
                    models = fetched
                    if (fetched.isNotEmpty() && selectedModel.isBlank()) {
                        selectedModel = fetched.first().id
                    }
                }.onFailure { e -> modelsError = e.message }
                isLoadingModels = false
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MafiaDarkBg)
        ) {
            // ─── Top bar ───
            Surface(color = MafiaCardBg, shadowElevation = 4.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.SmartToy, null, tint = Color(0xFFB388FF), modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("دستیار هوش مصنوعی", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimaryDark)
                        Text(
                            text = if (selectedModel.isNotBlank()) selectedModel else "مدل انتخاب نشده",
                            fontSize = 11.sp, color = TextMutedDark, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                    // Model dropdown
                    Box {
                        OutlinedButton(
                            onClick = { showModelDropdown = !showModelDropdown },
                            border = androidx.compose.foundation.BorderStroke(1.dp, MafiaBorder),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = if (models.isEmpty()) "مدل‌ها" else "${models.size} مدل",
                                fontSize = 11.sp, color = TextPrimaryDark
                            )
                            Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(16.dp), tint = TextMutedDark)
                        }
                        DropdownMenu(
                            expanded = showModelDropdown,
                            onDismissRequest = { showModelDropdown = false },
                            containerColor = MafiaCardBg,
                            modifier = Modifier.widthIn(max = 320.dp)
                        ) {
                            if (models.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("ابتدا API Key را تنظیم کنید", fontSize = 12.sp, color = TextMutedDark) },
                                    onClick = { showModelDropdown = false; showSettings = true }
                                )
                            } else {
                                models.forEach { model ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                model.id, fontSize = 12.sp,
                                                color = if (model.id == selectedModel) MafiaGold else TextPrimaryDark,
                                                fontWeight = if (model.id == selectedModel) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = { selectedModel = model.id; showModelDropdown = false },
                                        trailingIcon = {
                                            if (model.id == selectedModel)
                                                Icon(Icons.Default.Check, null, tint = MafiaGold, modifier = Modifier.size(16.dp))
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = { showSettings = !showSettings }) {
                        Icon(Icons.Default.Settings, "تنظیمات", tint = if (showSettings) MafiaGold else TextMutedDark)
                    }
                }
            }

            // ─── Settings panel ───
            AnimatedVisibility(visible = showSettings) {
                Surface(color = MafiaCardBg.copy(alpha = 0.95f), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("⚙️ تنظیمات API", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MafiaGold)

                        OutlinedTextField(
                            value = apiUrl,
                            onValueChange = { apiUrl = it },
                            label = { Text("آدرس API", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = aiFieldColors(),
                            shape = RoundedCornerShape(8.dp)
                        )

                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            label = { Text("API Key", fontSize = 12.sp) },
                            singleLine = true,
                            visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showApiKey = !showApiKey }) {
                                    Icon(
                                        if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        null, tint = TextMutedDark, modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = aiFieldColors(),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    if (apiKey.isBlank()) { modelsError = "لطفاً API Key را وارد کنید"; return@Button }
                                    isLoadingModels = true; modelsError = null
                                    scope.launch {
                                        val result = AiChatService.fetchModels(apiUrl, apiKey)
                                        result.onSuccess { fetched ->
                                            models = fetched
                                            if (fetched.isNotEmpty() && selectedModel.isBlank()) selectedModel = fetched.first().id
                                            if (fetched.isEmpty()) modelsError = "هیچ مدلی یافت نشد"
                                        }.onFailure { e -> modelsError = e.message }
                                        isLoadingModels = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MafiaGold, contentColor = Color.Black),
                                enabled = !isLoadingModels,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isLoadingModels) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.Black)
                                else Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("بارگذاری مدل‌ها", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { chatMessages.clear(); chatError = null },
                                border = androidx.compose.foundation.BorderStroke(1.dp, MafiaBorder),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.DeleteSweep, null, modifier = Modifier.size(16.dp), tint = MafiaCrimsonLight)
                                Spacer(Modifier.width(4.dp))
                                Text("پاک کردن چت", fontSize = 12.sp, color = MafiaCrimsonLight)
                            }
                        }

                        if (modelsError != null) Text("❌ ${modelsError}", fontSize = 11.sp, color = MafiaCrimsonLight)

                        // System prompt
                        Text("🧠 System Prompt", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextSecondaryDark, modifier = Modifier.padding(top = 4.dp))
                        OutlinedTextField(
                            value = systemPrompt,
                            onValueChange = { systemPrompt = it },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                            minLines = 3, maxLines = 6,
                            colors = aiFieldColors(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        TextButton(
                            onClick = { systemPrompt = AiSettingsManager.DEFAULT_SYSTEM_PROMPT },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(14.dp), tint = MafiaGold)
                            Spacer(Modifier.width(4.dp))
                            Text("بازنشانی پرامپت پیش‌فرض", fontSize = 11.sp, color = MafiaGold)
                        }
                    }
                }
            }

            // ─── Quick actions ───
            Surface(color = MafiaSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    QuickActionChip(
                        label = "📊 تحلیل بازی",
                        onClick = {
                            val ctx = gameContextProvider()
                            if (ctx.isBlank()) {
                                chatError = "ابتدا یک بازی فعال ایجاد کنید"
                                return@QuickActionChip
                            }
                            val prompt = "لطفاً بازی فعلی من را تحلیل کن. این داده‌های بازی من هست:\n\n$ctx\n\nلطفاً یک تحلیل جامع ارائه بده شامل:\n1. بازیکنان مشکوک و دلایل آن\n2. الگوهای تارگت‌زنی\n3. پیشنهاد استراتژی برای من\n4. نکات مهم و هشدارها"
                            inputText = prompt
                        }
                    )
                    QuickActionChip(
                        label = "🎯 کی رو تارگت کنم؟",
                        onClick = {
                            val ctx = gameContextProvider()
                            if (ctx.isBlank()) { chatError = "ابتدا یک بازی فعال ایجاد کنید"; return@QuickActionChip }
                            inputText = "بر اساس این داده‌ها:\n\n$ctx\n\nبه نظر شما الان بهترین تارگت من کیست و چرا؟"
                        }
                    )
                    QuickActionChip(
                        label = "🛡️ از کی دفاع کنم؟",
                        onClick = {
                            val ctx = gameContextProvider()
                            if (ctx.isBlank()) { chatError = "ابتدا یک بازی فعال ایجاد کنید"; return@QuickActionChip }
                            inputText = "بر اساس این داده‌ها:\n\n$ctx\n\nچه بازیکنانی احتمالاً بی‌گناه هستند و باید از آنها دفاع کنم؟"
                        }
                    )
                }
            }

            // ─── Chat messages ───
            if (chatMessages.isEmpty() && !isLoading) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SmartToy, null, tint = Color(0xFFB388FF), modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("دستیار هوش مصنوعی", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimaryDark)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "سوالت رو بپرس یا از دکمه‌های بالا استفاده کن\nAI به تمام داده‌های بازی دسترسی دارد",
                            fontSize = 13.sp, color = TextMutedDark, textAlign = TextAlign.Center
                        )
                        if (apiKey.isBlank()) {
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { showSettings = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MafiaGold, contentColor = Color.Black)
                            ) {
                                Icon(Icons.Default.Settings, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("تنظیم API Key", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(chatMessages) { bubble ->
                        ChatBubbleRow(bubble = bubble, onCopy = {
                            clipboardManager.setText(AnnotatedString(bubble.content))
                        })
                    }
                    if (isLoading) {
                        item {
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(28.dp).clip(CircleShape).background(Color(0xFFB388FF).copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color(0xFFB388FF))
                                }
                                Spacer(Modifier.width(8.dp))
                                Text("در حال فکر کردن...", fontSize = 12.sp, color = TextMutedDark)
                            }
                        }
                    }
                }
            }

            // ─── Error ───
            if (chatError != null) {
                Surface(
                    color = MafiaCrimson.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = MafiaCrimsonLight, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(chatError!!, fontSize = 11.sp, color = MafiaCrimsonLight, modifier = Modifier.weight(1f))
                        IconButton(onClick = { chatError = null }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, null, tint = MafiaCrimsonLight, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            // ─── Input bar ───
            Surface(color = MafiaCardBg, shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("پیام خود را بنویسید...", fontSize = 13.sp, color = TextMutedDark) },
                        modifier = Modifier.weight(1f),
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFB388FF),
                            unfocusedBorderColor = MafiaBorder,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark,
                            focusedContainerColor = MafiaSurfaceVariant,
                            unfocusedContainerColor = MafiaSurfaceVariant
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (inputText.isNotBlank() && !isLoading) {
                                sendMessage(inputText, chatMessages, apiUrl, apiKey, selectedModel, systemPrompt, scope,
                                    { isLoading = true; chatError = null }, { isLoading = false }, { chatError = it }, { inputText = "" })
                            }
                        })
                    )
                    Spacer(Modifier.width(6.dp))
                    FilledIconButton(
                        onClick = {
                            if (inputText.isNotBlank() && !isLoading) {
                                sendMessage(inputText, chatMessages, apiUrl, apiKey, selectedModel, systemPrompt, scope,
                                    { isLoading = true; chatError = null }, { isLoading = false }, { chatError = it }, { inputText = "" })
                            }
                        },
                        enabled = inputText.isNotBlank() && !isLoading && apiKey.isNotBlank() && selectedModel.isNotBlank(),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color(0xFF7E57C2), contentColor = Color.White,
                            disabledContainerColor = MafiaSurfaceVariant, disabledContentColor = TextMutedDark
                        ),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, "ارسال", modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}

private fun sendMessage(
    inputText: String,
    chatMessages: MutableList<ChatBubble>,
    apiUrl: String, apiKey: String, selectedModel: String, systemPrompt: String,
    scope: kotlinx.coroutines.CoroutineScope,
    onLoadStart: () -> Unit, onLoadEnd: () -> Unit, onError: (String) -> Unit, onResetInput: () -> Unit
) {
    if (apiKey.isBlank()) { onError("لطفاً ابتدا API Key را تنظیم کنید"); return }
    if (selectedModel.isBlank()) { onError("لطفاً یک مدل انتخاب کنید"); return }

    chatMessages.add(ChatBubble(role = "user", content = inputText.trim()))
    onResetInput()
    onLoadStart()

    val apiMessages = buildList {
        if (systemPrompt.isNotBlank()) add(ChatMessage(role = "system", content = systemPrompt))
        chatMessages.forEach { add(ChatMessage(role = it.role, content = it.content)) }
    }

    scope.launch {
        when (val result = AiChatService.chat(apiUrl, apiKey, selectedModel, apiMessages)) {
            is ChatResult.Success -> chatMessages.add(ChatBubble(role = "assistant", content = result.content))
            is ChatResult.Error -> onError(result.message)
            is ChatResult.Loading -> {}
        }
        onLoadEnd()
    }
}

@Composable
private fun QuickActionChip(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = MafiaCardBg,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MafiaBorder),
        modifier = Modifier.height(30.dp)
    ) {
        Text(
            text = label, fontSize = 11.sp, color = TextPrimaryDark,
            modifier = Modifier.padding(horizontal = 10.dp).wrapContentHeight(Alignment.CenterVertically)
        )
    }
}

@Composable
private fun ChatBubbleRow(bubble: ChatBubble, onCopy: () -> Unit) {
    val isUser = bubble.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.Start else Arrangement.End
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier.size(28.dp).clip(CircleShape).background(Color(0xFFB388FF).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.SmartToy, null, tint = Color(0xFFB388FF), modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(6.dp))
        }
        Column(
            modifier = Modifier.widthIn(max = 300.dp),
            horizontalAlignment = if (isUser) Alignment.Start else Alignment.End
        ) {
            Surface(
                color = if (isUser) Color(0xFF1565C0).copy(alpha = 0.8f) else MafiaSurfaceVariant,
                shape = RoundedCornerShape(
                    topStart = 12.dp, topEnd = 12.dp,
                    bottomStart = if (isUser) 12.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 12.dp
                )
            ) {
                Text(
                    text = bubble.content, fontSize = 13.sp, lineHeight = 20.sp, color = TextPrimaryDark,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
            if (!isUser) {
                TextButton(
                    onClick = onCopy,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(12.dp), tint = TextMutedDark)
                    Spacer(Modifier.width(2.dp))
                    Text("کپی", fontSize = 10.sp, color = TextMutedDark)
                }
            }
        }
        if (isUser) {
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier.size(28.dp).clip(CircleShape).background(MafiaGold.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, tint = MafiaGold, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun aiFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MafiaGold, unfocusedBorderColor = MafiaBorder,
    focusedTextColor = TextPrimaryDark, unfocusedTextColor = TextPrimaryDark,
    focusedLabelColor = MafiaGold, unfocusedLabelColor = TextMutedDark
)

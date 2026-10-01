package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.AiModelInfo
import com.example.data.local.AiSettingsEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.AiModelsFetchState
import com.example.ui.viewmodel.TestChatMessage

private val AiPurple = Color(0xFFB388FF)
private val AiPurpleDark = Color(0xFF7E57C2)

/** Field style that keeps URLs / API keys / model ids in LTR even inside the RTL app. */
private val LtrFieldStyle = TextStyle(
    textDirection = TextDirection.Ltr,
    textAlign = TextAlign.Left,
    fontSize = 14.sp
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsScreen(
    settings: AiSettingsEntity,
    models: List<AiModelInfo>,
    modelsFetchState: AiModelsFetchState,
    testChatMessages: List<TestChatMessage>,
    testChatLoading: Boolean,
    onSaveSettings: (AiSettingsEntity) -> Unit,
    onFetchModels: (String, String) -> Unit,
    onSendTestChat: (String) -> Unit,
    onClearTestChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    var urlDraft by remember(settings) { mutableStateOf(settings.baseUrl) }
    var keyDraft by remember(settings) { mutableStateOf(settings.apiKey) }
    var modelDraft by remember(settings) { mutableStateOf(settings.model) }
    var temperatureDraft by remember(settings) { mutableStateOf(settings.temperature) }
    var keyVisible by remember { mutableStateOf(false) }
    var modelMenuExpanded by remember { mutableStateOf(false) }
    var chatDraft by remember { mutableStateOf("") }

    val isFetching = modelsFetchState is AiModelsFetchState.Loading

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MafiaDarkBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 14.dp)
    ) {
        // ---------- Header / status ----------
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder()
                    .copy(brush = androidx.compose.ui.graphics.SolidColor(MafiaBorder)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🤖 تنظیمات هوش مصنوعی",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = AiPurple
                            )
                            Text(
                                text = "اتصال به سرویس سازگار با OpenAI (مثل 9router) برای تحلیل بازی و تست چت",
                                fontSize = 12.sp,
                                color = TextSecondaryDark
                            )
                        }
                        StatusChip(isConfigured = settings.isRouterConfigured)
                    }
                    if (settings.isRouterConfigured) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "سرویس فعال: ${settings.baseUrl}",
                            style = LtrFieldStyle.copy(fontSize = 11.sp, color = TextMutedDark),
                            maxLines = 2
                        )
                        Text(
                            text = "مدل فعال: ${settings.model}",
                            style = LtrFieldStyle.copy(fontSize = 11.sp, color = TextMutedDark),
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // ---------- Connection card ----------
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder()
                    .copy(brush = androidx.compose.ui.graphics.SolidColor(MafiaBorder)),
                modifier = Modifier.fillMaxWidth().testTag("ai_connection_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🔌 اتصال به سرویس",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MafiaGold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "آدرس سرویس از قبل با 9router پر شده است؛ فقط کلید API خود را وارد کنید، لیست مدل‌ها را بگیرید و یکی را انتخاب کنید.",
                        fontSize = 12.sp,
                        color = TextSecondaryDark,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Base URL
                    OutlinedTextField(
                        value = urlDraft,
                        onValueChange = { urlDraft = it },
                        label = { Text("آدرس سرویس (Base URL)") },
                        placeholder = { Text("https://…/v1", style = LtrFieldStyle) },
                        leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, tint = AiPurple) },
                        singleLine = true,
                        textStyle = LtrFieldStyle,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Next
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("ai_base_url_field")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // API Key
                    OutlinedTextField(
                        value = keyDraft,
                        onValueChange = { keyDraft = it },
                        label = { Text("کلید API") },
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = AiPurple) },
                        trailingIcon = {
                            IconButton(onClick = { keyVisible = !keyVisible }) {
                                Icon(
                                    imageVector = if (keyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (keyVisible) "پنهان کردن کلید" else "نمایش کلید",
                                    tint = TextSecondaryDark
                                )
                            }
                        },
                        singleLine = true,
                        textStyle = LtrFieldStyle,
                        visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("ai_api_key_field")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Fetch models row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onFetchModels(urlDraft.trim(), keyDraft.trim()) },
                            enabled = !isFetching && urlDraft.isNotBlank() && keyDraft.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = AiPurpleDark),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(44.dp).testTag("btn_fetch_models")
                        ) {
                            if (isFetching) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("در حال دریافت…", fontSize = 13.sp)
                            } else {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("دریافت لیست مدل‌ها", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = { urlDraft = AiSettingsEntity.DEFAULT_BASE_URL },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text("آدرس پیش‌فرض", fontSize = 12.sp, color = TextSecondaryDark)
                        }
                    }

                    // Fetch state message
                    Spacer(modifier = Modifier.height(8.dp))
                    when (val state = modelsFetchState) {
                        is AiModelsFetchState.Success -> Text(
                            text = "✔ ${state.count} مدل از سرویس دریافت شد — از لیست زیر انتخاب کنید",
                            fontSize = 12.sp,
                            color = SuspicionGreen,
                            modifier = Modifier.testTag("ai_models_success_msg")
                        )
                        is AiModelsFetchState.Error -> Text(
                            text = "⚠ ${state.message}",
                            fontSize = 12.sp,
                            color = MafiaCrimsonLight,
                            lineHeight = 18.sp,
                            modifier = Modifier.testTag("ai_models_error_msg")
                        )
                        else -> Text(
                            text = "برای دیدن مدل‌های موجود، دکمه «دریافت لیست مدل‌ها» را بزنید.",
                            fontSize = 12.sp,
                            color = TextMutedDark
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Model combo (editable dropdown)
                    ExposedDropdownMenuBox(
                        expanded = modelMenuExpanded,
                        onExpandedChange = { modelMenuExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = modelDraft,
                            onValueChange = { modelDraft = it },
                            label = { Text("مدل هوش مصنوعی") },
                            placeholder = { Text("مثلاً gpt-4o-mini", style = LtrFieldStyle) },
                            leadingIcon = { Icon(Icons.Default.SmartToy, contentDescription = null, tint = AiPurple) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelMenuExpanded)
                            },
                            singleLine = true,
                            textStyle = LtrFieldStyle,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Done
                            ),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AiPurple,
                                unfocusedBorderColor = MafiaBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryEditable, true)
                                .testTag("ai_model_combo")
                        )
                        ExposedDropdownMenu(
                            expanded = modelMenuExpanded,
                            onDismissRequest = { modelMenuExpanded = false }
                        ) {
                            if (models.isEmpty()) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "هنوز مدلی دریافت نشده؛ دکمه «دریافت لیست مدل‌ها» را بزنید یا نام مدل را دستی تایپ کنید",
                                            fontSize = 12.sp,
                                            color = TextMutedDark
                                        )
                                    },
                                    onClick = {},
                                    enabled = false
                                )
                            } else {
                                models.forEach { m ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = m.id,
                                                    style = LtrFieldStyle.copy(
                                                        color = if (m.id == modelDraft) AiPurple else TextPrimaryDark,
                                                        fontWeight = if (m.id == modelDraft) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                )
                                                if (m.ownedBy.isNotBlank()) {
                                                    Text(
                                                        text = m.ownedBy,
                                                        style = LtrFieldStyle.copy(fontSize = 10.sp, color = TextMutedDark)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            modelDraft = m.id
                                            modelMenuExpanded = false
                                        },
                                        modifier = Modifier.testTag("ai_model_option_${m.id}")
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Temperature
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("خلاقیت پاسخ (Temperature)", fontSize = 13.sp, color = TextPrimaryDark)
                        Text(
                            text = String.format(java.util.Locale.US, "%.2f", temperatureDraft),
                            fontSize = 13.sp,
                            color = AiPurple,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = temperatureDraft,
                        onValueChange = { temperatureDraft = it },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = AiPurple,
                            activeTrackColor = AiPurpleDark,
                            inactiveTrackColor = MafiaSurfaceVariant
                        ),
                        modifier = Modifier.testTag("ai_temperature_slider")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Save
                    Button(
                        onClick = {
                            onSaveSettings(
                                settings.copy(
                                    baseUrl = urlDraft.trim(),
                                    apiKey = keyDraft.trim(),
                                    model = modelDraft.trim(),
                                    temperature = temperatureDraft
                                )
                            )
                        },
                        enabled = urlDraft.isNotBlank() && keyDraft.isNotBlank() && modelDraft.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = MafiaCrimson),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("btn_save_ai_settings")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ذخیره تنظیمات هوش مصنوعی", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ---------- Test chat card ----------
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder()
                    .copy(brush = androidx.compose.ui.graphics.SolidColor(MafiaBorder)),
                modifier = Modifier.fillMaxWidth().testTag("ai_test_chat_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "💬 تست چت",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MafiaGold
                            )
                            Text(
                                text = "اتصال و مدل انتخابی را همین‌جا با یک گفتگوی سریع آزمایش کنید",
                                fontSize = 12.sp,
                                color = TextSecondaryDark
                            )
                        }
                        if (testChatMessages.isNotEmpty()) {
                            IconButton(
                                onClick = onClearTestChat,
                                modifier = Modifier.testTag("btn_clear_test_chat")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "پاک کردن چت", tint = TextSecondaryDark)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val chatScrollState = rememberScrollState()
                    LaunchedEffect(testChatMessages.size, testChatLoading) {
                        chatScrollState.scrollTo(chatScrollState.maxValue)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 90.dp, max = 340.dp)
                            .verticalScroll(chatScrollState),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (testChatMessages.isEmpty() && !testChatLoading) {
                            Text(
                                text = "هنوز پیامی ارسال نشده. مثلاً بنویسید: «سلام! خودت رو کوتاه معرفی کن»",
                                fontSize = 12.sp,
                                color = TextMutedDark,
                                modifier = Modifier.padding(vertical = 18.dp)
                            )
                        }
                        testChatMessages.forEach { msg -> ChatBubble(msg) }
                        if (testChatLoading) {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                                Surface(
                                    color = MafiaSurfaceVariant,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            color = AiPurple,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("در حال تایپ…", fontSize = 12.sp, color = TextSecondaryDark)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Chat input row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = chatDraft,
                            onValueChange = { chatDraft = it },
                            placeholder = { Text("پیام تست خود را بنویسید…", fontSize = 13.sp) },
                            maxLines = 4,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (!testChatLoading && chatDraft.isNotBlank()) {
                                        onSendTestChat(chatDraft)
                                        chatDraft = ""
                                    }
                                }
                            ),
                            modifier = Modifier.weight(1f).testTag("test_chat_input")
                        )
                        Button(
                            onClick = {
                                if (chatDraft.isNotBlank()) {
                                    onSendTestChat(chatDraft)
                                    chatDraft = ""
                                }
                            },
                            enabled = !testChatLoading && chatDraft.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = AiPurpleDark),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.size(48.dp).testTag("btn_send_test_chat")
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "ارسال", tint = Color.White)
                        }
                    }
                }
            }
        }

        // ---------- Info card ----------
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder()
                    .copy(brush = androidx.compose.ui.graphics.SolidColor(MafiaBorder)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "💡 این تنظیمات کجا استفاده می‌شود؟",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimaryDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• دکمه «شروع تحلیل با هوش مصنوعی» در تب گزارش‌ها، داده‌های بازی را با همین مدل و سرویس تحلیل می‌کند.\n" +
                                "• اگر سرویس در دسترس نباشد، تحلیل هوشمند محلی (آفلاین) به صورت خودکار جایگزین می‌شود.\n" +
                                "• کلید API فقط روی حافظه دستگاه شما ذخیره می‌شود و تنها به همان آدرسی که وارد کرده‌اید ارسال می‌گردد.\n" +
                                "• Temperature پایین‌تر = پاسخ جدی‌تر و دقیق‌تر؛ بالاتر = پاسخ خلاقانه‌تر.",
                        fontSize = 12.sp,
                        color = TextSecondaryDark,
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusChip(isConfigured: Boolean) {
    val bg = if (isConfigured) SuspicionGreen.copy(alpha = 0.15f) else SuspicionYellow.copy(alpha = 0.15f)
    val border = if (isConfigured) SuspicionGreen else SuspicionYellow
    val fg = if (isConfigured) SuspicionGreen else SuspicionYellow
    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, border.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isConfigured) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isConfigured) "آماده" else "ناقص",
                fontSize = 11.sp,
                color = fg,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ChatBubble(message: TestChatMessage) {
    val alignment = if (message.isFromUser) Alignment.CenterEnd else Alignment.CenterStart
    val containerColor = when {
        message.isError -> MafiaCrimsonDark.copy(alpha = 0.35f)
        message.isFromUser -> AiPurpleDark.copy(alpha = 0.45f)
        else -> MafiaSurfaceVariant
    }
    val borderColor = when {
        message.isError -> MafiaCrimson
        message.isFromUser -> AiPurple.copy(alpha = 0.5f)
        else -> MafiaBorder
    }
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Surface(
            color = containerColor,
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, borderColor),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Text(
                text = message.content,
                fontSize = 13.sp,
                color = if (message.isError) MafiaCrimsonLight else TextPrimaryDark,
                lineHeight = 21.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

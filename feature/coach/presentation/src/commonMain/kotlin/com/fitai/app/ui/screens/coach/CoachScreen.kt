package com.fitai.app.ui.screens.coach

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fitai.app.coach.CoachLimits
import com.fitai.app.localization.LocalStrings
import com.fitai.app.model.CoachMessage
import com.fitai.app.ui.components.FitAIProGateCard
import com.fitai.app.ui.theme.FitAIColors
import com.fitai.app.ui.theme.FitAIIcons
import com.fitai.app.ui.theme.FitAIShapes
import com.fitai.app.ui.theme.FitAISpacing
import com.fitai.app.viewmodel.CoachViewModel
import com.fitai.app.viewmodel.coach.CoachEffect
import com.fitai.app.viewmodel.coach.CoachIntent
import com.fitai.app.viewmodel.coach.CoachSendError
import com.fitai.app.viewmodel.coach.CoachState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CoachScreen(
    onBack: () -> Unit,
    onNavigatePaywall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: CoachViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val navigatePaywall by rememberUpdatedState(onNavigatePaywall)

    LaunchedEffect(viewModel) {
        viewModel.onIntent(CoachIntent.Load)
        viewModel.effects.collect { effect ->
            when (effect) {
                CoachEffect.NavigateToPaywall -> navigatePaywall()
                CoachEffect.ScrollToLatest -> scope.launch {
                    listState.scrollToLatest(viewModel.state.value.chatItems().size)
                }
            }
        }
    }

    CoachContent(
        state = state,
        listState = listState,
        onIntent = viewModel::onIntent,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
private fun CoachContent(
    state: CoachState,
    listState: LazyListState,
    onIntent: (CoachIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val chatItems = remember(state.messages, state.pendingMessage, state.sending, state.sendError) { state.chatItems() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FitAIColors.Background),
    ) {
        CoachTopBar(
            title = strings.coachTitle,
            onBack = onBack,
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = FitAISpacing.sm),
        )
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = FitAISpacing.lg, vertical = FitAISpacing.md),
            verticalArrangement = Arrangement.spacedBy(FitAISpacing.md),
        ) {
            item(key = "report") {
                WeeklyReportCard(
                    report = state.report,
                    loading = state.reportLoading,
                    error = state.reportError,
                    onRetry = { onIntent(CoachIntent.RetryReport) },
                    onUpgrade = { onIntent(CoachIntent.Upgrade) },
                )
            }
            item(key = "chat_header") {
                ChatHeader(
                    showClear = state.messages.isNotEmpty() && !state.chatLocked && !state.sending,
                    onClear = { onIntent(CoachIntent.ClearHistory) },
                )
            }
            if (chatItems.isEmpty()) {
                item(key = "empty") {
                    EmptyChat(
                        showPrompts = state.accessResolved && !state.chatLocked,
                        onPrompt = { onIntent(CoachIntent.SendPrompt(it)) },
                    )
                }
            } else {
                itemsIndexed(chatItems) { _, item ->
                    when (item) {
                        is ChatItem.Bubble -> MessageBubble(text = item.text, fromUser = item.fromUser)
                        ChatItem.Typing -> TypingBubble()
                        is ChatItem.Error -> SendErrorRow(error = item.error, onRetry = { onIntent(CoachIntent.RetrySend) })
                    }
                }
            }
        }
        CoachBottomBar(state = state, onIntent = onIntent)
    }
}

@Composable
private fun CoachTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = FitAIIcons.ArrowBack,
                contentDescription = LocalStrings.current.actionBack,
                tint = FitAIColors.Primary,
            )
        }
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = FitAIColors.OnBackground,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Spacer(Modifier.size(40.dp))
    }
}

@Composable
private fun ChatHeader(showClear: Boolean, onClear: () -> Unit) {
    val strings = LocalStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = strings.coachChatTitle,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            color = FitAIColors.OnSurface,
        )
        if (showClear) {
            TextButton(onClick = onClear) {
                Text(text = strings.coachClearHistory, color = FitAIColors.OnSurfaceMuted)
            }
        }
    }
}

@Composable
private fun EmptyChat(showPrompts: Boolean, onPrompt: (String) -> Unit) {
    val strings = LocalStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(FitAISpacing.sm)) {
        Text(
            text = strings.coachChatEmpty,
            style = MaterialTheme.typography.bodyMedium,
            color = FitAIColors.OnSurfaceMuted,
        )
        if (showPrompts) {
            listOf(strings.coachPromptKnee, strings.coachPromptProtein, strings.coachPromptHarder).forEach { prompt ->
                Text(
                    text = prompt,
                    style = MaterialTheme.typography.bodyMedium,
                    color = FitAIColors.OnSurface,
                    modifier = Modifier
                        .background(FitAIColors.SurfaceVariant, FitAIShapes.button)
                        .clickable { onPrompt(prompt) }
                        .padding(horizontal = FitAISpacing.md, vertical = FitAISpacing.sm),
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(text: String, fromUser: Boolean) {
    BubbleRow(fromUser = fromUser) { bubbleModifier ->
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = FitAIColors.OnSurface,
            modifier = bubbleModifier,
        )
    }
}

@Composable
private fun TypingBubble() {
    BubbleRow(fromUser = false) { bubbleModifier ->
        Row(
            modifier = bubbleModifier,
            horizontalArrangement = Arrangement.spacedBy(FitAISpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = FitAIColors.Primary,
                strokeWidth = 2.dp,
            )
            Text(
                text = LocalStrings.current.coachTyping,
                style = MaterialTheme.typography.bodySmall,
                color = FitAIColors.OnSurfaceMuted,
            )
        }
    }
}

@Composable
private fun BubbleRow(
    fromUser: Boolean,
    content: @Composable (Modifier) -> Unit,
) {
    val bubbleModifier = Modifier
        .background(if (fromUser) FitAIColors.SurfaceVariant else FitAIColors.Surface, FitAIShapes.card)
        .then(if (fromUser) Modifier else Modifier.border(BorderStroke(1.dp, FitAIColors.SurfaceVariant), FitAIShapes.card))
        .padding(horizontal = FitAISpacing.md, vertical = FitAISpacing.sm)
    Row(modifier = Modifier.fillMaxWidth()) {
        if (fromUser) Spacer(Modifier.weight(0.15f))
        Box(
            modifier = Modifier.weight(0.85f),
            contentAlignment = if (fromUser) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            content(bubbleModifier)
        }
        if (!fromUser) Spacer(Modifier.weight(0.15f))
    }
}

@Composable
private fun SendErrorRow(error: CoachSendError, onRetry: () -> Unit) {
    val strings = LocalStrings.current
    when (error) {
        CoachSendError.Failed -> InlineRetry(message = strings.coachSendFailed, onRetry = onRetry, color = FitAIColors.Error)
        CoachSendError.DailyLimit -> ErrorText(strings.coachDailyLimit)
        CoachSendError.TooLong -> ErrorText(strings.coachMessageTooLong(CoachLimits.MAX_MESSAGE_CHARS))
    }
}

@Composable
private fun ErrorText(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = FitAIColors.Warning)
}

@Composable
private fun CoachBottomBar(state: CoachState, onIntent: (CoachIntent) -> Unit) {
    val strings = LocalStrings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(FitAIColors.Background.copy(alpha = 0.95f))
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = FitAISpacing.lg, vertical = FitAISpacing.md),
        verticalArrangement = Arrangement.spacedBy(FitAISpacing.sm),
    ) {
        if (state.accessResolved) {
            if (state.chatLocked) {
                FitAIProGateCard(
                    title = strings.coachChatLockedTitle,
                    body = strings.coachChatLockedBody,
                    ctaLabel = strings.upgradeToPro,
                    onClick = { onIntent(CoachIntent.Upgrade) },
                    badgeLabel = strings.proBadge,
                )
            } else {
                ChatInput(
                    value = state.input,
                    sending = state.sending,
                    onValueChange = { onIntent(CoachIntent.InputChanged(it)) },
                    onSend = { onIntent(CoachIntent.Send) },
                )
            }
        }
        Text(
            text = strings.coachDisclaimer,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelSmall,
            color = FitAIColors.OnSurfaceMuted,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ChatInput(
    value: String,
    sending: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    val strings = LocalStrings.current
    val canSend = value.isNotBlank() && !sending
    Row(
        horizontalArrangement = Arrangement.spacedBy(FitAISpacing.sm),
        verticalAlignment = Alignment.Bottom,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(strings.coachInputPlaceholder) },
            supportingText = if (value.length > CHAR_COUNT_THRESHOLD) {
                { Text(strings.coachCharCount(value.length, CoachLimits.MAX_MESSAGE_CHARS)) }
            } else {
                null
            },
            maxLines = 4,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { if (canSend) onSend() }),
            shape = FitAIShapes.button,
            textStyle = MaterialTheme.typography.bodyLarge,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = FitAIColors.Primary,
                unfocusedBorderColor = FitAIColors.SurfaceVariant,
                cursorColor = FitAIColors.Primary,
                focusedTextColor = FitAIColors.OnBackground,
                unfocusedTextColor = FitAIColors.OnSurface,
                focusedPlaceholderColor = FitAIColors.OnSurfaceMuted,
                unfocusedPlaceholderColor = FitAIColors.OnSurfaceMuted,
                focusedSupportingTextColor = FitAIColors.OnSurfaceMuted,
                unfocusedSupportingTextColor = FitAIColors.OnSurfaceMuted,
            ),
        )
        IconButton(
            onClick = onSend,
            enabled = canSend,
            modifier = Modifier
                .padding(bottom = FitAISpacing.xs)
                .size(48.dp)
                .background(if (canSend) FitAIColors.Primary else FitAIColors.SurfaceVariant, CircleShape),
        ) {
            Icon(
                imageVector = FitAIIcons.ArrowForward,
                contentDescription = strings.coachSendA11y,
                tint = if (canSend) FitAIColors.OnPrimary else FitAIColors.OnSurfaceMuted,
            )
        }
    }
}

private sealed interface ChatItem {
    data class Bubble(val text: String, val fromUser: Boolean) : ChatItem
    data object Typing : ChatItem
    data class Error(val error: CoachSendError) : ChatItem
}

private fun CoachState.chatItems(): List<ChatItem> = buildList {
    messages.forEach { add(ChatItem.Bubble(it.text, fromUser = it.role == CoachMessage.ROLE_USER)) }
    pendingMessage?.let { add(ChatItem.Bubble(it, fromUser = true)) }
    if (sending) add(ChatItem.Typing)
    sendError?.let { add(ChatItem.Error(it)) }
}

/** Chat items follow the report card and the chat header. */
private suspend fun LazyListState.scrollToLatest(chatItemCount: Int) {
    if (chatItemCount == 0) return
    val lastIndex = LEADING_ITEMS + chatItemCount - 1
    withTimeoutOrNull(SCROLL_WAIT_MS) {
        snapshotFlow { layoutInfo.totalItemsCount }.first { it > lastIndex }
    }
    animateScrollToItem(lastIndex.coerceAtMost((layoutInfo.totalItemsCount - 1).coerceAtLeast(0)))
}

private const val LEADING_ITEMS = 2
private const val CHAR_COUNT_THRESHOLD = 800
private const val SCROLL_WAIT_MS = 500L

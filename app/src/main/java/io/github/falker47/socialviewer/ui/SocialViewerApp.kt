package io.github.falker47.socialviewer.ui

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebStorage
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RadioButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.falker47.socialviewer.R
import io.github.falker47.socialviewer.domain.SocialContent
import io.github.falker47.socialviewer.provider.ProviderRegistry
import io.github.falker47.socialviewer.util.UrlExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ViewerState {
    data object Home : ViewerState
    data class Loading(val url: String) : ViewerState
    data class Ready(val content: SocialContent) : ViewerState
    data class Error(val message: String, val url: String?) : ViewerState
}

private enum class AppScreen {
    Viewer,
    Settings,
}

private sealed interface ManualLinkResult {
    data class Valid(val url: String) : ManualLinkResult
    data class Invalid(val message: String, val displayValue: String) : ManualLinkResult
}

private const val PREFS_NAME = "social_viewer_ui"
private const val PREF_ONBOARDING_COMPLETE = "onboarding_complete"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialViewerApp(
    context: Context,
    incomingUrl: String?,
    registry: ProviderRegistry,
    resumeToken: Int,
    onIncomingConsumed: () -> Unit,
) {
    val initialIncoming = incomingUrl?.takeIf { it.isNotBlank() }
    var state by remember {
        mutableStateOf<ViewerState>(
            initialIncoming?.let { ViewerState.Loading(it) } ?: ViewerState.Home,
        )
    }
    var screen by remember { mutableStateOf(AppScreen.Viewer) }
    var manualUrl by remember { mutableStateOf(initialIncoming.orEmpty()) }
    var manualError by remember { mutableStateOf<String?>(null) }
    var directLinkActive by remember { mutableStateOf(isTikTokDirectLinkHandlingActive(context)) }
    var awaitingLinkSettings by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val uiPrefs = remember {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    var themeMode by remember { mutableStateOf(readThemeMode(uiPrefs)) }
    val darkTheme = themeMode.resolveDark(isSystemInDarkTheme())
    var onboardingStep by remember {
        mutableStateOf(
            if (uiPrefs.getBoolean(PREF_ONBOARDING_COMPLETE, false)) null else 1,
        )
    }
    var inputTarget by remember { mutableStateOf<Rect?>(null) }
    var directLinkTarget by remember { mutableStateOf<Rect?>(null) }

    fun completeOnboarding() {
        uiPrefs.edit().putBoolean(PREF_ONBOARDING_COMPLETE, true).apply()
        onboardingStep = null
    }

    fun start(url: String) {
        manualUrl = url
        manualError = null
        screen = AppScreen.Viewer
        state = ViewerState.Loading(url)
    }

    fun attemptManualOpen(rawValue: String) {
        when (val result = validateManualLink(rawValue, registry)) {
            is ManualLinkResult.Valid -> {
                manualUrl = result.url
                start(result.url)
            }

            is ManualLinkResult.Invalid -> {
                manualUrl = result.displayValue
                manualError = result.message
                state = ViewerState.Home
            }
        }
    }

    fun pasteAndOpen() {
        val clipboardText = readClipboardText(context)
        if (clipboardText.isNullOrBlank()) {
            manualError = "Nessun link negli appunti."
            return
        }
        attemptManualOpen(clipboardText)
    }

    fun openManualField() {
        if (manualUrl.isBlank()) {
            pasteAndOpen()
        } else {
            attemptManualOpen(manualUrl)
        }
    }

    fun launchDirectLinkSettings() {
        awaitingLinkSettings = true
        openDefaultLinkSettings(context)
    }

    LaunchedEffect(incomingUrl) {
        if (!incomingUrl.isNullOrBlank()) {
            screen = AppScreen.Viewer
            val currentLoading = state as? ViewerState.Loading
            if (currentLoading?.url != incomingUrl) {
                start(incomingUrl)
            }
            onIncomingConsumed()
        }
    }

    LaunchedEffect(state) {
        val loading = state as? ViewerState.Loading ?: return@LaunchedEffect
        state = try {
            val content = withContext(Dispatchers.IO) {
                registry.resolve(loading.url)
            }
            ViewerState.Ready(content)
        } catch (t: Throwable) {
            ViewerState.Error(
                message = t.message ?: "Impossibile aprire questo contenuto.",
                url = loading.url,
            )
        }
    }

    LaunchedEffect(resumeToken) {
        val wasActive = directLinkActive
        val isActive = isTikTokDirectLinkHandlingActive(context)
        directLinkActive = isActive

        if (awaitingLinkSettings) {
            awaitingLinkSettings = false
            if (isActive && !wasActive) {
                snackbarHostState.showSnackbar("Apertura diretta attivata")
            }
        }
    }

    MaterialTheme(
        colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme(),
    ) {
        ApplySystemBars(context = context, darkTheme = darkTheme)
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    when (screen) {
                        AppScreen.Settings -> {
                            TopAppBar(
                                title = { Text("Impostazioni") },
                                navigationIcon = {
                                    IconButton(onClick = { screen = AppScreen.Viewer }) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_arrow_back_24),
                                            contentDescription = "Indietro",
                                        )
                                    }
                                },
                            )
                        }

                        AppScreen.Viewer -> {
                            TopAppBar(
                                title = { Text("Social Viewer") },
                                actions = {
                                    if (state == ViewerState.Home) {
                                        IconButton(onClick = { screen = AppScreen.Settings }) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_settings_24),
                                                contentDescription = "Impostazioni",
                                            )
                                        }
                                    }
                                },
                            )
                        }
                    }
                },
            ) { padding ->
                when (screen) {
                    AppScreen.Settings -> SettingsScreen(
                        directLinkActive = directLinkActive,
                        themeMode = themeMode,
                        onThemeModeChange = { selectedMode ->
                            themeMode = selectedMode
                            writeThemeMode(uiPrefs, selectedMode)
                        },
                        onConfigureDirectLinks = ::launchDirectLinkSettings,
                        onClearSiteData = {
                            clearProviderSiteData(context) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Dati dei provider cancellati")
                                }
                            }
                        },
                        appVersion = appVersionName(context),
                        modifier = Modifier.padding(padding),
                    )

                    AppScreen.Viewer -> when (val current = state) {
                        ViewerState.Home -> HomeScreen(
                            value = manualUrl,
                            error = manualError,
                            directLinkActive = directLinkActive,
                            onValueChange = {
                                manualUrl = it
                                manualError = null
                            },
                            onPasteAndOpen = ::pasteAndOpen,
                            onOpen = ::openManualField,
                            onConfigureDirectLinks = ::launchDirectLinkSettings,
                            onInputTargetChanged = { inputTarget = it },
                            onDirectLinkTargetChanged = { directLinkTarget = it },
                            modifier = Modifier.padding(padding),
                        )

                        is ViewerState.Loading -> LoadingScreen(
                            modifier = Modifier.padding(padding),
                        )

                        is ViewerState.Ready -> PlayerScreen(
                            content = current.content,
                            onBack = { state = ViewerState.Home },
                            onOpenOriginal = { openExternal(context, current.content.canonicalUrl) },
                            modifier = Modifier.padding(padding),
                        )

                        is ViewerState.Error -> ErrorScreen(
                            message = current.message,
                            onBack = { state = ViewerState.Home },
                            onOpenOriginal = current.url?.let { url ->
                                { openExternal(context, url) }
                            },
                            modifier = Modifier.padding(padding),
                        )
                    }
                }
            }

            if (screen == AppScreen.Viewer && state == ViewerState.Home) {
                onboardingStep?.let { step ->
                    val target = if (step == 1) inputTarget else directLinkTarget
                    if (target != null) {
                        CoachMarkOverlay(
                            step = step,
                            target = target,
                            onNext = { onboardingStep = 2 },
                            onSkip = ::completeOnboarding,
                            onConfigure = {
                                completeOnboarding()
                                launchDirectLinkSettings()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    value: String,
    error: String?,
    directLinkActive: Boolean,
    onValueChange: (String) -> Unit,
    onPasteAndOpen: () -> Unit,
    onOpen: () -> Unit,
    onConfigureDirectLinks: () -> Unit,
    onInputTargetChanged: (Rect) -> Unit,
    onDirectLinkTargetChanged: (Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
    ) {
        Text(
            "Apri un contenuto",
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { onInputTargetChanged(it.boundsInRoot()) },
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text("Incolla un link TikTok") },
                singleLine = true,
                isError = error != null,
                supportingText = error?.let { message ->
                    { Text(message) }
                },
                trailingIcon = {
                    IconButton(onClick = onPasteAndOpen) {
                        Icon(
                            painter = painterResource(R.drawable.ic_content_paste_24),
                            contentDescription = "Incolla e apri",
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Go,
                ),
                keyboardActions = KeyboardActions(onGo = { onOpen() }),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onOpen,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Apri")
            }
        }

        Spacer(Modifier.height(24.dp))

        DirectLinkCard(
            active = directLinkActive,
            onConfigure = onConfigureDirectLinks,
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { onDirectLinkTargetChanged(it.boundsInRoot()) },
        )
    }
}

@Composable
private fun DirectLinkCard(
    active: Boolean,
    onConfigure: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        if (active) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    "Apertura diretta attiva",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Ti basterà cliccare sui link per aprirli con Social Viewer.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    "Apertura diretta",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Apri i link TikTok direttamente in Social Viewer.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Da configurare",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onConfigure) {
                        Text("Configura")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    directLinkActive: Boolean,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onConfigureDirectLinks: () -> Unit,
    onClearSiteData: () -> Unit,
    appVersion: String,
    modifier: Modifier = Modifier,
) {
    var confirmClear by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
    ) {
        SettingsSectionTitle("APERTURA DIRETTA")
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("TikTok", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (directLinkActive) "Attiva" else "Da configurare",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            OutlinedButton(onClick = onConfigureDirectLinks) {
                Text("Configura")
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Android gestisce quali link possono aprirsi automaticamente.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(24.dp))

        SettingsSectionTitle("ASPETTO")
        Spacer(Modifier.height(12.dp))
        Text("Tema", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        ThemeMode.entries.forEach { mode ->
            ThemeModeOption(
                label = mode.displayLabel,
                selected = themeMode == mode,
                onSelect = { onThemeModeChange(mode) },
            )
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(24.dp))

        SettingsSectionTitle("PRIVACY E DATI DEL SITO")
        Spacer(Modifier.height(12.dp))
        Text("Dati dei provider", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            "Cookie e preferenze dei provider sono conservati localmente.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(14.dp))
        OutlinedButton(onClick = { confirmClear = true }) {
            Text("Cancella dati del sito")
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(24.dp))

        SettingsSectionTitle("INFORMAZIONI")
        Spacer(Modifier.height(12.dp))
        Text("Social Viewer", style = MaterialTheme.typography.titleMedium)
        Text(
            "Versione $appVersion",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))
        Text("Privacy", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            "Nessun account Social Viewer, cronologia o analytics.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Cancellare i dati dei provider?") },
            text = {
                Text(
                    "Verranno rimossi cookie e preferenze dei provider. " +
                        "TikTok o Instagram potrebbero chiederti nuovamente le preferenze sui cookie.",
                )
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text("Annulla")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClear = false
                        onClearSiteData()
                    },
                ) {
                    Text("Cancella")
                }
            },
        )
    }
}

@Composable
private fun ThemeModeOption(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
        )
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun SettingsSectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun CoachMarkOverlay(
    step: Int,
    target: Rect,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onConfigure: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scrimColor = Color.Black.copy(alpha = 0.62f)
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val containerHeightPx = with(density) { maxHeight.toPx() }
        val navigationBarBottomPadding =
            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val alignment = if (target.center.y < containerHeightPx / 2f) {
            Alignment.BottomCenter
        } else {
            Alignment.TopCenter
        }
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                },
        ) {
            drawRect(scrimColor)

            val pad = 8.dp.toPx()
            val left = (target.left - pad).coerceAtLeast(0f)
            val top = (target.top - pad).coerceAtLeast(0f)
            val right = (target.right + pad).coerceAtMost(size.width)
            val bottom = (target.bottom + pad).coerceAtMost(size.height)

            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                cornerRadius = CornerRadius(16.dp.toPx()),
                blendMode = BlendMode.Clear,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {},
                ),
        )

        Surface(
            modifier = Modifier
                .align(alignment)
                .padding(
                    start = 20.dp,
                    end = 20.dp,
                    top = if (alignment == Alignment.TopCenter) 92.dp else 20.dp,
                    bottom = if (alignment == Alignment.BottomCenter) {
                        navigationBarBottomPadding + if (step == 1) 56.dp else 28.dp
                    } else {
                        20.dp
                    },
                )
                .widthIn(max = 520.dp),
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                if (step == 1) {
                    Text(
                        "Apri un link",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Incolla un link TikTok pubblico: Social Viewer lo aprirà subito.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Button(onClick = onNext) {
                            Text("Avanti")
                        }
                    }
                } else {
                    Text(
                        "Apri i link con un tocco",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Ti basta toccare un link TikTok, su WhatsApp o nel browser: " +
                            "si aprirà direttamente in Social Viewer.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = onSkip) {
                            Text("Non ora")
                        }
                        Spacer(Modifier.weight(1f))
                        Button(onClick = onConfigure) {
                            Text("Configura apertura diretta")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ApplySystemBars(
    context: Context,
    darkTheme: Boolean,
) {
    val barColor = MaterialTheme.colorScheme.surface.toArgb()

    SideEffect {
        val window = (context as? Activity)?.window ?: return@SideEffect
        window.statusBarColor = barColor
        window.navigationBarColor = barColor

        val lightBarFlags =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        val currentFlags = window.decorView.systemUiVisibility
        window.decorView.systemUiVisibility = if (darkTheme) {
            currentFlags and lightBarFlags.inv()
        } else {
            currentFlags or lightBarFlags
        }
    }
}

@Composable
private fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun PlayerScreen(
    content: SocialContent,
    onBack: () -> Unit,
    onOpenOriginal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) { Text("Indietro") }
            Column(Modifier.weight(1f)) {
                Text(content.providerName, style = MaterialTheme.typography.labelLarge)
                content.authorName?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
            TextButton(onClick = onOpenOriginal) { Text("Originale ↗") }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black),
        ) {
            EmbedWebView(
                html = content.embedHtml,
                baseUrl = content.documentBaseUrl,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun ErrorScreen(
    message: String,
    onBack: () -> Unit,
    onOpenOriginal: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Contenuto non disponibile", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(message)
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack) { Text("Indietro") }
            if (onOpenOriginal != null) {
                Button(onClick = onOpenOriginal) { Text("Apri originale") }
            }
        }
    }
}

private fun validateManualLink(
    rawValue: String,
    registry: ProviderRegistry,
): ManualLinkResult {
    val trimmed = rawValue.trim()
    val extracted = UrlExtractor.firstHttpUrl(trimmed)
    val candidate = extracted ?: trimmed

    if (candidate.isBlank()) {
        return ManualLinkResult.Invalid(
            message = "Nessun link negli appunti.",
            displayValue = "",
        )
    }

    val uri = Uri.parse(candidate)
    if (!uri.scheme.equals("https", ignoreCase = true) || uri.host.isNullOrBlank()) {
        return ManualLinkResult.Invalid(
            message = "Questo non sembra un link valido.",
            displayValue = candidate,
        )
    }

    if (registry.providerFor(candidate) == null) {
        return ManualLinkResult.Invalid(
            message = "Questo servizio non è ancora supportato.",
            displayValue = candidate,
        )
    }

    return ManualLinkResult.Valid(candidate)
}

private fun readClipboardText(context: Context): String? {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return null
    val clip = clipboard.primaryClip ?: return null
    if (clip.itemCount == 0) return null

    return clip.getItemAt(0)
        .coerceToText(context)
        ?.toString()
        ?.trim()
        ?.takeIf { it.isNotBlank() }
}

private fun clearProviderSiteData(
    context: Context,
    onComplete: () -> Unit,
) {
    WebStorage.getInstance().deleteAllData()
    val cookieManager = CookieManager.getInstance()
    cookieManager.removeAllCookies {
        cookieManager.flush()
        Handler(Looper.getMainLooper()).post(onComplete)
    }
}

private fun appVersionName(context: Context): String = runCatching {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName
}.getOrNull().orEmpty().ifBlank { "—" }

private fun openExternal(context: Context, url: String) {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

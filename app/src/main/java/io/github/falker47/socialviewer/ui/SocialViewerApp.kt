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
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.falker47.socialviewer.R
import io.github.falker47.socialviewer.domain.SocialContent
import io.github.falker47.socialviewer.provider.ProviderRegistry
import io.github.falker47.socialviewer.util.UrlExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

sealed interface ViewerState {
    data object Home : ViewerState
    data class XConsent(val url: String) : ViewerState
    data class Loading(val url: String) : ViewerState
    data class Ready(val content: SocialContent) : ViewerState
    data class Error(
        val title: String,
        val message: String,
        val url: String?,
    ) : ViewerState
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
private const val PREF_X_EMBED_CONSENT_GRANTED = "x_embed_consent_granted"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialViewerApp(
    context: Context,
    incomingUrl: String?,
    registry: ProviderRegistry,
    resumeToken: Int,
    onIncomingConsumed: () -> Unit,
) {
    val uiPrefs = remember {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    val initialIncoming = incomingUrl?.takeIf { it.isNotBlank() }
    val initialXEmbedConsentGranted =
        uiPrefs.getBoolean(PREF_X_EMBED_CONSENT_GRANTED, false)
    var xEmbedConsentGranted by remember {
        mutableStateOf(initialXEmbedConsentGranted)
    }
    var state by remember {
        mutableStateOf<ViewerState>(
            initialIncoming?.let {
                viewerStateBeforeResolve(it, registry, initialXEmbedConsentGranted)
            } ?: ViewerState.Home,
        )
    }
    var screen by remember { mutableStateOf(AppScreen.Viewer) }
    var manualUrl by remember { mutableStateOf(initialIncoming.orEmpty()) }
    var manualError by remember { mutableStateOf<String?>(null) }
    var directLinkState by remember { mutableStateOf(queryDirectLinkHandlingState(context)) }
    var awaitingLinkSettings by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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
        state = viewerStateBeforeResolve(url, registry, xEmbedConsentGranted)
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
            val copy = viewerErrorCopyFor(t)
            ViewerState.Error(
                title = copy.title,
                message = copy.message,
                url = loading.url,
            )
        }
    }

    LaunchedEffect(resumeToken) {
        val previousState = directLinkState
        val refreshedState = queryDirectLinkHandlingState(context)
        directLinkState = refreshedState

        if (awaitingLinkSettings) {
            awaitingLinkSettings = false
            when {
                refreshedState.allProvidersActive && !previousState.allProvidersActive ->
                    snackbarHostState.showSnackbar("Apertura diretta attivata")

                refreshedState.activeProviderCount > previousState.activeProviderCount ->
                    snackbarHostState.showSnackbar("Apertura diretta aggiornata")
            }
        }
    }

    SocialViewerTheme(darkTheme = darkTheme) {
        ApplySystemBars(context = context, darkTheme = darkTheme)
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    if (screen == AppScreen.Settings) {
                        TopAppBar(
                            title = { Text("Impostazioni") },
                            navigationIcon = {
                                IconButton(onClick = { screen = AppScreen.Viewer }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Indietro",
                                    )
                                }
                            },
                        )
                    }
                },
            ) { padding ->
                when (screen) {
                    AppScreen.Settings -> SettingsScreen(
                        directLinkState = directLinkState,
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
                        xEmbedConsentGranted = xEmbedConsentGranted,
                        onRevokeXEmbedConsent = {
                            xEmbedConsentGranted = false
                            uiPrefs.edit().remove(PREF_X_EMBED_CONSENT_GRANTED).apply()
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "Autorizzazione X revocata",
                                )
                            }
                        },
                        appVersion = appVersionName(context),
                        onOpenSource = {
                            openExternal(context, "https://github.com/falker47/SocialViewer")
                        },
                        modifier = Modifier.padding(padding),
                    )

                    AppScreen.Viewer -> when (val current = state) {
                        ViewerState.Home -> HomeScreen(
                            value = manualUrl,
                            error = manualError,
                            directLinkState = directLinkState,
                            onValueChange = {
                                manualUrl = it
                                manualError = null
                            },
                            onPasteAndOpen = ::pasteAndOpen,
                            onOpen = ::openManualField,
                            onConfigureDirectLinks = ::launchDirectLinkSettings,
                            onOpenSettings = { screen = AppScreen.Settings },
                            onInputTargetChanged = { inputTarget = it },
                            onDirectLinkTargetChanged = { directLinkTarget = it },
                            modifier = Modifier.padding(padding),
                        )

                        is ViewerState.XConsent -> XConsentScreen(
                            onBack = { state = ViewerState.Home },
                            onOpenOriginal = { openExternal(context, current.url) },
                            onLoadX = {
                                xEmbedConsentGranted = true
                                uiPrefs.edit()
                                    .putBoolean(PREF_X_EMBED_CONSENT_GRANTED, true)
                                    .apply()
                                state = ViewerState.Loading(current.url)
                            },
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
                            title = current.title,
                            message = current.message,
                            onBack = { state = ViewerState.Home },
                            onRetry = current.url?.let { url ->
                                { state = ViewerState.Loading(url) }
                            },
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
    directLinkState: DirectLinkHandlingState,
    onValueChange: (String) -> Unit,
    onPasteAndOpen: () -> Unit,
    onOpen: () -> Unit,
    onConfigureDirectLinks: () -> Unit,
    onOpenSettings: () -> Unit,
    onInputTargetChanged: (Rect) -> Unit,
    onDirectLinkTargetChanged: (Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val compactHeight = maxHeight < 640.dp

        if (compactHeight) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(8.dp))
                HomePrimaryContent(
                    value = value,
                    error = error,
                    onValueChange = onValueChange,
                    onPasteAndOpen = onPasteAndOpen,
                    onOpen = onOpen,
                    onInputTargetChanged = onInputTargetChanged,
                )
                Spacer(Modifier.height(32.dp))
                HomeUtilities(
                    directLinkState = directLinkState,
                    onConfigureDirectLinks = onConfigureDirectLinks,
                    onOpenSettings = onOpenSettings,
                    onDirectLinkTargetChanged = onDirectLinkTargetChanged,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(24.dp))
                    HomePrimaryContent(
                        value = value,
                        error = error,
                        onValueChange = onValueChange,
                        onPasteAndOpen = onPasteAndOpen,
                        onOpen = onOpen,
                        onInputTargetChanged = onInputTargetChanged,
                    )
                }

                HomeUtilities(
                    directLinkState = directLinkState,
                    onConfigureDirectLinks = onConfigureDirectLinks,
                    onOpenSettings = onOpenSettings,
                    onDirectLinkTargetChanged = onDirectLinkTargetChanged,
                )
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun HomePrimaryContent(
    value: String,
    error: String?,
    onValueChange: (String) -> Unit,
    onPasteAndOpen: () -> Unit,
    onOpen: () -> Unit,
    onInputTargetChanged: (Rect) -> Unit,
) {
    FocusFrameMark(
        modifier = Modifier.size(72.dp),
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(18.dp))
    Text(
        "Social Viewer",
        style = MaterialTheme.typography.headlineLarge.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.5).sp,
        ),
    )
    Spacer(Modifier.height(6.dp))
    Text(
        "Apri il contenuto, non il feed.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(32.dp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { onInputTargetChanged(it.boundsInRoot()) },
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("Incolla un link pubblico…") },
            singleLine = true,
            isError = error != null,
            supportingText = error?.let { message -> { Text(message) } },
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
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onOpen,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
        ) {
            Text("Apri")
        }
    }

    Spacer(Modifier.height(22.dp))
    SupportedProvidersStrip()
}

@Composable
private fun SupportedProvidersStrip() {
    val providers = listOf(
        "tiktok" to "TikTok",
        "instagram" to "Instagram",
        "threads" to "Threads",
        "youtube" to "YouTube",
        "reddit" to "Reddit",
        "pinterest" to "Pinterest",
        "x" to "X",
        "bluesky" to "Bluesky",
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Link supportati",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            providers.forEach { (providerId, label) ->
                ProviderMark(
                    providerId = providerId,
                    contentDescription = label,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun HomeUtilities(
    directLinkState: DirectLinkHandlingState,
    onConfigureDirectLinks: () -> Unit,
    onOpenSettings: () -> Unit,
    onDirectLinkTargetChanged: (Rect) -> Unit,
) {
    DirectLinkCard(
        state = directLinkState,
        onConfigure = onConfigureDirectLinks,
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { onDirectLinkTargetChanged(it.boundsInRoot()) },
    )
    Spacer(Modifier.height(10.dp))
    HomeNavigationRow(
        icon = Icons.Outlined.Settings,
        title = "Impostazioni",
        subtitle = "Aspetto, privacy e configurazione",
        onClick = onOpenSettings,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun DirectLinkCard(
    state: DirectLinkHandlingState,
    onConfigure: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val status = when {
        state.allProvidersActive -> "Configurata"
        state.anyProviderConfigured -> "Parziale"
        else -> "Da completare"
    }
    val icon = when {
        state.allProvidersActive -> Icons.Outlined.CheckCircle
        state.anyProviderConfigured -> Icons.Outlined.WarningAmber
        else -> Icons.Outlined.Public
    }
    HomeNavigationRow(
        icon = icon,
        title = "Apertura diretta",
        subtitle = status,
        onClick = onConfigure,
        modifier = modifier,
    )
}

@Composable
private fun HomeNavigationRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsScreen(
    directLinkState: DirectLinkHandlingState,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onConfigureDirectLinks: () -> Unit,
    onClearSiteData: () -> Unit,
    xEmbedConsentGranted: Boolean,
    onRevokeXEmbedConsent: () -> Unit,
    appVersion: String,
    onOpenSource: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmClear by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        SettingsSectionTitle("Apertura diretta")
        Spacer(Modifier.height(10.dp))
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            ),
        ) {
            directLinkState.providers.forEachIndexed { index, providerState ->
                ProviderSettingsRow(providerState, onConfigureDirectLinks)
                if (index < directLinkState.providers.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 58.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        DirectLinkHowToCard()
        Spacer(Modifier.height(28.dp))
        SettingsSectionTitle("Aspetto")
        Spacer(Modifier.height(10.dp))
        ThemeSegmentedControl(themeMode, onThemeModeChange)
        Spacer(Modifier.height(28.dp))
        SettingsSectionTitle("Privacy e dati")
        Spacer(Modifier.height(10.dp))
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            ),
        ) {
            SettingsActionRow(
                icon = Icons.Outlined.Shield,
                title = "Privacy",
                subtitle = "Nessun account Social Viewer, cronologia o analytics.",
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 58.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            SettingsActionRow(
                icon = Icons.Outlined.DeleteOutline,
                title = "Cancella dati dei siti",
                subtitle = "Rimuove cookie e preferenze locali dei provider.",
                onClick = { confirmClear = true },
            )
            if (xEmbedConsentGranted) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 58.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                SettingsActionRow(
                    icon = Icons.Outlined.CheckCircle,
                    title = "Autorizzazione X",
                    subtitle = "Autorizzata sul dispositivo",
                    actionLabel = "Revoca",
                    onClick = onRevokeXEmbedConsent,
                )
            }
        }
        Spacer(Modifier.height(28.dp))
        SettingsSectionTitle("Informazioni")
        Spacer(Modifier.height(10.dp))
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            ),
        ) {
            SettingsActionRow(
                icon = Icons.Outlined.Info,
                title = "Social Viewer",
                subtitle = "Versione " + appVersion,
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 58.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            SettingsActionRow(
                icon = Icons.Outlined.Code,
                title = "Codice sorgente",
                subtitle = "GitHub",
                trailingIcon = Icons.Outlined.OpenInNew,
                onClick = onOpenSource,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Cancellare i dati dei provider?") },
            text = {
                Text(
                    "Verranno rimossi cookie e preferenze dei provider. " +
                        "I provider potrebbero chiederti nuovamente le preferenze sui cookie.",
                )
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Annulla") }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClear = false
                        onClearSiteData()
                    },
                ) { Text("Cancella") }
            },
        )
    }
}

@Composable
private fun ProviderSettingsRow(
    providerState: DirectLinkProviderState,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProviderMark(
            providerId = providerState.definition.providerId,
            contentDescription = providerState.definition.displayName,
            modifier = Modifier.size(26.dp),
        )
        Spacer(Modifier.size(16.dp))
        Text(
            providerState.definition.displayName,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        ProviderStatusBadge(providerState.status)
        Spacer(Modifier.size(6.dp))
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun ProviderStatusBadge(status: DirectLinkProviderStatus) {
    val background = when (status) {
        DirectLinkProviderStatus.ACTIVE -> MaterialTheme.colorScheme.primaryContainer
        DirectLinkProviderStatus.PARTIAL -> MaterialTheme.colorScheme.secondaryContainer
        DirectLinkProviderStatus.NEEDS_SETUP -> MaterialTheme.colorScheme.surface
    }
    val foreground = when (status) {
        DirectLinkProviderStatus.ACTIVE -> MaterialTheme.colorScheme.onPrimaryContainer
        DirectLinkProviderStatus.PARTIAL -> MaterialTheme.colorScheme.onSecondaryContainer
        DirectLinkProviderStatus.NEEDS_SETUP -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        color = background,
        contentColor = foreground,
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            directLinkProviderStatusLabel(status),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun ThemeSegmentedControl(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
) {
    val options = listOf(
        Triple(ThemeMode.System, "Sistema", Icons.Outlined.Devices),
        Triple(ThemeMode.Light, "Chiaro", Icons.Outlined.LightMode),
        Triple(ThemeMode.Dark, "Scuro", Icons.Outlined.DarkMode),
    )
    Row(
        modifier = Modifier.fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .padding(2.dp),
    ) {
        options.forEach { (mode, label, icon) ->
            val selected = themeMode == mode
            Surface(
                modifier = Modifier.weight(1f).clickable { onThemeModeChange(mode) },
                shape = RoundedCornerShape(13.dp),
                color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                contentColor = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 11.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(label, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun DirectLinkHowToCard() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Come si attiva",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "• Configura\n• Aggiungi link\n• Seleziona i domini disponibili",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    "Se possiedi l’app ufficiale, questa potrebbe avere priorità rispetto a Social Viewer.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    trailingIcon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
) {
    val interactionModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Row(
        modifier = Modifier.fillMaxWidth().then(interactionModifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.size(18.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (actionLabel != null) {
            Text(
                actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        } else if (trailingIcon != null) {
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun SettingsSectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.SemiBold,
    )
}

private fun directLinkSummaryLabel(state: DirectLinkHandlingState): String {
    if (!state.platformStateAvailable) {
        return "Configura nelle impostazioni Android"
    }

    val total = state.providers.size
    return when {
        state.allProvidersActive -> "Attiva per tutti i provider"
        state.activeProviderCount == 0 && !state.anyProviderConfigured -> "Da configurare"
        else -> "${state.activeProviderCount} di $total provider con apertura diretta attiva"
    }
}

private fun directLinkProviderStatusLabel(status: DirectLinkProviderStatus): String =
    when (status) {
        DirectLinkProviderStatus.ACTIVE -> "Attivo"
        DirectLinkProviderStatus.PARTIAL -> "Parziale"
        DirectLinkProviderStatus.NEEDS_SETUP -> "Da configurare"
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
        val targetIsUpper = target.center.y < containerHeightPx / 2f
        val calloutGap = 54.dp
        val calloutGapPx = with(density) { calloutGap.toPx() }
        val targetBottomDp = with(density) { target.bottom.toDp() }
        val bottomDistanceToTargetTopDp =
            with(density) { (containerHeightPx - target.top).toDp() }

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

        CoachMarkArrow(
            target = target,
            calloutBelowTarget = targetIsUpper,
            gapPx = calloutGapPx,
            modifier = Modifier.fillMaxSize(),
        )

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
                .align(if (targetIsUpper) Alignment.TopCenter else Alignment.BottomCenter)
                .padding(
                    start = 20.dp,
                    end = 20.dp,
                    top = if (targetIsUpper) targetBottomDp + calloutGap else 20.dp,
                    bottom = if (targetIsUpper) 20.dp else bottomDistanceToTargetTopDp + calloutGap,
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
                        "Incolla un link pubblico supportato: Social Viewer lo aprirà subito.",
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
                        "Configura Android una volta: poi i link compatibili possono aprirsi " +
                            "direttamente in Social Viewer.",
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
private fun CoachMarkArrow(
    target: Rect,
    calloutBelowTarget: Boolean,
    gapPx: Float,
    modifier: Modifier = Modifier,
) {
    val color = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        val strokeWidth = 2.5.dp.toPx()
        val arrowHalfWidth = 6.dp.toPx()
        val arrowDepth = 9.dp.toPx()

        val start: Offset
        val control: Offset
        val end: Offset

        if (calloutBelowTarget) {
            start = Offset(
                x = target.center.x + 34.dp.toPx(),
                y = target.bottom + gapPx - 6.dp.toPx(),
            )
            control = Offset(
                x = target.center.x + 54.dp.toPx(),
                y = target.bottom + gapPx * 0.48f,
            )
            end = Offset(
                x = target.center.x + 8.dp.toPx(),
                y = target.bottom + 8.dp.toPx(),
            )
        } else {
            start = Offset(
                x = target.center.x - 34.dp.toPx(),
                y = target.top - gapPx + 6.dp.toPx(),
            )
            control = Offset(
                x = target.center.x - 54.dp.toPx(),
                y = target.top - gapPx * 0.48f,
            )
            end = Offset(
                x = target.center.x - 8.dp.toPx(),
                y = target.top - 8.dp.toPx(),
            )
        }

        val path = Path().apply {
            moveTo(start.x, start.y)
            quadraticBezierTo(control.x, control.y, end.x, end.y)
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )

        val tangentX = end.x - control.x
        val tangentY = end.y - control.y
        val tangentLength = sqrt(tangentX * tangentX + tangentY * tangentY)
            .coerceAtLeast(1f)
        val unitX = tangentX / tangentLength
        val unitY = tangentY / tangentLength
        val perpendicularX = -unitY
        val perpendicularY = unitX
        val baseCenter = Offset(
            x = end.x - unitX * arrowDepth,
            y = end.y - unitY * arrowDepth,
        )
        val left = Offset(
            x = baseCenter.x + perpendicularX * arrowHalfWidth,
            y = baseCenter.y + perpendicularY * arrowHalfWidth,
        )
        val right = Offset(
            x = baseCenter.x - perpendicularX * arrowHalfWidth,
            y = baseCenter.y - perpendicularY * arrowHalfWidth,
        )

        drawLine(
            color = color,
            start = end,
            end = left,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = end,
            end = right,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
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

private fun viewerStateBeforeResolve(
    url: String,
    registry: ProviderRegistry,
    xEmbedConsentGranted: Boolean,
): ViewerState =
    if (registry.providerFor(url)?.id == "x" && !xEmbedConsentGranted) {
        ViewerState.XConsent(url)
    } else {
        ViewerState.Loading(url)
    }

@Composable
private fun XConsentScreen(
    onBack: () -> Unit,
    onOpenOriginal: () -> Unit,
    onLoadX: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Indietro",
            )
        }
        Spacer(Modifier.height(28.dp))
        Icon(
            imageVector = Icons.Outlined.Shield,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(44.dp),
        )
        Spacer(Modifier.height(18.dp))
        Text("Caricare il contenuto da X?", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Text(
            "X può ricevere dati tecnici del dispositivo e della visualizzazione quando " +
                "carichiamo un post incorporato. Social Viewer blocca i cookie di terze parti " +
                "e usa dnt=true per disattivare l’uso dell’embed per personalizzazione.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "La scelta viene ricordata sul dispositivo e può essere revocata nelle impostazioni.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(28.dp))
        Button(onClick = onLoadX, modifier = Modifier.fillMaxWidth()) { Text("Carica post X") }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onOpenOriginal, modifier = Modifier.fillMaxWidth()) {
            Icon(
                imageVector = Icons.Outlined.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(8.dp))
            Text("Apri originale")
        }
    }
}

@Composable
private fun LoadingScreen(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "focus-frame")
    val pulse by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "focus-frame-alpha",
    )
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FocusFrameMark(
            modifier = Modifier.size(72.dp).graphicsLayer { alpha = pulse },
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(20.dp))
        Text(
            "Apertura del contenuto…",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PlayerScreen(
    content: SocialContent,
    onBack: () -> Unit,
    onOpenOriginal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var embedReady by remember(
        content.providerId,
        content.canonicalUrl,
        content.embedHtml,
    ) {
        mutableStateOf(false)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Indietro",
                    )
                }
                Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                    Text(
                        content.providerName,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                    )
                    content.authorName?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
                OutlinedButton(
                    onClick = onOpenOriginal,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                ) {
                    Text("Originale")
                    Spacer(Modifier.size(6.dp))
                    Icon(
                        imageVector = Icons.Outlined.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f))
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().fillMaxHeight()
                        .widthIn(max = 840.dp).background(Color.Black),
                ) {
                    EmbedWebView(
                        html = content.embedHtml,
                        baseUrl = content.documentBaseUrl,
                        modifier = Modifier.fillMaxSize(),
                        onContentReady = { embedReady = true },
                    )
                }
            }
        }

        if (!embedReady) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                LoadingScreen()
            }
        }
    }
}

@Composable
private fun ErrorScreen(
    title: String,
    message: String,
    onBack: () -> Unit,
    onRetry: (() -> Unit)?,
    onOpenOriginal: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(52.dp),
        )
        Spacer(Modifier.height(18.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        if (onRetry != null) {
            Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("Riprova") }
        }
        if (onOpenOriginal != null) {
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onOpenOriginal, modifier = Modifier.fillMaxWidth()) {
                Text("Originale")
                Spacer(Modifier.size(6.dp))
                Icon(
                    imageVector = Icons.Outlined.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        TextButton(onClick = onBack) { Text("Torna all’inizio") }
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

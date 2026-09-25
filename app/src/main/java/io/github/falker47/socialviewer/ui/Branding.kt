package io.github.falker47.socialviewer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import io.github.falker47.socialviewer.R

private val Graphite = Color(0xFF181C1B)
private val GraphiteDark = Color(0xFF0E1413)
private val Teal = Color(0xFF006B64)
private val TealLight = Color(0xFF7BDAD0)
private val TealContainer = Color(0xFFA3F2E9)
private val TealDarkContainer = Color(0xFF005049)

private val SocialViewerLightColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = TealContainer,
    onPrimaryContainer = Color(0xFF00201D),
    secondary = Color(0xFF4A635F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDE8E3),
    onSecondaryContainer = Color(0xFF06201D),
    background = Color(0xFFF7FAF9),
    onBackground = Graphite,
    surface = Color(0xFFF7FAF9),
    onSurface = Graphite,
    surfaceVariant = Color(0xFFE1E9E7),
    onSurfaceVariant = Color(0xFF414947),
    outline = Color(0xFF717977),
    outlineVariant = Color(0xFFC1C9C7),
)

private val SocialViewerDarkColors = darkColorScheme(
    primary = TealLight,
    onPrimary = Color(0xFF003733),
    primaryContainer = TealDarkContainer,
    onPrimaryContainer = TealContainer,
    secondary = Color(0xFFB1CCC7),
    onSecondary = Color(0xFF1C3531),
    secondaryContainer = Color(0xFF334B47),
    onSecondaryContainer = Color(0xFFCDE8E3),
    background = GraphiteDark,
    onBackground = Color(0xFFDDE5E2),
    surface = Color(0xFF111817),
    onSurface = Color(0xFFDDE5E2),
    surfaceVariant = Color(0xFF27312F),
    onSurfaceVariant = Color(0xFFBEC9C6),
    outline = Color(0xFF899390),
    outlineVariant = Color(0xFF414A48),
)

@Composable
internal fun SocialViewerTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) SocialViewerDarkColors else SocialViewerLightColors,
        content = content,
    )
}

@Composable
internal fun FocusFrameMark(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Canvas(modifier = modifier) {
        val side = size.minDimension
        val stroke = side * 0.075f
        val inset = side * 0.12f
        val corner = side * 0.25f
        val innerInset = side * 0.28f
        val innerSize = side - innerInset * 2f
        drawRoundRect(
            color = color.copy(alpha = 0.12f),
            topLeft = Offset(innerInset, innerInset),
            size = Size(innerSize, innerSize),
            cornerRadius = CornerRadius(side * 0.08f),
        )
        drawLine(color, Offset(inset, inset + corner), Offset(inset, inset), stroke, StrokeCap.Round)
        drawLine(color, Offset(inset, inset), Offset(inset + corner, inset), stroke, StrokeCap.Round)
        drawLine(color, Offset(side - inset - corner, inset), Offset(side - inset, inset), stroke, StrokeCap.Round)
        drawLine(color, Offset(side - inset, inset), Offset(side - inset, inset + corner), stroke, StrokeCap.Round)
        drawLine(color, Offset(inset, side - inset - corner), Offset(inset, side - inset), stroke, StrokeCap.Round)
        drawLine(color, Offset(inset, side - inset), Offset(inset + corner, side - inset), stroke, StrokeCap.Round)
        drawLine(color, Offset(side - inset - corner, side - inset), Offset(side - inset, side - inset), stroke, StrokeCap.Round)
        drawLine(color, Offset(side - inset, side - inset), Offset(side - inset, side - inset - corner), stroke, StrokeCap.Round)
    }
}

@Composable
internal fun ProviderMark(
    providerId: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val iconRes = when (providerId) {
        "tiktok" -> R.drawable.ic_provider_tiktok
        "instagram" -> R.drawable.ic_provider_instagram
        "threads" -> R.drawable.ic_provider_threads
        "pinterest" -> R.drawable.ic_provider_pinterest
        "bluesky" -> R.drawable.ic_provider_bluesky
        else -> R.drawable.ic_provider_generic
    }
    val tint = when (providerId) {
        "instagram" -> Color(0xFFE1306C)
        "pinterest" -> Color(0xFFE60023)
        "bluesky" -> Color(0xFF1185FE)
        else -> MaterialTheme.colorScheme.onSurface
    }
    Icon(
        painter = painterResource(iconRes),
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint,
    )
}

package com.iris.gallery.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.provider.DocumentsContract
import java.io.File
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ViewDay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.iris.gallery.data.OfflineMapRepository
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.PhotoAlbum
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.iris.gallery.R
import com.iris.gallery.data.AccentColor
import com.iris.gallery.data.CornerStyle
import com.iris.gallery.data.DeleteMode
import com.iris.gallery.data.GridSpacing
import com.iris.gallery.data.PreferredEditor
import com.iris.gallery.data.SecureSharingMode
import com.iris.gallery.data.SettingsPreferences
import com.iris.gallery.data.SettingsState
import com.iris.gallery.data.StartupTab
import com.iris.gallery.data.SUPPORTED_LANGUAGES
import com.iris.gallery.data.ThemeMode
import com.iris.gallery.data.TimelineDateFormat
import com.iris.gallery.data.getSystemDefaultLocale
import com.iris.gallery.data.search.IndexingStatus
import androidx.compose.material3.LinearProgressIndicator
import kotlin.math.roundToInt
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.iris.gallery.data.MediaRepository
import com.iris.gallery.data.MediaImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: SettingsState,
    preferences: SettingsPreferences,
    excludedFolders: Set<String> = emptySet(),
    availableAlbums: List<MediaAlbum> = emptyList(),
    onRemoveExcludedFolder: (String) -> Unit = {},
    onAddExcludedFolder: (String) -> Unit = {},
    onOpenAbout: () -> Unit = {},
    onRescanMedia: () -> Unit = {},
    indexingStatus: IndexingStatus = IndexingStatus(),
    indexedPhotosCount: Int = 0,
    onReindexPhotos: () -> Unit = {},
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var showResetDialog by remember { mutableStateOf(value = false) }
    var showReindexDialog by remember { mutableStateOf(value = false) }
    var showSetPinDialog by remember { mutableStateOf(value = false) }
    var showChangePinDialog by remember { mutableStateOf(value = false) }
    var showDisablePinDialog by remember { mutableStateOf(value = false) }
    var showLanguageDialog by remember { mutableStateOf(value = false) }
    var showDateFormatDialog by remember { mutableStateOf(value = false) }
    var showViewerHeaderDialog by remember { mutableStateOf(value = false) }
    var showTimePickerDialog by remember { mutableStateOf(value = false) }
    var showExcludedFoldersDialog by remember { mutableStateOf(value = false) }
    var showSecureSharingDialog by remember { mutableStateOf(value = false) }

    val coroutineScope = rememberCoroutineScope()
    val mapRepository = remember { OfflineMapRepository(context) }
    var isUsingCustomMap by remember { mutableStateOf(mapRepository.isUsingCustomMap()) }
    var isImportingMap by remember { mutableStateOf(value = false) }
    var mapImportProgress by remember { mutableStateOf<Float?>(null) }
    var mapErrorMessage by remember { mutableStateOf<String?>(null) }
    var showMapReindexDialog by remember { mutableStateOf(value = false) }
    var isReindexingMap by remember { mutableStateOf(value = false) }
    var mapReindexStatus by remember { mutableStateOf("") }
    var mapReindexCurrent by remember { mutableIntStateOf(0) }
    var mapReindexTotal by remember { mutableIntStateOf(0) }

    val mapCustomActiveMsg = stringResource(R.string.settings_map_custom_active)
    val mapFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isImportingMap = true
                mapImportProgress = 0f
                val result = mapRepository.importCustomPmTiles(uri) { progress ->
                    mapImportProgress = progress
                }
                result.onSuccess {
                    isImportingMap = false
                    mapImportProgress = null
                    isUsingCustomMap = mapRepository.isUsingCustomMap()
                    Toast.makeText(context, mapCustomActiveMsg, Toast.LENGTH_SHORT).show()
                }.onFailure { error ->
                    isImportingMap = false
                    mapImportProgress = null
                    mapErrorMessage = error.localizedMessage ?: "Import fehlgeschlagen"
                }
            }
        }
    }

    fun clearCache() {
        runCatching {
            context.cacheDir.deleteRecursively()
            context.externalCacheDir?.deleteRecursively()
        }
        Toast.makeText(context, context.applicationContext.getString(R.string.toast_cache_cleared), Toast.LENGTH_SHORT).show()
    }

    val (currentLangCode, _, currentLangNativeName, currentLangFlag) = remember(settings.language) {
        SUPPORTED_LANGUAGES.firstOrNull { (code) -> code == settings.language }
            ?: SUPPORTED_LANGUAGES.first()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.section_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { scaffoldPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
        // Section: Language & Region
        item {
            SettingsSectionHeader(Icons.Outlined.Language, stringResource(R.string.settings_section_language))
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                onClick = { showLanguageDialog = true },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.settings_language_title),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        val langLabel = if (currentLangCode.isEmpty()) {
                            val sysLocale = getSystemDefaultLocale(context)
                            "$currentLangFlag ${stringResource(R.string.settings_language_system_default)} (${sysLocale.getDisplayLanguage(sysLocale).replaceFirstChar { it.uppercase() }})"
                        } else {
                            "$currentLangFlag $currentLangNativeName"
                        }
                        Text(
                            text = langLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowForwardIos,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        // Section 1: Appearance & Theme
        item {
            SettingsSectionHeader(Icons.Outlined.Palette, stringResource(R.string.settings_section_appearance))
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(stringResource(R.string.settings_theme_mode), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.themeMode == mode,
                                onClick = { preferences.setThemeMode(mode) },
                                label = {
                                    Text(
                                        when (mode) {
                                            ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
                                            ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
                                            ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                    )
                                },
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_amoled_black_title),
                        subtitle = stringResource(R.string.settings_amoled_black_desc),
                        checked = settings.amoledBlack,
                    ) { preferences.setAmoledBlack(it) }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Material You dynamic color selector
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        SettingsSwitchRow(
                            title = stringResource(R.string.settings_material_you_title),
                            subtitle = stringResource(R.string.settings_material_you_desc),
                            checked = settings.accentColor == AccentColor.MATERIAL_YOU,
                        ) { isChecked ->
                            preferences.setAccentColor(if (isChecked) AccentColor.MATERIAL_YOU else AccentColor.IRIS)
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    }

                    Text(stringResource(R.string.settings_custom_accents), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        listOf(
                            AccentColor.IRIS to Brush.linearGradient(listOf(Color(0xFF8A4AF3), Color(0xFF6C5CE7))),
                            AccentColor.LAPIS_MESOPOTAMIA to Brush.linearGradient(listOf(Color(0xFF1976D2), Color(0xFF0288D1))),
                            AccentColor.EMERALD to Brush.linearGradient(listOf(Color(0xFF00897B), Color(0xFF26A69A))),
                            AccentColor.ISHTAR_AMBER to Brush.linearGradient(listOf(Color(0xFFF57C00), Color(0xFFFFA726))),
                            AccentColor.ROSE to Brush.linearGradient(listOf(Color(0xFFD81B60), Color(0xFFEC407A))),
                            AccentColor.MONOCHROME to Brush.linearGradient(listOf(Color(0xFF18181B), Color(0xFFA1A1AA))),
                        ).forEach { (accent, brush) ->
                            val isSelected = settings.accentColor == accent
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(brush)
                                    .clickable { preferences.setAccentColor(accent) }
                                    .then(
                                        if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                        else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), CircleShape),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Outlined.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Layout & Grid Customization
        item {
            SettingsSectionHeader(Icons.Outlined.GridView, stringResource(R.string.settings_section_layout))
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(stringResource(R.string.settings_corner_style), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    
                    // 2x2 grid for symmetrical, perfect alignment
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.cornerStyle == CornerStyle.SHARP,
                                onClick = { preferences.setCornerStyle(CornerStyle.SHARP) },
                                label = { Text(stringResource(R.string.settings_corner_sharp), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                            )
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.cornerStyle == CornerStyle.CLASSIC,
                                onClick = { preferences.setCornerStyle(CornerStyle.CLASSIC) },
                                label = { Text(stringResource(R.string.settings_corner_subtle), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.cornerStyle == CornerStyle.ROUNDED,
                                onClick = { preferences.setCornerStyle(CornerStyle.ROUNDED) },
                                label = { Text(stringResource(R.string.settings_corner_rounded), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                            )
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.cornerStyle == CornerStyle.SQUIRCLE,
                                onClick = { preferences.setCornerStyle(CornerStyle.SQUIRCLE) },
                                label = { Text(stringResource(R.string.settings_corner_pill), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Text(stringResource(R.string.settings_grid_spacing), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        GridSpacing.entries.forEach { spacing ->
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.gridSpacing == spacing,
                                onClick = { preferences.setGridSpacing(spacing) },
                                label = {
                                    Text(
                                        when (spacing) {
                                            GridSpacing.COMPACT -> stringResource(R.string.settings_spacing_compact)
                                            GridSpacing.STANDARD -> stringResource(R.string.settings_spacing_standard)
                                            GridSpacing.RELAXED -> stringResource(R.string.settings_spacing_relaxed)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                    )
                                },
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    val density = LocalDensity.current
                    val screenWidthDp = with(density) {
                        LocalWindowInfo.current.containerSize.width.toDp().value
                    }
                    val photoSpacingDp = settings.gridSpacing.dp.toFloat()
                    val photoAvailableWidth = (screenWidthDp - (2 * photoSpacingDp)).coerceAtLeast(100f)

                    // Dynamic slider boundaries for Photos
                    val minPhotoDp = maxOf(36f, (((photoAvailableWidth + photoSpacingDp) / 6.8f) - photoSpacingDp).coerceAtLeast(36f))
                    val maxPhotoDp = (photoAvailableWidth * 0.72f).coerceIn(160f, 320f)
                    val currentPhotoSize = settings.photoGridSize.coerceIn(minPhotoDp, maxPhotoDp)
                    val photoColumns = GridCalculations.calculatePhotoColumns(
                        screenWidthDp = screenWidthDp,
                        cellSizeDp = currentPhotoSize,
                        gridSpacingDp = photoSpacingDp,
                        density = density,
                    )

                    // Dynamic slider boundaries for Albums
                    val albumSpacingDp = (settings.gridSpacing.dp + 8).toFloat()
                    val albumPaddingDp = 12f
                    val albumAvailableWidth = (screenWidthDp - (2 * albumPaddingDp)).coerceAtLeast(100f)
                    val minAlbumDp = maxOf(48f, (((albumAvailableWidth + albumSpacingDp) / 4.8f) - albumSpacingDp).coerceAtLeast(48f))
                    val maxAlbumDp = (albumAvailableWidth * 0.85f).coerceIn(200f, 420f)
                    val currentAlbumSize = settings.albumGridSize.coerceIn(minAlbumDp, maxAlbumDp)
                    val albumColumns = GridCalculations.calculateAlbumColumns(
                        screenWidthDp = screenWidthDp,
                        cellSizeDp = currentAlbumSize,
                        gridSpacingDp = settings.gridSpacing.dp.toFloat(),
                        density = density,
                    )

                    val colorScheme = MaterialTheme.colorScheme
                    val materialYouTiles = listOf(
                        Triple(
                            listOf(colorScheme.primaryContainer, colorScheme.primary.copy(alpha = 0.35f)),
                            colorScheme.onPrimaryContainer,
                            Icons.Outlined.PhotoCamera,
                        ),
                        Triple(
                            listOf(colorScheme.secondaryContainer, colorScheme.secondary.copy(alpha = 0.3f)),
                            colorScheme.onSecondaryContainer,
                            Icons.Outlined.Landscape,
                        ),
                        Triple(
                            listOf(colorScheme.tertiaryContainer, colorScheme.tertiary.copy(alpha = 0.35f)),
                            colorScheme.onTertiaryContainer,
                            Icons.Outlined.Videocam,
                        ),
                        Triple(
                            listOf(colorScheme.surfaceVariant, colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                            colorScheme.onSurfaceVariant,
                            Icons.Outlined.AutoAwesome,
                        ),
                        Triple(
                            listOf(colorScheme.inversePrimary.copy(alpha = 0.45f), colorScheme.primaryContainer),
                            colorScheme.primary,
                            Icons.Outlined.WbSunny,
                        ),
                        Triple(
                            listOf(colorScheme.secondary.copy(alpha = 0.25f), colorScheme.tertiaryContainer),
                            colorScheme.onSecondaryContainer,
                            Icons.Outlined.Folder,
                        ),
                        Triple(
                            listOf(colorScheme.tertiary.copy(alpha = 0.25f), colorScheme.primaryContainer),
                            colorScheme.onTertiaryContainer,
                            Icons.Outlined.Image,
                        ),
                        Triple(
                            listOf(colorScheme.primary.copy(alpha = 0.2f), colorScheme.secondaryContainer),
                            colorScheme.onPrimaryContainer,
                            Icons.Outlined.PhotoAlbum,
                        ),
                    )

                    // Photos Grid Tile Size & Live Preview
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.settings_photos_tile_size), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

                        // Scaled Live Photos Grid Mockup (Dynamic Height, Full Width Fill)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .animateContentSize(),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            val spacing = settings.gridSpacing.dp.dp
                            val numRows = if (photoColumns <= 3) 1 else 2

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(spacing),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                for (row in 0 until numRows) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(spacing),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        for (col in 0 until photoColumns) {
                                            val index = ((row * photoColumns) + col) % materialYouTiles.size
                                            val (gradient, tint, icon) = materialYouTiles[index]
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .aspectRatio(1f)
                                                    .clip(RoundedCornerShape(settings.cornerStyle.dp.dp))
                                                    .background(Brush.linearGradient(gradient)),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(
                                                    icon,
                                                    contentDescription = null,
                                                    tint = tint.copy(alpha = 0.45f),
                                                    modifier = Modifier.size(
                                                        when {
                                                            photoColumns >= 6 -> 11.dp
                                                            photoColumns >= 5 -> 13.dp
                                                            photoColumns >= 4 -> 15.dp
                                                            photoColumns >= 3 -> 18.dp
                                                            else -> 26.dp
                                                        },
                                                    ),
                                                )
                                                when (index) {
                                                    0 -> {
                                                        if (settings.showVideoDurationBadge) {
                                                            Surface(
                                                                color = Color.Black.copy(alpha = 0.65f),
                                                                shape = RoundedCornerShape(3.dp),
                                                                modifier = Modifier.align(Alignment.BottomStart).padding(if (photoColumns <= 2) 4.dp else 2.dp),
                                                            ) {
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp),
                                                                ) {
                                                                    Icon(
                                                                        Icons.Outlined.PlayArrow,
                                                                        contentDescription = null,
                                                                        tint = Color.White,
                                                                        modifier = Modifier.size(if (photoColumns <= 2) 10.dp else 6.5.dp),
                                                                    )
                                                                    Text("0:24", color = Color.White, fontSize = if (photoColumns <= 2) 9.sp else 6.5.sp, fontWeight = FontWeight.Bold)
                                                                }
                                                            }
                                                        }
                                                    }
                                                    1 -> {
                                                        if (settings.showMediaFormatBadge) {
                                                            Surface(
                                                                color = Color.Black.copy(alpha = 0.65f),
                                                                shape = RoundedCornerShape(3.dp),
                                                                modifier = Modifier.align(Alignment.BottomStart).padding(if (photoColumns <= 2) 4.dp else 2.dp),
                                                            ) {
                                                                Text("RAW", color = Color.White, fontSize = if (photoColumns <= 2) 9.sp else 6.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp))
                                                            }
                                                        }
                                                    }
                                                    2 -> {
                                                        Icon(
                                                            Icons.Filled.Favorite,
                                                            contentDescription = null,
                                                            tint = tint.copy(alpha = 0.9f),
                                                            modifier = Modifier
                                                                .align(Alignment.TopEnd)
                                                                .padding(if (photoColumns <= 2) 5.dp else 2.5.dp)
                                                                .size(if (photoColumns <= 2) 14.dp else 8.dp),
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Column Presets for Photos
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            listOf(2, 3, 4, 5, 6).forEach { targetCols ->
                                val isCurrentCol = photoColumns == targetCols
                                FilterChip(
                                    modifier = Modifier.weight(1f),
                                    selected = isCurrentCol,
                                    onClick = {
                                        val idealDp = GridCalculations.idealPhotoCellSizeForColumns(
                                            targetCols = targetCols,
                                            screenWidthDp = screenWidthDp,
                                            gridSpacingDp = photoSpacingDp,
                                            density = density,
                                        ).coerceIn(minPhotoDp, maxPhotoDp)
                                        preferences.setPhotoGridSize(idealDp)
                                    },
                                    label = {
                                        Text(
                                            targetCols.toString(),
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Center,
                                            fontSize = 12.sp,
                                            fontWeight = if (isCurrentCol) FontWeight.Bold else FontWeight.Medium,
                                        )
                                    },
                                    shape = CircleShape,
                                )
                            }
                        }

                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape,
                            ) {
                                Text(
                                    "${currentPhotoSize.roundToInt()} dp · $photoColumns ${if (photoColumns == 1) "col" else "cols"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                )
                            }
                        }

                        Slider(
                            value = currentPhotoSize,
                            onValueChange = { preferences.setPhotoGridSize(it) },
                            valueRange = minPhotoDp..maxPhotoDp,
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Albums Grid Tile Size & Live Preview
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.settings_albums_tile_size), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

                        // Compact Scaled Live Albums Grid Mockup
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(98.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                val sampleAlbums = listOf(
                                    Triple(stringResource(R.string.album_sample_camera), listOf(colorScheme.primaryContainer, colorScheme.primary.copy(alpha = 0.35f)), colorScheme.onPrimaryContainer),
                                    Triple(stringResource(R.string.album_sample_screenshots), listOf(colorScheme.secondaryContainer, colorScheme.secondary.copy(alpha = 0.3f)), colorScheme.onSecondaryContainer),
                                    Triple(stringResource(R.string.album_sample_downloads), listOf(colorScheme.tertiaryContainer, colorScheme.tertiary.copy(alpha = 0.35f)), colorScheme.onTertiaryContainer),
                                    Triple(stringResource(R.string.album_sample_wallpapers), listOf(colorScheme.surfaceVariant, colorScheme.surfaceVariant.copy(alpha = 0.6f)), colorScheme.onSurfaceVariant),
                                )
                                val countShown = minOf(albumColumns, sampleAlbums.size)
                                for (i in 0 until countShown) {
                                    val (albumName, albumGrad, albumTint) = sampleAlbums[i]
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f, matchHeightConstraintsFirst = true)
                                                .clip(RoundedCornerShape(settings.cornerStyle.dp.dp))
                                                .background(Brush.linearGradient(albumGrad)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                Icons.Outlined.Folder,
                                                contentDescription = null,
                                                tint = albumTint.copy(alpha = 0.5f),
                                                modifier = Modifier.size(if (albumColumns >= 3) 14.dp else 20.dp),
                                            )
                                        }
                                        Text(
                                            albumName,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = if (albumColumns >= 3) 8.sp else 9.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                }
                            }
                        }

                        // Column Presets for Albums
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            listOf(1, 2, 3, 4).forEach { targetCols ->
                                val isCurrentCol = albumColumns == targetCols
                                FilterChip(
                                    modifier = Modifier.weight(1f),
                                    selected = isCurrentCol,
                                    onClick = {
                                        val idealDp = GridCalculations.idealAlbumCellSizeForColumns(
                                            targetCols = targetCols,
                                            screenWidthDp = screenWidthDp,
                                            gridSpacingDp = settings.gridSpacing.dp.toFloat(),
                                            density = density,
                                        ).coerceIn(minAlbumDp, maxAlbumDp)
                                        preferences.setAlbumGridSize(idealDp)
                                    },
                                    label = {
                                        Text(
                                            targetCols.toString(),
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Center,
                                            fontSize = 12.sp,
                                            fontWeight = if (isCurrentCol) FontWeight.Bold else FontWeight.Medium,
                                        )
                                    },
                                    shape = CircleShape,
                                )
                            }
                        }

                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape,
                            ) {
                                Text(
                                    "${currentAlbumSize.roundToInt()} dp · $albumColumns ${if (albumColumns == 1) "col" else "cols"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                )
                            }
                        }

                        Slider(
                            value = currentAlbumSize,
                            onValueChange = { preferences.setAlbumGridSize(it) },
                            valueRange = minAlbumDp..maxAlbumDp,
                        )
                    }
                }
            }
        }

        // Section 3: Media Badges & Timeline
        item {
            SettingsSectionHeader(Icons.Outlined.ViewDay, stringResource(R.string.settings_section_badges))
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_show_timeline_title),
                        subtitle = stringResource(R.string.settings_show_timeline_desc),
                        checked = settings.showTimelineHeaders,
                        onCheckedChange = preferences::setShowTimelineHeaders,
                    )

                    AnimatedVisibility(
                        visible = settings.showTimelineHeaders,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showDateFormatDialog = true }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    Text(
                                        stringResource(R.string.settings_date_format_title),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        if (settings.timelineDateFormat == TimelineDateFormat.CUSTOM) {
                                            "${stringResource(R.string.settings_date_format_custom)} (${settings.customTimelineDateFormat})"
                                        } else {
                                            stringResource(settings.timelineDateFormat.getDisplayNameRes())
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                Icon(
                                    Icons.AutoMirrored.Outlined.ArrowForwardIos,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp),
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                            SettingsSwitchRow(
                                title = stringResource(R.string.settings_relative_dates_title),
                                subtitle = stringResource(R.string.settings_relative_dates_desc),
                                checked = settings.useRelativeDates,
                                onCheckedChange = preferences::setUseRelativeDates,
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                            SettingsSwitchRow(
                                title = stringResource(R.string.settings_show_day_of_week_title),
                                subtitle = stringResource(R.string.settings_show_day_of_week_desc),
                                checked = settings.showDayOfWeek,
                                onCheckedChange = preferences::setShowDayOfWeek,
                            )

                            AnimatedVisibility(
                                visible = settings.showDayOfWeek,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically(),
                            ) {
                                Column {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(bottom = 14.dp))

                                    SettingsSwitchRow(
                                        title = stringResource(R.string.settings_abbreviate_day_title),
                                        subtitle = stringResource(R.string.settings_abbreviate_day_desc),
                                        checked = settings.abbreviateDayOfWeek,
                                        onCheckedChange = preferences::setAbbreviateDayOfWeek,
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                            SettingsSwitchRow(
                                title = stringResource(R.string.settings_smart_year_title),
                                subtitle = stringResource(R.string.settings_smart_year_desc),
                                checked = settings.smartYearHiding,
                                onCheckedChange = preferences::setSmartYearHiding,
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_badge_duration_title),
                        subtitle = stringResource(R.string.settings_badge_duration_desc),
                        checked = settings.showVideoDurationBadge,
                        onCheckedChange = preferences::setShowVideoDurationBadge,
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_badge_formats_title),
                        subtitle = stringResource(R.string.settings_badge_formats_desc),
                        checked = settings.showMediaFormatBadge,
                        onCheckedChange = preferences::setShowMediaFormatBadge,
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_group_raw_photos_title),
                        subtitle = stringResource(R.string.settings_group_raw_photos_desc),
                        checked = settings.groupRawPhotos,
                        onCheckedChange = preferences::setGroupRawPhotos,
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_album_count_title),
                        subtitle = stringResource(R.string.settings_album_count_desc),
                        checked = settings.showAlbumCount,
                        onCheckedChange = preferences::setShowAlbumCount,
                    )
                }
            }
        }

        // Section 4: Viewer & Playback
        item {
            SettingsSectionHeader(Icons.Outlined.PlayCircle, stringResource(R.string.settings_section_viewer))
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showViewerHeaderDialog = true }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                stringResource(R.string.settings_viewer_header_title),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                stringResource(settings.viewerHeaderStyle.getDisplayNameRes()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowForwardIos,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_autoplay_video_title),
                        subtitle = stringResource(R.string.settings_autoplay_video_desc),
                        checked = settings.autoPlayVideo,
                        onCheckedChange = preferences::setAutoPlayVideo,
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_loop_video_title),
                        subtitle = stringResource(R.string.settings_loop_video_desc),
                        checked = settings.loopVideo,
                        onCheckedChange = preferences::setLoopVideo,
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_video_double_tap_zoom_title),
                        subtitle = stringResource(R.string.settings_video_double_tap_zoom_desc),
                        checked = settings.videoDoubleTapToZoom,
                        onCheckedChange = preferences::setVideoDoubleTapToZoom,
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_video_gesture_controls_title),
                        subtitle = stringResource(R.string.settings_video_gesture_controls_desc),
                        checked = settings.videoGestureControls,
                        onCheckedChange = preferences::setVideoGestureControls,
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_show_user_comments_title),
                        subtitle = stringResource(R.string.settings_show_user_comments_desc),
                        checked = settings.showViewerUserComments,
                        onCheckedChange = preferences::setShowViewerUserComments,
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_show_filmstrip_title),
                        subtitle = stringResource(R.string.settings_show_filmstrip_desc),
                        checked = settings.showFilmstrip,
                        onCheckedChange = preferences::setShowFilmstrip,
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_pinch_to_rotate_title),
                        subtitle = stringResource(R.string.settings_pinch_to_rotate_desc),
                        checked = settings.pinchToRotate,
                        onCheckedChange = preferences::setPinchToRotate,
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_multi_stage_zoom_title),
                        subtitle = stringResource(R.string.settings_multi_stage_zoom_desc),
                        checked = settings.multiStageZoom,
                        onCheckedChange = preferences::setMultiStageZoom,
                    )

                    AnimatedVisibility(
                        visible = !settings.multiStageZoom,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            
                            Text(stringResource(R.string.settings_zoom_scale_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                listOf(2.0f, 2.5f, 3.0f, 4.0f).forEach { zoom ->
                                    FilterChip(
                                        modifier = Modifier.weight(1f),
                                        selected = settings.doubleTapZoomLevel == zoom,
                                        onClick = { preferences.setDoubleTapZoomLevel(zoom) },
                                        label = { Text("${zoom}×", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                                    )
                                }
                                FilterChip(
                                    modifier = Modifier.weight(1f),
                                    selected = settings.doubleTapZoomLevel == -1.0f,
                                    onClick = { preferences.setDoubleTapZoomLevel(-1.0f) },
                                    label = { Text("1:1", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Memories & Notifications
        item {
            SettingsSectionHeader(Icons.Outlined.Notifications, stringResource(R.string.settings_section_memories))
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SettingsSwitchRow(
                        title = stringResource(R.string.setting_memories_notifications),
                        subtitle = stringResource(R.string.setting_memories_notifications_desc),
                        checked = settings.memoriesNotificationEnabled,
                    ) { enabled ->
                        preferences.setMemoriesNotificationEnabled(enabled)
                        com.iris.gallery.MemoriesNotifications.schedule(
                            context = context,
                            enabled = enabled,
                            hour = settings.memoriesNotificationHour,
                            minute = settings.memoriesNotificationMinute,
                        )
                    }

                    AnimatedVisibility(
                        visible = settings.memoriesNotificationEnabled,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        Column {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 6.dp))

                            val formattedTime = remember(settings.memoriesNotificationHour, settings.memoriesNotificationMinute) {
                                val cal = java.util.Calendar.getInstance().apply {
                                    set(java.util.Calendar.HOUR_OF_DAY, settings.memoriesNotificationHour)
                                    set(java.util.Calendar.MINUTE, settings.memoriesNotificationMinute)
                                }
                                java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(cal.time)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showTimePickerDialog = true }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                    Text(
                                        text = stringResource(R.string.setting_memories_time),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Text(
                                        text = stringResource(R.string.setting_memories_time_desc),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.clickable { showTimePickerDialog = true },
                                ) {
                                    Text(
                                        text = formattedTime,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 5: Startup & Security
        item {
            SettingsSectionHeader(Icons.Outlined.Dashboard, stringResource(R.string.settings_section_behavior))
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(stringResource(R.string.settings_navigation_tabs_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.settings_navigation_tabs_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.showMediaTab,
                                onClick = {
                                    val next = !settings.showMediaTab
                                    preferences.setShowMediaTab(next)
                                    if (!next && (settings.startupTab == StartupTab.MEDIA)) {
                                        val fallback = when {
                                            settings.showAlbumsTab -> StartupTab.ALBUMS
                                            settings.showFavoritesTab -> StartupTab.FAVORITES
                                            settings.showLibraryTab -> StartupTab.LIBRARY
                                            else -> StartupTab.MEDIA
                                        }
                                        preferences.setStartupTab(fallback)
                                    }
                                },
                                label = { Text(stringResource(R.string.tab_media), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                            )
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.showAlbumsTab,
                                onClick = {
                                    val next = !settings.showAlbumsTab
                                    preferences.setShowAlbumsTab(next)
                                    if (!next && (settings.startupTab == StartupTab.ALBUMS)) {
                                        val fallback = when {
                                            settings.showMediaTab -> StartupTab.MEDIA
                                            settings.showFavoritesTab -> StartupTab.FAVORITES
                                            settings.showLibraryTab -> StartupTab.LIBRARY
                                            else -> StartupTab.MEDIA
                                        }
                                        preferences.setStartupTab(fallback)
                                    }
                                },
                                label = { Text(stringResource(R.string.tab_albums), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.showFavoritesTab,
                                onClick = {
                                    val next = !settings.showFavoritesTab
                                    preferences.setShowFavoritesTab(next)
                                    if (!next && (settings.startupTab == StartupTab.FAVORITES)) {
                                        val fallback = when {
                                            settings.showMediaTab -> StartupTab.MEDIA
                                            settings.showAlbumsTab -> StartupTab.ALBUMS
                                            settings.showLibraryTab -> StartupTab.LIBRARY
                                            else -> StartupTab.MEDIA
                                        }
                                        preferences.setStartupTab(fallback)
                                    }
                                },
                                label = { Text(stringResource(R.string.tab_favorites), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                            )
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.showLibraryTab,
                                onClick = {
                                    val next = !settings.showLibraryTab
                                    preferences.setShowLibraryTab(next)
                                    if (!next && (settings.startupTab == StartupTab.LIBRARY)) {
                                        val fallback = when {
                                            settings.showMediaTab -> StartupTab.MEDIA
                                            settings.showAlbumsTab -> StartupTab.ALBUMS
                                            settings.showFavoritesTab -> StartupTab.FAVORITES
                                            else -> StartupTab.MEDIA
                                        }
                                        preferences.setStartupTab(fallback)
                                    }
                                },
                                label = { Text(stringResource(R.string.tab_library), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Text(stringResource(R.string.settings_startup_tab), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    
                    // 2x2 grid for symmetrical, perfect alignment
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.startupTab == StartupTab.MEDIA,
                                enabled = settings.showMediaTab,
                                onClick = { preferences.setStartupTab(StartupTab.MEDIA) },
                                label = { Text(stringResource(R.string.tab_media), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                            )
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.startupTab == StartupTab.ALBUMS,
                                enabled = settings.showAlbumsTab,
                                onClick = { preferences.setStartupTab(StartupTab.ALBUMS) },
                                label = { Text(stringResource(R.string.tab_albums), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.startupTab == StartupTab.FAVORITES,
                                enabled = settings.showFavoritesTab,
                                onClick = { preferences.setStartupTab(StartupTab.FAVORITES) },
                                label = { Text(stringResource(R.string.tab_favorites), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                            )
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.startupTab == StartupTab.LIBRARY,
                                enabled = settings.showLibraryTab,
                                onClick = { preferences.setStartupTab(StartupTab.LIBRARY) },
                                label = { Text(stringResource(R.string.tab_library), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_app_lock_title),
                        subtitle = stringResource(R.string.settings_app_lock_desc),
                        checked = settings.appLockEnabled && settings.hasPin,
                    ) { isChecked ->
                        if (isChecked) {
                            if (settings.hasPin) {
                                preferences.setAppLockEnabled(enabled = true)
                            } else {
                                showSetPinDialog = true
                            }
                        } else {
                            if (settings.hasPin) {
                                showDisablePinDialog = true
                            } else {
                                preferences.setAppLockEnabled(enabled = false)
                            }
                        }
                    }

                    if (settings.hasPin) {
                        OutlinedButton(
                            onClick = { showChangePinDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.settings_change_pin))
                        }

                        if (isBiometricHardwareAvailable(context)) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                            SettingsSwitchRow(
                                title = stringResource(R.string.settings_biometric_unlock_title),
                                subtitle = stringResource(R.string.settings_biometric_unlock_desc),
                                checked = settings.appLockBiometricsEnabled,
                                onCheckedChange = preferences::setAppLockBiometricsEnabled,
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_hide_vault_title),
                        subtitle = stringResource(R.string.settings_hide_vault_desc),
                        checked = settings.vaultHideFromStorage,
                        onCheckedChange = preferences::setVaultHideFromStorage,
                    )

                    if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) && settings.vaultHideFromStorage) {
                        val hasAllFiles = remember(context) { Environment.isExternalStorageManager() }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    stringResource(R.string.settings_silent_vault_title),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    if (hasAllFiles) stringResource(R.string.settings_silent_vault_desc_granted)
                                    else stringResource(R.string.settings_silent_vault_desc_prompt),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (hasAllFiles) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Outlined.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            } else {
                                Button(
                                    onClick = {
                                        val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                            data = "package:${context.packageName}".toUri()
                                        }
                                        runCatching { context.startActivity(intent) }
                                            .onFailure {
                                                runCatching {
                                                    context.startActivity(Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                                                }
                                            }
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                ) {
                                    Text(stringResource(R.string.action_grant), style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_vault_biometric_title),
                        subtitle = stringResource(R.string.settings_vault_biometric_desc),
                        checked = settings.biometricLockEnabled,
                        onCheckedChange = preferences::setBiometricLockEnabled,
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_confirm_delete_title),
                        subtitle = stringResource(R.string.settings_confirm_delete_desc),
                        checked = settings.confirmDelete,
                        onCheckedChange = preferences::setConfirmDelete,
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(R.string.settings_delete_mode_title),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            stringResource(R.string.settings_delete_mode_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.deleteMode == DeleteMode.TRASH,
                                onClick = { preferences.setDeleteMode(DeleteMode.TRASH) },
                                label = { Text(stringResource(R.string.settings_delete_mode_trash), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            )
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.deleteMode == DeleteMode.PERMANENT,
                                onClick = { preferences.setDeleteMode(DeleteMode.PERMANENT) },
                                label = { Text(stringResource(R.string.settings_delete_mode_permanent), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            )
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.deleteMode == DeleteMode.ALWAYS_ASK,
                                onClick = { preferences.setDeleteMode(DeleteMode.ALWAYS_ASK) },
                                label = { Text(stringResource(R.string.settings_delete_mode_always_ask), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            )
                        }
                    }

                    if (Build.VERSION.SDK_INT >= 30) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        SettingsSwitchRow(
                            title = stringResource(R.string.settings_system_trash_title),
                            subtitle = stringResource(R.string.settings_system_trash_desc),
                            checked = settings.useSystemTrash,
                            onCheckedChange = preferences::setUseSystemTrash,
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(R.string.settings_preferred_editor_title),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            stringResource(R.string.settings_preferred_editor_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.preferredEditor == PreferredEditor.ALWAYS_ASK,
                                onClick = { preferences.setPreferredEditor(PreferredEditor.ALWAYS_ASK) },
                                label = { Text(stringResource(R.string.settings_editor_always_ask), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                            )
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.preferredEditor == PreferredEditor.BUILT_IN,
                                onClick = { preferences.setPreferredEditor(PreferredEditor.BUILT_IN) },
                                label = { Text(stringResource(R.string.settings_editor_builtin), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                            )
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.preferredEditor == PreferredEditor.EXTERNAL,
                                onClick = { preferences.setPreferredEditor(PreferredEditor.EXTERNAL) },
                                label = { Text(stringResource(R.string.settings_editor_external), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_editor_overwrite_title),
                        subtitle = stringResource(R.string.settings_editor_overwrite_desc),
                        checked = settings.editorOverwriteOriginal,
                        onCheckedChange = preferences::setEditorOverwriteOriginal,
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showSecureSharingDialog = true }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f).padding(end = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    Icons.Outlined.Security,
                                    contentDescription = null,
                                    tint = if (settings.secureSharingMode != SecureSharingMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    stringResource(R.string.settings_secure_sharing_title),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                            Text(
                                stringResource(settings.secureSharingMode.getTitleRes()),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (settings.secureSharingMode != SecureSharingMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                stringResource(settings.secureSharingMode.getDescriptionRes()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowForwardIos,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }

        // Section: AI & Search / Künstliche Intelligenz (KI)
        item {
            SettingsSectionHeader(Icons.Outlined.AutoAwesome, stringResource(R.string.settings_section_ai))
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Master switch: AI Search
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_ai_search_title),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = stringResource(R.string.settings_ai_search_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = settings.aiSearchEnabled,
                            onCheckedChange = preferences::setAiSearchEnabled,
                        )
                    }

                    if (settings.aiSearchEnabled) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Switch: OCR
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.settings_ai_ocr_title),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = stringResource(R.string.settings_ai_ocr_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Switch(
                                checked = settings.aiOcrEnabled,
                                onCheckedChange = preferences::setAiOcrEnabled,
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Switch: Object & Scene Labeling
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.settings_ai_labeling_title),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = stringResource(R.string.settings_ai_labeling_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Switch(
                                checked = settings.aiLabelingEnabled,
                                onCheckedChange = preferences::setAiLabelingEnabled,
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Slider: Confidence Threshold
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_ai_confidence_title),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "${(settings.aiConfidenceThreshold * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Text(
                                text = stringResource(R.string.settings_ai_confidence_desc, (settings.aiConfidenceThreshold * 100).toInt()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(4.dp))
                            Slider(
                                value = settings.aiConfidenceThreshold,
                                onValueChange = { preferences.setAiConfidenceThreshold((it * 20).roundToInt() / 20f) },
                                valueRange = 0.50f..0.90f,
                                steps = 7,
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Switch: Show AI info in photo details sheet
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.settings_ai_info_in_details_title),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = stringResource(R.string.settings_ai_info_in_details_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Switch(
                                checked = settings.aiInfoInDetailsEnabled,
                                onCheckedChange = preferences::setAiInfoInDetailsEnabled,
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Status info: indexed count or indexing in progress
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            if (indexingStatus.isIndexing && (indexingStatus.totalCount > 0)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = stringResource(R.string.settings_ai_indexing_status, indexingStatus.indexedCount, indexingStatus.totalCount),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = {
                                        (indexingStatus.indexedCount.toFloat() / indexingStatus.totalCount.toFloat()).coerceIn(0f, 1f)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.settings_ai_indexed_status, indexedPhotosCount),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        // Reindex Button
                        OutlinedButton(
                            onClick = { showReindexDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Outlined.RestartAlt, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.settings_ai_reindex_title))
                        }
                    }
                }
            }
        }

        // Section: Offline Map
        item {
            SettingsSectionHeader(Icons.Outlined.Map, stringResource(R.string.settings_section_map))
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isUsingCustomMap) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.secondaryContainer,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (isUsingCustomMap) Icons.Outlined.Layers else Icons.Outlined.Map,
                                contentDescription = null,
                                tint = if (isUsingCustomMap) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(22.dp),
                            )
                        }

                        Column(Modifier.weight(1f)) {
                            Text(
                                text = if (isUsingCustomMap) stringResource(R.string.settings_map_custom_active)
                                else stringResource(R.string.settings_map_base_active),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = stringResource(R.string.settings_map_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.settings_map_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    AnimatedVisibility(visible = isImportingMap) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_map_importing),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Text(
                                    text = mapImportProgress?.let { "${(it * 100).toInt()}%" } ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            LinearProgressIndicator(
                                progress = { mapImportProgress ?: 0f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilledTonalButton(
                            onClick = { mapFilePickerLauncher.launch(arrayOf("*/*")) },
                            enabled = !isImportingMap,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(Icons.Outlined.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.settings_map_import_btn))
                        }

                        if (isUsingCustomMap) {
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        mapRepository.deleteCustomMap()
                                        isUsingCustomMap = mapRepository.isUsingCustomMap()
                                        Toast.makeText(context, "Auf Basiskarte zurückgesetzt", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = !isImportingMap,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            ) {
                                Icon(Icons.Outlined.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.settings_map_reset_btn))
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )

                    // Map Appearance / Theme Mode override
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.settings_map_theme_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    text = stringResource(R.string.settings_map_theme_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ThemeMode.entries.forEach { mode ->
                                FilterChip(
                                    modifier = Modifier.weight(1f),
                                    selected = settings.mapThemeMode == mode,
                                    onClick = { preferences.setMapThemeMode(mode) },
                                    label = {
                                        Text(
                                            when (mode) {
                                                ThemeMode.SYSTEM -> stringResource(R.string.settings_map_theme_auto)
                                                ThemeMode.LIGHT -> stringResource(R.string.settings_map_theme_light)
                                                ThemeMode.DARK -> stringResource(R.string.settings_map_theme_dark)
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Center,
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    },
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.settings_map_reindex_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    text = stringResource(R.string.settings_map_reindex_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        // Reindex Progress Bar
                        AnimatedVisibility(visible = isReindexingMap) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = mapReindexStatus,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        text = if (mapReindexTotal > 0) "$mapReindexCurrent / $mapReindexTotal" else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = {
                                        if (mapReindexTotal > 0) mapReindexCurrent.toFloat() / mapReindexTotal else 0f
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { showMapReindexDialog = true },
                            enabled = !isReindexingMap && !isImportingMap,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(Icons.Outlined.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.settings_map_reindex_title))
                        }
                    }
                }
            }
        }

        // Section 6: Cache & Reset
        item {
            SettingsSectionHeader(Icons.Outlined.CleaningServices, stringResource(R.string.settings_section_storage))
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onRescanMedia,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_rescan_media))
                    }

                    OutlinedButton(
                        onClick = { showExcludedFoldersDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.FolderOff, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.excluded_folders_title))
                    }

                    OutlinedButton(
                        onClick = ::clearCache,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.CleaningServices, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_clear_cache))
                    }

                    OutlinedButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.RestartAlt, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_reset_all))
                    }
                }
            }
        }

        // Section 7: About Iris Gallery
        item {
            SettingsSectionHeader(Icons.Outlined.Info, stringResource(R.string.library_about_title))
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                onClick = onOpenAbout,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            stringResource(R.string.library_about_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            stringResource(R.string.library_about_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowForwardIos,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
    }

    if (showSetPinDialog) {
        PinSetupBottomSheet(
            onDismiss = { showSetPinDialog = false },
        ) { pin ->
            preferences.setPin(pin)
            showSetPinDialog = false
            Toast.makeText(context, context.applicationContext.getString(R.string.toast_pin_enabled), Toast.LENGTH_SHORT).show()
        }
    }

    if (showChangePinDialog) {
        PinChangeBottomSheet(
            onDismiss = { showChangePinDialog = false },
            onVerifyOldPin = preferences::verifyPin,
        ) { pin ->
            preferences.setPin(pin)
            showChangePinDialog = false
            Toast.makeText(context, context.applicationContext.getString(R.string.toast_pin_changed), Toast.LENGTH_SHORT).show()
        }
    }

    if (showDisablePinDialog) {
        PinDisableBottomSheet(
            onDismiss = { showDisablePinDialog = false },
            onVerifyPin = preferences::verifyPin,
        ) {
            preferences.removePin()
            showDisablePinDialog = false
            Toast.makeText(context, context.applicationContext.getString(R.string.toast_pin_disabled), Toast.LENGTH_SHORT).show()
        }
    }

    if (showReindexDialog) {
        AlertDialog(
            onDismissRequest = { showReindexDialog = false },
            title = { Text(stringResource(R.string.settings_ai_reindex_dialog_title)) },
            text = { Text(stringResource(R.string.settings_ai_reindex_dialog_msg)) },
            confirmButton = {
                Button(
                    onClick = {
                        showReindexDialog = false
                        onReindexPhotos()
                        Toast.makeText(context, context.applicationContext.getString(R.string.settings_ai_reindex_started), Toast.LENGTH_SHORT).show()
                    },
                ) {
                    Text(stringResource(R.string.settings_ai_reindex_title))
                }
            },
            dismissButton = {
                TextButton(onClick = { showReindexDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    if (showMapReindexDialog) {
        val mapReindexSuccessPattern = stringResource(R.string.settings_map_reindex_success)
        AlertDialog(
            onDismissRequest = { showMapReindexDialog = false },
            title = { Text(stringResource(R.string.settings_map_reindex_dialog_title)) },
            text = { Text(stringResource(R.string.settings_map_reindex_dialog_msg)) },
            confirmButton = {
                Button(
                    onClick = {
                        showMapReindexDialog = false
                        coroutineScope.launch(Dispatchers.IO) {
                            isReindexingMap = true
                            try {
                                PhotoMapViewModel.clearGpsCache(context)
                                PhotoMarkerCache.clearAll(context)

                                val mediaRepo = MediaRepository(context)
                                val allImages = mediaRepo.loadImages()

                                mapReindexStatus = "Standorte werden gesucht…"
                                mapReindexTotal = allImages.size
                                mapReindexCurrent = 0

                                val geotaggedList = mutableListOf<MediaImage>()
                                allImages.forEachIndexed { index, img ->
                                    val coords = PhotoMapViewModel.extractGps(context, img.uri, img.path)
                                    if (coords != null) {
                                        PhotoMapViewModel.updateGpsEntry(img.id, coords.first, coords.second, img.dateModified)
                                        geotaggedList.add(img)
                                    } else {
                                        PhotoMapViewModel.updateGpsEntry(img.id, Double.NaN, Double.NaN, img.dateModified)
                                    }
                                    if (((index % 5) == 0) || (index == allImages.lastIndex)) {
                                        mapReindexCurrent = index + 1
                                    }
                                }
                                PhotoMapViewModel.persistGpsCache(context)

                                mapReindexStatus = "Vorschauen werden generiert…"
                                mapReindexTotal = geotaggedList.size
                                mapReindexCurrent = 0

                                geotaggedList.forEachIndexed { index, img ->
                                    val raw = loadPhotoThumbnailBitmap(context, img.uri.toString(), 200)
                                    if (raw != null) {
                                        val marker = createCircularMarkerBitmap(raw, sizePx = 136, borderWidthPx = 7f)
                                        PhotoMarkerCache.save(context, img.id, marker)
                                    }
                                    mapReindexCurrent = index + 1
                                }

                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        mapReindexSuccessPattern.format(geotaggedList.size),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Fehler beim Neuaufbau: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            } finally {
                                isReindexingMap = false
                            }
                        }
                    },
                ) {
                    Text(stringResource(R.string.settings_map_reindex_title))
                }
            },
            dismissButton = {
                TextButton(onClick = { showMapReindexDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.settings_reset_dialog_title)) },
            text = { Text(stringResource(R.string.settings_reset_dialog_desc)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        preferences.resetToDefaults()
                        showResetDialog = false
                        Toast.makeText(context, context.applicationContext.getString(R.string.toast_settings_reset), Toast.LENGTH_SHORT).show()
                    },
                ) {
                    Text(stringResource(R.string.settings_reset_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    if (showLanguageDialog) {
        LanguageSelectionBottomSheet(
            currentLanguageCode = settings.language,
            onDismiss = { showLanguageDialog = false },
        ) { code ->
            preferences.setLanguage(code)
            setAppLanguage(context, code)
        }
    }

    if (showDateFormatDialog) {
        DateFormatBottomSheet(
            currentFormat = settings.timelineDateFormat,
            customPattern = settings.customTimelineDateFormat,
            showDayOfWeek = settings.showDayOfWeek,
            abbreviateDayOfWeek = settings.abbreviateDayOfWeek,
            onDismiss = { showDateFormatDialog = false },
            onFormatSelected = preferences::setTimelineDateFormat,
            onCustomPatternChange = preferences::setCustomTimelineDateFormat,
        )
    }

    if (showViewerHeaderDialog) {
        ViewerHeaderBottomSheet(
            currentStyle = settings.viewerHeaderStyle,
            showPageCount = settings.showViewerPageCount,
            showTime = settings.showViewerTime,
            onDismiss = { showViewerHeaderDialog = false },
            onStyleSelected = preferences::setViewerHeaderStyle,
            onShowPageCountChange = preferences::setShowViewerPageCount,
            onShowTimeChange = preferences::setShowViewerTime,
        )
    }

    if (showTimePickerDialog) {
        val is24Hour = android.text.format.DateFormat.is24HourFormat(context)
        val timePickerState = rememberTimePickerState(
            initialHour = settings.memoriesNotificationHour,
            initialMinute = settings.memoriesNotificationMinute,
            is24Hour = is24Hour,
        )
        AlertDialog(
            onDismissRequest = { showTimePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        preferences.setMemoriesNotificationTime(timePickerState.hour, timePickerState.minute)
                        com.iris.gallery.MemoriesNotifications.schedule(
                            context = context,
                            enabled = settings.memoriesNotificationEnabled,
                            hour = timePickerState.hour,
                            minute = timePickerState.minute,
                        )
                        showTimePickerDialog = false
                    },
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePickerDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            title = { Text(stringResource(R.string.setting_memories_time)) },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    TimePicker(state = timePickerState)
                }
            },
        )
    }

    if (showSecureSharingDialog) {
        AlertDialog(
            onDismissRequest = { showSecureSharingDialog = false },
            icon = {
                Icon(
                    Icons.Outlined.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
            },
            title = { Text(stringResource(R.string.settings_secure_sharing_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.settings_secure_sharing_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    SecureSharingMode.entries.forEach { mode ->
                        val isSelected = settings.secureSharingMode == mode
                        val modeIcon = when (mode) {
                            SecureSharingMode.OFF -> Icons.Outlined.Share
                            SecureSharingMode.STRIP_LOCATION -> Icons.Outlined.LocationOff
                            SecureSharingMode.STRIP_ALL -> Icons.Outlined.Security
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    preferences.setSecureSharingMode(mode)
                                    showSecureSharingDialog = false
                                },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(
                                    modeIcon,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp),
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        stringResource(mode.getTitleRes()),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        stringResource(mode.getDescriptionRes()),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        preferences.setSecureSharingMode(mode)
                                        showSecureSharingDialog = false
                                    },
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showSecureSharingDialog = false },
                ) {
                    Text(stringResource(R.string.action_done_editing))
                }
            },
        )
    }

    if (showExcludedFoldersDialog) {
        ExcludedFoldersDialog(
            excludedFolders = excludedFolders,
            availableAlbums = availableAlbums,
            onRemoveExcludedFolder = onRemoveExcludedFolder,
            onAddExcludedFolder = onAddExcludedFolder,
        ) { showExcludedFoldersDialog = false }
    }

    if (mapErrorMessage != null) {
        AlertDialog(
            onDismissRequest = { mapErrorMessage = null },
            title = { Text(stringResource(R.string.settings_section_map)) },
            text = { Text(mapErrorMessage.orEmpty()) },
            confirmButton = {
                TextButton(onClick = { mapErrorMessage = null }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
        )
    }
}

private fun getPathFromTreeUri(uri: Uri): String? {
    val authority = uri.authority.orEmpty()
    val docId = try {
        DocumentsContract.getTreeDocumentId(uri)
    } catch (_: Exception) {
        try {
            DocumentsContract.getDocumentId(uri)
        } catch (_: Exception) {
            uri.lastPathSegment.orEmpty()
        }
    }
    if (docId.isBlank()) return null
    if ((authority == "com.android.externalstorage.documents") || authority.endsWith(".documents")) {
        val split = docId.split(":")
        val type = split.getOrNull(0).orEmpty()
        val relPath = if (split.size > 1) split[1].removePrefix("/") else ""
        return if (type.equals("primary", ignoreCase = true)) {
            if (relPath.isBlank()) "/storage/emulated/0" else "/storage/emulated/0/$relPath"
        } else if (type.isNotBlank()) {
            if (relPath.isBlank()) "/storage/$type" else "/storage/$type/$relPath"
        } else {
            relPath
        }
    }
    if (docId.startsWith("raw:")) {
        return docId.removePrefix("raw:")
    }
    if (docId.startsWith("/")) {
        return docId
    }
    return null
}

@Composable
fun ExcludedFoldersDialog(
    excludedFolders: Set<String>,
    availableAlbums: List<MediaAlbum> = emptyList(),
    onRemoveExcludedFolder: (String) -> Unit,
    onAddExcludedFolder: (String) -> Unit = {},
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    var showChooserDialog by remember { mutableStateOf(value = false) }
    var showAlbumPickerDialog by remember { mutableStateOf(value = false) }

    val openDocumentTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri: Uri? ->
        if (uri != null) {
            val resolved = getPathFromTreeUri(uri)
            if (!resolved.isNullOrBlank()) {
                val clean = resolved.trimEnd('/')
                if (clean in excludedFolders) {
                    Toast.makeText(context, R.string.dialog_folder_already_excluded, Toast.LENGTH_SHORT).show()
                } else {
                    onAddExcludedFolder(clean)
                    Toast.makeText(context, R.string.toast_folder_excluded, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.excluded_folders_title)) },
        text = {
            if (excludedFolders.isEmpty()) {
                Text(stringResource(R.string.no_excluded_folders), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                ) {
                    items(excludedFolders.toList()) { folderPath ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(folderPath.substringAfterLast('/'), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(folderPath, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(
                                onClick = {
                                    onRemoveExcludedFolder(folderPath)
                                    Toast.makeText(context, R.string.toast_folder_unexcluded, Toast.LENGTH_SHORT).show()
                                },
                            ) {
                                Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { showChooserDialog = true }) {
                Icon(Icons.Outlined.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.action_add_excluded_folder))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.action_done_editing))
            }
        },
    )

    if (showChooserDialog) {
        AlertDialog(
            onDismissRequest = { showChooserDialog = false },
            title = { Text(stringResource(R.string.dialog_add_excluded_folder_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showChooserDialog = false
                                showAlbumPickerDialog = true
                            },
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                Icons.Outlined.PhotoAlbum,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    stringResource(R.string.exclude_option_choose_album),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    stringResource(R.string.exclude_option_choose_album_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showChooserDialog = false
                                openDocumentTreeLauncher.launch(null)
                            },
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                Icons.Outlined.FolderOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    stringResource(R.string.exclude_option_system_picker),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    stringResource(R.string.exclude_option_system_picker_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showChooserDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (showAlbumPickerDialog) {
        AlertDialog(
            onDismissRequest = { showAlbumPickerDialog = false },
            title = { Text(stringResource(R.string.dialog_choose_album_to_exclude_title)) },
            text = {
                if (availableAlbums.isEmpty()) {
                    Text(
                        stringResource(R.string.no_albums_to_exclude),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp),
                    ) {
                        items(availableAlbums, key = MediaAlbum::id) { album ->
                            val folderPath = remember(album) {
                                album.images.firstOrNull { (_, _, path) -> path.isNotBlank() }?.let { (_, _, path) -> File(path).parent }
                                    ?: File(album.cover.path).parent
                                    ?: ""
                            }
                            val isAlreadyExcluded = remember(folderPath, excludedFolders) {
                                folderPath.isNotBlank() && (folderPath in excludedFolders)
                            }

                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isAlreadyExcluded)
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isAlreadyExcluded) {
                                        if (folderPath.isNotBlank()) {
                                            val clean = folderPath.trimEnd('/')
                                            onAddExcludedFolder(clean)
                                            Toast.makeText(context, R.string.toast_folder_excluded, Toast.LENGTH_SHORT).show()
                                            showAlbumPickerDialog = false
                                        }
                                    },
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Card(
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.size(52.dp),
                                    ) {
                                        MediaThumbnail(album.cover, Modifier.fillMaxSize())
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = album.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = if (isAlreadyExcluded) {
                                                stringResource(R.string.dialog_folder_already_excluded)
                                            } else {
                                                "${album.images.size} items • $folderPath"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isAlreadyExcluded)
                                                MaterialTheme.colorScheme.error
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAlbumPickerDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun SettingsSectionHeader(icon: ImageVector, title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

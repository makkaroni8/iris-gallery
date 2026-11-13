package com.iris.gallery.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.util.LruCache
import android.util.Size
import java.io.File
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.net.ConnectivityReceiver
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonOptions
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

private const val PHOTO_SOURCE_ID = "iris_photo_source"
private const val CLUSTER_LAYER_ID = "iris_photo_cluster_layer"
private const val CLUSTER_SYMBOL_LAYER_ID = "iris_photo_cluster_symbol_layer"
private const val CLUSTER_BADGE_LAYER_ID = "iris_photo_cluster_badge_layer"
private const val UNCLUSTERED_LAYER_ID = "iris_photo_unclustered_layer"
private const val PHOTO_SYMBOL_LAYER_ID = "iris_photo_symbol_layer"

/**
 * Saver to persist MapLibre camera position across configuration changes (e.g. rotation).
 */
private val CameraPositionSaver: androidx.compose.runtime.saveable.Saver<CameraPosition?, Any> = mapSaver(
    save = { pos ->
        if (pos == null) {
            emptyMap<String, Any>()
        } else {
            mapOf(
                "lat" to (pos.target?.latitude ?: 0.0),
                "lng" to (pos.target?.longitude ?: 0.0),
                "zoom" to pos.zoom,
                "tilt" to pos.tilt,
                "bearing" to pos.bearing,
            )
        }
    },
) { map ->
        if (map.isEmpty()) {
            null
        } else {
            val lat = (map["lat"] as? Double) ?: 0.0
            val lng = (map["lng"] as? Double) ?: 0.0
            val zoom = (map["zoom"] as? Double) ?: 1.0
            val tilt = (map["tilt"] as? Double) ?: 0.0
            val bearing = (map["bearing"] as? Double) ?: 0.0
            CameraPosition.Builder()
                .target(LatLng(lat, lng))
                .zoom(zoom)
                .tilt(tilt)
                .bearing(bearing)
                .build()
        }
    }

/**
 * 100% Offline Jetpack Compose screen showing geotagged photos on a local MapLibre PMTiles map.
 * Displays large, high-visibility photo thumbnails for both individual photos and clusters!
 */
@Composable
fun OfflinePhotoMapScreen(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(0.dp),
    viewModel: PhotoMapViewModel = viewModel(),
    isDark: Boolean = isSystemInDarkTheme(),
    onPhotoClick: (photoId: Long) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentOnPhotoClick by rememberUpdatedState(onPhotoClick)

    // Synchronize map style with app/system light vs dark theme
    LaunchedEffect(isDark) {
        viewModel.refreshStyle(isDark)
    }

    // Convert photos into GeoJSON FeatureCollection on Dispatchers.Default
    val featureCollection by produceState(
        initialValue = FeatureCollection.fromFeatures(emptyList()),
        uiState.photos,
    ) {
        value = withContext(Dispatchers.Default) {
            val features = uiState.photos.map { (id, imageUri, latitude, longitude) ->
                val point = Point.fromLngLat(longitude, latitude)
                val feature = Feature.fromGeometry(point)
                feature.addNumberProperty("id", id)
                feature.addNumberProperty("lead_photo_id", id)
                feature.addStringProperty("imageUri", imageUri)
                feature.addStringProperty("icon_id", "photo_$id")
                feature
            }
            FeatureCollection.fromFeatures(features)
        }
    }

    var savedCameraPosition by rememberSaveable(stateSaver = CameraPositionSaver) {
        mutableStateOf(null)
    }

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var mapLibreMapRef by remember { mutableStateOf<MapLibreMap?>(null) }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, mapViewRef) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    mapViewRef?.onStart()
                    MapLibre.setConnected(false)
                    try {
                        ConnectivityReceiver.instance(context).deactivate()
                    } catch (_: Exception) {}
                }
                Lifecycle.Event.ON_RESUME -> {
                    mapViewRef?.onResume()
                    MapLibre.setConnected(false)
                }
                Lifecycle.Event.ON_PAUSE -> {
                    mapLibreMapRef?.cameraPosition?.let { savedCameraPosition = it }
                    mapViewRef?.onPause()
                }
                Lifecycle.Event.ON_STOP -> mapViewRef?.onStop()
                Lifecycle.Event.ON_DESTROY -> mapViewRef?.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            mapLibreMapRef?.cameraPosition?.let { savedCameraPosition = it }
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var isGeneratingThumbnails by remember { mutableStateOf(value = false) }
    var loadedThumbnailsCount by remember { mutableIntStateOf(0) }
    var totalThumbnailsToLoad by remember { mutableIntStateOf(0) }

    // Asynchronously decode and prepare circular photo marker bitmaps into persistent cache
    LaunchedEffect(uiState.photos) {
        val photos = uiState.photos
        if (photos.isEmpty()) {
            isGeneratingThumbnails = false
            return@LaunchedEffect
        }

        withContext(Dispatchers.IO) {
            // Find photos that haven't been generated yet (neither in memory nor on disk)
            val ungeneratedPhotos = photos.filter { (id) -> !PhotoMarkerCache.hasMarker(context, id) }

            // Ensure any already-generated markers on disk are warmed into memory
            photos.forEach { (id) ->
                if (PhotoMarkerCache.memoryCache[id] == null) {
                    PhotoMarkerCache.getOrLoad(context, id)
                }
            }

            // Only show progress if there are genuinely ungenerated markers to create!
            if (ungeneratedPhotos.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    isGeneratingThumbnails = true
                    loadedThumbnailsCount = 0
                    totalThumbnailsToLoad = ungeneratedPhotos.size
                }

                ungeneratedPhotos.forEach { (id, imageUri) ->
                    val rawBmp = loadPhotoThumbnailBitmap(context, imageUri, targetSize = 200)
                    if (rawBmp != null) {
                        val markerBmp = createCircularMarkerBitmap(rawBmp, sizePx = 136, borderWidthPx = 7f)
                        PhotoMarkerCache.save(context, id, markerBmp)
                    }
                    withContext(Dispatchers.Main) {
                        loadedThumbnailsCount++
                    }
                }

                withContext(Dispatchers.Main) {
                    isGeneratingThumbnails = false
                }
            } else {
                withContext(Dispatchers.Main) {
                    isGeneratingThumbnails = false
                    totalThumbnailsToLoad = 0
                    loadedThumbnailsCount = 0
                }
            }
        }
    }

    // Synchronize cached marker bitmaps into MapLibre style as soon as map and style are ready
    LaunchedEffect(uiState.photos, mapLibreMapRef, uiState.styleUri, loadedThumbnailsCount) {
        val map = mapLibreMapRef ?: return@LaunchedEffect
        val style = map.style
        if ((style != null) && style.isFullyLoaded) {
            withContext(Dispatchers.IO) {
                uiState.photos.forEach { (id) ->
                    val iconId = "photo_$id"
                    val markerBmp = PhotoMarkerCache.getOrLoad(context, id)
                    if (markerBmp != null) {
                        withContext(Dispatchers.Main) {
                            if (style.getImage(iconId) == null) {
                                style.addImage(iconId, markerBmp)
                            }
                        }
                    }
                }
            }
        }
    }

    val mapBgColorInt = if (isDark) {
        "#16191f".toColorInt()
    } else {
        "#d8e2eb".toColorInt()
    }
    val mapBgColorCompose = if (isDark) {
        androidx.compose.ui.graphics.Color(0xFF16191F)
    } else {
        androidx.compose.ui.graphics.Color(0xFFD8E2EB)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(padding)
            .background(mapBgColorCompose),
    ) {
        // MapLibre MapView encapsulated in AndroidView
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .background(mapBgColorCompose),
            factory = { ctx ->
                // CRITICAL: Call MapLibre.getInstance BEFORE creating MapView
                MapLibre.getInstance(ctx)
                MapLibre.setConnected(false)
                try {
                    ConnectivityReceiver.instance(ctx).deactivate()
                } catch (_: Exception) {}

                val options = MapLibreMapOptions.createFromAttributes(ctx)
                    .textureMode(true)
                    .foregroundLoadColor(mapBgColorInt)

                MapView(ctx, options).apply {
                    setBackgroundColor(mapBgColorInt)
                    mapViewRef = this
                    onCreate(null)
                    getMapAsync { map ->
                        mapLibreMapRef = map

                        map.uiSettings.isAttributionEnabled = false
                        map.uiSettings.isLogoEnabled = false

                        // Restore saved camera position
                        savedCameraPosition?.let { pos ->
                            map.cameraPosition = pos
                        }

                        // Track camera changes
                        map.addOnCameraIdleListener {
                            savedCameraPosition = map.cameraPosition
                        }

                        // Register OnMapClickListener ONCE
                        map.addOnMapClickListener { point ->
                            val screenPoint = map.projection.toScreenLocation(point)

                            // 1. Check for cluster click (on preview photo, badge, or base ring)
                            val clusterFeatures = map.queryRenderedFeatures(
                                screenPoint,
                                CLUSTER_BADGE_LAYER_ID,
                                CLUSTER_SYMBOL_LAYER_ID,
                                CLUSTER_LAYER_ID,
                            )
                            if (clusterFeatures.isNotEmpty()) {
                                val clusterFeature = clusterFeatures[0]
                                val pointGeometry = clusterFeature.geometry() as? Point
                                val targetLatLng = pointGeometry?.let {
                                    LatLng(it.latitude(), it.longitude())
                                } ?: point
                                val currentZoom = map.cameraPosition.zoom
                                map.animateCamera(
                                    CameraUpdateFactory.newLatLngZoom(targetLatLng, currentZoom + 2.0),
                                    350,
                                )
                                return@addOnMapClickListener true
                            }

                            // 2. Check for single photo click
                            val photoFeatures = map.queryRenderedFeatures(
                                screenPoint,
                                PHOTO_SYMBOL_LAYER_ID,
                                UNCLUSTERED_LAYER_ID,
                            )
                            if (photoFeatures.isNotEmpty()) {
                                val photoFeature = photoFeatures[0]
                                val photoId = photoFeature.getNumberProperty("id")?.toLong()
                                    ?: photoFeature.getStringProperty("id")?.toLongOrNull()
                                if (photoId != null) {
                                    currentOnPhotoClick(photoId)
                                    return@addOnMapClickListener true
                                }
                            }
                            false
                        }

                        // Initial style load if already available
                        val initialStyleUri = uiState.styleUri
                        if (!initialStyleUri.isNullOrBlank()) {
                            tag = initialStyleUri
                            map.setStyle(Style.Builder().fromUri(initialStyleUri)) { style ->
                                setupPhotoLayers(ctx, style, featureCollection, uiState.photos)
                            }
                        }
                    }
                }
            },
        ) { mapView ->
            mapView.setBackgroundColor(mapBgColorInt)
            val map = mapLibreMapRef ?: return@AndroidView
            val lastLoadedStyleUri = mapView.tag as? String

            val styleUri = uiState.styleUri
            if ((styleUri != null) && (styleUri != lastLoadedStyleUri)) {
                // Style URI changed -> Load new style
                mapView.tag = styleUri
                map.setStyle(Style.Builder().fromUri(styleUri)) { style ->
                    setupPhotoLayers(mapView.context, style, featureCollection, uiState.photos)
                }
            } else {
                // Style unchanged -> Only update existing GeoJsonSource
                val currentStyle = map.style
                if ((currentStyle != null) && currentStyle.isFullyLoaded) {
                    val source = currentStyle.getSourceAs<GeoJsonSource>(PHOTO_SOURCE_ID)
                    source?.setGeoJson(featureCollection)
                }
            }
        }

        // Top Bar & Live Progress Overlay
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            val isScanning = uiState.isScanningGps && (uiState.totalToScan > 0)
            val isGenerating = isGeneratingThumbnails && (totalThumbnailsToLoad > 0)
            val showProgress = isScanning || isGenerating

            // Center: Live Progress Pill (GPS scan + Thumbnail generation)
            AnimatedVisibility(
                visible = showProgress,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically(),
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    shadowElevation = 6.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = if (isScanning) {
                                    "Standorte suchen…"
                                } else {
                                    "Vorschauen laden…"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = if (isScanning) {
                                    "${uiState.scannedCount} / ${uiState.totalToScan}"
                                } else {
                                    "$loadedThumbnailsCount / $totalThumbnailsToLoad"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = {
                                if (isScanning) {
                                    uiState.scannedCount.toFloat() / uiState.totalToScan
                                } else if (totalThumbnailsToLoad > 0) {
                                    loadedThumbnailsCount.toFloat() / totalThumbnailsToLoad
                                } else {
                                    0f
                                }
                            },
                            modifier = Modifier
                                .width(180.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                        )
                    }
                }
            }

            // Right: Photo Count Badge
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shadowElevation = 4.dp,
                modifier = Modifier.align(Alignment.TopEnd),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (uiState.isUsingCustomMap) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.secondary
                                },
                            ),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${uiState.photos.size} Fotos",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        // Error Dialog
        if (uiState.errorMessage != null) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissError() },
                title = { Text("Kartenfehler") },
                text = { Text(uiState.errorMessage.orEmpty()) },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissError() }) {
                        Text("OK")
                    }
                },
            )
        }
    }
}

/**
 * Adds the clustered GeoJsonSource, CircleLayers, and Photo SymbolLayers:
 * - Single photos display large, high-res circular photo pins.
 * - Clusters display the lead photo preview with a circular count badge in the corner.
 */
@Suppress("UsePropertyAccessSyntax")
private fun setupPhotoLayers(
    context: Context,
    style: Style,
    featureCollection: FeatureCollection,
    photos: List<PhotoLocation>,
) {
    // Clean up existing layers & source if re-applying
    val layerIds = listOf(
        CLUSTER_BADGE_LAYER_ID,
        CLUSTER_SYMBOL_LAYER_ID,
        CLUSTER_LAYER_ID,
        PHOTO_SYMBOL_LAYER_ID,
        UNCLUSTERED_LAYER_ID,
    )
    for (layerId in layerIds) {
        if (style.getLayer(layerId) != null) {
            style.removeLayer(layerId)
        }
    }
    if (style.getSource(PHOTO_SOURCE_ID) != null) {
        style.removeSource(PHOTO_SOURCE_ID)
    }

    // Register count badges for clusters (2, 3, 4, 5, 6, 7, 8, 9, 10+, 25+, 50+, 99+)
    registerClusterBadgeIcons(style)

    // Populate any already cached photo marker bitmaps into the newly loaded style
    for ((id) in photos) {
        val iconId = "photo_$id"
        val cached = PhotoMarkerCache.getOrLoad(context, id)
        if ((cached != null) && (style.getImage(iconId) == null)) {
            style.addImage(iconId, cached)
        }
    }

    // Configure clustered GeoJsonSource with custom cluster property: "lead_photo_id"
    val geoJsonOptions = GeoJsonOptions()
        .withCluster(true)
        .withClusterMaxZoom(14)
        .withClusterRadius(55)
        .withClusterProperty(
            "lead_photo_id",
            Expression.max(Expression.accumulated(), Expression.get("lead_photo_id")),
            Expression.get("lead_photo_id"),
        )

    val photoSource = GeoJsonSource(PHOTO_SOURCE_ID, featureCollection, geoJsonOptions)
    style.addSource(photoSource)

    // 1. Cluster Base Ring Layer: colored halo / stacked album indicator
    @Suppress("UsePropertyAccessSyntax")
    val clusterBaseLayer = CircleLayer(CLUSTER_LAYER_ID, PHOTO_SOURCE_ID).apply {
        setFilter(Expression.has("point_count"))
        setProperties(
            PropertyFactory.circleColor(
                Expression.step(
                    Expression.get("point_count"),
                    Expression.color("#3B82F6".toColorInt()), // Blue (<10)
                    Expression.stop(10, Expression.color("#F59E0B".toColorInt())), // Amber (10-49)
                    Expression.stop(50, Expression.color("#EF4444".toColorInt())), // Red (>=50)
                ),
            ),
            PropertyFactory.circleRadius(
                Expression.step(
                    Expression.get("point_count"),
                    Expression.literal(24f),
                    Expression.stop(10, Expression.literal(28f)),
                    Expression.stop(50, Expression.literal(32f)),
                ),
            ),
            PropertyFactory.circleStrokeWidth(3f),
            PropertyFactory.circleStrokeColor(Color.WHITE),
            PropertyFactory.circleOpacity(0.95f),
        )
    }
    style.addLayer(clusterBaseLayer)

    // 2. Cluster Preview Photo Symbol Layer: displays the preview image of the cluster!
    val clusterSymbolLayer = SymbolLayer(CLUSTER_SYMBOL_LAYER_ID, PHOTO_SOURCE_ID).apply {
        setFilter(Expression.has("point_count"))
        setProperties(
            PropertyFactory.iconImage(
                Expression.concat(
                    Expression.literal("photo_"),
                    Expression.toString(Expression.get("lead_photo_id")),
                ),
            ),
            PropertyFactory.iconAllowOverlap(true),
            PropertyFactory.iconIgnorePlacement(true),
            PropertyFactory.iconSize(0.9f),
        )
    }
    style.addLayer(clusterSymbolLayer)

    // 3. Cluster Count Badge Layer: displays a badge in the top-right corner with the item count
    val clusterBadgeLayer = SymbolLayer(CLUSTER_BADGE_LAYER_ID, PHOTO_SOURCE_ID).apply {
        setFilter(Expression.has("point_count"))
        setProperties(
            PropertyFactory.iconImage(
                Expression.step(
                    Expression.get("point_count"),
                    Expression.literal("cluster_badge_2"),
                    Expression.stop(3, Expression.literal("cluster_badge_3")),
                    Expression.stop(4, Expression.literal("cluster_badge_4")),
                    Expression.stop(5, Expression.literal("cluster_badge_5")),
                    Expression.stop(6, Expression.literal("cluster_badge_6")),
                    Expression.stop(7, Expression.literal("cluster_badge_7")),
                    Expression.stop(8, Expression.literal("cluster_badge_8")),
                    Expression.stop(9, Expression.literal("cluster_badge_9")),
                    Expression.stop(10, Expression.literal("cluster_badge_10")),
                    Expression.stop(25, Expression.literal("cluster_badge_25")),
                    Expression.stop(50, Expression.literal("cluster_badge_50")),
                    Expression.stop(100, Expression.literal("cluster_badge_100")),
                ),
            ),
            PropertyFactory.iconOffset(arrayOf(26f, -26f)),
            PropertyFactory.iconAllowOverlap(true),
            PropertyFactory.iconIgnorePlacement(true),
            PropertyFactory.iconSize(1.0f),
        )
    }
    style.addLayer(clusterBadgeLayer)

    // 4. Unclustered Single Photo Layer: background circle marker
    val unclusteredLayer = CircleLayer(UNCLUSTERED_LAYER_ID, PHOTO_SOURCE_ID).apply {
        setFilter(Expression.not(Expression.has("point_count")))
        setProperties(
            PropertyFactory.circleColor("#8B5CF6".toColorInt()), // Violet
            PropertyFactory.circleRadius(16f),
            PropertyFactory.circleStrokeWidth(3.5f),
            PropertyFactory.circleStrokeColor(Color.WHITE),
            PropertyFactory.circleOpacity(1.0f),
        )
    }
    style.addLayer(unclusteredLayer)

    // 5. Unclustered Single Photo Symbol Layer: large, clear circular photo thumbnail
    val photoSymbolLayer = SymbolLayer(PHOTO_SYMBOL_LAYER_ID, PHOTO_SOURCE_ID).apply {
        setFilter(Expression.not(Expression.has("point_count")))
        setProperties(
            PropertyFactory.iconImage(Expression.get("icon_id")),
            PropertyFactory.iconAllowOverlap(true),
            PropertyFactory.iconIgnorePlacement(true),
            PropertyFactory.iconSize(0.95f),
        )
    }
    style.addLayer(photoSymbolLayer)
}

/**
 * Registers pre-rendered badge bitmaps for cluster counts without requiring any font PBFs.
 */
private fun registerClusterBadgeIcons(style: Style) {
    val blueColor = "#3B82F6".toColorInt()
    val amberColor = "#F59E0B".toColorInt()
    val redColor = "#EF4444".toColorInt()
    val badges = listOf(
        "cluster_badge_2" to ("2" to blueColor),
        "cluster_badge_3" to ("3" to blueColor),
        "cluster_badge_4" to ("4" to blueColor),
        "cluster_badge_5" to ("5" to blueColor),
        "cluster_badge_6" to ("6" to blueColor),
        "cluster_badge_7" to ("7" to blueColor),
        "cluster_badge_8" to ("8" to blueColor),
        "cluster_badge_9" to ("9" to blueColor),
        "cluster_badge_10" to ("10+" to amberColor),
        "cluster_badge_25" to ("25+" to amberColor),
        "cluster_badge_50" to ("50+" to redColor),
        "cluster_badge_100" to ("99+" to redColor),
    )
    for ((id, info) in badges) {
        if (style.getImage(id) == null) {
            style.addImage(id, createClusterCountBadgeBitmap(info.first, info.second))
        }
    }
}

/**
 * Creates a circular count badge bitmap using Android's native 2D Canvas (100% offline, no font PBF).
 */
private fun createClusterCountBadgeBitmap(
    countText: String,
    backgroundColor: Int,
    sizePx: Int = 54,
): Bitmap {
    val bitmap = createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = sizePx / 2f
    val radius = center - 2f

    // 1. Outer white border with drop shadow
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        setShadowLayer(3f, 0f, 1f, Color.argb(130, 0, 0, 0))
    }
    canvas.drawCircle(center, center, radius, borderPaint)

    // 2. Inner colored circle
    val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = backgroundColor
        style = Paint.Style.FILL
    }
    canvas.drawCircle(center, center, radius - 3.5f, circlePaint)

    // 3. Count text drawn using Android system fonts
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = if (countText.length > 2) sizePx * 0.38f else sizePx * 0.46f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    val fontMetrics = textPaint.fontMetrics
    val textY = center - ((fontMetrics.descent + fontMetrics.ascent) / 2f)
    canvas.drawText(countText, center, textY, textPaint)

    return bitmap
}

/**
 * Creates a circular photo marker bitmap with a crisp white border and drop shadow.
 */
internal fun createCircularMarkerBitmap(
    source: Bitmap,
    sizePx: Int = 136,
    borderWidthPx: Float = 7f,
): Bitmap {
    val output = createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)

    val center = sizePx / 2f
    val imageRadius = center - borderWidthPx

    // 1. Draw outer circle (white border) with shadow
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        setShadowLayer(5f, 0f, 2f, Color.argb(130, 0, 0, 0))
    }
    canvas.drawCircle(center, center, center - 2f, borderPaint)

    // 2. Scale & center-crop the source image into the circle
    val shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    val matrix = Matrix()
    val minDim = minOf(source.width, source.height).toFloat().coerceAtLeast(1f)
    val scale = (imageRadius * 2f) / minDim
    val dx = center - ((source.width * scale) / 2f)
    val dy = center - ((source.height * scale) / 2f)
    matrix.setScale(scale, scale)
    matrix.postTranslate(dx, dy)
    shader.setLocalMatrix(matrix)

    val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.shader = shader
    }
    canvas.drawCircle(center, center, imageRadius, imagePaint)

    // 3. Subtle inner stroke for photo border contrast
    val innerStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(45, 0, 0, 0)
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }
    canvas.drawCircle(center, center, imageRadius, innerStrokePaint)

    return output
}

/**
 * Decodes a downsampled thumbnail Bitmap for a photo URI.
 */
internal fun loadPhotoThumbnailBitmap(
    context: Context,
    uriString: String,
    targetSize: Int = 200,
): Bitmap? {
    val uri = runCatching { uriString.toUri() }.getOrNull() ?: return null

    // 1. On Android 10+ (API 29+), contentResolver.loadThumbnail uses the OS thumbnail cache
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        try {
            return context.contentResolver.loadThumbnail(uri, Size(targetSize, targetSize), null)
        } catch (_: Exception) {}
    }

    // 2. Fallback stream decode with downsampling
    return try {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOpts)
        }
        val sample = maxOf(boundsOpts.outWidth / targetSize, boundsOpts.outHeight / targetSize, 1)
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
        }
    } catch (_: Exception) {
        null
    }
}

/**
 * Singleton managing in-memory and persistent disk caching for circular map photo pins.
 * Ensures that photo markers are created exactly ONCE and reused across navigations and app restarts.
 */
object PhotoMarkerCache {
    val memoryCache = LruCache<Long, Bitmap>(400)

    private fun getMarkerDir(context: Context): File {
        val dir = File(context.filesDir, "map_markers")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun hasMarker(context: Context, photoId: Long): Boolean {
        if (memoryCache[photoId] != null) return true
        val file = File(getMarkerDir(context), "$photoId.png")
        return file.exists()
    }

    fun getOrLoad(context: Context, photoId: Long): Bitmap? {
        val mem = memoryCache[photoId]
        if (mem != null) return mem
        val file = File(getMarkerDir(context), "$photoId.png")
        if (file.exists()) {
            val bmp = BitmapFactory.decodeFile(file.absolutePath)
            if (bmp != null) {
                memoryCache.put(photoId, bmp)
                return bmp
            }
        }
        return null
    }

    fun save(context: Context, photoId: Long, bitmap: Bitmap) {
        memoryCache.put(photoId, bitmap)
        try {
            val dir = getMarkerDir(context)
            val file = File(dir, "$photoId.png")
            val tmp = File(dir, "$photoId.tmp")
            tmp.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            tmp.renameTo(file)
        } catch (_: Exception) {}
    }

    fun clearAll(context: Context) {
        memoryCache.evictAll()
        try {
            val dir = getMarkerDir(context)
            dir.listFiles()?.forEach { it.delete() }
        } catch (_: Exception) {}
    }
}

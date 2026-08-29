/*
 * Copyright 2025-2026 The FairScan authors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 */
package org.fairscan.app.ui.screens.document

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.toImmutableList
import net.engawapg.lib.zoomable.ZoomState
import net.engawapg.lib.zoomable.zoomable
import org.fairscan.app.R
import org.fairscan.app.domain.Jpeg
import org.fairscan.app.domain.PageViewKey
import org.fairscan.app.domain.Rotation
import org.fairscan.app.ui.Navigation
import org.fairscan.app.ui.components.AppOverflowMenu
import org.fairscan.app.ui.components.ConfirmationDialog
import org.fairscan.app.ui.components.MainActionButton
import org.fairscan.app.ui.dummyNavigation
import org.fairscan.app.ui.fakeDocument
import org.fairscan.app.ui.fakeImage
import org.fairscan.app.ui.theme.FairScanTheme
import org.fairscan.imageprocessing.ColorMode
import org.fairscan.imageprocessing.ColorMode.COLOR
import org.fairscan.imageprocessing.ColorMode.GRAYSCALE

enum class DocumentViewMode {
    GRID,
    SINGLE_PAGE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentScreen(
    uiState: DocumentUiState,
    navigation: Navigation,
    onExportClick: () -> Unit,
    onDeleteImage: () -> Unit,
    onRotateImage: (Boolean) -> Unit,
    onToggleColorMode: () -> Unit,
    onCropClick: () -> Unit,
    onPageReorder: (String, Int) -> Unit,
    onPageSelected: (Int) -> Unit,
    onShareSinglePagePdf: (Int) -> Unit = {},
) {
    val showDeletePageDialog = rememberSaveable { mutableStateOf(false) }
    var viewMode by rememberSaveable { mutableStateOf(DocumentViewMode.GRID) }

    val document = uiState.document
    val currentPageIndex = uiState.currentPageIndex
    val totalPages = document.pageCount()

    // Handle back press
    BackHandler {
        if (viewMode == DocumentViewMode.SINGLE_PAGE) {
            viewMode = DocumentViewMode.GRID
        } else {
            navigation.back()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (viewMode == DocumentViewMode.GRID) {
                        Text(
                            text = stringResource(R.string.document) + " ($totalPages)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = "${currentPageIndex + 1} / $totalPages",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (viewMode == DocumentViewMode.SINGLE_PAGE) {
                            viewMode = DocumentViewMode.GRID
                        } else {
                            navigation.back()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    if (viewMode == DocumentViewMode.SINGLE_PAGE) {
                        // Share single page as PDF
                        IconButton(onClick = { onShareSinglePagePdf(currentPageIndex) }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = stringResource(R.string.share),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        // Toggle to Grid view
                        IconButton(onClick = { viewMode = DocumentViewMode.GRID }) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = "Grid View"
                            )
                        }
                    }
                    AppOverflowMenu(navigation)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            DocumentBottomBar(
                onAddPageClick = navigation.toCameraScreen,
                onExportClick = onExportClick,
                pageCount = totalPages
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            AnimatedContent(
                targetState = viewMode,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ViewModeAnimation"
            ) { mode ->
                when (mode) {
                    DocumentViewMode.GRID -> {
                        AllPagesGridView(
                            uiState = uiState,
                            onPageClick = { index ->
                                onPageSelected(index)
                                viewMode = DocumentViewMode.SINGLE_PAGE
                            }
                        )
                    }
                    DocumentViewMode.SINGLE_PAGE -> {
                        SinglePageEditView(
                            uiState = uiState,
                            onDeleteClick = { showDeletePageDialog.value = true },
                            onRotateImage = onRotateImage,
                            onToggleColorMode = onToggleColorMode,
                            onCropClick = onCropClick,
                            onNextPage = {
                                if (currentPageIndex < totalPages - 1) {
                                    onPageSelected(currentPageIndex + 1)
                                }
                            },
                            onPreviousPage = {
                                if (currentPageIndex > 0) {
                                    onPageSelected(currentPageIndex - 1)
                                }
                            }
                        )
                    }
                }
            }

            if (showDeletePageDialog.value) {
                ConfirmationDialog(
                    title = stringResource(R.string.delete_page),
                    message = stringResource(R.string.delete_page_warning),
                    showDialog = showDeletePageDialog
                ) {
                    onDeleteImage()
                    if (document.pageCount() <= 1) {
                        navigation.back()
                    }
                }
            }
        }
    }
}

/**
 * 2-Column All Pages Grid View (vFlat style)
 */
@Composable
private fun AllPagesGridView(
    uiState: DocumentUiState,
    onPageClick: (Int) -> Unit,
) {
    val document = uiState.document
    val pageCount = document.pageCount()

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(pageCount) { index ->
            val thumbnail = document.thumbnail(index)
            val isSelected = index == uiState.currentPageIndex

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp),
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPageClick(index) }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.72f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        if (thumbnail != null) {
                            Image(
                                bitmap = thumbnail.asImageBitmap(),
                                contentDescription = "Page ${index + 1}",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Page number badge
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Single Page Edit View (vFlat style)
 */
@Composable
private fun SinglePageEditView(
    uiState: DocumentUiState,
    onDeleteClick: () -> Unit,
    onRotateImage: (Boolean) -> Unit,
    onToggleColorMode: () -> Unit,
    onCropClick: () -> Unit,
    onNextPage: () -> Unit,
    onPreviousPage: () -> Unit,
) {
    var dragAmount by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (dragAmount < -60f) {
                            onNextPage()
                        } else if (dragAmount > 60f) {
                            onPreviousPage()
                        }
                        dragAmount = 0f
                    },
                    onHorizontalDrag = { _, drag ->
                        dragAmount += drag
                    }
                )
            }
    ) {
        val bitmap = uiState.currentPage?.bitmap
        val pageKey = uiState.currentPage?.key

        if (bitmap != null && pageKey != null) {
            val imageBitmap = bitmap.asImageBitmap()
            val zoomState = remember(pageKey) {
                ZoomState(
                    contentSize = Size(bitmap.width.toFloat(), bitmap.height.toFloat())
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 60.dp)
                    .align(Alignment.Center)
            ) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center)
                        .zoomable(zoomState)
                )
            }
        }

        if (uiState.currentPage?.isLoading ?: false) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        // Floating Action Pill (vFlat Style Bar)
        VFlatActionPill(
            uiState = uiState,
            onToggleColorMode = onToggleColorMode,
            onCropClick = onCropClick,
            onRotateImage = onRotateImage,
            onDeleteClick = onDeleteClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}

/**
 * Modern floating action pill with Magic Wand, Crop, Rotate Left, Rotate Right, Trash
 */
@Composable
private fun VFlatActionPill(
    uiState: DocumentUiState,
    onToggleColorMode: () -> Unit,
    onCropClick: () -> Unit,
    onRotateImage: (Boolean) -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E242B).copy(alpha = 0.95f)
        ),
        border = BorderStroke(1.dp, Color(0xFF333E4A)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = modifier.shadow(12.dp, RoundedCornerShape(32.dp))
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Magic Wand / Color mode
                uiState.currentPage?.colorMode?.let {
                    ColorModeActionButton(
                        currentColorMode = it,
                        onToggle = onToggleColorMode
                    )
                }

                // Crop Tool
                if (uiState.currentPage?.canBeCropped ?: false) {
                    PillIconButton(
                        icon = Icons.Default.Crop,
                        contentDescription = stringResource(R.string.crop),
                        onClick = onCropClick
                    )
                }

                // Rotate CCW (Left)
                @Suppress("DEPRECATION")
                PillIconButton(
                    icon = Icons.Default.RotateLeft,
                    contentDescription = stringResource(R.string.rotate_left),
                    onClick = { onRotateImage(false) }
                )

                // Rotate CW (Right)
                @Suppress("DEPRECATION")
                PillIconButton(
                    icon = Icons.Default.RotateRight,
                    contentDescription = stringResource(R.string.rotate_right),
                    onClick = { onRotateImage(true) }
                )

                // Delete (Trash)
                PillIconButton(
                    icon = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.delete_page),
                    onClick = onDeleteClick,
                    tint = Color(0xFFFF6B6B)
                )
            }
        }
    }
}

@Composable
private fun PillIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    tint: Color = Color.White,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun ColorModeActionButton(
    currentColorMode: ColorMode,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.AutoFixHigh,
                contentDescription = stringResource(R.string.color_mode),
                tint = if (currentColorMode == COLOR) Color(0xFF4ECCA3) else Color.White,
                modifier = Modifier.size(22.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.color_mode_color)) },
                leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null) },
                onClick = {
                    if (currentColorMode != COLOR) onToggle()
                    expanded = false
                },
                trailingIcon = {
                    if (currentColorMode == COLOR) {
                        Icon(Icons.Default.Check, contentDescription = null)
                    }
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.color_mode_grayscale)) },
                leadingIcon = { Icon(Icons.Default.Contrast, contentDescription = null) },
                onClick = {
                    if (currentColorMode != GRAYSCALE) onToggle()
                    expanded = false
                },
                trailingIcon = {
                    if (currentColorMode == GRAYSCALE) {
                        Icon(Icons.Default.Check, contentDescription = null)
                    }
                }
            )
        }
    }
}

@Composable
private fun DocumentBottomBar(
    onAddPageClick: () -> Unit,
    onExportClick: () -> Unit,
    pageCount: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        OutlinedButton(
            onClick = onAddPageClick,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.add_page),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.width(16.dp))

        MainActionButton(
            onClick = onExportClick,
            enabled = pageCount > 0,
            icon = Icons.Default.Done,
            text = stringResource(R.string.export),
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
        )
    }
}

@Composable
@Preview
@Preview(locale = "tr")
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
fun DocumentScreenPreview() {
    FairScanTheme {
        val image = fakeImage("gallica.bnf.fr-bpt6k5530456s-1", LocalContext.current).toBitmap()
        val document = fakeDocument(
            listOf(1, 2, 3, 4).map { "gallica.bnf.fr-bpt6k5530456s-$it" }.toImmutableList(),
            LocalContext.current
        )
        val key = PageViewKey("123", Rotation.R0, null, 0)
        DocumentScreen(
            uiState = DocumentUiState(0, CurrentPageUiState(key, image, COLOR, true), document),
            navigation = dummyNavigation(),
            onExportClick = {},
            onDeleteImage = { },
            onRotateImage = { _ -> },
            onToggleColorMode = { },
            onCropClick = { },
            onPageReorder = { _, _ -> },
            onPageSelected = { _ -> },
            onShareSinglePagePdf = { _ -> }
        )
    }
}

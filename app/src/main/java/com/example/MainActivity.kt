package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.RestorePage
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.AppDatabase
import com.example.data.BarcodeGenerator
import com.example.data.CardEntity
import com.example.data.CardRepository
import com.example.ui.BarcodeAnalyzer
import com.example.ui.WalletViewModel
import com.example.ui.WalletViewModelFactory
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.InputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainWalletAppScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainWalletAppScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Database & Repository initialization locally
    val db = remember { AppDatabase.getDatabase(context) }
    val repository = remember { CardRepository(db.cardDao()) }
    val viewModel: WalletViewModel = viewModel(factory = WalletViewModelFactory(repository))

    // State bindings
    val cards by viewModel.cards.collectAsStateWithLifecycle()
    val editCardId by viewModel.editCardId.collectAsStateWithLifecycle()
    val nameInput by viewModel.nameInput.collectAsStateWithLifecycle()
    val codeInput by viewModel.codeInput.collectAsStateWithLifecycle()
    val formatInput by viewModel.formatInput.collectAsStateWithLifecycle()
    val logoUrl by viewModel.logoUrl.collectAsStateWithLifecycle()
    val logoBase64 by viewModel.logoBase64.collectAsStateWithLifecycle()
    val selectedColorHex by viewModel.selectedColorHex.collectAsStateWithLifecycle()
    val logoSuggestions by viewModel.logoSuggestions.collectAsStateWithLifecycle()

    // Overlay control states
    var showCreatorOverlay by remember { mutableStateOf(false) }
    var showScannerOverlay by remember { mutableStateOf(false) }
    var selectedPresenterCard by remember { mutableStateOf<CardEntity?>(null) }
    var showUrlInputDialog by remember { mutableStateOf(false) }
    var cardToDelete by remember { mutableStateOf<CardEntity?>(null) }

    // Backup & Restore file picker launchers
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            val backupData = viewModel.exportBackup(context)
            if (backupData != null) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(backupData.toByteArray())
                    }
                    Toast.makeText(context, "Backup salvato con successo!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Errore salvataggio backup: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    viewModel.importBackup(inputStream) { success, count ->
                        if (success) {
                            Toast.makeText(context, "Ripristinate $count tessere con successo!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Errore nel formato del file backup", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Errore caricamento file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Media Picker for Custom Image Logo
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    // Fast crop center square before saving
                    val size = Math.min(bitmap.width, bitmap.height)
                    val x = (bitmap.width - size) / 2
                    val y = (bitmap.height - size) / 2
                    val cropped = Bitmap.createBitmap(bitmap, x, y, size, size)
                    viewModel.setCustomImageLogo(cropped)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Errore caricamento immagine: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1C1B1F)) // Sleek Theme Dark charcoal background
                .padding(innerPadding)
        ) {
            // Main content column
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // Header (Sleek minimalist panel)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "Wallet",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE6E1E5),
                            lineHeight = 40.sp,
                            letterSpacing = (-0.5).sp
                        )
                        val activeLabel = if (cards.size == 1) "1 tessera attiva" else "${cards.size} tessere attive"
                        Text(
                            text = activeLabel,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF939094),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    // Backup & Ripristina Actions (Sleek theme capsule buttons)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF2B2930))
                                .clickable {
                                    val checkContents = viewModel.exportBackup(context)
                                    if (checkContents.isNullOrEmpty()) {
                                        Toast.makeText(context, "Wallet vuoto! Carica o crea tessere prima.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        exportLauncher.launch("WALLET_BACKUP.json")
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "BACKUP",
                                color = Color(0xFFD0BCFF),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF2B2930))
                                .clickable {
                                    importLauncher.launch("*/*")
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "RIPRISTINA",
                                color = Color(0xFFD0BCFF),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                // Cards list with dynamic visual stacking effect
                if (cards.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = "Empty list",
                            tint = Color.DarkGray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Nessuna tessera nel wallet",
                            color = Color.Gray,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Premi il pulsante '+' in basso per salvare la tua prima card in formato XL.",
                            color = Color.DarkGray,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = WindowInsets.navigationBars.asPaddingValues()
                    ) {
                        itemsIndexed(cards) { index, card ->
                            // Custom overlapping card-deck layout with exact scroll constraints (no offsets needed!)
                            val containerHeight = if (index == cards.lastIndex) 180.dp else 115.dp
                            
                            // Multi-layer staggered 3D perspective scale calculation
                            // Deeper cards (lower index) are slightly scaled down to look further away in 3D space
                            val scaleFactor = (1.0f - (cards.size - 1 - index) * 0.015f).coerceIn(0.9f, 1.0f)
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(containerHeight)
                                    .padding(horizontal = 20.dp)
                                    .zIndex(index.toFloat()) // Ensure lower cards render on top of previous cards
                            ) {
                                WalletCardItem(
                                    card = card,
                                    cardScale = scaleFactor,
                                    onClick = { selectedPresenterCard = card },
                                    onEdit = {
                                        viewModel.startEditing(card)
                                        showCreatorOverlay = true
                                    },
                                    onDelete = {
                                        cardToDelete = card
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // High-Contrast Floating Action Button (FAB) (Sleek Theme M3 style)
            IconButton(
                onClick = {
                    viewModel.startEditing(null)
                    showCreatorOverlay = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 30.dp, end = 24.dp)
                    .size(60.dp)
                    .shadow(elevation = 12.dp, shape = RoundedCornerShape(16.dp))
                    .background(Color(0xFFD0BCFF), shape = RoundedCornerShape(16.dp))
                    .testTag("add_card_fab")
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Create Card",
                    tint = Color(0xFF381E72),
                    modifier = Modifier.size(32.dp)
                )
            }

            // LAYER 1: Card Creator Overlay (Full bleed responsive sheet)
            AnimatedVisibility(
                visible = showCreatorOverlay,
                enter = slideInVertically(animationSpec = spring(), initialOffsetY = { it }),
                exit = slideOutVertically(animationSpec = spring(), targetOffsetY = { it })
            ) {
                CreatorOverlayView(
                    viewModel = viewModel,
                    editCardId = editCardId,
                    nameInput = nameInput,
                    codeInput = codeInput,
                    formatInput = formatInput,
                    logoUrl = logoUrl,
                    logoBase64 = logoBase64,
                    selectedColorHex = selectedColorHex,
                    logoSuggestions = logoSuggestions,
                    onClose = { showCreatorOverlay = false },
                    onLaunchGallery = { galleryLauncher.launch("image/*") },
                    onLaunchScanner = { showScannerOverlay = true },
                    onSave = {
                        viewModel.saveCard {
                            showCreatorOverlay = false
                            Toast.makeText(context, "Card salvata!", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            // LAYER 2: Live Camera Scanning Sheet Overlay
            AnimatedVisibility(
                visible = showScannerOverlay,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                ScannerOverlayView(
                    onDetected = { code, format ->
                        viewModel.onCodeChange(code)
                        viewModel.onFormatChange(format)
                        showScannerOverlay = false
                        Toast.makeText(context, "Codice scansionato con successo: $format", Toast.LENGTH_SHORT).show()
                    },
                    onClose = { showScannerOverlay = false }
                )
            }

            // LAYER 3: Interactive White Fullscreen Card Presenter Overlay
            AnimatedVisibility(
                visible = selectedPresenterCard != null,
                enter = slideInVertically(animationSpec = spring(), initialOffsetY = { it }),
                exit = slideOutVertically(animationSpec = spring(), targetOffsetY = { it })
            ) {
                selectedPresenterCard?.let { card ->
                    PresenterOverlayView(
                        card = card,
                        onClose = { selectedPresenterCard = null }
                    )
                }
            }

            // Elegant Confirmation Dialog for Card Deletion
            if (cardToDelete != null) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { cardToDelete = null },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Rimuovi Card",
                            tint = Color(0xFFF2B8B5)
                        )
                    },
                    title = {
                        Text(
                            text = "Elimina tessera",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    text = {
                        Text(
                            text = "Sei sicuro di voler eliminare la tessera \"${cardToDelete?.name}\" definitivamente? Questa azione non può essere annullata.",
                            color = Color(0xFFCAC4D0),
                            fontSize = 14.sp
                        )
                    },
                    containerColor = Color(0xFF2B2930),
                    confirmButton = {
                        androidx.compose.material3.TextButton(
                            onClick = {
                                cardToDelete?.let { card ->
                                    viewModel.deleteCard(card.id)
                                    Toast.makeText(context, "${card.name} rimossa con successo!", Toast.LENGTH_SHORT).show()
                                }
                                cardToDelete = null
                            }
                        ) {
                            Text("ELIMINA", color = Color(0xFFF2B8B5), fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(
                            onClick = { cardToDelete = null }
                        ) {
                            Text("ANNULLA", color = Color.White)
                        }
                    }
                )
            }
        }
    }
}

// Custom Loyalty Wallet Card Item layout (Material 3 overlay styled with brand colors)
@Composable
fun WalletCardItem(
    card: CardEntity,
    cardScale: Float,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val cardColor = remember(card.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(card.colorHex))
        } catch (e: Exception) {
            Color(0xFF1C1C1E)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        // LAYER 1: 3D Plastic Card Thickness Extrusion Bezel (shifts down card bottom shadow edge)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(174.dp)
                .offset(y = 5.dp)
                .graphicsLayer {
                    rotationX = 12f
                    cameraDistance = 16f
                    scaleX = cardScale
                    scaleY = cardScale
                }
                .background(
                    color = cardColor.darken(0.40f),
                    shape = RoundedCornerShape(24.dp)
                )
        )

        // LAYER 2: Ultra Deep Semi-Transparent Soft Ambient 3D Drop Shadow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(174.dp)
                .offset(y = 2.dp)
                .graphicsLayer {
                    rotationX = 12f
                    cameraDistance = 16f
                    scaleX = cardScale
                    scaleY = cardScale
                }
                .shadow(
                    elevation = 22.dp,
                    shape = RoundedCornerShape(24.dp),
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.65f),
                    spotColor = Color.Black.copy(alpha = 0.82f)
                )
        )

        // LAYER 3: Primary Foreground Card styled with a glassmorphic linear sheen gradient
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(174.dp)
                .clickable(onClick = onClick)
                .graphicsLayer {
                    rotationX = 12f
                    cameraDistance = 16f
                    scaleX = cardScale
                    scaleY = cardScale
                }
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.50f),
                            Color.White.copy(alpha = 0.10f),
                            Color.White.copy(alpha = 0.40f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = cardColor),
            shape = RoundedCornerShape(24.dp)
        ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.38f),
                            Color.White.copy(alpha = 0.12f),
                            Color.Transparent,
                            Color.White.copy(alpha = 0.08f),
                            Color.White.copy(alpha = 0.28f)
                        ),
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(1000f, 1000f)
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.TopStart),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Card branding side
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(85.dp)
                            .shadow(2.dp, RoundedCornerShape(16.dp))
                            .background(Color.White, RoundedCornerShape(16.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        RenderCardLogo(logoUrl = card.logoUrl, logoBase64 = card.logoBase64, name = card.name)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = card.name,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = card.format.replace("_", " "),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.5f),
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Secondary Action row (Edit / Delete)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit Card",
                            tint = Color(0xFFFFB300), // Vibrant amber/yellow for edit/pencil
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete Card",
                            tint = Color(0xFFFF2E2E), // Vibrant warning red for delete/trash bin
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Bottom stylized barcode placeholder preview layout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = card.code,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    letterSpacing = 2.sp,
                    maxLines = 1
                )
                
                // Pure CSS-like canvas stripes representing simulated native barcode icon
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    val stripes = listOf(3, 1, 5, 2, 4, 1, 3, 2, 5, 1)
                    stripes.forEach { w ->
                        Box(
                            modifier = Modifier
                                .width(w.dp)
                                .height(22.dp)
                                .background(Color.White.copy(alpha = 0.6f))
                        )
                    }
                }
            }
        }
    }
}
}

// Custom logo renderer supporting Base64 or standard URLs
@Composable
fun RenderCardLogo(logoUrl: String?, logoBase64: String?, name: String, modifier: Modifier = Modifier) {
    if (!logoBase64.isNullOrEmpty()) {
        val bitmap = remember(logoBase64) {
            try {
                val cleanBase64 = if (logoBase64.contains(",")) {
                    logoBase64.substringAfter("base64,")
                } else {
                    logoBase64
                }
                val decodedString = Base64.decode(cleanBase64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
            } catch (e: Exception) {
                null
            }
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "$name logo",
                modifier = modifier.fillMaxSize(),
                contentScale = ContentScale.Inside
            )
        } else {
            DefaultLogoLetter(name = name, modifier = modifier)
        }
    } else if (!logoUrl.isNullOrEmpty()) {
        AsyncImage(
            model = logoUrl,
            contentDescription = "$name logo",
            modifier = modifier.fillMaxSize(),
            contentScale = ContentScale.Inside
        )
    } else {
        DefaultLogoLetter(name = name, modifier = modifier)
    }
}

@Composable
fun DefaultLogoLetter(name: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.firstOrNull()?.uppercase() ?: "",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF1C1C1E)
        )
    }
}

// LAYER 1 VIEW implementation: Nuova/Modifica Card editing pane
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorOverlayView(
    viewModel: WalletViewModel,
    editCardId: Long?,
    nameInput: String,
    codeInput: String,
    formatInput: String,
    logoUrl: String?,
    logoBase64: String?,
    selectedColorHex: String,
    logoSuggestions: List<WalletViewModel.LogoSuggestion>,
    onClose: () -> Unit,
    onLaunchGallery: () -> Unit,
    onLaunchScanner: () -> Unit,
    onSave: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showUrlInputDialog by remember { mutableStateOf(false) }
    var manualUrlText by remember { mutableStateOf("") }
    var showCropDialog by remember { mutableStateOf(false) }
    var bitmapToCrop by remember { mutableStateOf<Bitmap?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF1C1B1F)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .statusBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editCardId != null) "Modifica Card" else "Nuova Card",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFE6E1E5)
                )
                Text(
                    text = "Esci",
                    color = Color(0xFFD0BCFF),
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    modifier = Modifier.clickable { onClose() }
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // STEP 1: Name and predictive Clearbit autocomplete logos
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF2B2930), RoundedCornerShape(20.dp))
                            .border(1.dp, Color(0x15D0BCFF), RoundedCornerShape(20.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "1. NOME & RICERCA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD0BCFF),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { viewModel.onNameChange(it) },
                            placeholder = { Text("Cerca negozio (es: Conad, Lidl...)", color = Color(0xFF939094)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF1C1B1F),
                                unfocusedContainerColor = Color(0xFF1C1B1F),
                                focusedBorderColor = Color(0xFFD0BCFF),
                                unfocusedBorderColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Clearbit horizontally scrolling results suggestions grid
                        if (logoSuggestions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(logoSuggestions) { suggest ->
                                    Box(
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White)
                                            .border(
                                                1.5.dp,
                                                if (logoUrl == suggest.url) Color(0xFFD0BCFF) else Color.Transparent,
                                                RoundedCornerShape(12.dp)
                                              )
                                            .clickable {
                                                viewModel.selectLogoSuggestion(context, suggest)
                                            }
                                            .padding(6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = suggest.url,
                                            contentDescription = "Suggestion logo",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Inside
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // STEP 2: Branding Logo XL and Customizing Upload Actions
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF2B2930), RoundedCornerShape(20.dp))
                            .border(1.dp, Color(0x15D0BCFF), RoundedCornerShape(20.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "2. LOGO XL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD0BCFF),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Logo large Preview container Box styled with dynamic extracted brand background
                        val previewBg = remember(selectedColorHex) {
                            try {
                                Color(android.graphics.Color.parseColor(selectedColorHex))
                            } catch (e: Exception) {
                                Color(0xFF111111)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(previewBg)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.35f),
                                            Color.White.copy(alpha = 0.12f),
                                            Color.Transparent,
                                            Color.White.copy(alpha = 0.08f),
                                            Color.White.copy(alpha = 0.25f)
                                        ),
                                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                        end = androidx.compose.ui.geometry.Offset(1000f, 1000f)
                                    )
                                )
                                .border(
                                    width = 1.5.dp,
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.50f),
                                            Color.White.copy(alpha = 0.10f),
                                            Color.White.copy(alpha = 0.40f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (logoUrl != null || logoBase64 != null) {
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .shadow(2.dp, RoundedCornerShape(16.dp))
                                        .background(Color.White, RoundedCornerShape(16.dp))
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    RenderCardLogo(logoUrl = logoUrl, logoBase64 = logoBase64, name = nameInput)
                                }
                                
                                // Overlay crop floating button in bottom-right of the logo preview background
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF1C1B1F).copy(alpha = 0.85f))
                                        .clickable {
                                            if (!logoBase64.isNullOrEmpty()) {
                                                try {
                                                    val cleanBase64 = if (logoBase64.contains(",")) {
                                                        logoBase64.substringAfter("base64,")
                                                    } else {
                                                        logoBase64
                                                    }
                                                    val decodedString = Base64.decode(cleanBase64, Base64.DEFAULT)
                                                    val bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                                                    if (bitmap != null) {
                                                        bitmapToCrop = bitmap
                                                        showCropDialog = true
                                                    }
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Impossibile caricare l'immagine", Toast.LENGTH_SHORT).show()
                                                }
                                            } else if (!logoUrl.isNullOrEmpty()) {
                                                scope.launch(Dispatchers.IO) {
                                                    try {
                                                        val loader = coil.ImageLoader(context)
                                                        val request = coil.request.ImageRequest.Builder(context)
                                                            .data(logoUrl)
                                                            .allowHardware(false)
                                                            .build()
                                                        val result = loader.execute(request)
                                                        val drawable = result.drawable
                                                        if (drawable is android.graphics.drawable.BitmapDrawable) {
                                                            val bitmap = drawable.bitmap
                                                            launch(Dispatchers.Main) {
                                                                bitmapToCrop = bitmap
                                                                showCropDialog = true
                                                            }
                                                        } else {
                                                            launch(Dispatchers.Main) {
                                                                Toast.makeText(context, "Formato logo non supportato per ritaglio", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    } catch (e: Exception) {
                                                        launch(Dispatchers.Main) {
                                                            Toast.makeText(context, "Errore caricamento logo: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Edit,
                                            contentDescription = "Ritaglia",
                                            tint = Color(0xFFD0BCFF),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "RITAGLIA",
                                            color = Color(0xFFD0BCFF),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "Anteprima Logo",
                                    color = Color(0xFF939094),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Trigger actions grid (Gallery, Clipboard, URL input, Google links)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onLaunchGallery,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1B1F)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.Image, null, modifier = Modifier.size(16.dp), tint = Color(0xFFD0BCFF))
                                Spacer(Modifier.width(4.dp))
                                Text("FOTO", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            Button(
                                onClick = {
                                    try {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clipData = clipboard.primaryClip
                                        if (clipData != null && clipData.itemCount > 0) {
                                            val item = clipData.getItemAt(0)
                                            val uri = item.uri
                                            val text = item.text?.toString()?.trim()
                                            val htmlText = item.htmlText?.toString()?.trim()

                                            // 1. Direct Image content URI (copied from Gallery, Google Photos, or Chat)
                                            if (uri != null) {
                                                try {
                                                    context.contentResolver.openInputStream(uri).use { inputStream ->
                                                        val bitmap = BitmapFactory.decodeStream(inputStream)
                                                        if (bitmap != null) {
                                                            viewModel.setCustomImageLogo(bitmap)
                                                            Toast.makeText(context, "Immagine incollata!", Toast.LENGTH_SHORT).show()
                                                            return@Button
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    // Fallback to text matching if input stream fails
                                                }
                                            }

                                            // 2. HTML text format containing an inline image reference
                                            if (!htmlText.isNullOrEmpty()) {
                                                val srcRegex = """<img[^>]+src=["']([^"']+)["']""".toRegex(RegexOption.IGNORE_CASE)
                                                val matchResult = srcRegex.find(htmlText)
                                                val src = matchResult?.groups?.get(1)?.value
                                                if (!src.isNullOrEmpty()) {
                                                    if (src.startsWith("data:image")) {
                                                        try {
                                                            val base64Data = src.substringAfter("base64,")
                                                            val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
                                                            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                                                            if (bitmap != null) {
                                                                viewModel.setCustomImageLogo(bitmap)
                                                                Toast.makeText(context, "Immagine incollata!", Toast.LENGTH_SHORT).show()
                                                                return@Button
                                                            }
                                                        } catch (e: Exception) {}
                                                    } else if (src.startsWith("http")) {
                                                        viewModel.setCustomImageUrl(context, src)
                                                        Toast.makeText(context, "Link immagine incollato!", Toast.LENGTH_SHORT).show()
                                                        return@Button
                                                    }
                                                }
                                            }

                                            // 3. Plain Text string (Image Web URL, Base64 representation, or URL string)
                                            if (!text.isNullOrEmpty()) {
                                                if (text.startsWith("data:image")) {
                                                    try {
                                                        val base64Data = text.substringAfter("base64,")
                                                        val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
                                                        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                                                        if (bitmap != null) {
                                                            viewModel.setCustomImageLogo(bitmap)
                                                            Toast.makeText(context, "Immagine incollata!", Toast.LENGTH_SHORT).show()
                                                            return@Button
                                                        }
                                                    } catch (e: Exception) {}
                                                } else if (text.startsWith("http://") || text.startsWith("https://")) {
                                                    viewModel.setCustomImageUrl(context, text)
                                                    Toast.makeText(context, "URL incollato!", Toast.LENGTH_SHORT).show()
                                                    return@Button
                                                } else if (text.length > 100) {
                                                    // Let's see if this could be encoded base64 image bytes
                                                    try {
                                                        val decodedBytes = Base64.decode(text, Base64.DEFAULT)
                                                        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                                                        if (bitmap != null) {
                                                            viewModel.setCustomImageLogo(bitmap)
                                                            Toast.makeText(context, "Immagine incollata!", Toast.LENGTH_SHORT).show()
                                                            return@Button
                                                        }
                                                    } catch (e: Exception) {}
                                                }
                                            }

                                            Toast.makeText(context, "Nessun URL o immagine trovata negli appunti", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "Gli appunti sono vuoti", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Errore di incollamento: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD0BCFF)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.ContentPaste, null, modifier = Modifier.size(16.dp), tint = Color(0xFF381E72))
                                Spacer(Modifier.width(4.dp))
                                Text("INCOLLA", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF381E72))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { showUrlInputDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1B1F)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.Link, null, modifier = Modifier.size(16.dp), tint = Color(0xFFD0BCFF))
                                Spacer(Modifier.width(4.dp))
                                Text("URL", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            Button(
                                onClick = {
                                    val queryTerm = if(nameInput.isNotEmpty()) nameInput else "store"
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?tbm=isch&q=${Uri.encode(queryTerm)}+logo+png"))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)), // Google color branding
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.Search, null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(Modifier.width(4.dp))
                                Text("GOOGLE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        // Theme Manual Color hex input field
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "COLORE BRANDING (HEX)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF939094)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(previewBg)
                                    .border(1.dp, Color.White.copy(0.4f), CircleShape)
                            )
                            OutlinedTextField(
                                value = selectedColorHex,
                                onValueChange = { viewModel.setSelectedColor(it) },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color(0xFF1C1B1F),
                                    unfocusedContainerColor = Color(0xFF1C1B1F),
                                    focusedBorderColor = Color(0xFFD0BCFF),
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }

                // STEP 3: Barcode numeric info and Formats selector dropdown
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF2B2930), RoundedCornerShape(20.dp))
                            .border(1.dp, Color(0x15D0BCFF), RoundedCornerShape(20.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "3. CODICE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD0BCFF),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = codeInput,
                            onValueChange = { viewModel.onCodeChange(it) },
                            placeholder = { Text("Codice tessera", color = Color(0xFF939094)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF1C1B1F),
                                unfocusedContainerColor = Color(0xFF1C1B1F),
                                focusedBorderColor = Color(0xFFD0BCFF),
                                unfocusedBorderColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Type standard format dropdown spinner selection
                        val formats = listOf("CODE_128", "QR_CODE", "EAN_13", "EAN_8", "UPC_A", "CODE_39", "ITF")
                        var expandedDropDown by remember { mutableStateOf(false) }

                        ExposedDropdownMenuBox(
                            expanded = expandedDropDown,
                            onExpandedChange = { expandedDropDown = !expandedDropDown }
                        ) {
                            TextField(
                                readOnly = true,
                                value = formatInput.replace("_", " "),
                                onValueChange = {},
                                label = { Text("Standard Formato", color = Color(0xFF939094)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropDown) },
                                colors = ExposedDropdownMenuDefaults.textFieldColors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color(0xFF1C1B1F),
                                    unfocusedContainerColor = Color(0xFF1C1B1F)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = expandedDropDown,
                                onDismissRequest = { expandedDropDown = false },
                                modifier = Modifier.background(Color(0xFF1C1B1F))
                            ) {
                                formats.forEach { selection ->
                                    DropdownMenuItem(
                                        text = { Text(selection.replace("_", " "), color = Color.White) },
                                        onClick = {
                                            viewModel.onFormatChange(selection)
                                            expandedDropDown = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Scanner Camera activator
                        Button(
                            onClick = onLaunchScanner,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF258540)), // Sleek emerald green
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Filled.QrCodeScanner, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("📷 AVVIA SCANSIONE FOTO", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            }
                        }
                    }
                }

                // Confirm Action Button save (Sleek Theme M3 Premium button)
                item {
                    val canSave = nameInput.trim().isNotEmpty() && codeInput.trim().isNotEmpty() && (logoUrl != null || logoBase64 != null)
                    Button(
                        onClick = onSave,
                        enabled = canSave,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD0BCFF),
                            contentColor = Color(0xFF381E72),
                            disabledContainerColor = Color(0xFF2B2930),
                            disabledContentColor = Color(0xFF939094)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(65.dp)
                            .shadow(12.dp, shape = RoundedCornerShape(16.dp))
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = if (editCardId != null) "AGGIORNA NEL WALLET" else "SALVA NEL WALLET",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (canSave) Color(0xFF381E72) else Color(0xFF939094)
                        )
                    }
                }
            }
        }
    }

    // Direct manual URL input dialog
    if (showUrlInputDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showUrlInputDialog = false },
            title = { Text("Carica URL Logo", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = manualUrlText,
                    onValueChange = { manualUrlText = it },
                    placeholder = { Text("https://example.com/logo.png", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (manualUrlText.trim().startsWith("http")) {
                            viewModel.setCustomImageUrl(context, manualUrlText.trim())
                            showUrlInputDialog = false
                            manualUrlText = ""
                        } else {
                            Toast.makeText(context, "Inserisci un indirizzo HTTP valido", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showUrlInputDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                ) {
                    Text("Annulla")
                }
            },
            containerColor = Color(0xFF1C1C1E)
        )
    }

    // Elegant and premium Interactive Logo Cropping Dialog
    if (showCropDialog && bitmapToCrop != null) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { 
                showCropDialog = false 
                bitmapToCrop = null
            }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, Color(0x33D0BCFF), RoundedCornerShape(24.dp)),
                color = Color(0xFF1C1B1F)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Ritaglia Logo",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Trascina per centrare e usa lo zoom",
                        fontSize = 12.sp,
                        color = Color(0xFF939094),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    // Crop parameters state local to the dialog supporting free rectangular aspect ratio
                    var zoom by remember { mutableStateOf(1.0f) }
                    var panX by remember { mutableStateOf(0.0f) }
                    var panY by remember { mutableStateOf(0.0f) }
                    var cropWidthFactor by remember { mutableStateOf(0.75f) }
                    var cropHeightFactor by remember { mutableStateOf(0.75f) }

                    // We display the image inside a square viewport of 240.dp
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        val imageBitmap = remember(bitmapToCrop) {
                            bitmapToCrop!!.asImageBitmap()
                        }
                        
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        // Update panning based on dragging with respect to zoom
                                        val sensitivity = 1.0f / (zoom.coerceAtLeast(1.0f))
                                        panX = (panX + (dragAmount.x / 120f) * sensitivity).coerceIn(-1.0f, 1.0f)
                                        panY = (panY + (dragAmount.y / 120f) * sensitivity).coerceIn(-1.0f, 1.0f)
                                    }
                                }
                        ) {
                            // Render the image with offset & scale of zoom
                            Image(
                                bitmap = imageBitmap,
                                contentDescription = "Logo in cropper",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = zoom
                                        scaleY = zoom
                                        
                                        // Max offset translation bounds in absolute pixel space
                                        val maxOffsetPx = (size.width * (zoom - 1f)) / 2f
                                        translationX = panX * maxOffsetPx
                                        translationY = panY * maxOffsetPx
                                    },
                                contentScale = ContentScale.Fit
                            )

                            // Render the 4 rectangular semi-transparent borders around the rectangular crop frame
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val canvasWidth = size.width
                                val canvasHeight = size.height
                                
                                // Dynamic rectangular sizing based on user-adjusted factors
                                val sDispW = canvasWidth * cropWidthFactor
                                val sDispH = canvasHeight * cropHeightFactor
                                val topDisp = (canvasHeight - sDispH) / 2f
                                val leftDisp = (canvasWidth - sDispW) / 2f
                                
                                val overlayColor = Color.Black.copy(alpha = 0.5f)
                                
                                // Top overlay segment
                                drawRect(
                                    color = overlayColor,
                                    topLeft = androidx.compose.ui.geometry.Offset.Zero,
                                    size = androidx.compose.ui.geometry.Size(canvasWidth, topDisp)
                                )
                                // Bottom overlay segment
                                drawRect(
                                    color = overlayColor,
                                    topLeft = androidx.compose.ui.geometry.Offset(0f, topDisp + sDispH),
                                    size = androidx.compose.ui.geometry.Size(canvasWidth, canvasHeight - (topDisp + sDispH))
                                )
                                // Left overlay segment
                                drawRect(
                                    color = overlayColor,
                                    topLeft = androidx.compose.ui.geometry.Offset(0f, topDisp),
                                    size = androidx.compose.ui.geometry.Size(leftDisp, sDispH)
                                )
                                // Right overlay segment
                                drawRect(
                                    color = overlayColor,
                                    topLeft = androidx.compose.ui.geometry.Offset(leftDisp + sDispW, topDisp),
                                    size = androidx.compose.ui.geometry.Size(canvasWidth - (leftDisp + sDispW), sDispH)
                                )
                                
                                // Custom rectangular crop boundary border
                                drawRect(
                                    color = Color(0xFFD0BCFF),
                                    topLeft = androidx.compose.ui.geometry.Offset(leftDisp, topDisp),
                                    size = androidx.compose.ui.geometry.Size(sDispW, sDispH),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Interactive sliders for precise zoom and custom rectangular dimension scaling
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // ZOOM Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ZOOM",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF939094),
                                modifier = Modifier.width(72.dp)
                            )
                            androidx.compose.material3.Slider(
                                value = zoom,
                                onValueChange = { 
                                    zoom = it 
                                    if (zoom <= 1.01f) {
                                        panX = 0f
                                        panY = 0f
                                    }
                                },
                                valueRange = 1.0f..5.0f,
                                colors = androidx.compose.material3.SliderDefaults.colors(
                                    activeTrackColor = Color(0xFFD0BCFF),
                                    thumbColor = Color(0xFFD0BCFF)
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // LARGHEZZA Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LARGHEZZA",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF939094),
                                modifier = Modifier.width(72.dp)
                            )
                            androidx.compose.material3.Slider(
                                value = cropWidthFactor,
                                onValueChange = { cropWidthFactor = it },
                                valueRange = 0.15f..1.0f,
                                colors = androidx.compose.material3.SliderDefaults.colors(
                                    activeTrackColor = Color(0xFFD0BCFF),
                                    thumbColor = Color(0xFFD0BCFF)
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // ALTEZZA Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ALTEZZA",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF939094),
                                modifier = Modifier.width(72.dp)
                            )
                            androidx.compose.material3.Slider(
                                value = cropHeightFactor,
                                onValueChange = { cropHeightFactor = it },
                                valueRange = 0.15f..1.0f,
                                colors = androidx.compose.material3.SliderDefaults.colors(
                                    activeTrackColor = Color(0xFFD0BCFF),
                                    thumbColor = Color(0xFFD0BCFF)
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                showCropDialog = false
                                bitmapToCrop = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B2930)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("ANNULLA", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Button(
                            onClick = {
                                try {
                                    val bitmap = bitmapToCrop!!
                                    val w = bitmap.width.toFloat()
                                    val h = bitmap.height.toFloat()
                                    
                                    val viewportSize = 240f
                                    val fitScale = minOf(viewportSize / w, viewportSize / h)
                                    val dispW = w * fitScale
                                    val dispH = h * fitScale
                                    
                                    val cropWidthDisp = viewportSize * cropWidthFactor
                                    val cropHeightDisp = viewportSize * cropHeightFactor
                                    
                                    val croppedWidthInBitmap = cropWidthDisp / (fitScale * zoom)
                                    val croppedHeightInBitmap = cropHeightDisp / (fitScale * zoom)
                                    
                                    val maxPixelPanX = ((dispW * (zoom - 1f)) / 2f) / (fitScale * zoom)
                                    val maxPixelPanY = ((dispH * (zoom - 1f)) / 2f) / (fitScale * zoom)
                                    
                                    val centerXPix = (w / 2f) - (panX * maxPixelPanX)
                                    val centerYPix = (h / 2f) - (panY * maxPixelPanY)
                                    
                                    val xLeft = (centerXPix - (croppedWidthInBitmap / 2f)).toInt()
                                    val yTop = (centerYPix - (croppedHeightInBitmap / 2f)).toInt()
                                    
                                    val finalWidth = croppedWidthInBitmap.toInt()
                                        .coerceAtMost(bitmap.width)
                                        .coerceAtLeast(10)
                                    val finalHeight = croppedHeightInBitmap.toInt()
                                        .coerceAtMost(bitmap.height)
                                        .coerceAtLeast(10)
                                    
                                    val finalLeft = xLeft.coerceIn(0, (bitmap.width - finalWidth).coerceAtLeast(0))
                                    val finalTop = yTop.coerceIn(0, (bitmap.height - finalHeight).coerceAtLeast(0))
                                    
                                    val croppedBitmap = Bitmap.createBitmap(bitmap, finalLeft, finalTop, finalWidth, finalHeight)
                                    viewModel.setCustomImageLogo(croppedBitmap)
                                    
                                    showCropDialog = false
                                    bitmapToCrop = null
                                    Toast.makeText(context, "Logo ritagliato con successo!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Errore nel ritaglio: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD0BCFF)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("CONFERMA", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF381E72))
                        }
                    }
                }
            }
        }
    }
}

// LAYER 2 VIEW implementation: Real on-device camera tracking ML kit scanner UI
@Composable
fun ScannerOverlayView(
    onDetected: (String, String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Handle Runtime Camera Permission automatically before loading camera provider
    var hasCameraPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            hasCameraPermission = true
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().apply {
                            setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        imageAnalysis.setAnalyzer(
                            ContextCompat.getMainExecutor(ctx),
                            BarcodeAnalyzer { code, format ->
                                // Shut down camera cleanly and return standard decoded codes
                                try {
                                    cameraProvider.unbindAll()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                onDetected(code, format)
                            }
                        )

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageAnalysis
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Scanning visual boundary reticle layer overlying camera preview
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(260.dp, 160.dp)
                            .border(3.dp, Color(0xFF34C759), RoundedCornerShape(16.dp))
                            .background(Color.Transparent)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Inquadra il codice a barre o QR",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            // Camera permissions denied UI
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Permesso fotocamera necessario",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
                    ) {
                        Text("Abilita fotocamera")
                    }
                }
            }
        }

        // Bottom manual cancellation actionable drawer
        Button(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
                .shadow(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
        ) {
            Text("ANNULLA", fontWeight = FontWeight.Bold)
        }
    }
}

// LAYER 3 VIEW implementation: White presenter container matching physical scanners
@Composable
fun PresenterOverlayView(
    card: CardEntity,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val brandingColor = remember(card.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(card.colorHex))
        } catch (e: Exception) {
            Color(0xFF0A0A0C)
        }
    }

    // Dynamic high-fidelity barcode generator in native thread pool (asynchronous)
    var generatedBarcodeBitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(card.code, card.format) {
        launch(Dispatchers.Default) {
            val bitmap = BarcodeGenerator.generateBarcode(card.code, card.format, width = 850, height = 280)
            launch(Dispatchers.Main) {
                generatedBarcodeBitmap = bitmap
            }
        }
    }

    // Fullscreen bright white container representing real scan environment optimizing scanner screens
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClose() },
        color = Color.White // Perfect contrast for scanner devices
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Stylized background header representing branding color identity arc
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .background(brandingColor)
            )

            // Dynamic card presenter container details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 24.dp)
                    .statusBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(40.dp))

                // Brand Emblem Floating Card
                Card(
                    modifier = Modifier
                        .size(160.dp, 100.dp)
                        .shadow(4.dp, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        RenderCardLogo(logoUrl = card.logoUrl, logoBase64 = card.logoBase64, name = card.name)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = card.name,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(40.dp))

                // Barcode generated container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(16.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (generatedBarcodeBitmap != null) {
                        Image(
                            bitmap = generatedBarcodeBitmap!!.asImageBitmap(),
                            contentDescription = "Barcode",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Generazione codice...", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Copiable card codes monospace indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Card Code", card.code)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Codice copiato negli appunti!", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = card.code,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        letterSpacing = 6.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = "Copia Codice",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Decoded standards identifier indicator labels
                Text(
                    text = "Standard: ${card.format.replace("_", " ")}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )
            }

            // Centralized prompt trigger overlay exit helper
            Text(
                text = "Tocca ovunque per chiudere",
                color = Color.LightGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 30.dp)
            )
        }
    }
}

// Utility extension to darken any Compose Color (used for 3D extrusion card thickness)
fun Color.darken(factor: Float = 0.6f): Color {
    return Color(
        red = (this.red * factor).coerceIn(0f, 1f),
        green = (this.green * factor).coerceIn(0f, 1f),
        blue = (this.blue * factor).coerceIn(0f, 1f),
        alpha = this.alpha
    )
}

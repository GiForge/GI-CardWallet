package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.data.CardEntity
import com.example.data.CardRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.URLEncoder

class WalletViewModel(private val repository: CardRepository) : ViewModel() {

    // Main local wallet state
    val cards: StateFlow<List<CardEntity>> = repository.allCards
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Form editing states
    private val _editCardId = MutableStateFlow<Long?>(null)
    val editCardId = _editCardId.asStateFlow()

    private val _nameInput = MutableStateFlow("")
    val nameInput = _nameInput.asStateFlow()

    private val _codeInput = MutableStateFlow("")
    val codeInput = _codeInput.asStateFlow()

    private val _formatInput = MutableStateFlow("CODE_128")
    val formatInput = _formatInput.asStateFlow()

    private val _logoUrl = MutableStateFlow<String?>(null)
    val logoUrl = _logoUrl.asStateFlow()

    private val _logoBase64 = MutableStateFlow<String?>(null)
    val logoBase64 = _logoBase64.asStateFlow()

    private val _selectedColorHex = MutableStateFlow("#1C1C1E")
    val selectedColorHex = _selectedColorHex.asStateFlow()

    // Logo suggestions
    private val _logoSuggestions = MutableStateFlow<List<LogoSuggestion>>(emptyList())
    val logoSuggestions = _logoSuggestions.asStateFlow()

    data class LogoSuggestion(val domain: String, val url: String)

    fun onNameChange(newName: String) {
        _nameInput.value = newName
        val query = newName.lowercase().trim().replace("\\s".toRegex(), "")
        if (query.length >= 2) {
            val variants = listOf("$query.it", "$query.com", query)
            _logoSuggestions.value = variants.flatMap { domain ->
                listOf(
                    LogoSuggestion(domain, "https://logo.clearbit.com/$domain"),
                    LogoSuggestion(domain, "https://www.google.com/s2/favicons?sz=128&domain_url=$domain")
                )
            }
        } else {
            _logoSuggestions.value = emptyList()
        }
    }

    fun onCodeChange(newCode: String) {
        _codeInput.value = newCode
    }

    fun onFormatChange(newFormat: String) {
        _formatInput.value = newFormat
    }

    fun selectLogoSuggestion(context: Context, suggestion: LogoSuggestion) {
        val encodedUrl = URLEncoder.encode(suggestion.url, "UTF-8")
        val proxiedUrl = "https://wsrv.nl/?url=$encodedUrl&w=128&n=-1"
        _logoUrl.value = proxiedUrl
        _logoBase64.value = null
        extractDominantColorFromUrl(context, proxiedUrl)
    }

    fun setCustomImageLogo(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.Default) {
            val dominantColorInt = getDominantColor(bitmap)
            val colorHexStr = String.format("#%06X", 0xFFFFFF and dominantColorInt)
            
            val resized = resizeBitmap(bitmap, 400)
            val base64Str = bitmapToBase64(resized)
            
            _logoBase64.value = base64Str
            _logoUrl.value = null
            _selectedColorHex.value = colorHexStr
        }
    }

    fun setCustomImageUrl(context: Context, url: String) {
        val encodedUrl = URLEncoder.encode(url, "UTF-8")
        val proxiedUrl = "https://wsrv.nl/?url=$encodedUrl&w=400"
        _logoUrl.value = proxiedUrl
        _logoBase64.value = null
        extractDominantColorFromUrl(context, proxiedUrl)
    }

    fun setSelectedColor(hex: String) {
        _selectedColorHex.value = hex
    }

    fun startEditing(card: CardEntity?) {
        if (card != null) {
            _editCardId.value = card.id
            _nameInput.value = card.name
            _codeInput.value = card.code
            _formatInput.value = card.format
            _logoUrl.value = card.logoUrl
            _logoBase64.value = card.logoBase64
            _selectedColorHex.value = card.colorHex
            _logoSuggestions.value = emptyList()
        } else {
            _editCardId.value = null
            _nameInput.value = ""
            _codeInput.value = ""
            _formatInput.value = "CODE_128"
            _logoUrl.value = null
            _logoBase64.value = null
            _selectedColorHex.value = "#1C1C1E"
            _logoSuggestions.value = emptyList()
        }
    }

    fun saveCard(onSuccess: () -> Unit) {
        val name = _nameInput.value.trim()
        val code = _codeInput.value.trim()
        val format = _formatInput.value
        val logoUrlValue = _logoUrl.value
        val logoBase64Value = _logoBase64.value
        val colorHexValue = _selectedColorHex.value

        if (name.isEmpty() || code.isEmpty() || (logoUrlValue == null && logoBase64Value == null)) {
            return
        }

        viewModelScope.launch {
            val card = CardEntity(
                id = _editCardId.value ?: 0L,
                name = name,
                code = code,
                format = format,
                logoBase64 = logoBase64Value,
                logoUrl = logoUrlValue,
                colorHex = colorHexValue
            )
            repository.insert(card)
            onSuccess()
        }
    }

    fun deleteCard(cardId: Long) {
        viewModelScope.launch {
            repository.deleteById(cardId)
        }
    }

    private fun extractDominantColorFromUrl(context: Context, url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .allowHardware(false)
                    .build()
                val result = (loader.execute(request) as? SuccessResult)?.drawable
                val bitmap = (result as? BitmapDrawable)?.bitmap
                if (bitmap != null) {
                    val dominantColorInt = getDominantColor(bitmap)
                    val hexStr = String.format("#%06X", 0xFFFFFF and dominantColorInt)
                    withContext(Dispatchers.Main) {
                        _selectedColorHex.value = hexStr
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // A simple, fast pixel-averaging algorithm skipping very light or dark pixels
    private fun getDominantColor(bitmap: Bitmap): Int {
        var redSum = 0L
        var greenSum = 0L
        var blueSum = 0L
        var count = 0
        val width = bitmap.width
        val height = bitmap.height
        val stepX = (width / 20).coerceAtLeast(1)
        val stepY = (height / 20).coerceAtLeast(1)

        for (x in 0 until width step stepX) {
            for (y in 0 until height step stepY) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                // Skip near-white, near-black backgrounds to extract the core color
                if (r > 240 && g > 240 && b > 240) continue
                if (r < 20 && g < 20 && b < 20) continue

                val diffRG = Math.abs(r - g)
                val diffGB = Math.abs(g - b)
                val diffBR = Math.abs(b - r)
                val saturation = diffRG + diffGB + diffBR

                if (saturation > 25) {
                    redSum += r
                    greenSum += g
                    blueSum += b
                    count++
                }
            }
        }

        if (count == 0) return 0xFF1C1C1E.toInt()
        val r = (redSum / count).toInt().coerceIn(0, 255)
        val g = (greenSum / count).toInt().coerceIn(0, 255)
        val b = (blueSum / count).toInt().coerceIn(0, 255)
        return (0xFF000000u or (r.toUInt() shl 16) or (g.toUInt() shl 8) or b.toUInt()).toInt()
    }

    private fun resizeBitmap(bitmap: Bitmap, targetWidth: Int): Bitmap {
        val aspectRatio = bitmap.height.toFloat() / bitmap.width.toFloat()
        val targetHeight = (targetWidth * aspectRatio).toInt()
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    internal fun hexToRgbString(hexStr: String): String {
        return try {
            val colorInt = android.graphics.Color.parseColor(hexStr)
            val r = android.graphics.Color.red(colorInt)
            val g = android.graphics.Color.green(colorInt)
            val b = android.graphics.Color.blue(colorInt)
            "rgb($r,$g,$b)"
        } catch (e: Exception) {
            "rgb(28,28,30)"
        }
    }

    internal fun parseColorToHex(colorStr: String): String {
        val trimmed = colorStr.trim()
        if (trimmed.startsWith("#")) {
            return trimmed
        }
        if (trimmed.startsWith("rgb", ignoreCase = true)) {
            try {
                val parts = trimmed
                    .substringAfter("(")
                    .substringBefore(")")
                    .split(",")
                    .map { it.trim().toInt() }
                if (parts.size >= 3) {
                    return String.format("#%02X%02X%02X", parts[0], parts[1], parts[2])
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return "#1C1C1E"
    }

    // Backup & Restore
    fun exportBackup(context: Context, customList: List<CardEntity>? = null): String? {
        val list = customList ?: cards.value
        if (list.isEmpty()) return null
        return try {
            val jsonArray = JSONArray()
            for (card in list) {
                val jsonObject = JSONObject().apply {
                    put("id", card.id)
                    put("name", card.name)
                    put("code", card.code)
                    put("format", card.format)

                    val logoVal = if (!card.logoBase64.isNullOrEmpty()) {
                        "data:image/png;base64,${card.logoBase64}"
                    } else if (!card.logoUrl.isNullOrEmpty()) {
                        card.logoUrl
                    } else {
                        null
                    }
                    put("logo", logoVal ?: JSONObject.NULL)
                    put("color", hexToRgbString(card.colorHex))

                    // Keep original properties for backward compatibility
                    put("colorHex", card.colorHex)
                    put("logoUrl", card.logoUrl ?: "")
                    put("logoBase64", card.logoBase64 ?: "")
                }
                jsonArray.put(jsonObject)
            }
            jsonArray.toString(2)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun importBackup(inputStream: InputStream, onComplete: (Boolean, Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val text = inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(text)
                var importedCount = 0
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val name = obj.getString("name")
                    val code = obj.getString("code")
                    val format = obj.getString("format")

                    val colorVal = if (obj.has("color")) {
                        obj.getString("color")
                    } else {
                        obj.optString("colorHex", "#1C1C1E")
                    }
                    val colorHex = parseColorToHex(colorVal)

                    var logoBase64Val: String? = null
                    var logoUrlVal: String? = null

                    if (obj.has("logo") && !obj.isNull("logo")) {
                        val logoVal = obj.getString("logo")
                        if (logoVal.isNotEmpty()) {
                            if (logoVal.startsWith("data:")) {
                                logoBase64Val = logoVal.substringAfter("base64,")
                            } else if (logoVal.startsWith("http")) {
                                logoUrlVal = logoVal
                            } else {
                                logoBase64Val = logoVal
                            }
                        }
                    } else {
                        logoUrlVal = obj.optString("logoUrl").takeIf { it.isNotEmpty() }
                        logoBase64Val = obj.optString("logoBase64").takeIf { it.isNotEmpty() }
                    }

                    val card = CardEntity(
                        name = name,
                        code = code,
                        format = format,
                        colorHex = colorHex,
                        logoUrl = logoUrlVal,
                        logoBase64 = logoBase64Val
                    )
                    repository.insert(card)
                    importedCount++
                }
                withContext(Dispatchers.Main) {
                    onComplete(true, importedCount)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onComplete(false, 0)
                }
            }
        }
    }
}

class WalletViewModelFactory(private val repository: CardRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WalletViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WalletViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

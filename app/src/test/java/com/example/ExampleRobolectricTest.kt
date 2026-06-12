package com.example

import android.content.Context
import android.graphics.Color
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.CardEntity
import com.example.data.CardRepository
import com.example.ui.WalletViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: CardRepository
    private lateinit var viewModel: WalletViewModel
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = CardRepository(db.cardDao())
        viewModel = WalletViewModel(repository)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testColorParsing() {
        val hex1 = viewModel.parseColorToHex("rgb(164,27,20)")
        assertEquals("#A41B14", hex1)

        val hex2 = viewModel.parseColorToHex("#FFFFFF")
        assertEquals("#FFFFFF", hex2)

        val rgbStr = viewModel.hexToRgbString("#A41B14")
        assertEquals("rgb(164,27,20)", rgbStr)
    }

    @Test
    fun testImportCompatibleBackup() = runBlocking {
        val jsonStr = """
            [
              {
                "id": 1770546929439,
                "name": "anvedi",
                "code": "004022",
                "format": "QR_CODE",
                "logo": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA",
                "color": "rgb(164,27,20)"
              },
              {
                "id": 1770512340005,
                "name": "brico center",
                "code": "93010102016702018",
                "format": "CODE_128",
                "logo": null,
                "color": "rgb(228,5,19)"
              }
            ]
        """.trimIndent()

        val inputStream = ByteArrayInputStream(jsonStr.toByteArray())
        
        var isSuccess = false
        var count = 0
        viewModel.importBackup(inputStream) { success, importedCount ->
            isSuccess = success
            count = importedCount
        }

        // Wait or run blocking check
        val savedCards = repository.allCards.first { it.size == 2 }
        assertEquals(2, savedCards.size)
        
        val card1 = savedCards.firstOrNull { it.name == "anvedi" }
        assertNotNull(card1)
        assertEquals("004022", card1?.code)
        assertEquals("#A41B14", card1?.colorHex)
        assertEquals("iVBORw0KGgoAAAANSUhEUgAA", card1?.logoBase64)
    }

    @Test
    fun testExportCompatibleBackup() = runBlocking {
        // Insert a card
        val card = CardEntity(
            name = "Test Card",
            code = "123456",
            format = "EAN_13",
            colorHex = "#A41B14",
            logoBase64 = "iVBORw0K",
            logoUrl = null
        )
        repository.insert(card)
        
        // Wait for flow to emit
        val list = repository.allCards.first()

        val backupJson = viewModel.exportBackup(context, list)
        assertNotNull(backupJson)
        
        val jsonArray = JSONArray(backupJson)
        assertEquals(1, jsonArray.length())
        val obj = jsonArray.getJSONObject(0)
        assertEquals("Test Card", obj.getString("name"))
        assertEquals("123456", obj.getString("code"))
        assertEquals("EAN_13", obj.getString("format"))
        assertEquals("rgb(164,27,20)", obj.getString("color"))
        assertEquals("data:image/png;base64,iVBORw0K", obj.getString("logo"))
        
        // Back-compatibility fields should be present as well
        assertEquals("#A41B14", obj.getString("colorHex"))
        assertEquals("iVBORw0K", obj.getString("logoBase64"))
    }
}

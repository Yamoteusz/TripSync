package pl.tripsync

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class TripSyncUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val graph get() = (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as TripSyncApp).graph
    @Before fun prepareDemo() {
        runBlocking { graph.repository.logout(); graph.repository.demo() }
        compose.waitUntil(15000) { compose.onAllNodesWithText("Twoje wyjazdy").fetchSemanticsNodes().isNotEmpty() }
    }
    private fun openTrip() {compose.onNodeWithText("Weekend w Tatrach").performScrollTo().performClick()}
    private fun screenshot(name: String) {
        compose.waitForIdle()
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val dir=File(instrumentation.targetContext.getExternalFilesDir(null),"screenshots").apply {mkdirs()}
        val image=instrumentation.uiAutomation.takeScreenshot()
        File(dir,"$name.png").outputStream().use {image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
        image.recycle()
    }
    @Test fun mainScreensAndRealScreenshots() {
        screenshot("01-wyjazdy")
        openTrip()
        screenshot("02-podsumowanie")
        compose.onNodeWithText("Plan",useUnmergedTree=true).performClick()
        compose.onNodeWithText("Spotkanie na dworcu").assertIsDisplayed()
        screenshot("03-harmonogram")
        compose.onNodeWithText("Zadania",useUnmergedTree=true).performClick()
        compose.onNodeWithText("Zadania i pakowanie").assertIsDisplayed()
        screenshot("04-zadania")
        compose.onNodeWithText("Pomysły",useUnmergedTree=true).performClick()
        compose.onNodeWithText("Wjazd na Kasprowy Wierch").assertIsDisplayed()
        screenshot("05-glosowanie")
        compose.onNodeWithText("Koszty",useUnmergedTree=true).performClick()
        compose.onNodeWithText("900,00 PLN").assertIsDisplayed()
        screenshot("06-rozliczenia")
        compose.onNodeWithText("Dodaj wydatek").performClick()
        screenshot("07-formularz")
    }
    @Test fun expenseCrudChangesBalances() {
        openTrip();compose.onNodeWithText("Koszty",useUnmergedTree=true).performClick()
        compose.onNodeWithText("Dodaj wydatek").performClick()
        compose.onNode(hasSetTextAction() and hasText("Nazwa")).performTextInput("Woda na szlak")
        compose.onNode(hasSetTextAction() and hasText("Kwota (PLN)")).performTextInput("12,34")
        compose.onNodeWithText("Zapisz").performScrollTo().performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("912,34 PLN").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("912,34 PLN").assertIsDisplayed()
        val record=runBlocking {graph.db.dao().records().first {it.kind=="expense" && it.json.contains("Woda na szlak")}}
        runBlocking {graph.repository.save(record.tripId,record.kind,graph.repository.gson.fromJson(record.json,com.google.gson.JsonObject::class.java),record.id,true)}
        compose.waitUntil(10000) {compose.onAllNodesWithText("900,00 PLN").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("900,00 PLN").assertIsDisplayed()
    }
    @Test fun editingUnequalExpensePreservesShares() {
        val trip="demo-tatry"
        val expected=mapOf("demo-anna" to 100L,"demo-bartek" to 200L,"demo-celina" to 700L)
        val id=runBlocking {graph.repository.save(trip,"expense",pl.tripsync.domain.ExpenseData("Nierówny podział",1000,"demo-anna",expected,"2026-12-31"))}
        openTrip();compose.onNodeWithText("Koszty",useUnmergedTree=true).performClick()
        compose.onAllNodesWithContentDescription("Edytuj wydatek").onFirst().performScrollTo().performClick()
        compose.onNodeWithText("Zapisz").performScrollTo().performClick()
        compose.waitUntil(10000) {compose.onAllNodesWithText("Wspólne koszty").fetchSemanticsNodes().isNotEmpty()}
        val actual=runBlocking {graph.repository.decode(graph.db.dao().record(id)!!,pl.tripsync.domain.ExpenseData::class.java)}
        org.junit.Assert.assertEquals(expected,actual.shares)
    }
    @Test fun draftSurvivesActivityRecreation() {
        openTrip();compose.onNodeWithText("Koszty",useUnmergedTree=true).performClick();compose.onNodeWithText("Dodaj wydatek").performClick()
        compose.onNode(hasSetTextAction() and hasText("Nazwa")).performTextInput("Zachowany formularz")
        compose.activityRule.scenario.recreate()
        compose.waitUntil(10000) {compose.onAllNodesWithText("Zachowany formularz").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Zachowany formularz").assertIsDisplayed()
    }
    @Test fun blankFormShowsValidationError() {
        openTrip();compose.onNodeWithText("Koszty",useUnmergedTree=true).performClick();compose.onNodeWithText("Dodaj wydatek").performClick()
        compose.onNodeWithText("Zapisz").performScrollTo().performClick()
        compose.onNodeWithText("Nazwa jest wymagana (maks. 120 znaków).").assertIsDisplayed()
    }
    @Test fun votingCanBeAddedRemovedAndAddedAgain() {
        openTrip();compose.onNodeWithText("Pomysły",useUnmergedTree=true).performClick()
        compose.onNodeWithText("1 · Głosuj").performClick()
        compose.waitUntil(10000) {compose.onAllNodesWithText("2 · Cofnij").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("2 · Cofnij").performClick()
        compose.waitUntil(10000) {compose.onAllNodesWithText("1 · Głosuj").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("1 · Głosuj").performClick()
        compose.waitUntil(10000) {compose.onAllNodesWithText("2 · Cofnij").fetchSemanticsNodes().isNotEmpty()}
    }
    @Test fun persistedDataSurvivesActivityRestart() {
        openTrip();compose.activityRule.scenario.recreate()
        compose.waitUntil(10000) {compose.onAllNodesWithText("Weekend w Tatrach").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Weekend w Tatrach").assertIsDisplayed()
        runBlocking {check(graph.db.dao().records().count {it.kind=="expense"}==2)}
    }
}

package pl.tripsync

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pl.tripsync.domain.BudgetData
import java.io.File

@RunWith(AndroidJUnit4::class)
class FormRegressionTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val graph get() = (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as TripSyncApp).graph
    @Before fun prepare() {
        runBlocking { graph.repository.logout(); graph.repository.demo(); graph.prefs.setDark(false) }
        compose.waitUntil(15000) { compose.onAllNodesWithText("Twoje wyjazdy").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Weekend w Tatrach").performScrollTo().performClick()
    }
    private fun openBudget() {
        compose.onNodeWithText("Koszty", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Zobacz lub zmień budżet").performScrollTo().performClick()
    }
    private fun budget() = runBlocking { graph.repository.decode(graph.db.dao().record("budget:demo-tatry")!!, BudgetData::class.java) }
    private fun screenshot(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.waitForIdle(200, 3000)
        val dir = File(instrumentation.targetContext.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        File(dir, "$name.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
    @Test fun budgetDraftCanBeKeptOrDiscardedWithoutChangingSavedData() {
        openBudget()
        compose.onNode(hasSetTextAction() and hasText("Łączny budżet")).performTextReplacement("2400")
        compose.onNodeWithContentDescription("Wstecz").performClick()
        compose.onNodeWithText("Odrzucić zmiany?").assertIsDisplayed()
        screenshot("19-ochrona-formularza")
        compose.onNodeWithText("Zostań").performClick()
        compose.onNode(hasSetTextAction() and hasText("Łączny budżet")).assertTextContains("2400")
        compose.onNodeWithContentDescription("Wstecz").performClick()
        compose.onNodeWithText("Odrzuć zmiany").performClick()
        compose.onNodeWithText("Wspólne koszty").performScrollTo().assertIsDisplayed()
        assertEquals(180000L, budget().limit)
    }
    @Test fun placeDraftSurvivesCancelledSystemBack() {
        compose.onNodeWithText("Mapa miejsc i pogoda").performScrollTo().performClick()
        compose.onNodeWithText("Dodaj miejsce ręcznie").performScrollTo().performClick()
        compose.onNode(hasSetTextAction() and hasText("Nazwa miejsca")).performTextInput("Niezapisany punkt")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.waitForIdle()
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText("Odrzucić zmiany?").assertIsDisplayed()
        compose.onNodeWithText("Zostań").performClick()
        compose.onNodeWithText("Niezapisany punkt").assertIsDisplayed()
        assertFalse(runBlocking { graph.db.dao().records().any { it.kind == "place" && it.json.contains("Niezapisany punkt") } })
    }
    @Test fun repaymentDraftRequiresConfirmationBeforeDiscarding() {
        compose.onNodeWithText("Koszty", useUnmergedTree = true).performClick()
        compose.onNode(hasText("Zapisz spłatę") and hasClickAction()).performScrollTo().performClick()
        compose.onNode(hasSetTextAction() and hasText("Notatka")).performScrollTo().performTextInput("Niezapisana spłata")
        compose.onNodeWithContentDescription("Wstecz").performClick()
        compose.onNodeWithText("Odrzucić zmiany?").assertIsDisplayed()
        compose.onNodeWithText("Odrzuć zmiany").performClick()
        compose.onNodeWithText("Wspólne koszty").performScrollTo().assertIsDisplayed()
        assertFalse(runBlocking { graph.db.dao().records().any { it.kind == "settlement" && !it.deleted } })
    }
    @Test fun budgetDeletionRequiresConfirmationAndLeavesExpenses() {
        openBudget()
        compose.onNodeWithText("Usuń budżet").performScrollTo().performClick()
        compose.onNodeWithText("Usunąć budżet?").assertIsDisplayed()
        assertFalse(runBlocking { graph.db.dao().record("budget:demo-tatry")!!.deleted })
        screenshot("20-usuwanie-budzetu")
        compose.onNodeWithText("Anuluj").performClick()
        compose.onNodeWithText("Usuń budżet").performScrollTo().performClick()
        compose.onNodeWithText("Usuń", useUnmergedTree = true).performClick()
        compose.waitUntil(10000) { runBlocking { graph.db.dao().record("budget:demo-tatry")!!.deleted } }
        assertEquals(2, runBlocking { graph.db.dao().records().count { it.kind == "expense" && !it.deleted } })
    }
    @Test fun zeroCategoryLimitCanBeSavedFromForm() {
        openBudget()
        compose.onNode(hasSetTextAction() and hasText("Limit: Jedzenie")).performScrollTo().performTextReplacement("0,00")
        compose.onNodeWithText("Zapisz budżet").performScrollTo().performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("Wspólne koszty").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(0L, budget().categoryLimits["food"])
    }
}

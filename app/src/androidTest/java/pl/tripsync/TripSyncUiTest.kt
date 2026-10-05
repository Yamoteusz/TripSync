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
        instrumentation.uiAutomation.waitForIdle(200,3000)
        val dir=File(instrumentation.targetContext.getExternalFilesDir(null),"screenshots").apply {mkdirs()}
        val image=instrumentation.uiAutomation.takeScreenshot()
        File(dir,"$name.png").outputStream().use {image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
        image.recycle()
    }
    @Test fun dateAndTimeDialogsSaveIsoValues() {
        openTrip();compose.onNodeWithText("Plan",useUnmergedTree=true).performClick()
        compose.onNodeWithText("Dodaj punkt planu").performClick()
        compose.onNode(hasSetTextAction() and hasText("Nazwa")).performTextInput("Test kalendarza")
        compose.onNodeWithText("Data").performScrollTo().performClick()
        val tripData=runBlocking {graph.repository.decode(graph.db.dao().record("demo-tatry")!!,pl.tripsync.domain.TripData::class.java)}
        val start=java.time.LocalDate.parse(tripData.startDate)
        val chosen=start.plusDays(1)
        val configuration=compose.activity.resources.configuration
        val compact=configuration.screenHeightDp<640 || configuration.screenWidthDp<360 || configuration.fontScale>1.2f
        if(compact) {
            androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom(android.widget.DatePicker::class.java)).perform(object:androidx.test.espresso.ViewAction {
                override fun getDescription()="Wybierz dzień w systemowym kalendarzu"
                override fun getConstraints()=androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom(android.widget.DatePicker::class.java)
                override fun perform(controller:androidx.test.espresso.UiController,view:android.view.View) { (view as android.widget.DatePicker).updateDate(chosen.year,chosen.monthValue-1,chosen.dayOfMonth);controller.loopMainThreadUntilIdle() }
            })
            screenshot("14-kalendarz")
            androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withText("Wybierz datę")).perform(androidx.test.espresso.action.ViewActions.click())
        } else {
            compose.onNodeWithText("Wybierz datę").assertIsDisplayed()
            val locale=configuration.locales[0]
            val format=java.text.SimpleDateFormat(android.text.format.DateFormat.getBestDateTimePattern(locale,"yMMMMEEEEd"),locale).apply {timeZone=java.util.TimeZone.getTimeZone("UTC")}
            if(chosen.month!=start.month) {
                val nextMonth=compose.activity.resources.getIdentifier("date_picker_switch_to_next_month","string",compose.activity.packageName)
                compose.onNodeWithContentDescription(compose.activity.getString(nextMonth)).performClick()
            }
            compose.onNodeWithText(format.format(java.util.Date(pl.tripsync.domain.Dates.millis(chosen.toString()))),substring=true).performScrollTo().performClick()
            screenshot("14-kalendarz")
            compose.onNodeWithText("Wybierz datę").performClick()
        }
        compose.onNodeWithText("Godzina").performScrollTo().performClick()
        compose.onNodeWithText("Wybierz godzinę").assertIsDisplayed()
        val context=compose.activity
        val hours=context.resources.getIdentifier("time_picker_hour_24h_suffix","string",context.packageName)
        val minutes=context.resources.getIdentifier("time_picker_minute_suffix","string",context.packageName)
        if(compact) {
            val hourInput=context.resources.getIdentifier("time_picker_hour_text_field","string",context.packageName)
            val minuteInput=context.resources.getIdentifier("time_picker_minute_text_field","string",context.packageName)
            compose.onNode(hasSetTextAction() and hasContentDescription(context.getString(hourInput))).performTextReplacement("14")
            compose.onNode(hasSetTextAction() and hasContentDescription(context.getString(minuteInput))).performTextReplacement("30")
        } else {
            compose.onNodeWithContentDescription(context.getString(hours,14)).performClick()
            compose.onNodeWithContentDescription(context.getString(minutes,30)).performClick()
        }
        screenshot("15-zegar")
        compose.onNodeWithText("Ustaw godzinę").performClick()
        compose.onNodeWithText("14:30").assertIsDisplayed()
        compose.onNodeWithText("Zapisz").performScrollTo().performClick()
        compose.waitUntil(10000) {runBlocking {graph.db.dao().records().any {it.json.contains("Test kalendarza")}}}
        val event=runBlocking {graph.repository.decode(graph.db.dao().records().first {it.kind=="event" && it.json.contains("Test kalendarza")},pl.tripsync.domain.EventData::class.java)}
        org.junit.Assert.assertEquals("14:30",event.time)
        org.junit.Assert.assertEquals(chosen.toString(),event.date)
    }
    @Test fun backRequiresConfirmationForUnsavedForm() {
        openTrip();compose.onNodeWithText("Koszty",useUnmergedTree=true).performClick();compose.onNodeWithText("Dodaj wydatek").performClick()
        compose.onNode(hasSetTextAction() and hasText("Nazwa")).performTextInput("Niezapisany wydatek")
        compose.onNodeWithContentDescription("Wstecz").performClick()
        compose.onNodeWithText("Odrzucić zmiany?").assertIsDisplayed()
        compose.onNodeWithText("Zostań").performClick()
        compose.onNodeWithText("Niezapisany wydatek").assertIsDisplayed()
    }
    @Test fun expenseSearchAndTabStateArePreserved() {
        openTrip();compose.onNodeWithText("Koszty",useUnmergedTree=true).performClick()
        compose.onNode(hasSetTextAction() and hasText("Szukaj wydatku")).performScrollTo().performTextInput("nie istnieje")
        compose.onNodeWithText("Brak pasujących wydatków").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Plan",useUnmergedTree=true).performClick()
        compose.onNodeWithText("Koszty",useUnmergedTree=true).performClick()
        compose.onNode(hasSetTextAction() and hasText("Szukaj wydatku")).performScrollTo().assertTextContains("nie istnieje")
    }
    @Test fun mainScreensAndRealScreenshots() {
        screenshot("01-wyjazdy")
        runBlocking {graph.prefs.setDark(true)}
        compose.waitForIdle()
        compose.runOnIdle {
            val bars=androidx.core.view.WindowCompat.getInsetsController(compose.activity.window,compose.activity.window.decorView)
            org.junit.Assert.assertFalse(bars.isAppearanceLightStatusBars)
            org.junit.Assert.assertFalse(bars.isAppearanceLightNavigationBars)
        }
        screenshot("16-ciemny")
        runBlocking {graph.prefs.setDark(false)}
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
    @Test fun joiningMemberDoesNotReassignDraftExpenseShares() {
        val expected=mapOf("demo-anna" to 100L,"demo-bartek" to 200L,"demo-celina" to 700L)
        val id=runBlocking {graph.repository.save("demo-tatry","expense",pl.tripsync.domain.ExpenseData("Podział przed dołączeniem",1000,"demo-anna",expected,"2026-12-31"))}
        openTrip();compose.onNodeWithText("Koszty",useUnmergedTree=true).performClick()
        compose.onAllNodesWithContentDescription("Edytuj wydatek").onFirst().performScrollTo().performClick()
        runBlocking {graph.repository.save("demo-tatry","member",pl.tripsync.domain.MemberData("demo-a-first","Aleks"),"demo-tatry:demo-a-first")}
        compose.waitUntil(10000) {compose.onAllNodesWithText("Aleks").fetchSemanticsNodes().isNotEmpty()}
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
    @Test fun budgetScreenUpdatesLimits() {
        openTrip();compose.onNodeWithText("Koszty",useUnmergedTree=true).performClick()
        compose.onNodeWithText("Zobacz lub zmień budżet").performScrollTo().performClick()
        screenshot("08-budzet")
        compose.onNode(hasSetTextAction() and hasText("Łączny budżet")).performTextClearance()
        compose.onNode(hasSetTextAction() and hasText("Łączny budżet")).performTextInput("2500")
        compose.onNodeWithText("Zapisz budżet").performScrollTo().performClick()
        compose.waitUntil(10000) {compose.onAllNodesWithText("Wspólne koszty").fetchSemanticsNodes().isNotEmpty()}
        val data=runBlocking {graph.repository.decode(graph.db.dao().record("budget:demo-tatry")!!,pl.tripsync.domain.BudgetData::class.java)}
        org.junit.Assert.assertEquals(250000L,data.limit)
        org.junit.Assert.assertEquals(90000L,data.categoryLimits["accommodation"])
    }
    @Test fun repaymentFormChangesBalancesAndCanBeDeleted() {
        openTrip();compose.onNodeWithText("Koszty",useUnmergedTree=true).performClick()
        compose.onNode(hasText("Zapisz spłatę") and hasClickAction()).performScrollTo().performClick()
        compose.onNodeWithText("Wybierz").performClick()
        compose.onNodeWithText("Bartek").performClick()
        compose.onNode(hasSetTextAction() and hasText("Kwota (PLN)")).performTextInput("50")
        screenshot("09-splata")
        compose.onNode(hasText("Zapisz spłatę") and hasClickAction()).performScrollTo().performClick()
        compose.waitUntil(10000) {runBlocking {graph.db.dao().records().any {it.kind=="settlement"}}}
        val payment=runBlocking {graph.db.dao().records().first {it.kind=="settlement"}}
        val data=graph.repository.decode(payment,pl.tripsync.domain.SettlementData::class.java)
        org.junit.Assert.assertEquals(5000L,data.amount)
        org.junit.Assert.assertEquals("demo-anna",data.from)
        compose.onNodeWithText("Edytuj spłatę").performScrollTo().performClick()
        compose.onNodeWithText("Usuń spłatę").performScrollTo().performClick()
        compose.onNodeWithText("Usuń").performClick()
        compose.waitUntil(10000) {runBlocking {graph.db.dao().record(payment.id)?.deleted==true}}
    }
    @Test fun placeFormAndHistoryArePersisted() {
        openTrip();compose.onNodeWithText("Mapa miejsc i pogoda").performScrollTo().performClick()
        val mapHeight=java.util.concurrent.atomic.AtomicInteger(0)
        fun webView(view:android.view.View):android.webkit.WebView? {
            if(view is android.webkit.WebView) return view
            if(view is android.view.ViewGroup) for(i in 0 until view.childCount) webView(view.getChildAt(i))?.let {return it}
            return null
        }
        compose.waitUntil(10000) {
            compose.activity.runOnUiThread {webView(compose.activity.window.decorView)?.evaluateJavascript("document.getElementById('map') ? document.getElementById('map').clientHeight : 0") {value -> mapHeight.set(value.toIntOrNull() ?: 0)}}
            mapHeight.get() >= 320
        }
        screenshot("10-mapa")
        compose.onNodeWithText("Dodaj miejsce ręcznie").performScrollTo().performClick()
        compose.onNode(hasSetTextAction() and hasText("Nazwa miejsca")).performTextInput("Punkt testowy")
        compose.onNode(hasSetTextAction() and hasText("Szerokość geograficzna")).performTextInput("49.30")
        compose.onNode(hasSetTextAction() and hasText("Długość geograficzna")).performTextInput("19.95")
        compose.onNodeWithText("Zapisz miejsce").performScrollTo().performClick()
        compose.waitUntil(10000) {runBlocking {graph.db.dao().records().any {it.kind=="place" && it.json.contains("Punkt testowy")}}}
        compose.onNodeWithContentDescription("Wstecz").performClick()
        compose.onNodeWithText("Historia zmian").performScrollTo().performClick()
        compose.onNodeWithText("Miejsce · Punkt testowy").assertIsDisplayed()
        screenshot("11-historia")
    }

}

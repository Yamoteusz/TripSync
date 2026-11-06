package pl.tripsync

import org.junit.Assert.*
import org.junit.Test
import pl.tripsync.domain.*

class ExtendedMoneyTest {
    private val expense=ExpenseData("Hotel",30000,"anna",mapOf("anna" to 10000L,"bartek" to 10000L,"celina" to 10000L))
    @Test fun repaymentReducesDebtAndPreservesZeroSum() {
        val payments=listOf(SettlementData("bartek","anna",7000))
        val balances=Money.balances(listOf(expense),payments)
        assertEquals(13000L,balances["anna"])
        assertEquals(-3000L,balances["bartek"])
        assertEquals(-10000L,balances["celina"])
        assertEquals(0L,balances.values.sum())
        assertEquals(13000L,Money.settle(listOf(expense),payments).sumOf {it.amount})
    }
    @Test fun allRepaymentsRemoveSuggestedTransfers() {
        assertTrue(Money.settle(listOf(expense),listOf(SettlementData("bartek","anna",10000),SettlementData("celina","anna",10000))).isEmpty())
    }
    @Test fun overpaymentCreatesReverseDebt() {
        val balances=Money.balances(listOf(expense),listOf(SettlementData("bartek","anna",15000)))
        assertEquals(5000L,balances["bartek"])
        assertEquals(0L,balances.values.sum())
    }
    @Test(expected=IllegalArgumentException::class) fun selfPaymentRejected() {Money.balances(emptyList(),listOf(SettlementData("a","a",100)))}
    @Test fun legacyExpenseUsesOtherCategory() {
        val e=com.google.gson.Gson().fromJson("""{"title":"Hotel","amount":123,"payer":"a","shares":{"a":123},"date":"2026-10-04"}""",ExpenseData::class.java)
        assertEquals(123L,Categories.totals(listOf(e))["other"])
    }
    @Test fun budgetValidatesTotalOfCategories() {
        Validation.budget(BudgetData(10000,mapOf("food" to 4000,"transport" to 6000)))
        assertThrows(IllegalArgumentException::class.java) {Validation.budget(BudgetData(10000,mapOf("food" to 8000,"transport" to 8000)))}
        assertThrows(IllegalArgumentException::class.java) {Validation.budget(BudgetData(10000,mapOf("unknown" to 1)))}
    }
    @Test fun coordinatesRejectNanAndOutOfRange() {
        Validation.place(PlaceData("Szlak",49.2,20.1))
        assertThrows(IllegalArgumentException::class.java) {Validation.place(PlaceData("Szlak",Double.NaN,20.1))}
        assertThrows(IllegalArgumentException::class.java) {Validation.place(PlaceData("Szlak",49.2,181.0))}
    }
    @Test fun zeroCategoryLimitsAreValidButExpensesRemainPositive() {
        assertEquals(0L, Money.parse("0", allowZero = true))
        assertEquals(0L, Money.parse("0,00", allowZero = true))
        Validation.budget(BudgetData(10000, mapOf("food" to Money.parse("0.00", allowZero = true))))
        assertThrows(IllegalArgumentException::class.java) { Money.parse("0") }
    }
    @Test fun categoryAmountsRejectNegativeValuesOverflowAndExcessPrecision() {
        for (value in listOf("-1", "0.001", "1000000.01", "999999999999999999999999")) {
            assertThrows(IllegalArgumentException::class.java) { Money.parse(value, allowZero = true) }
        }
        assertEquals(100_000_000L, Money.parse("1000000", allowZero = true))
    }
}

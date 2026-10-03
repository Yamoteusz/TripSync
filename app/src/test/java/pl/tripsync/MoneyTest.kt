package pl.tripsync

import org.junit.Assert.*
import org.junit.Test
import pl.tripsync.domain.*
import kotlin.random.Random

class MoneyTest {
    @Test fun decimalParsingIsExact() {
        assertEquals(12345L, Money.parse("123,45"))
        assertEquals(10L, Money.parse("0.10"))
        assertEquals(100_000_000L, Money.parse("1000000.00"))
    }
    @Test fun rejectsUnsafeAmounts() {
        listOf("0","-1","1.001","NaN","1e9","1000000.01","999999999999999999999999999").forEach {input -> assertThrows(IllegalArgumentException::class.java) {Money.parse(input)}}
    }
    @Test fun distributesRemainderWithoutLosingCents() {
        assertEquals(mapOf("a" to 34L,"b" to 34L,"c" to 33L),Money.equalSplit(101,listOf("c","b","a")))
        assertEquals(101L,Money.equalSplit(101,listOf("a","b","c")).values.sum())
    }
    @Test fun exampleTripHasCorrectBalancesAndTransfers() {
        val people=listOf("anna","bartek","celina")
        val expenses=listOf(ExpenseData(amount=72000,payer="anna",shares=Money.equalSplit(72000,people)),ExpenseData(amount=18000,payer="bartek",shares=Money.equalSplit(18000,people)))
        assertEquals(mapOf("anna" to 42000L,"bartek" to -12000L,"celina" to -30000L),Money.balances(expenses))
        assertEquals(listOf(Transfer("bartek","anna",12000),Transfer("celina","anna",30000)),Money.settle(expenses))
    }
    @Test fun randomSettlementsConserveMoneyAndClearBalances() {
        val random=Random(42)
        repeat(200) {
            val people=(1..random.nextInt(2,15)).map {"p$it"}
            val expenses=(1..30).map {val amount=random.nextLong(1,10000000);ExpenseData(amount=amount,payer=people.random(random),shares=Money.equalSplit(amount,people.shuffled(random).take(random.nextInt(1,people.size+1))))}
            val balances=Money.balances(expenses).toMutableMap()
            assertEquals(0L,balances.values.sum())
            val transfers=Money.settle(expenses)
            assertTrue(transfers.size <= people.size-1)
            transfers.forEach {assertTrue(it.amount>0);balances[it.from]=balances.getValue(it.from)+it.amount;balances[it.to]=balances.getValue(it.to)-it.amount}
            assertTrue(balances.values.all {it==0L})
        }
    }
    @Test fun rejectsInvalidShares() {
        assertThrows(IllegalArgumentException::class.java) {Money.balances(listOf(ExpenseData(amount=100,payer="a",shares=mapOf("a" to 99))))}
        assertThrows(IllegalArgumentException::class.java) {Money.equalSplit(100,emptyList())}
        assertThrows(IllegalArgumentException::class.java) {Money.equalSplit(100,listOf("a","a"))}
    }
}

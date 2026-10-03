package pl.tripsync.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalTime

data class TripData(val title: String = "", val destination: String = "", val startDate: String = LocalDate.now().toString(), val endDate: String = LocalDate.now().plusDays(3).toString(), val currency: String = "PLN", val description: String = "")
data class MemberData(val userId: String = "", val name: String = "", val role: String = "participant")
data class EventData(val title: String = "", val date: String = LocalDate.now().toString(), val time: String = "10:00", val place: String = "", val description: String = "", val done: Boolean = false, val rank: Int = 0, val reminder: Boolean = false)
data class TaskData(val title: String = "", val category: String = "task", val assignee: String = "", val done: Boolean = false, val date: String = LocalDate.now().toString(), val time: String = "09:00", val reminder: Boolean = false)
data class ProposalData(val title: String = "", val place: String = "", val description: String = "")
data class VoteData(val proposalId: String = "", val userId: String = "")
data class ExpenseData(val title: String = "", val amount: Long = 0, val payer: String = "", val shares: Map<String, Long> = emptyMap(), val date: String = LocalDate.now().toString())
data class User(val id: String = "", val name: String = "", val email: String = "")
data class Session(val user: User = User(), val token: String = "", val demo: Boolean = false)
data class Transfer(val from: String, val to: String, val amount: Long)

object Money {
    fun parse(value: String): Long {
        val cleaned = value.trim().replace(',', '.')
        require(Regex("[0-9]+(\\.[0-9]{1,2})?").matches(cleaned)) { "Podaj kwotę z maksymalnie dwoma miejscami po przecinku." }
        val amount = try { BigDecimal(cleaned).movePointRight(2).longValueExact() } catch (_: ArithmeticException) { throw IllegalArgumentException("Kwota jest zbyt duża.") }
        require(amount in 1..100_000_000L) { "Kwota musi być dodatnia i nie większa niż 1 000 000." }
        return amount
    }
    fun format(amount: Long, currency: String = "PLN") = BigDecimal.valueOf(amount, 2).setScale(2, RoundingMode.UNNECESSARY).toPlainString().replace('.', ',') + " " + currency
    fun equalSplit(amount: Long, participants: List<String>): Map<String, Long> {
        require(amount > 0 && participants.isNotEmpty() && participants.distinct().size == participants.size) { "Wybierz uczestników kosztu." }
        val ordered = participants.sorted()
        return ordered.mapIndexed { i, id -> id to amount / ordered.size + if (i < amount % ordered.size) 1 else 0 }.toMap()
    }
    fun balances(expenses: List<ExpenseData>): Map<String, Long> {
        val result = sortedMapOf<String, Long>()
        expenses.forEach { e ->
            require(e.amount > 0 && e.shares.isNotEmpty() && e.shares.values.all { it >= 0 } && e.shares.values.sum() == e.amount) { "Nieprawidłowy podział wydatku." }
            result[e.payer] = Math.addExact(result[e.payer] ?: 0, e.amount)
            e.shares.forEach { (id, amount) -> result[id] = Math.subtractExact(result[id] ?: 0, amount) }
        }
        return result
    }
    fun settle(expenses: List<ExpenseData>): List<Transfer> {
        val balances = balances(expenses)
        val debtors = balances.filterValues { it < 0 }.map { it.key to -it.value }.toMutableList()
        val creditors = balances.filterValues { it > 0 }.toList().toMutableList()
        val result = mutableListOf<Transfer>()
        var d = 0; var c = 0
        while (d < debtors.size && c < creditors.size) {
            val amount = minOf(debtors[d].second, creditors[c].second)
            result += Transfer(debtors[d].first, creditors[c].first, amount)
            debtors[d] = debtors[d].first to debtors[d].second - amount
            creditors[c] = creditors[c].first to creditors[c].second - amount
            if (debtors[d].second == 0L) d++
            if (creditors[c].second == 0L) c++
        }
        return result
    }
}

object Validation {
    fun title(value: String) { require(value.isNotBlank() && value.length <= 120) { "Nazwa jest wymagana (maks. 120 znaków)." } }
    fun date(value: String): LocalDate = try { LocalDate.parse(value) } catch (e: Exception) { throw IllegalArgumentException("Data musi mieć format RRRR-MM-DD.") }
    fun time(value: String): LocalTime = try { require(Regex("[0-2][0-9]:[0-5][0-9]").matches(value)); LocalTime.parse(value) } catch (e: Exception) { throw IllegalArgumentException("Godzina musi mieć format GG:MM.") }
    fun trip(t: TripData) {
        title(t.title); title(t.destination)
        require(date(t.startDate) <= date(t.endDate)) { "Koniec wyjazdu nie może poprzedzać początku." }
        require(t.currency in listOf("PLN", "EUR", "USD")) { "Wybierz obsługiwaną walutę." }
        require(t.description.length <= 2000) { "Opis może mieć maks. 2000 znaków." }
    }
    fun event(e: EventData, t: TripData) {
        title(e.title); time(e.time)
        require(date(e.date) in date(t.startDate)..date(t.endDate)) { "Data punktu musi należeć do terminu wyjazdu." }
        require(e.place.length <= 240 && e.description.length <= 2000) { "Miejsce lub opis są zbyt długie." }
    }
}

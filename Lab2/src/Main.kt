package lab02

import java.util.Locale

enum class Tariff(val pricePerMinute: Double) {
    START(3.0),
    STANDARD(2.0),
    PREMIUM(1.0)
}

enum class AccountState {
    ACTIVE,
    BLOCKED
}

data class Account(
    val accountNumber: String,
    var balance: Double,
    var state: AccountState = AccountState.ACTIVE
)

data class Subscriber(
    val id: Int,
    val name: String,
    val phoneNumber: String,
    val email: String?,
    val tariff: Tariff,
    val account: Account
)

enum class BillingStatus {
    SUCCESS,
    REJECTED_INSUFFICIENT_FUNDS,
    REJECTED_BLOCKED
}

data class BillingResult(
    val durationSeconds: Int,
    val chargedMinutes: Int,
    val callCost: Double,
    val balanceBefore: Double,
    val balanceAfter: Double,
    val status: BillingStatus
)

fun getEmailDisplay(subscriber: Subscriber): String = subscriber.email ?: "не указан"

fun calculateChargedMinutes(durationSeconds: Int): Int {
    require(durationSeconds >= 0) { "продолжительность не может быть отрицательной" }
    return durationSeconds / 60 + if (durationSeconds % 60 == 0) 0 else 1
}

fun calculateCallCost(durationMinutes: Int, pricePerMinute: Double): Double {
    require(durationMinutes >= 0) { "продолжительность не может быть отрицательной" }
    require(pricePerMinute >= 0 && pricePerMinute.isFinite()) { "некорректная стоимость минуты" }
    return durationMinutes * pricePerMinute
}

fun billCall(subscriber: Subscriber, durationSeconds: Int): BillingResult {
    val chargedMinutes = calculateChargedMinutes(durationSeconds)
    val callCost = calculateCallCost(chargedMinutes, subscriber.tariff.pricePerMinute)
    val account = subscriber.account
    val balanceBefore = account.balance
    require(balanceBefore >= 0 && balanceBefore.isFinite()) { "некорректный баланс" }

    val status = when {
        account.state == AccountState.BLOCKED -> BillingStatus.REJECTED_BLOCKED
        balanceBefore < callCost -> BillingStatus.REJECTED_INSUFFICIENT_FUNDS
        else -> {
            account.balance -= callCost
            BillingStatus.SUCCESS
        }
    }

    return BillingResult(
        durationSeconds,
        chargedMinutes,
        callCost,
        balanceBefore,
        account.balance,
        status
    )
}

private fun money(value: Double): String = String.format(Locale.US, "%.2f ₽", value)

private fun printBilling(subscriber: Subscriber, result: BillingResult) {
    println("Абонент: ${subscriber.name} (ID: ${subscriber.id})")
    println("Номер: ${subscriber.phoneNumber}")
    println("Email: ${getEmailDisplay(subscriber)}")
    println("Тариф: ${subscriber.tariff}")
    println("Стоимость минуты: ${money(subscriber.tariff.pricePerMinute)}")
    println("Продолжительность: ${result.durationSeconds} сек.")
    println("Тарифицируемая продолжительность: ${result.chargedMinutes} мин.")
    println("Стоимость вызова: ${money(result.callCost)}")
    println("Счёт: ${subscriber.account.accountNumber}")
    println("Баланс до: ${money(result.balanceBefore)}")
    println("Баланс после: ${money(result.balanceAfter)}")
    when (result.status) {
        BillingStatus.SUCCESS -> println("Статус: SUCCESS")
        BillingStatus.REJECTED_INSUFFICIENT_FUNDS -> {
            println("Тарификация отклонена. Недостаточно средств.")
            println("Статус: REJECTED")
        }
        BillingStatus.REJECTED_BLOCKED -> {
            println("Тарификация отклонена.")
            println("Лицевой счёт заблокирован.")
            println("Статус: REJECTED")
        }
    }
}

fun main() {
    val subscribers = listOf(
        Subscriber(
            1001, "Иван Петров", "+79991234567", "ivan@example.com",
            Tariff.STANDARD, Account("ACC-1001", 100.0)
        ),
        Subscriber(
            1002, "Анна Смирнова", "+79997654321", null,
            Tariff.PREMIUM, Account("ACC-1002", 250.0)
        )
    )

    println("========== TELECOM BILLING ==========")
    subscribers.forEachIndexed { index, subscriber ->
        if (index > 0) println("-------------------------------------")
        printBilling(subscriber, billCall(subscriber, 125))
    }
    println("=====================================")
}

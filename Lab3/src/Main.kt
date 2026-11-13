package lab03

import lab02.Account
import lab02.AccountState
import lab02.Subscriber
import lab02.Tariff
import lab02.calculateChargedMinutes
import java.math.BigDecimal
import java.util.Locale

sealed interface UsageEvent {
    val subscriberId: Int
}

data class CallEvent(override val subscriberId: Int, val durationSeconds: Int) : UsageEvent
data class SmsEvent(override val subscriberId: Int, val messageCount: Int) : UsageEvent
data class InternetEvent(override val subscriberId: Int, val megabytes: Int) : UsageEvent

enum class ProcessingStatus {
    SUCCESS, SUBSCRIBER_NOT_FOUND, INSUFFICIENT_FUNDS, BLOCKED, INVALID_EVENT
}

data class ProcessingResult(
    val status: ProcessingStatus,
    val cost: Double?,
    val balanceBefore: Double?,
    val balanceAfter: Double?,
    val message: String
)

fun calculateEventCost(event: UsageEvent): Double = when (event) {
    is CallEvent -> calculateChargedMinutes(event.durationSeconds) * 2.0
    is SmsEvent -> {
        require(event.messageCount >= 0) { "Количество SMS не может быть отрицательным" }
        event.messageCount.toDouble()
    }
    is InternetEvent -> {
        require(event.megabytes >= 0) { "Объём трафика не может быть отрицательным" }
        event.megabytes / 10.0
    }
}

fun processEvent(event: UsageEvent, subscribers: Map<Int, Subscriber>): ProcessingResult {
    val subscriber = subscribers[event.subscriberId]
        ?: return ProcessingResult(ProcessingStatus.SUBSCRIBER_NOT_FOUND, null, null, null, "Абонент не найден")
    val account = subscriber.account
    val balanceBefore = account.balance
    val cost = try {
        calculateEventCost(event)
    } catch (error: IllegalArgumentException) {
        return ProcessingResult(ProcessingStatus.INVALID_EVENT, null, balanceBefore, balanceBefore,
            error.message ?: "Некорректные данные события")
    }

    if (account.state == AccountState.BLOCKED) {
        return ProcessingResult(ProcessingStatus.BLOCKED, cost, balanceBefore, balanceBefore, "Лицевой счёт заблокирован")
    }
    if (balanceBefore < cost) {
        return ProcessingResult(ProcessingStatus.INSUFFICIENT_FUNDS, cost, balanceBefore, balanceBefore, "Недостаточно средств")
    }

    // Вычитание в десятичном виде сохраняет точность денежных остатков.
    account.balance = BigDecimal.valueOf(balanceBefore).subtract(BigDecimal.valueOf(cost)).toDouble()
    return ProcessingResult(ProcessingStatus.SUCCESS, cost, balanceBefore, account.balance, "Услуга оплачена")
}

private fun money(value: Double): String = String.format(Locale.US, "%.2f ₽", value)

private fun demonstrate(event: UsageEvent, subscribers: Map<Int, Subscriber>) {
    val description = when (event) {
        is CallEvent -> "Звонок: ${event.durationSeconds} сек."
        is SmsEvent -> "SMS: ${event.messageCount} шт."
        is InternetEvent -> "Интернет: ${event.megabytes} MB"
    }
    println("\n$description; ID абонента: ${event.subscriberId}")
    subscribers[event.subscriberId]?.let { println("Абонент: ${it.name}; тариф: ${it.tariff}") }
    val result = processEvent(event, subscribers)
    result.cost?.let { println("Стоимость: ${money(it)}") }
    result.balanceBefore?.let { println("Баланс до: ${money(it)}") }
    result.balanceAfter?.let { println("Баланс после: ${money(it)}") }
    println("Результат: ${result.status}. ${result.message}")
}

fun main() {
    val subscribers = listOf(
        Subscriber(1001, "Иван Петров", "+79991234567", "ivan@example.com",
            Tariff.STANDARD, Account("ACC-1001", 100.0)),
        Subscriber(1002, "Анна Смирнова", "+79997654321", null,
            Tariff.PREMIUM, Account("ACC-1002", 250.0))
    ).associateBy { it.id }

    println("========== ЛАБОРАТОРНАЯ №3 ==========")
    println("Контрольный сценарий: события обрабатываются последовательно")
    val events: List<UsageEvent> = listOf(CallEvent(1001, 125), SmsEvent(1001, 1), InternetEvent(1002, 750))
    events.forEach { demonstrate(it, subscribers) }
    println("\nИтоговые балансы контрольного сценария:")
    subscribers.values.forEach { println("${it.name}: ${money(it.account.balance)}") }

    println("\nДополнительные проверки")
    demonstrate(SmsEvent(9999, 1), subscribers)
    demonstrate(InternetEvent(1001, 1000), subscribers)
    demonstrate(CallEvent(1001, 60), subscribers)
    demonstrate(CallEvent(1001, 61), subscribers)
    demonstrate(CallEvent(1001, 0), subscribers)
    demonstrate(SmsEvent(1001, -1), subscribers)
    println("\nПроверка точного списания 0,10 ₽ за 1 MB")
    demonstrate(InternetEvent(1002, 1), subscribers)
    subscribers.getValue(1002).account.state = AccountState.BLOCKED
    demonstrate(SmsEvent(1002, 1), subscribers)
    println("=====================================")
}

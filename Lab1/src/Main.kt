import java.util.Locale

fun getPricePerMinute(tariff: String): Double = when (tariff) {
    "START" -> 3.0
    "STANDARD" -> 2.0
    "PREMIUM" -> 1.0
    else -> throw IllegalArgumentException("неизвестный тариф $tariff")
}

fun calculateChargedMinutes(durationSeconds: Int): Int {
    require(durationSeconds >= 0) { "продолжительность не может быть отрицательной" }
    return durationSeconds / 60 + if (durationSeconds % 60 == 0) 0 else 1
}

fun calculateCallCost(
    durationMinutes: Int,
    pricePerMinute: Double,
    discountPercent: Int = 0
): Double {
    require(durationMinutes >= 0) { "продолжительность не может быть отрицательной" }
    require(pricePerMinute >= 0 && pricePerMinute.isFinite()) { "некорректная стоимость минуты" }
    require(discountPercent in 0..100) { "скидка должна быть от 0 до 100%" }
    return durationMinutes * pricePerMinute * (100 - discountPercent) / 100
}

private fun money(value: Double): String = String.format(Locale.US, "%.2f ₽", value)

fun main(args: Array<String>) {
    val phoneNumber = "+79991234567"
    val tariff: String
    var balance: Double
    val durationSeconds: Int
    val discountPercent: Int

    try {
        require(args.size == 0 || args.size == 3 || args.size == 4) {
            "аргументы: <тариф> <баланс> <секунды> [скидка_%]"
        }
        tariff = args.getOrNull(0) ?: "STANDARD"
        balance = args.getOrNull(1)?.toDouble() ?: 100.0
        durationSeconds = args.getOrNull(2)?.toInt() ?: 125
        discountPercent = args.getOrNull(3)?.toInt() ?: 0
        require(balance >= 0 && balance.isFinite()) { "некорректный баланс" }

        val pricePerMinute = getPricePerMinute(tariff)
        val chargedMinutes = calculateChargedMinutes(durationSeconds)
        val callCost = calculateCallCost(chargedMinutes, pricePerMinute, discountPercent)

        println("========== TELECOM BILLING ==========")
        println("Абонент: $phoneNumber")
        println("Тариф: $tariff")
        println("Продолжительность вызова: $durationSeconds сек.")
        println("Тарифицируемая продолжительность: $chargedMinutes мин.")
        println("Стоимость минуты: ${money(pricePerMinute)}")
        println("Скидка: $discountPercent%")
        println("Стоимость вызова: ${money(callCost)}")
        println("Баланс до вызова: ${money(balance)}")

        if (balance >= callCost) {
            balance -= callCost
            println("Баланс после вызова: ${money(balance)}")
            println("Статус: SUCCESS")
        } else {
            println("Недостаточно средств")
            println("Необходимо: ${money(callCost)}")
            println("Доступно: ${money(balance)}")
            println("Баланс после вызова: ${money(balance)}")
            println("Статус: REJECTED")
        }
        println("=====================================")
    } catch (error: IllegalArgumentException) {
        println("Ошибка тарификации: ${error.message}")
    }
}

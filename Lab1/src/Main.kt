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

private fun runCall(tariff: String, initialBalance: Double, durationSeconds: Int, discountPercent: Int = 0) {
    val phoneNumber = "+79991234567"
    var balance = initialBalance

    try {
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
        if (discountPercent > 0) {
            println("Стоимость без скидки: ${money(calculateCallCost(chargedMinutes, pricePerMinute))}")
        }
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

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Самостоятельная проверка")
        println("1. STANDARD, 100 ₽, 125 сек. — списание 6 ₽")
        runCall("STANDARD", 100.0, 125)
        println("\n2. START, 100 ₽, 60 сек. — списание 3 ₽")
        runCall("START", 100.0, 60)
        println("\n3. PREMIUM, 10 ₽, 121 сек. — списание 3 ₽")
        runCall("PREMIUM", 10.0, 121)
        println("\n4. STANDARD, 1 ₽, 61 сек. — недостаточно средств")
        runCall("STANDARD", 1.0, 61)
        println("\n5. STANDARD, 100 ₽, 0 сек. — стоимость 0 ₽")
        runCall("STANDARD", 100.0, 0)
        println("\n6. UNKNOWN, 100 ₽, 60 сек. — ошибка тарифа")
        runCall("UNKNOWN", 100.0, 60)
        println("\nДополнительное задание: STANDARD, 10 мин., скидка 20% — стоимость 16 ₽")
        runCall("STANDARD", 100.0, 600, 20)
        println("\nПроверка некорректной скидки: 120%")
        runCall("STANDARD", 100.0, 60, 120)
        return
    }

    try {
        require(args.size == 3 || args.size == 4) {
            "аргументы: <тариф> <баланс> <секунды> [скидка_%]"
        }
        runCall(args[0], args[1].toDouble(), args[2].toInt(), args.getOrNull(3)?.toInt() ?: 0)
    } catch (error: IllegalArgumentException) {
        println("Ошибка тарификации: ${error.message}")
    }
}

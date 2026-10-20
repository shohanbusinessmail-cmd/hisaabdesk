package com.shohankhan.hisaabdesk.core.formatting

/**
 * Money is stored as integer minor units (paisa): 1 BDT = 100 paisa.
 * All arithmetic happens on [Long] values — never on binary floating point.
 */
object Money {
    const val PAISA_PER_TAKA = 100L
    const val SYMBOL = "\u09F3" // ৳

    /** Long upper bound guard so totals cannot silently overflow. */
    const val MAX_ABS_PAISA = 1_000_000_000_000_000L // 10,000 crore taka

    fun takaToPaisa(taka: Long): Long = safeMul(taka, PAISA_PER_TAKA)

    fun paisaToTakaDouble(paisa: Long): Double = paisa / PAISA_PER_TAKA.toDouble()

    /** Checked addition. */
    fun safeAdd(a: Long, b: Long): Long {
        val r = Math.addExact(a, b)
        require(kotlin.math.abs(r) <= MAX_ABS_PAISA) { "amount out of bounds" }
        return r
    }

    fun safeSub(a: Long, b: Long): Long {
        val r = Math.subtractExact(a, b)
        require(kotlin.math.abs(r) <= MAX_ABS_PAISA) { "amount out of bounds" }
        return r
    }

    fun safeMul(a: Long, b: Long): Long {
        val r = Math.multiplyExact(a, b)
        require(kotlin.math.abs(r) <= MAX_ABS_PAISA) { "amount out of bounds" }
        return r
    }

    /**
     * Multiply paisa by a percent with exact half-up rounding.
     * result = amount * percent / 100
     */
    fun percentOf(amountPaisa: Long, percent: Double): Long {
        require(percent.isFinite() && percent >= 0) { "invalid percent" }
        val result = java.math.BigDecimal(amountPaisa)
            .multiply(java.math.BigDecimal(percent))
            .divide(java.math.BigDecimal(100), 0, java.math.RoundingMode.HALF_UP)
        return result.longValueExact()
    }

    enum class DigitStyle { BENGALI, ENGLISH }
    enum class Grouping { SOUTH_ASIAN, WESTERN }

    private val BENGALI_DIGITS = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

    fun toBengaliDigits(text: String): String {
        val sb = StringBuilder(text.length)
        for (ch in text) {
            if (ch in '0'..'9') sb.append(BENGALI_DIGITS[ch - '0']) else sb.append(ch)
        }
        return sb.toString()
    }

    fun toEnglishDigits(text: String): String {
        val sb = StringBuilder(text.length)
        for (ch in text) {
            val idx = BENGALI_DIGITS.indexOf(ch)
            if (idx >= 0) sb.append(('0' + idx)) else sb.append(ch)
        }
        return sb.toString()
    }

    fun normalizeDigits(text: String): String = toEnglishDigits(text)

    /** Format integer part with grouping. */
    fun groupDigits(intPart: String, grouping: Grouping): String {
        if (intPart.length <= 3) return intPart
        return when (grouping) {
            Grouping.WESTERN -> {
                val sb = StringBuilder()
                var count = 0
                for (i in intPart.indices.reversed()) {
                    sb.append(intPart[i])
                    count++
                    if (count % 3 == 0 && i > 0) sb.append(',')
                }
                sb.reverse().toString()
            }
            Grouping.SOUTH_ASIAN -> {
                // last 3 digits, then groups of 2: 1250000 -> 12,50,000
                val last3 = intPart.takeLast(3)
                var rest = intPart.dropLast(3)
                val groups = ArrayDeque<String>()
                while (rest.length > 2) {
                    groups.addFirst(rest.takeLast(2))
                    rest = rest.dropLast(2)
                }
                if (rest.isNotEmpty()) groups.addFirst(rest)
                (groups + last3).joinToString(",")
            }
        }
    }

    /**
     * Formats a paisa amount for display, e.g. ৳১,২৫০.০০ or ৳12,50,000.00.
     */
    fun format(
        paisa: Long,
        digitStyle: DigitStyle = DigitStyle.BENGALI,
        grouping: Grouping = Grouping.SOUTH_ASIAN,
        withSymbol: Boolean = true,
    ): String {
        val negative = paisa < 0
        val abs = kotlin.math.abs(paisa)
        val takaPart = (abs / PAISA_PER_TAKA).toString()
        val poishaPart = (abs % PAISA_PER_TAKA).toString().padStart(2, '0')
        val grouped = groupDigits(takaPart, grouping)
        var body = "$grouped.$poishaPart"
        body = when (digitStyle) {
            DigitStyle.BENGALI -> toBengaliDigits(body)
            DigitStyle.ENGLISH -> body
        }
        val sign = if (negative) "-" else ""
        return if (withSymbol) "$sign$SYMBOL$body" else "$sign$body"
    }

    /**
     * Parses user input like "1,250.50", "৳১২৫০", "১,২৫০.৭৫" into paisa.
     * Returns null when the input is not a valid non-negative money value.
     */
    fun parsePaisa(raw: String): Long? {
        val text = normalizeDigits(raw.trim())
            .replace(SYMBOL, "")
            .replace(",", "")
            .replace(" ", "")
        if (text.isEmpty()) return null
        val regex = Regex("^-?\\d{1,15}(\\.\\d{1,2})?$")
        if (!regex.matches(text)) return null
        return try {
            val paisa = java.math.BigDecimal(text)
                .movePointRight(2)
                .setScale(0, java.math.RoundingMode.HALF_UP)
                .longValueExact()
            if (kotlin.math.abs(paisa) > MAX_ABS_PAISA) null else paisa
        } catch (e: Exception) {
            null
        }
    }
}

package au.edu.jcu.assessment.utilityapp.model

import java.util.Calendar

// The 3 answers a shop can have.
enum class OpenStatus(val label: String) {
    OPEN("Open now"),
    CLOSED("Closed now"),
    UNKNOWN("Hours unknown")
}

// Reads opening hours like "Mo-Fr 09:00-17:00; Sa,Su 09:00-12:00; PH off"
// and says if the shop is open right now.
// If it can't read the text, it says UNKNOWN instead of guessing.
object OpeningHours {

    private val DAYS = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")

    fun statusNow(text: String?): OpenStatus {
        // No hours, or open all the time
        if (text == null || text.isBlank()) return OpenStatus.UNKNOWN
        if (text.trim() == "24/7") return OpenStatus.OPEN

        // Today ("We") and the time now in minutes (14:30 -> 870)
        val now = Calendar.getInstance()
        val today = DAYS[(now.get(Calendar.DAY_OF_WEEK) + 5) % 7]
        val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        // Some shops use ", " between rules instead of ";":
        // "Mo-Fr 09:00-17:00, Sa 09:00-12:00" -> "Mo-Fr 09:00-17:00; Sa 09:00-12:00"
        var fixed: String = text
        for (day in DAYS + "PH") fixed = fixed.replace(", $day", "; $day")

        var result = OpenStatus.CLOSED   // if no rule is about today, the shop is closed

        // Check each rule: "Mo-Fr 09:00-17:00", "Sa,Su 09:00-12:00", "PH off"
        for (part in fixed.split(";")) {
            val rule = part.trim()
            if (rule.isEmpty() || rule.startsWith("PH")) continue   // skip empty and public holiday rules

            // Split the rule into days and hours.
            // "Mo-Fr 09:00-17:00" -> days "Mo-Fr", hours "09:00-17:00"
            // "09:00-17:00"       -> no days written, so it means every day
            var days = "Mo-Su"
            var hours = rule
            if (rule[0].isLetter()) {
                days = rule.substringBefore(" ")
                hours = rule.substringAfter(" ", "").trim()
            }

            val dayList = readDays(days)                  // "Mo-Fr" -> [Mo, Tu, We, Th, Fr]
            if (dayList.isEmpty()) return OpenStatus.UNKNOWN
            if (today !in dayList) continue               // rule is about other days

            if (hours == "off" || hours == "closed") {
                result = OpenStatus.CLOSED
                continue
            }

            // One or more opening times: "09:00-17:00" or "09:00-12:00, 13:00-17:00"
            result = OpenStatus.CLOSED
            for (time in hours.split(",")) {
                val open = toMinutes(time.trim().substringBefore("-"))   // "09:00" -> 540
                val close = toMinutes(time.trim().substringAfter("-"))   // "17:00" -> 1020
                if (open == -1 || close == -1 || close <= open) return OpenStatus.UNKNOWN
                if (nowMinutes >= open && nowMinutes < close) result = OpenStatus.OPEN
            }
        }
        return result
    }

    // "Mo-Fr" -> [Mo, Tu, We, Th, Fr]   "Sa,Su" -> [Sa, Su]   "Sa-Mo" -> [Sa, Su, Mo]
    // Can't read it (e.g. "Mon-Fri") -> empty list
    private fun readDays(text: String): List<String> {
        val result = mutableListOf<String>()
        for (piece in text.split(",")) {
            if (piece == "PH") continue
            val first = DAYS.indexOf(piece.substringBefore("-"))   // "Mo" -> 0
            val last = DAYS.indexOf(piece.substringAfter("-"))     // "Fr" -> 4
            if (first == -1 || last == -1) return emptyList()

            if (first <= last) {
                for (i in first..last) result.add(DAYS[i])          // Mo-Fr
            } else {
                for (i in first..6) result.add(DAYS[i])             // Sa-Mo: Sa, Su ...
                for (i in 0..last) result.add(DAYS[i])              // ... then Mo
            }
        }
        return result
    }

    // "09:30" -> 9 * 60 + 30 = 570.  Not a time -> -1
    private fun toMinutes(time: String): Int {
        if (!time.contains(":")) return -1
        val hour = time.substringBefore(":").toIntOrNull() ?: return -1
        val minute = time.substringAfter(":").toIntOrNull() ?: return -1
        return hour * 60 + minute
    }
}
package au.edu.jcu.assessment.utilityapp.model

import java.util.Calendar

enum class OpenStatus(val label: String) {
    OPEN("Open now"),
    CLOSED("Closed now"),
    UNKNOWN("Hours unknown")
}

// Reads OpenStreetMap opening hours, like "Mo-Fr 09:00-17:00; Sa 09:00-12:00; Su off",
// and says if the shop is open at a given day and time.
// Anything it can't read (months, sunrise, overnight hours...) gives UNKNOWN instead of a wrong guess.
object OpeningHours {

    private val DAYS = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")   // Monday = 0 ... Sunday = 6
    private val TIME = Regex("""(\d{1,2}):(\d\d)-(\d{1,2}):(\d\d)""")     //  "09:00-17:00"

    // Uses the phone's clock right now.
    fun statusNow(text: String?): OpenStatus {
        val now = Calendar.getInstance()
        val day = (now.get(Calendar.DAY_OF_WEEK) + 5) % 7   // because Calendar starts at Sunday = 1
        val minute = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        return status(text, day, minute)
    }

    // day: 0 = Monday ... 6 = Sunday. minute: minutes since midnight (14:30 = 870).
    fun status(text: String?, day: Int, minute: Int): OpenStatus {
        if (text.isNullOrBlank()) return OpenStatus.UNKNOWN
        if (text.trim() == "24/7") return OpenStatus.OPEN

        var result = OpenStatus.CLOSED   // if no rule mentions today, the shop is closed today

        // Read the rules one by one: "Mo-Fr 09:00-17:00", then "Sa 09:00-12:00", then "Su off"
        for (rule in text.split(";").map { it.trim() }) {
            if (rule.isEmpty() || rule.startsWith("PH")) continue   // skip public holiday rules

            // Split "Mo-Fr 09:00-17:00" into days and hours. A rule with no days means every day.
            val hasDays = rule[0].isLetter()
            val days = if (hasDays) readDays(rule.substringBefore(" ")) else (0..6).toList()
            val hours = if (hasDays) rule.substringAfter(" ", "") else rule

            if (days == null) return OpenStatus.UNKNOWN   // couldn't read the days
            if (day !in days) continue                     // this rule isn't about today

            // A later rule for today replaces an earlier one, e.g. "Mo-Sa 09:00-17:00; Sa off"
            result = if (hours == "off" || hours == "closed") OpenStatus.CLOSED
            else isOpenAt(hours, minute) ?: return OpenStatus.UNKNOWN
        }
        return result
    }

    // "Mo-Fr" -> [0,1,2,3,4]   "Mo,We" -> [0,2]   "Sa-Mo" -> [5,6,0]   Can't read it -> null
    private fun readDays(text: String): List<Int>? {
        val result = mutableListOf<Int>()
        for (piece in text.split(",")) {
            val first = DAYS.indexOf(piece.substringBefore("-"))
            val last = DAYS.indexOf(piece.substringAfter("-"))   // same as first when there's no "-"
            if (first < 0 || last < 0) return null

            var d = first
            result += d
            while (d != last) {
                d = (d + 1) % 7   // % 7 goes from Sunday (6) back to Monday (0), for ranges like "Sa-Mo"
                result += d
            }
        }
        return result
    }

    // "09:00-12:00,13:00-17:00" -> OPEN if `minute` is inside one of the times, else CLOSED. Can't read it -> null
    private fun isOpenAt(hours: String, minute: Int): OpenStatus? {
        for (range in hours.split(",")) {
            val match = TIME.matchEntire(range.trim()) ?: return null
            val (h1, m1, h2, m2) = match.destructured.toList().map { it.toInt() }
            val open = h1 * 60 + m1
            val close = h2 * 60 + m2
            if (close <= open) return null   // overnight hours like "22:00-02:00" aren't supported
            if (minute in open until close) return OpenStatus.OPEN
        }
        return OpenStatus.CLOSED
    }
}